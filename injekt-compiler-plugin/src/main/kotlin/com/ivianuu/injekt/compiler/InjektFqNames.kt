/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.name.*

object InjektFqNames {
  val InjektPackage = FqName("com.ivianuu.injekt")
  val inject = InjektPackage.child("inject".asNameId())
  val Provide = InjektPackage.child("Provide".asNameId())

  val InternalPackage = InjektPackage.child("internal".asNameId())
  val Index = InternalPackage.child("Index".asNameId())
  val IndicesPackage = InternalPackage.child("indices".asNameId())

  val Any = FqName("kotlin.Any")
  val Composable = FqName("androidx.compose.runtime.Composable")
}
