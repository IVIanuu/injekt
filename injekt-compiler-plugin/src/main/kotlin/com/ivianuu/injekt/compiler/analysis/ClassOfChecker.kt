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
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

class ClassOfChecker(private val typeAnnotationChecker: TypeAnnotationChecker) : CallChecker {

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        if (resolvedCall.resultingDescriptor.fqNameSafe.asString() != "com.ivianuu.injekt.classOf") return

        val enclosingInjektDslFunction = findEnclosingFunctionContext(context) {
            val typeAnnotations = typeAnnotationChecker.getTypeAnnotations(context.trace, it)
            InjektFqNames.Module in typeAnnotations ||
                    InjektFqNames.Factory in typeAnnotations ||
                    InjektFqNames.ChildFactory in typeAnnotations ||
                    InjektFqNames.CompositionFactory in typeAnnotations ||
                    InjektFqNames.InstanceFactory in typeAnnotations
        }
        if (enclosingInjektDslFunction == null) {
            context.trace.report(
                InjektErrors.CLASS_OF_OUTSIDE_OF_MODULE
                    .on(reportOn)
            )
        } else {
            val typeArgument = resolvedCall.typeArguments.values.single()
            if (!typeArgument.isTypeParameter()) {
                context.trace.report(
                    InjektErrors.CLASS_OF_WITH_CONCRETE_TYPE
                        .on(reportOn)
                )
            }

            if (!enclosingInjektDslFunction.isInline) {
                context.trace.report(
                    InjektErrors.CLASS_OF_CALLING_MODULE_MUST_BE_INLINE
                        .on(reportOn)
                )
            }

            enclosingInjektDslFunction.typeParameters.forEach {
                if (it.isReified) {
                    context.trace.report(
                        InjektErrors.MODULE_CANNOT_USE_REIFIED
                            .on(it.findPsi() ?: reportOn)
                    )
                }
            }
        }
    }

}
