/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.classifierDescriptorForFqName
import com.ivianuu.injekt.compiler.findAnnotation
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.lookupLocation
import com.ivianuu.shaded_injekt.Inject
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

data class ProviderImport(val element: KtElement?, val importPath: String?)

data class ResolvedProviderImport(
  val element: KtElement?,
  val importPath: String?,
  val packageFqName: FqName
)

fun ProviderImport.toResolvedImport(packageFqName: FqName) = ResolvedProviderImport(
  element, importPath, packageFqName
)

fun ProviderImport.resolve(@Inject ctx: Context): ResolvedProviderImport? {
  if (!isValidImport()) return null
  val packageFqName: FqName = if (importPath!!.endsWith("*")) {
    val packageFqName = FqName(importPath.removeSuffix(".**").removeSuffix(".*"))
    val objectForFqName = classifierDescriptorForFqName(packageFqName, element.lookupLocation)
    objectForFqName?.findPackage()?.fqName ?: packageFqName
  } else {
    val fqName = FqName(importPath)
    val parentFqName = fqName.parent()
    val objectForFqName = classifierDescriptorForFqName(parentFqName, NoLookupLocation.FROM_BACKEND)
    objectForFqName?.findPackage()?.fqName ?: parentFqName
  }

  return toResolvedImport(packageFqName)
}

fun KtAnnotated.getProviderImports(@Inject ctx: Context): List<ProviderImport> =
  findAnnotation(injektFqNames().providers)
    ?.valueArguments
    ?.map { it.toProviderImport() } ?: emptyList()

fun ResolvedCall<*>.getProviderImports(@Inject ctx: Context): List<ProviderImport> =
  valueArguments
    .values
    .flatMap { it.arguments }
    .map { it.toProviderImport() }

fun ValueArgument.toProviderImport() = ProviderImport(
  getArgumentExpression(), getArgumentExpression()?.text?.removeSurrounding("\"")
)

fun ProviderImport.isValidImport() = importPath != null &&
    importPath.isNotEmpty() &&
    importPath
      .none {
        !it.isLetterOrDigit() &&
            it != '.' &&
            it != '_' &&
            it != '*'
      }
