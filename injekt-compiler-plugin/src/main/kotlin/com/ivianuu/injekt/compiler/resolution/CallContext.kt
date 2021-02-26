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

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.descriptors.CallableDescriptor

enum class CallContext {
    DEFAULT, COMPOSABLE, SUSPEND
}

fun CallContext.canCall(other: CallContext) =
    this == other || other == CallContext.DEFAULT

val CallableDescriptor.callContext: CallContext
    get() = when {
        isSuspend -> CallContext.SUSPEND
        hasAnnotation(InjektFqNames.Composable) -> CallContext.COMPOSABLE
        else -> CallContext.DEFAULT
    }

val TypeRef.callContext: CallContext
    get() = when {
        fullyExpandedType.isComposable -> CallContext.COMPOSABLE
        classifier.fqName.asString()
            .startsWith("kotlin.coroutines.SuspendFunction") -> CallContext.SUSPEND
        else -> CallContext.DEFAULT
    }
