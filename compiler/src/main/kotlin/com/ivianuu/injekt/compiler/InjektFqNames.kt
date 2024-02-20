/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.name.*

object InjektFqNames {
  val InjektPackage = FqName("com.ivianuu.injekt")
  val inject = CallableId(InjektPackage, "inject".asNameId())
  val Provide = ClassId(InjektPackage, "Provide".asNameId())
  val Tag = ClassId(InjektPackage, "Tag".asNameId())
  val Spread = ClassId(InjektPackage, "Spread".asNameId())

  private val InternalPackage = InjektPackage.child("internal".asNameId())
  val InjectablesPackage = InternalPackage.child("injectables".asNameId())
  val InjectablesLookup = CallableId(InjectablesPackage, "\$\$\$\$\$".asNameId())

  val CommonPackage = InjektPackage.child("common".asNameId())
  val TypeKey = ClassId(CommonPackage, "TypeKey".asNameId())

  val Composable = ClassId(FqName("androidx.compose.runtime"), "Composable".asNameId())
}

fun String.asNameId() = Name.identifier(this)
