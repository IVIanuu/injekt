/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.checkers.*

class ClassInfoGenerator(private val baseCtx: Context) : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    // requesting infos triggers saving them
    if (descriptor is ClassDescriptor && descriptor.visibility.shouldPersistInfo()) {
      val ctx = baseCtx.withTrace(context.trace)
      descriptor.declaresInjectables(ctx)
      descriptor.primaryConstructorPropertyParameters()
    }
  }
}
