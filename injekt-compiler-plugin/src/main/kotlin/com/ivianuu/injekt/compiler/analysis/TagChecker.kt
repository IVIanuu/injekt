/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.reportError
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.source.getPsi

context(Context) class TagChecker : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) = with(withTrace(context.trace)) {
    if (descriptor is ClassDescriptor && descriptor.hasAnnotation(InjektFqNames.Tag) &&
      descriptor.unsubstitutedPrimaryConstructor?.valueParameters?.isNotEmpty() == true)
      reportError(
        descriptor.annotations.findAnnotation(InjektFqNames.Tag)
          ?.source?.getPsi() ?: declaration,
        "tag cannot have value parameters"
      )
  }
}
