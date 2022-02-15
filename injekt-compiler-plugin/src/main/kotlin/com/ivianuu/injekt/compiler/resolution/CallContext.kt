/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
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

enum class CallContext {
  DEFAULT, COMPOSABLE, SUSPEND
}

fun CallContext.canCall(other: CallContext) = this == other || other == CallContext.DEFAULT

fun CallableDescriptor.callContext(ctx: Context): CallContext {
  if (this is ConstructorDescriptor) return CallContext.DEFAULT

  if (this !is FunctionDescriptor && this !is PropertyDescriptor) return CallContext.DEFAULT

  if (ctx.trace == null) return callContextOfThis(ctx)

  return ctx.trace.getOrPut(InjektWritableSlices.CALL_CONTEXT, this) {
    if (composeCompilerInClasspath && isComposableCallable(ctx.trace.bindingContext))
      return@getOrPut CallContext.COMPOSABLE

    val initialNode = findPsi() ?: return@getOrPut callContextOfThis(ctx)

    var node: PsiElement? = initialNode
    if (node == null)
      return@getOrPut callContextOfThis(ctx)
    try {
      loop@ while (node != null) {
        when (node) {
          is KtFunctionLiteral -> {
            // keep going, as this is a "KtFunction", but we actually want the
            // KtLambdaExpression
          }
          is KtLambdaExpression -> {
            val descriptor = ctx.trace.bindingContext[BindingContext.FUNCTION, node.functionLiteral]
              ?: return@getOrPut callContextOfThis(ctx)
            val arg = getArgumentDescriptor(node.functionLiteral, ctx.trace.bindingContext)
            val inlined = arg != null &&
                canBeInlineArgument(node.functionLiteral) &&
                isInline(arg.containingDeclaration) &&
                isInlineParameter(arg)
            if (!inlined)
              return@getOrPut descriptor.callContextOfThis(ctx)
          }
          is KtFunction -> {
            val descriptor = ctx.trace.bindingContext[BindingContext.FUNCTION, node]
            return@getOrPut descriptor?.callContextOfThis(ctx) ?: CallContext.DEFAULT
          }
          is KtProperty -> {
            if (!node.isLocal) {
              val descriptor =
                ctx.trace.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, node] as? CallableDescriptor
              return@getOrPut descriptor?.callContextOfThis(ctx) ?: CallContext.DEFAULT
            }
          }
        }
        node = node.parent as? KtElement
      }
    } catch (e: Throwable) {
      e.printStackTrace()
    }

    return@getOrPut callContextOfThis(ctx)
  }
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

private fun CallableDescriptor.callContextOfThis(ctx: Context): CallContext = when {
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
