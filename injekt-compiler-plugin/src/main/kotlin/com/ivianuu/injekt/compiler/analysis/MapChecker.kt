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
import com.ivianuu.injekt.compiler.SupportedMapKeyTypes
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class MapChecker : CallChecker {

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val descriptor = resolvedCall.resultingDescriptor
        if (descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.map") {
            val mapKeyType = resolvedCall.typeArguments.toList()
                .first()
                .second
                ?.constructor?.declarationDescriptor?.fqNameSafe

            if (mapKeyType !in SupportedMapKeyTypes) {
                context.trace.report(InjektErrors.UNSUPPORTED_MAP_KEY_TYPE.on(reportOn))
            }
        } else if (descriptor.dispatchReceiverParameter?.type?.constructor?.declarationDescriptor ==
            context.moduleDescriptor.findClassAcrossModuleDependencies(
                ClassId.topLevel(
                    InjektFqNames.MapDsl
                )
            )
            && descriptor.name.asString() == "put"
        ) {
            val keyArg = resolvedCall.call.valueArguments.single()
            val constant = ConstantExpressionEvaluator.getConstant(
                keyArg.getArgumentExpression()!!,
                context.trace.bindingContext
            )
            if (constant == null && keyArg.getArgumentExpression() !is KtClassLiteralExpression &&
                keyArg.getArgumentExpression()?.text?.contains("classOf") != true
            ) {
                context.trace.report(InjektErrors.MAP_KEY_MUST_BE_CONSTANT.on(reportOn))
            }
        }
    }

}