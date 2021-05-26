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

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.diagnostics.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class InjektDiagnosticSuppressor : DiagnosticSuppressor {
  override fun isSuppressed(diagnostic: Diagnostic): Boolean =
    isSuppressed(diagnostic, null)

  override fun isSuppressed(diagnostic: Diagnostic, bindingContext: BindingContext?): Boolean {
    if (bindingContext == null)
      return false

    if (diagnostic is DiagnosticWithParameters1<*, *> &&
        diagnostic.factory == Errors.NO_VALUE_FOR_PARAMETER) {
      val valueParameter = diagnostic.a as? ValueParameterDescriptor
      if (valueParameter?.isInject(valueParameter.module.injektContext,
          valueParameter.module.injektContext.trace) == true)
        return true
    }

    if (diagnostic.factory == Errors.INAPPLICABLE_INFIX_MODIFIER ||
      diagnostic.factory == Errors.INAPPLICABLE_OPERATOR_MODIFIER
    )
      return diagnostic.psiElement.parent.parent.safeAs<KtNamedFunction>()
        ?.valueParameters
        ?.count { !it.hasAnnotation(InjektFqNames.Inject) } == 1

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
          ?.hasAnnotation(InjektFqNames.Qualifier) == true
      )
        return true
    }

    if (diagnostic.factory == Errors.UNUSED_PARAMETER ||
      diagnostic.factory == Errors.UNUSED_VARIABLE
    ) {
      val descriptor =
        (diagnostic.psiElement as KtDeclaration).descriptor<DeclarationDescriptor>(
          bindingContext
        )
          ?: return false
      try {
        if (bindingContext[InjektWritableSlices.USED_INJECTABLE, descriptor] != null) return true
      } catch (e: Throwable) {
      }
    }

    if (diagnostic.factory == InjektErrors.UNUSED_INJECTABLE_IMPORT)
      return bindingContext[InjektWritableSlices.USED_IMPORT,
          SourcePosition(
            diagnostic.psiElement.containingFile.cast<KtFile>().virtualFilePath,
            diagnostic.psiElement.startOffset,
            diagnostic.psiElement.endOffset
          )] != null

    return false
  }
}
