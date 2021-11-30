/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.name.*

class InjektFqNames(injektPackage: FqName) {
  val inject = injektPackage.child("Inject".asNameId())
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
