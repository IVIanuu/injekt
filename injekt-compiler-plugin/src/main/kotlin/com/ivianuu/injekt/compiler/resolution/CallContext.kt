/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.composeCompilerInClasspath
import com.ivianuu.injekt.compiler.getOrPut
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi2ir.generators.isSamConstructor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getValueArgumentForExpression
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.inline.InlineUtil.canBeInlineArgument
import org.jetbrains.kotlin.resolve.inline.InlineUtil.isInline
import org.jetbrains.kotlin.resolve.inline.InlineUtil.isInlineParameter
import org.jetbrains.kotlin.resolve.sam.getSingleAbstractMethodOrNull
import org.jetbrains.kotlin.utils.addToStdlib.cast

enum class CallContext {
  DEFAULT, COMPOSABLE, SUSPEND
}

fun CallContext.canCall(other: CallContext) = this == other || other == CallContext.DEFAULT

fun CallableDescriptor.callContext(ctx: Context): CallContext {
  if (this is ConstructorDescriptor) return CallContext.DEFAULT

  if (this !is FunctionDescriptor && this !is PropertyDescriptor) return CallContext.DEFAULT

  if (ctx.trace == null) return callContextOfThis

  return ctx.trace.getOrPut(InjektWritableSlices.CALL_CONTEXT, this) {
    val initialNode = findPsi() ?: return@getOrPut callContextOfThis

    var node: PsiElement? = initialNode
    try {
      loop@ while (node != null) {
        when (node) {
          is KtFunctionLiteral -> {
            // keep going, as this is a "KtFunction", but we actually want the
            // KtLambdaExpression
          }
          is KtLambdaExpression -> {
            val descriptor = ctx.trace.bindingContext[BindingContext.FUNCTION, node.functionLiteral]
              ?: return@getOrPut callContextOfThis

            if (composeCompilerInClasspath) {
              val parentCall = node.getParentResolvedCall(ctx.trace.bindingContext)
              if (parentCall?.candidateDescriptor?.isSamConstructor() == true)
                return@getOrPut parentCall.candidateDescriptor.returnType
                  ?.constructor
                  ?.declarationDescriptor
                  ?.cast<ClassDescriptor>()
                  ?.let { getSingleAbstractMethodOrNull(it) }
                  ?.callContext(ctx) ?: callContextOfThis
            }

            val arg = getArgumentDescriptor(node.functionLiteral, ctx.trace.bindingContext)

            val inlined = arg != null &&
                canBeInlineArgument(node.functionLiteral) &&
                isInline(arg.containingDeclaration) &&
                isInlineParameter(arg) &&
                !arg.isCrossinline

            if (!inlined)
              return@getOrPut arg?.type?.toTypeRef(ctx)?.callContext
                ?: descriptor.callContextOfThis
          }
          is KtFunction -> {
            val descriptor = ctx.trace.bindingContext[BindingContext.FUNCTION, node]
            return@getOrPut descriptor?.callContextOfThis ?: CallContext.DEFAULT
          }
          is KtProperty -> {
            if (!node.isLocal) {
              val descriptor =
                ctx.trace.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, node] as? CallableDescriptor
              return@getOrPut descriptor?.callContextOfThis ?: CallContext.DEFAULT
            }
          }
        }
        node = node.parent as? KtElement
      }
    } catch (e: Throwable) {
      e.printStackTrace()
    }

    return@getOrPut callContextOfThis
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
    classifier.fqName == InjektFqNames.Composable -> CallContext.COMPOSABLE
    else -> CallContext.DEFAULT
  }
