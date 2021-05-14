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

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.extensions.internal.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.expressions.*
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

@Suppress("INVISIBLE_REFERENCE", "EXPERIMENTAL_IS_NOT_ENABLED", "IllegalExperimentalApiUsage")
@OptIn(org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints::class)
class InjektTypeResolutionInterceptorExtension : TypeResolutionInterceptorExtension {
  override fun interceptType(
    element: KtElement,
    context: ExpressionTypingContext,
    resultType: KotlinType,
  ): KotlinType {
    if (resultType === TypeUtils.NO_EXPECTED_TYPE) return resultType
    if (element !is KtLambdaExpression) return resultType
    if (! resultType.hasAnnotation(InjektFqNames.Given)) return resultType
    val annotation = element.safeAs<KtAnnotated>()
      ?.findAnnotation(InjektFqNames.Given)
      ?: element.parent.safeAs<KtAnnotated>()?.findAnnotation(InjektFqNames.Given)
      ?: context.expectedType.safeAs<KtAnnotated>()?.findAnnotation(InjektFqNames.Given)
    if (annotation != null) {
      val annotationDescriptor = context.trace[BindingContext.ANNOTATION, annotation]
      if (annotationDescriptor != null) {
        return resultType.replaceAnnotations(
          Annotations.create(resultType.annotations + annotationDescriptor)
        )
      }
    }
    return resultType
  }
}
