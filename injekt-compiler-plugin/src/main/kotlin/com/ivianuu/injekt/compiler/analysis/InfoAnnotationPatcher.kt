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

import com.ivianuu.injekt.*
import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.checkers.*

class InfoAnnotationPatcher(@Provide private val context: InjektContext) : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    @Provide val trace = context.trace
    // requesting infos triggers saving them
    when (descriptor) {
      is ClassDescriptor -> {
        descriptor.classifierInfo()
        descriptor.declaredTypeParameters
          .forEach { it.classifierInfo() }
        descriptor.constructors
          .forEach { it.callableInfo() }
      }
      is CallableDescriptor -> {
        descriptor.callableInfo()
        descriptor.typeParameters
          .forEach { it.classifierInfo() }
      }
    }
  }
}
