/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.name.*

object InjektFqNames {
  val InjektPackage = FqName("com.ivianuu.injekt")
  val AddOn = ClassId(InjektPackage, "AddOn".asNameId())
  val inject = CallableId(InjektPackage, "inject".asNameId())
  val Provide = ClassId(InjektPackage, "Provide".asNameId())
  val Tag = ClassId(InjektPackage, "Tag".asNameId())

  val InternalPackage = InjektPackage.child("internal".asNameId())
  val DeclarationInfo = ClassId(InternalPackage, "DeclarationInfo".asNameId())

  val InjectablesPackage = InternalPackage.child("injectables".asNameId())
  val InjectablesLookup = CallableId(InjectablesPackage, "\$\$\$\$\$".asNameId())

  val Composable = ClassId.topLevel(FqName("androidx.compose.runtime.Composable"))

  val Any = StandardNames.FqNames.any.toSafe()
  val Nothing = StandardNames.FqNames.nothing.toSafe()
}
