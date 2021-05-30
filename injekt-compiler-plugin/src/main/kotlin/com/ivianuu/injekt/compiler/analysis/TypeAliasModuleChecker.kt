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

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.checkers.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class TypeAliasModuleChecker(private val context: InjektContext) : DeclarationChecker {
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    if (descriptor !is ClassDescriptor) return

    if (!descriptor.name.asString().endsWith("Module"))
      return

    val lookupLocation = declaration.lookupLocation
    val correspondingTypeAlias = this.context.memberScopeForFqName(
      descriptor.findPackage().fqName,
      lookupLocation
    )?.getContributedClassifier(
      descriptor.name.asString().removeSuffix("Module").asNameId(),
      lookupLocation
    )?.safeAs<TypeAliasDescriptor>() ?: return

    if (descriptor.kind != ClassKind.OBJECT) {
      context.trace.report(
        InjektErrors.TYPE_ALIAS_MODULE_NOT_OBJECT
          .on(declaration)
      )
    }

    val injectablesModule = descriptor.module
    val typeAliasModule = correspondingTypeAlias.module

    if (injectablesModule != typeAliasModule) {
      context.trace.report(
        InjektErrors.TYPE_ALIAS_MODULE_NOT_DECLARED_IN_SAME_COMPILATION_UNIT
          .on(declaration)
      )
    }
  }
}
