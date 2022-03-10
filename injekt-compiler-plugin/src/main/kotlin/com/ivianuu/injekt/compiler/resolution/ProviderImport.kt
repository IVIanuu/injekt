/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.model.*

data class ProviderImport(val element: KtElement?, val importPath: String?)

data class ResolvedProviderImport(
  val element: KtElement?,
  val importPath: String?,
  val packageFqName: FqName
)

fun ProviderImport.toResolvedImport(packageFqName: FqName) = ResolvedProviderImport(
  element, importPath, packageFqName
)

fun ProviderImport.resolve(ctx: Context): ResolvedProviderImport? {
  if (!isValidImport()) return null
  val packageFqName: FqName = if (importPath!!.endsWith("*")) {
    val packageFqName = FqName(importPath.removeSuffix(".**").removeSuffix(".*"))
    val objectForFqName = classifierDescriptorForFqName(
      packageFqName,
      element?.let { KotlinLookupLocation(it) } ?: NoLookupLocation.FROM_BACKEND,
      ctx
    )
    objectForFqName?.findPackage()?.fqName ?: packageFqName
  } else {
    val fqName = FqName(importPath)
    val parentFqName = fqName.parent()
    val objectForFqName = classifierDescriptorForFqName(parentFqName, NoLookupLocation.FROM_BACKEND, ctx)
    objectForFqName?.findPackage()?.fqName ?: parentFqName
  }

  return toResolvedImport(packageFqName)
}

fun KtAnnotated.getProviderImports(): List<ProviderImport> =
  annotationEntries.firstOrNull { it.shortName == InjektFqNames.Providers.shortName() }
    ?.valueArguments
    ?.map { it.toProviderImport() } ?: emptyList()

fun ResolvedCall<*>.getProviderImports(): List<ProviderImport> =
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
