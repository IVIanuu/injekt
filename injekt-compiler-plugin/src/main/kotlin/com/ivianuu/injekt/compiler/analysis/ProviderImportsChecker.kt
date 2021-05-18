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
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.com.intellij.psi.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.checkers.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.checkers.*
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.constants.evaluate.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class ProviderImportsChecker(private val context: InjektContext) : DeclarationChecker, CallChecker {
  private val checkedFiles = mutableSetOf<KtFile>()
  override fun check(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext
  ) {
    val file = declaration.containingKtFile
    checkFile(file, context.trace)
    if (!declaration.hasAnnotation(InjektFqNames.Providers)) return
    val outerImports = file.getProviderImports() + descriptor.parents
      .distinct()
      .flatMap { parent ->
        parent.findPsi()
          .safeAs<KtAnnotated>()
          ?.getProviderImports() ?: emptyList()
      }
      .toList()
    checkImports(
      file.packageFqName, outerImports,
      declaration.getProviderImports(), context.trace
    )
  }

  override fun check(
    resolvedCall: ResolvedCall<*>,
    reportOn: PsiElement,
    context: CallCheckerContext
  ) {
    if (resolvedCall.resultingDescriptor.fqNameSafe != InjektFqNames.withProviders) return

    val file = context.scope
      .ownerDescriptor
      .findPsi()!!
      .cast<KtElement>()
      .containingKtFile

    val outerImports = file.getProviderImports() + context.scope.parentsWithSelf
      .filterIsInstance<LexicalScope>()
      .distinctBy { it.ownerDescriptor }
      .flatMap { scope ->
        scope.ownerDescriptor.findPsi()
          .safeAs<KtAnnotated>()
          ?.getProviderImports() ?: emptyList()
      }
      .toList()

    resolvedCall.valueArguments
      .values
      .firstOrNull()
      ?.arguments
      ?.map { it.toProviderImport() }
      ?.let {
        checkImports(file.packageFqName, outerImports, it, context.trace)
      }
  }

  private fun checkFile(file: KtFile, trace: BindingTrace) {
    if (file in checkedFiles) return
    checkedFiles += file
    checkImports(file.packageFqName, emptyList(), file.getProviderImports(), trace)
  }

  private fun checkImports(
    currentPackage: FqName,
    outerImports: List<ProviderImport>,
    imports: List<ProviderImport>,
    trace: BindingTrace
  ) {
    if (imports.isEmpty()) return

    imports
      .filter { it.importPath != null }
      .filter { import ->
        outerImports.any {
          it.importPath == import.importPath
        }
      }
      .forEach { (element, _) ->
        trace.report(
          InjektErrors.DUPLICATED_INJECTABLE_IMPORT
            .on(element!!)
        )
      }

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
      if (importPath == null || importPath
          .any {
            !it.isLetter() &&
                it != '.' &&
                it != '_' &&
                it != '*'
          }
      ) {
        trace.report(
          InjektErrors.MALFORMED_INJECTABLE_IMPORT
            .on(element!!)
        )
        return@forEach
      }
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

      trace.report(
        InjektErrors.UNUSED_INJECTABLE_IMPORT
          .on(element!!)
      )
    }
  }
}
