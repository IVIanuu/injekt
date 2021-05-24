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

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.name.*

object InjektFqNames {
  val InjektPackage = FqName("com.ivianuu.injekt")

  val DefaultOnAllErrors = InjektPackage.child("DefaultOnAllErrors".asNameId())
  val IgnoreElementsWithErrors = InjektPackage.child("IgnoreElementsWithErrors".asNameId())
  val Inject = InjektPackage.child("Inject".asNameId())
  val Provide = InjektPackage.child("Provide".asNameId())
  val Providers = InjektPackage.child("Providers".asNameId())
  val Qualifier = InjektPackage.child("Qualifier".asNameId())
  val Spread = InjektPackage.child("Spread".asNameId())
  val withProviders = InjektPackage.child("withProviders".asNameId())

  val InternalPackage = InjektPackage.child("internal".asNameId())
  val CallableInfo = InternalPackage.child("CallableInfo".asNameId())
  val ClassifierInfo = InternalPackage.child("ClassifierInfo".asNameId())
  val TypeParameterInfos = InternalPackage.child("TypeParameterInfos".asNameId())

  val CommonPackage = InjektPackage.child("common".asNameId())
  val TypeKey = CommonPackage.child("TypeKey".asNameId())

  val Composable = FqName("androidx.compose.runtime.Composable")

  val Any = StandardNames.FqNames.any.toSafe()
  val Nothing = StandardNames.FqNames.nothing.toSafe()
}
