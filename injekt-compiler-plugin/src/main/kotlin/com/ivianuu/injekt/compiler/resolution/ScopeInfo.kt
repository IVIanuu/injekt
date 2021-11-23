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

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.shaded_injekt.Inject
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

data class ScopeInfo(val scopeComponent: TypeRef, val isEager: Boolean)

fun ScopeInfo.substitute(map: Map<ClassifierRef, TypeRef>): ScopeInfo =
  copy(scopeComponent = scopeComponent.substitute(map), isEager = isEager)

fun Annotated.scopeInfo(@Inject ctx: Context): ScopeInfo? {
  val scopeAnnotation = (annotations.findAnnotation(injektFqNames().scoped) ?:
  safeAs<ConstructorDescriptor>()?.constructedClass?.annotations?.findAnnotation(injektFqNames().scoped))
  ?: return null

  return ScopeInfo(
    scopeAnnotation.type.arguments.single().type.toTypeRef(),
    scopeAnnotation.allValueArguments.values.singleOrNull()?.value == true
  )
}
