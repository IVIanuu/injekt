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

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt_shaded.Inject
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.extensions.internal.TypeResolutionInterceptorExtension
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getValueArgumentForExpression
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext

@Suppress("INVISIBLE_REFERENCE", "EXPERIMENTAL_IS_NOT_ENABLED", "IllegalExperimentalApiUsage")
@OptIn(org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints::class)
class InjectTypeResolutionInterceptorExtension(
  @Inject private val injektFqNames: InjektFqNames
) : TypeResolutionInterceptorExtension {
  override fun interceptFunctionLiteralDescriptor(
    expression: KtLambdaExpression,
    context: ExpressionTypingContext,
    descriptor: AnonymousFunctionDescriptor
  ): AnonymousFunctionDescriptor =
    if (context.expectedType.hasAnnotation(injektFqNames.inject2) &&
      !descriptor.hasAnnotation(injektFqNames.inject2)) {
      AnonymousFunctionDescriptor(
        descriptor.containingDeclaration,
        Annotations.create(descriptor.annotations + context.expectedType.annotations.filter {
          it.fqName == injektFqNames.inject2
        }),
        descriptor.kind,
        descriptor.source,
        descriptor.isSuspend
      )
    } else descriptor

  override fun interceptType(
    element: KtElement,
    context: ExpressionTypingContext,
    resultType: KotlinType
  ): KotlinType {
    if (resultType === TypeUtils.NO_EXPECTED_TYPE) return resultType
    if (element !is KtLambdaExpression) return resultType

    val arg = getArgumentDescriptor(element.functionLiteral, context.trace.bindingContext)

    val argTypeDescriptor = arg
      ?.type
      ?.constructor
      ?.declarationDescriptor as? ClassDescriptor

    /*fun KotlinType.addInjectAnnotations(): KotlinType {
      if (hasComposableAnnotation()) return this
      val annotation = ComposeFqNames.makeComposableAnnotation(module)
      return replaceAnnotations(Annotations.create(annotations + annotation))
    }

    if (argTypeDescriptor != null) {
      val sam = getSingleAbstractMethodOrNull(argTypeDescriptor)
      if (sam != null && sam.hasAnnotation(injektFqNames.inject2)) {
        return resultType.addInjectAnnotations(context.scope.ownerDescriptor.module)
      }
    }

    if (element.safeAs<KtAnnotated>()?.hasAnnotation(injektFqNames.inject2) == true ||
      context.expectedType.hasAnnotation(injektFqNames.inject2)) {
      return resultType.addInjectAnnotations(context.scope.ownerDescriptor.module)
    }*/

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