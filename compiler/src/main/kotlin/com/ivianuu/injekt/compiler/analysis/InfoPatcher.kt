/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.checkers.*

class InfoPatcher(private val baseCtx: Context) : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    val ctx = baseCtx.withTrace(context.trace)

    // requesting infos triggers saving them
    when (descriptor) {
      is ClassDescriptor -> if (descriptor.visibility.shouldPersistInfo()) {
        descriptor.classifierInfo(ctx)
        descriptor.declaredTypeParameters
          .forEach { it.classifierInfo(ctx) }
        descriptor.constructors
          .forEach { it.callableInfo(ctx) }
      }
      is CallableDescriptor -> if (descriptor.visibility.shouldPersistInfo()) {
        descriptor.callableInfo(ctx)
        descriptor.typeParameters
          .forEach { it.classifierInfo(ctx) }
      }
      is TypeAliasDescriptor -> if (descriptor.visibility.shouldPersistInfo()) {
        descriptor.classifierInfo(ctx)
        descriptor.declaredTypeParameters
          .forEach { it.classifierInfo(ctx) }
      }
    }
  }
}
