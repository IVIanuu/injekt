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
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

class DslCallChecker(
    private val typeAnnotationChecker: TypeAnnotationChecker
) : CallChecker {
    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        if (resolvedCall.resultingDescriptor.fqNameSafe !in InjektFqNames.DslNames) return
        val enclosingInjektDslFunction = findEnclosingModuleFunctionContext(context) {
            val typeAnnotations = typeAnnotationChecker.getTypeAnnotations(context.trace, it)
            InjektFqNames.Module in typeAnnotations ||
                    InjektFqNames.Factory in typeAnnotations ||
                    InjektFqNames.ChildFactory in typeAnnotations
        }

        if (enclosingInjektDslFunction != null &&
            (resolvedCall.resultingDescriptor.name.asString() == "transient" ||
                    resolvedCall.resultingDescriptor.name.asString() == "scoped") &&
            resolvedCall.resultingDescriptor.valueParameters.isEmpty() &&
            resolvedCall.typeArguments.values.single().isTypeParameter() &&
            !enclosingInjektDslFunction.isInline
        ) {
            context.trace.report(
                InjektErrors.GENERIC_BINDING_WITHOUT_INLINE_AND_DEFINITION
                    .on(reportOn)
            )
        }

        if (enclosingInjektDslFunction == null) {
            context.trace.report(
                InjektErrors.FORBIDDEN_DSL_FUNCTION_INVOCATION
                    .on(reportOn)
            )
        }
    }
}