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

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.resolution.injectNParameters
import com.ivianuu.injekt.compiler.trace
import com.ivianuu.shaded_injekt.Inject
import com.ivianuu.shaded_injekt.Provide
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.extensions.internal.TypeResolutionInterceptorExtension
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getValueArgumentForExpression
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.sam.getSingleAbstractMethodOrNull
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

@Suppress("INVISIBLE_REFERENCE", "EXPERIMENTAL_IS_NOT_ENABLED", "IllegalExperimentalApiUsage")
@OptIn(org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints::class)
class InjectTypeResolutionInterceptorExtension(
  @Inject private val injektFqNames: InjektFqNames
) : TypeResolutionInterceptorExtension {
  override fun interceptFunctionLiteralDescriptor(
    expression: KtLambdaExpression,
    context: ExpressionTypingContext,
    descriptor: AnonymousFunctionDescriptor
  ): AnonymousFunctionDescriptor {
    if (context.expectedType === TypeUtils.NO_EXPECTED_TYPE) return descriptor

    @Provide val ctx = Context(context.scope.ownerDescriptor.module, injektFqNames, context.trace)

    if (context.expectedType.hasAnnotation(injektFqNames.inject2) &&
      !descriptor.hasAnnotation(injektFqNames.inject2)) {
      return AnonymousFunctionDescriptor(
        descriptor.containingDeclaration,
        Annotations.create(descriptor.annotations + context.expectedType.annotations.filter {
          it.fqName == injektFqNames.inject2 ||
              it.fqName == injektFqNames.injectNInfo
        }),
        descriptor.kind,
        descriptor.source,
        descriptor.isSuspend
      )
    }
    val arg = getArgumentDescriptor(expression.functionLiteral, trace()!!.bindingContext)

    val argTypeDescriptor = arg
      ?.type
      ?.constructor
      ?.declarationDescriptor as? ClassDescriptor
    if (argTypeDescriptor != null) {
      val sam = getSingleAbstractMethodOrNull(argTypeDescriptor)
      if (sam != null && sam.hasAnnotation(injektFqNames.inject2)) {
        trace()!!.record(InjektWritableSlices.INJECT_N_PARAMETERS, descriptor,
          sam.injectNParameters())
      }
    }

    return descriptor
  }

  override fun interceptType(
    element: KtElement,
    context: ExpressionTypingContext,
    resultType: KotlinType
  ): KotlinType {
    if (resultType === TypeUtils.NO_EXPECTED_TYPE) return resultType
    if (element !is KtLambdaExpression) return resultType

    @Provide val ctx = Context(context.scope.ownerDescriptor.module, injektFqNames, context.trace)

    val arg = getArgumentDescriptor(element.functionLiteral, trace()!!.bindingContext)

    val argTypeDescriptor = arg
      ?.type
      ?.constructor
      ?.declarationDescriptor as? ClassDescriptor

    if (argTypeDescriptor != null) {
      val sam = getSingleAbstractMethodOrNull(argTypeDescriptor)
      if (sam != null && sam.hasAnnotation(injektFqNames.inject2)) {
        return resultType.replaceAnnotations(
          Annotations.create(
            resultType.annotations + sam.annotations.filter {
              it.fqName == injektFqNames.inject2
            }
          )
        )
      }
    }

    if (element.safeAs<KtAnnotated>()?.hasAnnotation(injektFqNames.inject2) == true ||
      context.expectedType.hasAnnotation(injektFqNames.inject2)) {
      return resultType.replaceAnnotations(
        Annotations.create(
          resultType.annotations + context.expectedType.annotations.filter {
            it.fqName == injektFqNames.inject2 ||
                it.fqName == injektFqNames.injectNInfo
          }
        )
      )
    }

    return resultType
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