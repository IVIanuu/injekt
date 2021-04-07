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

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.findAnnotation
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.resolution.substitute
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt.compiler.toTypeRef
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.types.KotlinType

class QualifierChecker(private val context: InjektContext) : DeclarationChecker, AdditionalTypeChecker {
    override fun checkType(
        expression: KtExpression,
        expressionType: KotlinType,
        expressionTypeWithSmartCast: KotlinType,
        c: ResolutionContext<*>
    ) {
        val resolvedCall = expression.getResolvedCall(c.trace.bindingContext) ?: return
        val callee = resolvedCall.resultingDescriptor
        if (!callee.original.isExternalDeclaration()) return
        val info = context.callableInfoFor(callee, c.trace) ?: return
        val expressionTypeRef = expressionType.toTypeRef(context, c.trace)
        val actualExpressionType = info.type.toTypeRef(context, c.trace)
            .substitute(resolvedCall.typeArguments.mapKeys {
                it.key.toClassifierRef(context, c.trace)
            }.mapValues { it.value.toTypeRef(context, c.trace) })
        if (expressionTypeRef != actualExpressionType) {
            c.trace.record(InjektWritableSlices.EXPECTED_TYPE, expression, actualExpressionType)
        }
    }
    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor.hasAnnotation(InjektFqNames.Qualifier) && descriptor is ClassDescriptor) {
            if (descriptor.unsubstitutedPrimaryConstructor?.valueParameters?.isNotEmpty() == true) {
                context.trace.report(
                    InjektErrors.QUALIFIER_WITH_VALUE_PARAMETERS
                        .on(declaration)
                )
            }
        } else {
            val qualifiers = descriptor.getAnnotatedAnnotations(InjektFqNames.Qualifier)
            if (qualifiers.isNotEmpty() && descriptor !is ClassDescriptor) {
                context.trace.report(
                    InjektErrors.QUALIFIER_ON_NON_CLASS_AND_NON_TYPE
                        .on(declaration.findAnnotation(qualifiers.first().fqName!!)
                            ?: declaration)
                )
            }
        }
    }
}
