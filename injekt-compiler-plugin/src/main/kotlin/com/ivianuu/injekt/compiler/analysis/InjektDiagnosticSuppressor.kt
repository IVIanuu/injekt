/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.SourcePosition
import com.ivianuu.injekt.compiler.descriptor
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.resolution.anyType
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import org.jetbrains.kotlin.builtins.isFunctionOrSuspendFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters2
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class InjektDiagnosticSuppressor : DiagnosticSuppressor {
  override fun isSuppressed(diagnostic: Diagnostic): Boolean =
    isSuppressed(diagnostic, null)

  override fun isSuppressed(diagnostic: Diagnostic, bindingContext: BindingContext?): Boolean {
    if (diagnostic.factory == Errors.FINAL_UPPER_BOUND)
      return true

    // todo remove once compose fun interface support is fixed
    if (diagnostic.factory.name == "COMPOSABLE_INVOCATION")
      return true

    // todo remove once compose fun interface support is fixed
    if (diagnostic.factory.name.contains("TYPE_MISMATCH") &&
      diagnostic is DiagnosticWithParameters2<*, *, *> &&
      diagnostic.a.safeAs<KotlinType>()?.isFunctionOrSuspendFunctionType == true &&
      diagnostic.b.safeAs<KotlinType>()?.isFunctionOrSuspendFunctionType == true)
      return true

    if (diagnostic.factory == Errors.NO_CONTEXT_RECEIVER)
      return true

    if (diagnostic.factory == Errors.SUBTYPING_BETWEEN_CONTEXT_RECEIVERS)
      return true

    if (diagnostic.factory == Errors.UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL)
      return true

    if (diagnostic.factory == Errors.ANNOTATION_USED_AS_ANNOTATION_ARGUMENT)
      return true

    if (bindingContext == null)
      return false

    val ctx = bindingContext[InjektWritableSlices.INJEKT_CONTEXT, Unit]
      ?: return false

    if (diagnostic.factory == Errors.WRONG_ANNOTATION_TARGET) {
      val annotationDescriptor =
        bindingContext[BindingContext.ANNOTATION, diagnostic.psiElement.cast()]
      if (annotationDescriptor?.type?.constructor?.declarationDescriptor
          ?.hasAnnotation(InjektFqNames.Tag) == true
      )
        return true
    }

    if (diagnostic.factory == InjektErrors.UNUSED_PROVIDER_IMPORT) {
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
          it.hasAnnotation(InjektFqNames.Context) ||
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

    return false
  }
}
