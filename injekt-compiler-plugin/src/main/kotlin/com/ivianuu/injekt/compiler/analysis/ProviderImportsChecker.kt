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

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.isIde
import com.ivianuu.injekt.compiler.lookupLocation
import com.ivianuu.injekt.compiler.resolution.ProviderImport
import com.ivianuu.injekt.compiler.resolution.getProviderImports
import com.ivianuu.injekt.compiler.resolution.isValidImport
import com.ivianuu.injekt_shaded.Inject
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext

class ProviderImportsChecker(@Inject private val context: InjektContext) : DeclarationChecker {
  private val checkedFiles = mutableSetOf<KtFile>()

  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    val file = declaration.containingKtFile
    checkFile(file, context.trace)
    if (!declaration.hasAnnotation(injektFqNames().providers)) return
    checkImports(file.packageFqName, declaration.getProviderImports(), context.trace)
  }

  private fun checkFile(file: KtFile, trace: BindingTrace) {
    if (file in checkedFiles) return
    checkedFiles += file
    checkImports(file.packageFqName, file.getProviderImports(), trace)
  }

  private fun checkImports(
    currentPackage: FqName,
    imports: List<ProviderImport>,
    trace: BindingTrace
  ) {
    if (imports.isEmpty()) return

    imports
      .filter { it.importPath != null }
      .groupBy { it.importPath }
      .filter { it.value.size > 1 }
      .forEach { (_, imports) ->
        imports.forEach {
          trace.report(
            InjektErrors.DUPLICATED_INJECTABLE_IMPORT
              .on(it.element!!)
          )
        }
      }

    imports.forEach { import ->
      val (element, importPath) = import
      if (!import.isValidImport()) {
        trace.report(
          InjektErrors.MALFORMED_INJECTABLE_IMPORT
            .on(element!!)
        )
        return@forEach
      }
      importPath!!
      if (importPath.endsWith(".*")) {
        val packageFqName = FqName(importPath.removeSuffix(".*"))
        if (packageFqName == currentPackage) {
          trace.report(
            InjektErrors.DECLARATION_PACKAGE_INJECTABLE_IMPORT
              .on(element!!)
          )
          return@forEach
        }
        if (context.memberScopeForFqName(packageFqName, import.element.lookupLocation) == null) {
          trace.report(
            InjektErrors.UNRESOLVED_INJECTABLE_IMPORT
              .on(element!!)
          )
          return@forEach
        }
      } else {
        val fqName = FqName(importPath.removeSuffix(".*"))
        val parentFqName = fqName.parent()
        if (parentFqName == currentPackage) {
          trace.report(
            InjektErrors.DECLARATION_PACKAGE_INJECTABLE_IMPORT
              .on(element!!)
          )
          return@forEach
        }
        val shortName = fqName.shortName()
        val importedDeclarations = context.memberScopeForFqName(parentFqName,
          import.element.lookupLocation)
          ?.getContributedDescriptors()
          ?.filter {
            it !is PackageViewDescriptor &&
                (it.name == shortName ||
                    (it is ClassConstructorDescriptor &&
                        it.constructedClass.name == shortName))
          }
        if (importedDeclarations == null || importedDeclarations.isEmpty()) {
          trace.report(
            InjektErrors.UNRESOLVED_INJECTABLE_IMPORT
              .on(element!!)
          )
          return@forEach
        }
      }

      if (!isIde) {
        trace.report(
          InjektErrors.UNUSED_INJECTABLE_IMPORT
            .on(element!!)
        )
      }
    }
  }
}
