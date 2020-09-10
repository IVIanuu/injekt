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

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

class KeyTypeParameterChecker : CallChecker, DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor is TypeParameterDescriptor &&
            descriptor.annotations.hasAnnotation(InjektFqNames.ForKey) &&
            descriptor.containingDeclaration !is FunctionDescriptor &&
            descriptor.containingDeclaration !is PropertyDescriptor
        ) {
            context.trace.report(
                InjektErrors.FOR_KEY_TYPE_PARAMETER_INVALID_PARENT
                    .on(declaration)
            )
        }
    }

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        resolvedCall.typeArguments.forEach { (typeParameter, typeArgument) ->
            if (typeParameter.annotations.hasAnnotation(InjektFqNames.ForKey) &&
                typeArgument.isTypeParameter() &&
                !typeArgument.constructor.declarationDescriptor!!.annotations.hasAnnotation(
                    InjektFqNames.ForKey
                )
            ) {
                context.trace.report(
                    InjektErrors.MUST_BE_FOR_KEY.on(resolvedCall.call.callElement)
                )
            }
        }
    }

}
