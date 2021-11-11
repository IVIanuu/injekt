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

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.callableInfo
import com.ivianuu.injekt.compiler.classifierInfo
import com.ivianuu.injekt.compiler.shouldPersistInfo
import com.ivianuu.shaded_injekt.Inject
import com.ivianuu.shaded_injekt.Provide
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext

class InfoPatcher(@Inject private val baseCtx: Context) : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    @Provide val ctx = baseCtx.withTrace(context.trace)

    if (descriptor !is DeclarationDescriptorWithVisibility ||
        !descriptor.visibility.shouldPersistInfo())
      return

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
