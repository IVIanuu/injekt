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
import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.isIde
import com.ivianuu.injekt.compiler.lookupLocation
import com.ivianuu.injekt.compiler.memberScopeForFqName
import com.ivianuu.injekt.compiler.resolution.getProviderImports
import com.ivianuu.injekt.compiler.resolution.isValidImport
import com.ivianuu.injekt.compiler.trace
import com.ivianuu.shaded_injekt.Inject
import com.ivianuu.shaded_injekt.Provide
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ProviderImportsChecker(@Inject private val baseCtx: Context) : CallChecker {
  override fun check(
    resolvedCall: ResolvedCall<*>,
    reportOn: PsiElement,
    context: CallCheckerContext
  ) {
    val resulting = resolvedCall.resultingDescriptor
    if (resulting !is ConstructorDescriptor ||
        resulting.constructedClass.fqNameSafe != injektFqNames().providers)
          return
    @Provide val ctx = baseCtx.withTrace(context.trace)
    val imports = resolvedCall.getProviderImports()

    imports
      .filter { it.importPath != null }
      .groupBy { it.importPath }
      .filter { it.value.size > 1 }
      .forEach { (_, imports) ->
        for (import in imports) {
          trace()!!.report(
            InjektErrors.DUPLICATED_INJECTABLE_IMPORT
              .on(import.element!!, import.element)
          )
        }
      }

    val currentPackage = context.scope.ownerDescriptor.findPackage().fqName

    for (import in imports) {
      val (element, importPath) = import
      if (!import.isValidImport()) {
        trace()!!.report(
          InjektErrors.MALFORMED_INJECTABLE_IMPORT
            .on(element!!, element)
        )
        continue
      }
      importPath!!
      if (importPath.endsWith("*")) {
        val packageFqName = FqName(importPath.removeSuffix(".**").removeSuffix(".*"))
        if (packageFqName == currentPackage) {
          trace()!!.report(
            InjektErrors.DECLARATION_PACKAGE_INJECTABLE_IMPORT
              .on(element!!, element)
          )
          continue
        }
        if (memberScopeForFqName(packageFqName, import.element.lookupLocation) == null) {
          trace()!!.report(
            InjektErrors.UNRESOLVED_INJECTABLE_IMPORT
              .on(element!!, element)
          )
          continue
        }
      } else {
        val fqName = FqName(importPath.removeSuffix(".*"))
        val parentFqName = fqName.parent()
        if (parentFqName == currentPackage) {
          trace()!!.report(
            InjektErrors.DECLARATION_PACKAGE_INJECTABLE_IMPORT
              .on(element!!, element)
          )
          continue
        }
        val shortName = fqName.shortName()
        val importedDeclarations = memberScopeForFqName(
          parentFqName,
          import.element.lookupLocation
        )
          ?.first
          ?.getContributedDescriptors()
          ?.filter {
            it !is PackageViewDescriptor &&
                (it.name == shortName ||
                    (it is ClassConstructorDescriptor &&
                        it.constructedClass.name == shortName))
          }
        if (importedDeclarations == null || importedDeclarations.isEmpty()) {
          trace()!!.report(
            InjektErrors.UNRESOLVED_INJECTABLE_IMPORT
              .on(element!!, element)
          )
          continue
        }
      }

      if (!isIde) {
        trace()!!.report(
          InjektErrors.UNUSED_INJECTABLE_IMPORT
            .on(element!!, element)
        )
      }
    }
  }
}
