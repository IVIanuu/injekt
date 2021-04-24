/*
 * Copyright 2021 Manuel Wrage
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

import androidx.compose.compiler.plugins.kotlin.*
import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.com.intellij.psi.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.callUtil.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.inline.InlineUtil.*
import org.jetbrains.kotlin.resolve.scopes.*

enum class CallContext {
    DEFAULT, COMPOSABLE, SUSPEND
}

fun CallContext.canCall(other: CallContext) =
    this == other || other == CallContext.DEFAULT

fun CallableDescriptor.callContext(
    bindingContext: BindingContext
): CallContext {
    if (this is ConstructorDescriptor) return CallContext.DEFAULT
    var node: PsiElement? = findPsi()
    if (node == null) return callContextOfThis
    loop@while (node != null) {
        when (node) {
            is KtFunctionLiteral -> {
                // keep going, as this is a "KtFunction", but we actually want the
                // KtLambdaExpression
            }
            is KtLambdaExpression -> {
                val descriptor = bindingContext[BindingContext.FUNCTION, node.functionLiteral]
                    ?: return callContextOfThis
                val arg = getArgumentDescriptor(node.functionLiteral, bindingContext)
                val inlined = arg != null &&
                        canBeInlineArgument(node.functionLiteral) &&
                        isInline(arg.containingDeclaration) &&
                        isInlineParameter(arg)
                if (!inlined)
                    return descriptor.callContextOfThis
            }
            is KtFunction -> {
                val descriptor = bindingContext[BindingContext.FUNCTION, node]
                return descriptor?.callContextOfThis ?: CallContext.DEFAULT
            }
            is KtProperty -> {
                val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, node] as? CallableDescriptor
                return descriptor?.callContextOfThis ?: CallContext.DEFAULT
            }
        }
        node = node.parent as? KtElement
    }

    return callContextOfThis
}

private fun getArgumentDescriptor(
    argument: KtFunction,
    bindingContext: BindingContext
): ValueParameterDescriptor? {
    val call = KtPsiUtil.getParentCallIfPresent(argument) ?: return null
    val resolvedCall = call.getResolvedCall(bindingContext) ?: return null
    val valueArgument = resolvedCall.call.getValueArgumentForExpression(argument) ?: return null
    val mapping = resolvedCall.getArgumentMapping(valueArgument) as? ArgumentMatch ?: return null
    return mapping.valueParameter
}

private val CallableDescriptor.callContextOfThis: CallContext
    get() = when {
        isSuspend -> CallContext.SUSPEND
        (hasAnnotation(InjektFqNames.Composable) ||
                (this is PropertyDescriptor &&
                        getter?.hasAnnotation(InjektFqNames.Composable) == true)) -> CallContext.COMPOSABLE
        else -> CallContext.DEFAULT
    }

val TypeRef.callContext: CallContext
    get() = when {
        classifier.fqName.asString()
            .startsWith("kotlin.coroutines.SuspendFunction") -> CallContext.SUSPEND
        isComposableType -> CallContext.COMPOSABLE
        else -> CallContext.DEFAULT
    }

private val composeCompilerInClasspath = try {
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
            } else it.callContext(bindingContext)
        }
        ?: CallContext.DEFAULT
}
