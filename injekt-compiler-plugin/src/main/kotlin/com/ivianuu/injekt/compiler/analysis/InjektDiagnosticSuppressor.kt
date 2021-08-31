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

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.SourcePosition
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class InjektDiagnosticSuppressor : DiagnosticSuppressor {
  override fun isSuppressed(diagnostic: Diagnostic): Boolean =
    isSuppressed(diagnostic, null)

  override fun isSuppressed(diagnostic: Diagnostic, bindingContext: BindingContext?): Boolean {
    if (bindingContext == null)
      return false

    if (diagnostic.factory == Errors.INAPPLICABLE_INFIX_MODIFIER ||
      diagnostic.factory == Errors.INAPPLICABLE_OPERATOR_MODIFIER
    )
      return diagnostic.psiElement.parent.parent.safeAs<KtNamedFunction>()
        ?.valueParameters
        ?.count { !it.hasAnnotation(InjektFqNames.Inject) }
        ?.let { it <= 1 } == true

    if (diagnostic.factory == Errors.UNUSED_TYPEALIAS_PARAMETER)
      return true

    if (diagnostic.factory == Errors.ANNOTATION_USED_AS_ANNOTATION_ARGUMENT)
      return true

    if (diagnostic.factory == Errors.UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE)
      return true

    if (diagnostic.factory == Errors.FINAL_UPPER_BOUND)
      return true

    if (diagnostic.factory == Errors.NOTHING_TO_INLINE) {
      val function = diagnostic.psiElement.getParentOfType<KtNamedFunction>(false)
      if (function?.hasAnnotation(InjektFqNames.Provide) == true)
        return true
    }

    if (diagnostic.factory == Errors.UNSUPPORTED) {
      val typeParameter = diagnostic.psiElement.parent?.parent as? KtTypeParameter
      if (typeParameter?.hasAnnotation(InjektFqNames.Spread) == true) return true
    }

    if (diagnostic.factory == Errors.FINAL_UPPER_BOUND) {
      val typeParameter = diagnostic.psiElement.parent as? KtTypeParameter
      if (typeParameter?.hasAnnotation(InjektFqNames.Spread) == true) return true
    }

    if (diagnostic.factory == Errors.WRONG_ANNOTATION_TARGET) {
      val annotationDescriptor =
        bindingContext[BindingContext.ANNOTATION, diagnostic.psiElement.cast()]
      if (annotationDescriptor?.type?.constructor?.declarationDescriptor
          ?.hasAnnotation(InjektFqNames.Tag) == true
      )
        return true
    }

    if (diagnostic.factory == InjektErrors.UNUSED_INJECTABLE_IMPORT) {
      val filePath = diagnostic.psiElement.containingFile.safeAs<KtFile>()?.virtualFilePath
      if (filePath != null) {
        return bindingContext[InjektWritableSlices.USED_IMPORT,
            SourcePosition(
              filePath,
              diagnostic.psiElement.startOffset,
              diagnostic.psiElement.endOffset
            )] != null
      }
    }

    return false
  }
}
