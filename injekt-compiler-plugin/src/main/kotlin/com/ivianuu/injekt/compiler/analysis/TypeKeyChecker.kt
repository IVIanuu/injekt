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
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType

class TypeKeyChecker : CallChecker {
    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        resolvedCall
            .typeArguments
            .filterKeys { it.hasAnnotation(InjektFqNames.ForTypeKey) }
            .forEach { it.value.checkAllForTypeKey(reportOn, context.trace) }
    }

    private fun KotlinType.checkAllForTypeKey(
        reportOn: PsiElement,
        trace: BindingTrace
    ) {
        if (constructor.declarationDescriptor is TypeParameterDescriptor &&
                !constructor.declarationDescriptor!!.hasAnnotation(InjektFqNames.ForTypeKey)) {
            trace.report(
                InjektErrors.NON_FOR_TYPE_KEY_TYPE_PARAMETER_AS_FOR_TYPE_KEY
                    .on(reportOn, constructor.declarationDescriptor as TypeParameterDescriptor)
            )
        }

        arguments.forEach { it.type.checkAllForTypeKey(reportOn, trace) }
    }
}
