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
import com.ivianuu.injekt.compiler.descriptor
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class InjektDiagnosticSuppressor : DiagnosticSuppressor {

    override fun isSuppressed(diagnostic: Diagnostic): Boolean =
        isSuppressed(diagnostic, null)

    override fun isSuppressed(diagnostic: Diagnostic, bindingContext: BindingContext?): Boolean {
        if (bindingContext == null) return false

        if (diagnostic.factory == Errors.FINAL_UPPER_BOUND) {
            val typeParameter = diagnostic.psiElement.parent as? KtTypeParameter
            if (typeParameter?.hasAnnotation(InjektFqNames.Given) == true) return true
        }

        if (diagnostic.factory == Errors.WRONG_ANNOTATION_TARGET) {
            val annotationDescriptor = bindingContext[BindingContext.ANNOTATION, diagnostic.psiElement.cast()]
            if (annotationDescriptor?.type?.constructor?.declarationDescriptor
                    ?.hasAnnotation(InjektFqNames.Qualifier) == true)
                        return true
        }

        if (diagnostic.factory == Errors.UNUSED_PARAMETER) {
            val descriptor =
                (diagnostic.psiElement as KtDeclaration).descriptor<ParameterDescriptor>(
                    bindingContext)
                    ?: return false
            try {
                if (bindingContext[InjektWritableSlices.USED_GIVEN, descriptor] != null) return true
            } catch (e: Throwable) {
            }
        }

        return false
    }
}
