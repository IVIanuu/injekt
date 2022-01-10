/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import com.ivianuu.shaded_injekt.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.diagnostics.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class InjektDiagnosticSuppressor : DiagnosticSuppressor {
  override fun isSuppressed(diagnostic: Diagnostic): Boolean =
    isSuppressed(diagnostic, null)

  override fun isSuppressed(diagnostic: Diagnostic, bindingContext: BindingContext?): Boolean {
    if (bindingContext == null)
      return false

    @Provide val ctx = bindingContext[InjektWritableSlices.INJEKT_CONTEXT, Unit]
      ?: return false

    if (diagnostic.factory == Errors.INAPPLICABLE_INFIX_MODIFIER ||
      diagnostic.factory == Errors.INAPPLICABLE_OPERATOR_MODIFIER)
      return diagnostic.psiElement.parent.parent.safeAs<KtNamedFunction>()
        ?.descriptor<CallableDescriptor>()
        ?.valueParameters
        ?.filterNot { it.isInject() }
        ?.size
        ?.let { it <= 1 } == true

    if (diagnostic.factory == Errors.ANNOTATION_USED_AS_ANNOTATION_ARGUMENT)
      return true

    // todo remove on kotlin 1.6.0
    if (diagnostic.factory == Errors.UNSUPPORTED) {
      val typeParameter = diagnostic.psiElement.parent?.parent as? KtTypeParameter
      if (typeParameter?.hasAnnotation(injektFqNames().spread) == true) return true
    }

    if (diagnostic.factory == Errors.WRONG_ANNOTATION_TARGET) {
      val annotationDescriptor =
        bindingContext[BindingContext.ANNOTATION, diagnostic.psiElement.cast()]
      if (annotationDescriptor?.type?.constructor?.declarationDescriptor
          ?.hasAnnotation(injektFqNames().tag) == true
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

    if (diagnostic.factory == Errors.NOTHING_TO_INLINE) {
      val descriptor = diagnostic.psiElement.getParentOfType<KtNamedDeclaration>(false)
        ?.descriptor<CallableDescriptor>()
      if (descriptor?.hasAnnotation(injektFqNames().provide) == true ||
        descriptor?.valueParameters?.any {
          it.hasAnnotation(injektFqNames().inject) ||
              it.hasAnnotation(injektFqNames().provide)
        } == true)
          return true
    }

    if (diagnostic.factory == Errors.UNUSED_TYPEALIAS_PARAMETER) {
      val typeParameter = diagnostic.psiElement
        .cast<KtTypeParameter>().descriptor<TypeParameterDescriptor>()
      return diagnostic.psiElement.getParentOfType<KtTypeAlias>(false)
        ?.descriptor<TypeAliasDescriptor>()
        ?.expandedType
        ?.toTypeRef()
        ?.anyType { it.classifier.descriptor == typeParameter } == true
    }


    // todo remove on kotlin 1.6.0 update
    if (diagnostic.factory == Errors.SUPERTYPE_IS_SUSPEND_FUNCTION_TYPE)
      return true

    // todo remove once compose fun interface support is fixed
    if (diagnostic.factory.name == "COMPOSABLE_INVOCATION")
      return true

    if (diagnostic.factory == Errors.FINAL_UPPER_BOUND)
      return true

    return false
  }
}
