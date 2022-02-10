/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.checkers.*

class TagChecker(private val baseCtx: Context) : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    val ctx = baseCtx.withTrace(context.trace)

    if (descriptor.hasAnnotation(InjektFqNames.Tag) && descriptor is ClassDescriptor) {
      if (descriptor.unsubstitutedPrimaryConstructor?.valueParameters?.isNotEmpty() == true) {
        ctx.trace!!.report(
          InjektErrors.TAG_WITH_VALUE_PARAMETERS
            .on(
              declaration.findAnnotation(InjektFqNames.Tag)
                ?: declaration
            )
        )
      }
    }
  }
}
