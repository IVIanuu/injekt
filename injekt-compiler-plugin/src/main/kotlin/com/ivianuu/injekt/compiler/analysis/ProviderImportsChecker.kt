/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.com.intellij.psi.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.calls.checkers.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*

class ProviderImportsChecker(private val baseCtx: Context) : CallChecker {
  override fun check(
    resolvedCall: ResolvedCall<*>,
    reportOn: PsiElement,
    context: CallCheckerContext
  ) {
    val resulting = resolvedCall.resultingDescriptor
    if (resulting !is ConstructorDescriptor ||
        resulting.constructedClass.fqNameSafe != InjektFqNames.Providers)
          return
    val ctx = baseCtx.withTrace(context.trace)
    val imports = resolvedCall.getProviderImports()

    imports
      .filter { it.importPath != null }
      .groupBy { it.importPath }
      .filter { it.value.size > 1 }
      .forEach { (_, imports) ->
        for (import in imports) {
          ctx.trace!!.report(
            InjektErrors.DUPLICATED_INJECTABLE_IMPORT
              .on(import.element!!, import.element)
          )
        }
      }

    val currentPackage = context.scope.ownerDescriptor.findPackage().fqName

    for (import in imports) {
      val (element, importPath) = import
      if (!import.isValidImport()) {
        ctx.trace!!.report(
          InjektErrors.MALFORMED_INJECTABLE_IMPORT
            .on(element!!, element)
        )
        continue
      }
      importPath!!
      if (importPath.endsWith("*")) {
        val packageFqName = FqName(importPath.removeSuffix(".**").removeSuffix(".*"))
        if (packageFqName == currentPackage) {
          ctx.trace!!.report(
            InjektErrors.DECLARATION_PACKAGE_INJECTABLE_IMPORT
              .on(element!!, element)
          )
          continue
        }
        if (memberScopeForFqName(packageFqName, import.element.lookupLocation, ctx) == null) {
          ctx.trace!!.report(
            InjektErrors.UNRESOLVED_INJECTABLE_IMPORT
              .on(element!!, element)
          )
          continue
        }
      } else {
        val fqName = FqName(importPath.removeSuffix(".*"))
        val parentFqName = fqName.parent()
        if (parentFqName == currentPackage) {
          ctx.trace!!.report(
            InjektErrors.DECLARATION_PACKAGE_INJECTABLE_IMPORT
              .on(element!!, element)
          )
          continue
        }
        val shortName = fqName.shortName()
        val importedDeclarations = memberScopeForFqName(
          parentFqName,
          import.element.lookupLocation,
          ctx
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
          ctx.trace!!.report(
            InjektErrors.UNRESOLVED_INJECTABLE_IMPORT
              .on(element!!, element)
          )
          continue
        }
      }

      if (!isIde) {
        ctx.trace!!.report(
          InjektErrors.UNUSED_INJECTABLE_IMPORT
            .on(element!!, element)
        )
      }
    }
  }
}
