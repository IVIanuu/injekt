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

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.FqName

class InjektFqNames(injektPackage: FqName) {
  val ambiguousInjectable = injektPackage.child("AmbiguousInjectable".asNameId())
  val inject = injektPackage.child("Inject".asNameId())
  val injectableNotFound = injektPackage.child("InjectableNotFound".asNameId())
  val provide = injektPackage.child("Provide".asNameId())
  val providers = injektPackage.child("Providers".asNameId())
  val tag = injektPackage.child("Tag".asNameId())
  val spread = injektPackage.child("Spread".asNameId())

  val internalPackage = injektPackage.child("internal".asNameId())
  val callableInfo = internalPackage.child("CallableInfo".asNameId())
  val classifierInfo = internalPackage.child("ClassifierInfo".asNameId())
  val index = internalPackage.child("Index".asNameId())
  val typeParameterInfo = internalPackage.child("TypeParameterInfo".asNameId())
  val indicesPackage = internalPackage.child("indices".asNameId())

  val commonPackage = injektPackage.child("common".asNameId())
  val component = commonPackage.child("Component".asNameId())
  val componentObserver = commonPackage.child("ComponentObserver".asNameId())
  val conversion = commonPackage.child("Conversion".asNameId())
  val disposable = commonPackage.child("Disposable".asNameId())
  val entryPoint = commonPackage.child("EntryPoint".asNameId())
  val scoped = commonPackage.child("Scoped".asNameId())
  val sourceKey = commonPackage.child("SourceKey".asNameId())
  val typeKey = commonPackage.child("TypeKey".asNameId())

  val composable = FqName("androidx.compose.runtime.Composable")

  val any = StandardNames.FqNames.any.toSafe()
  val nothing = StandardNames.FqNames.nothing.toSafe()

  companion object {
    val Default = InjektFqNames(FqName("com.ivianuu.injekt"))
  }
}

fun String.combine(other: String) = this + other
