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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

class ObjectGraphFunctionChecker : CallChecker {

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val resultingDescriptor = resolvedCall.resultingDescriptor as? FunctionDescriptor ?: return
        if ((resultingDescriptor.fqNameSafe.asString() == "com.ivianuu.injekt.composition.inject" &&
                    resultingDescriptor.valueParameters.size == 1) ||
            resultingDescriptor.fqNameSafe.asString() == "com.ivianuu.injekt.composition.get"
        ) {
            val receiver = resolvedCall.extensionReceiver!!.type
            if (receiver.constructor.declarationDescriptor?.annotations?.hasAnnotation(InjektFqNames.CompositionComponent) != true &&
                !receiver.isTypeParameter()
            ) {
                context.trace.report(
                    InjektErrors.NOT_A_COMPOSITION_COMPONENT
                        .on(reportOn)
                )
            }
        }
    }

}
