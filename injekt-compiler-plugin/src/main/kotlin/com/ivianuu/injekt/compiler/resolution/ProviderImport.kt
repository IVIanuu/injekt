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

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*

data class ProviderImport(val element: KtElement?, val importPath: String?)

data class ResolvedProviderImport(
  val element: KtElement?,
  val importPath: String?,
  val packageFqName: FqName
)

fun ProviderImport.toResolvedImport(packageFqName: FqName) = ResolvedProviderImport(
  element, importPath, packageFqName
)

fun ProviderImport.resolve(context: InjektContext): ResolvedProviderImport? {
  if (!isValidImport()) return null
  val packageFqName: FqName = if (importPath!!.endsWith(".*")) {
    val packageFqName = FqName(importPath.removeSuffix(".*"))
    val objectForFqName = context.classifierDescriptorForFqName(packageFqName,
      element.lookupLocation)
    objectForFqName?.findPackage()?.fqName ?: packageFqName
  } else {
    val fqName = FqName(importPath)
    val parentFqName = fqName.parent()
    val objectForFqName = context.classifierDescriptorForFqName(
      parentFqName, NoLookupLocation.FROM_BACKEND)
    objectForFqName?.findPackage()?.fqName ?: parentFqName
  }

  return toResolvedImport(packageFqName)
}

fun KtAnnotated.getProviderImports(): List<ProviderImport> = findAnnotation(InjektFqNames.Providers)
  ?.valueArguments
  ?.map { it.toProviderImport() } ?: emptyList()

fun ValueArgument.toProviderImport() = ProviderImport(
  getArgumentExpression(), getArgumentExpression()?.text?.removeSurrounding("\"")
)

fun ProviderImport.isValidImport() = importPath != null && importPath.isNotEmpty() && importPath
  .none {
    !it.isLetterOrDigit() &&
        it != '.' &&
        it != '_' &&
        it != '*'
  }
