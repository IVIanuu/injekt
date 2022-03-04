/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.name.*

object InjektFqNames {
  val InjektPackage = FqName("com.ivianuu.injekt")
  val Inject = InjektPackage.child("Inject".asNameId())
  val Provide = InjektPackage.child("Provide".asNameId())
  val Providers = InjektPackage.child("Providers".asNameId())

  val InternalPackage = InjektPackage.child("internal".asNameId())
  val DeclaresInjectables = InternalPackage.child("DeclaresInjectables".asNameId())
  val Index = InternalPackage.child("Index".asNameId())
  val PrimaryConstructorPropertyParameters = InternalPackage.child("PrimaryConstructorPropertyParameters".asNameId())
  val IndicesPackage = InternalPackage.child("indices".asNameId())

  val CommonPackage = InjektPackage.child("common".asNameId())
  val SourceKey = CommonPackage.child("SourceKey".asNameId())
  val TypeKey = CommonPackage.child("TypeKey".asNameId())

  val Composable = FqName("androidx.compose.runtime.Composable")

  val Any = StandardNames.FqNames.any.toSafe()
  val Nothing = StandardNames.FqNames.nothing.toSafe()
}
