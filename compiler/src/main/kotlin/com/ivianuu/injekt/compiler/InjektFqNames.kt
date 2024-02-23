/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.name.*

object InjektFqNames {
  val InjektPackage = FqName("com.ivianuu.injekt")
  val AddOn = InjektPackage.child("AddOn".asNameId())
  val inject = InjektPackage.child("inject".asNameId())
  val Provide = InjektPackage.child("Provide".asNameId())
  val Tag = InjektPackage.child("Tag".asNameId())

  val InternalPackage = InjektPackage.child("internal".asNameId())
  val DeclarationInfo = InternalPackage.child("DeclarationInfo".asNameId())
  val TypeParameterInfo = InternalPackage.child("TypeParameterInfo".asNameId())

  val InjectablesPackage = InternalPackage.child("injectables".asNameId())
  val InjectablesLookup = InjectablesPackage.child("\$\$\$\$\$".asNameId())

  val Composable = FqName("androidx.compose.runtime.Composable")

  val Any = StandardNames.FqNames.any.toSafe()
  val Nothing = StandardNames.FqNames.nothing.toSafe()
  val Function = StandardNames.FqNames.functionSupertype.toSafe()
}
