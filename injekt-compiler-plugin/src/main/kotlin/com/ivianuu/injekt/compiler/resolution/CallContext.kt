/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.resolution

import androidx.compose.compiler.plugins.kotlin.isComposableCallable
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope

enum class CallContext {
    DEFAULT, COMPOSABLE, SUSPEND
}

fun CallContext.canCall(other: CallContext) =
    this == other || other == CallContext.DEFAULT

val CallableDescriptor.callContext: CallContext
    get() = when {
        isSuspend -> CallContext.SUSPEND
        (hasAnnotation(InjektFqNames.Composable) ||
                (this is PropertyDescriptor &&
                        getter?.hasAnnotation(InjektFqNames.Composable) == true)) -> CallContext.COMPOSABLE
        else -> CallContext.DEFAULT
    }

val TypeRef.callContext: CallContext
    get() = when {
        fullyExpandedType.isMarkedComposable -> CallContext.COMPOSABLE
        classifier.fqName.asString()
            .startsWith("kotlin.coroutines.SuspendFunction") -> CallContext.SUSPEND
        else -> CallContext.DEFAULT
    }

private var composeCompilerInClasspath = try {
    Class.forName("androidx.compose.compiler.plugins.kotlin.analysis.ComposeWritableSlices")
    true
} catch (e: ClassNotFoundException) {
    false
}

fun HierarchicalScope.callContext(bindingContext: BindingContext): CallContext {
    return generateSequence(this) { it.parent }
        .filterIsInstance<LexicalScope>()
        .mapNotNull { it.ownerDescriptor as? FunctionDescriptor }
        .firstOrNull()
        ?.let {
            if (composeCompilerInClasspath && it.isComposableCallable(bindingContext)) {
                CallContext.COMPOSABLE
            } else it.callContext
        }
        ?: CallContext.DEFAULT
}
