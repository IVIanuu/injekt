/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast

class InjektDiagnosticSuppressor : DiagnosticSuppressor {
  override fun isSuppressed(diagnostic: Diagnostic): Boolean =
    isSuppressed(diagnostic, null)

  override fun isSuppressed(diagnostic: Diagnostic, bindingContext: BindingContext?): Boolean {
    if (diagnostic.factory == Errors.SUBTYPING_BETWEEN_CONTEXT_RECEIVERS)
      return true

    if (diagnostic.factory == Errors.NO_CONTEXT_RECEIVER)
      return true

    if (diagnostic.factory == Errors.ANNOTATION_USED_AS_ANNOTATION_ARGUMENT)
      return true

    if (bindingContext == null)
      return false

    if (diagnostic.factory == Errors.WRONG_ANNOTATION_TARGET) {
      val annotationDescriptor =
        bindingContext[BindingContext.ANNOTATION, diagnostic.psiElement.cast()]
      if (annotationDescriptor?.type?.constructor?.declarationDescriptor
          ?.hasAnnotation(InjektFqNames.Tag) == true
      )
        return true
    }

    return false
  }
}
