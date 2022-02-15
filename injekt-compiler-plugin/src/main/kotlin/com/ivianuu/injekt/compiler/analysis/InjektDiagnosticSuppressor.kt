/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
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

    val ctx = bindingContext[InjektWritableSlices.INJEKT_CONTEXT, Unit]
      ?: return false

    if (diagnostic.factory == Errors.INAPPLICABLE_INFIX_MODIFIER ||
      diagnostic.factory == Errors.INAPPLICABLE_OPERATOR_MODIFIER)
      return diagnostic.psiElement.parent.parent.safeAs<KtNamedFunction>()
        ?.descriptor<CallableDescriptor>(ctx)
        ?.valueParameters
        ?.filterNot { it.isInject(ctx) }
        ?.size
        ?.let { it <= 1 } == true

    if (diagnostic.factory == Errors.ANNOTATION_USED_AS_ANNOTATION_ARGUMENT)
      return true

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
        ?.descriptor<CallableDescriptor>(ctx)
      if (descriptor?.hasAnnotation(InjektFqNames.Provide) == true ||
        descriptor?.valueParameters?.any {
          it.hasAnnotation(InjektFqNames.Inject) ||
              it.hasAnnotation(InjektFqNames.Provide)
        } == true)
          return true
    }

    if (diagnostic.factory == Errors.UNUSED_TYPEALIAS_PARAMETER) {
      val typeParameter = diagnostic.psiElement
        .cast<KtTypeParameter>().descriptor<TypeParameterDescriptor>(ctx)
      return diagnostic.psiElement.getParentOfType<KtTypeAlias>(false)
        ?.descriptor<TypeAliasDescriptor>(ctx)
        ?.expandedType
        ?.toTypeRef(ctx)
        ?.anyType { it.classifier.descriptor == typeParameter } == true
    }

    // todo remove once compose fun interface support is fixed
    if (diagnostic.factory.name == "COMPOSABLE_INVOCATION")
      return true

    if (diagnostic.factory == Errors.FINAL_UPPER_BOUND)
      return true

    return false
  }
}
