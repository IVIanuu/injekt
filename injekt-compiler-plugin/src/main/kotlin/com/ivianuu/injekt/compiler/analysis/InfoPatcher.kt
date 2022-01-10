/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import com.ivianuu.shaded_injekt.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.checkers.*

class InfoPatcher(@Inject private val baseCtx: Context) : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    @Provide val ctx = baseCtx.withTrace(context.trace)

    // requesting infos triggers saving them
    when (descriptor) {
      is ClassDescriptor -> {
        if (descriptor.visibility.shouldPersistInfo()) {
          descriptor.classifierInfo()
          descriptor.declaredTypeParameters
            .forEach { it.classifierInfo() }
          descriptor.constructors
            .forEach { it.callableInfo() }
        }
      }
      is CallableDescriptor -> {
        if (descriptor.visibility.shouldPersistInfo()) {
          descriptor.callableInfo()
          descriptor.typeParameters
            .forEach { it.classifierInfo() }
        }
      }
      is TypeAliasDescriptor -> {
        if (descriptor.visibility.shouldPersistInfo()) {
          descriptor.classifierInfo()
          descriptor.declaredTypeParameters
            .forEach { it.classifierInfo() }
        }
      }
    }
  }
}
