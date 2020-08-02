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

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.extensions.internal.TypeResolutionInterceptorExtension
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations

@OptIn(org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints::class)
@Suppress("INVISIBLE_REFERENCE", "EXPERIMENTAL_IS_NOT_ENABLED")
class ReaderTypeInterceptor(
    private val implicitChecker: ImplicitChecker
) : TypeResolutionInterceptorExtension {

    override fun interceptFunctionLiteralDescriptor(
        expression: KtLambdaExpression,
        context: ExpressionTypingContext,
        descriptor: AnonymousFunctionDescriptor
    ): AnonymousFunctionDescriptor {
        if (context.expectedType !== TypeUtils.NO_EXPECTED_TYPE &&
            context.expectedType !== TypeUtils.UNIT_EXPECTED_TYPE &&
            context.expectedType.hasAnnotation(InjektFqNames.Reader)
        ) {
            context.trace.record(InjektWritableSlices.IS_IMPLICIT, descriptor, true)
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
        val isReader = implicitChecker.isImplicit(context.trace, element, resultType)
        return if (isReader) resultType.withReaderAnnotation(context.scope.ownerDescriptor.module)
        else resultType
    }

    private fun KotlinType.withReaderAnnotation(
        module: ModuleDescriptor
    ): KotlinType {
        return replaceAnnotations(
            Annotations.create(
                annotations + AnnotationDescriptorImpl(
                    module.findClassAcrossModuleDependencies(
                        ClassId.topLevel(InjektFqNames.Reader)
                    )!!.defaultType,
                    emptyMap(),
                    SourceElement.NO_SOURCE
                )
            )
        )
    }
}
