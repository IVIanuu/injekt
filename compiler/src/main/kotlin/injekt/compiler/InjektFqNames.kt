/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.compiler

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.name.*

object InjektFqNames {
  val InjektPackage = FqName("injekt")
  val AddOn = ClassId(InjektPackage, "AddOn".asNameId())
  val inject = CallableId(InjektPackage, "inject".asNameId())
  val Provide = ClassId(InjektPackage, "Provide".asNameId())
  val Tag = ClassId(InjektPackage, "Tag".asNameId())

  val CommonPackage = InjektPackage.child("common".asNameId())
  val SourceKey = ClassId(CommonPackage, "SourceKey".asNameId())
  val TypeKey = ClassId(CommonPackage, "TypeKey".asNameId())

  val InternalPackage = InjektPackage.child("internal".asNameId())
  val InjektMetadata = ClassId(InternalPackage, "InjektMetadata".asNameId())

  val InjectablesPackage = InternalPackage.child("injectables".asNameId())
  val InjectablesLookup = CallableId(InjectablesPackage, "\$\$\$\$\$".asNameId())

  val Composable = ClassId.topLevel(FqName("androidx.compose.runtime.Composable"))

  val Any = StandardClassIds.Any
  val Nothing = StandardClassIds.Nothing

  val function = StandardClassIds.Function.asFqNameString()
  val kFunction = StandardClassIds.KFunction.asFqNameString()
  val suspendFunction = StandardClassIds.BASE_COROUTINES_PACKAGE.child("SuspendFunction".asNameId()).asString()
  val kSuspendFunction = StandardClassIds.BASE_COROUTINES_PACKAGE.child("KSuspendFunction".asNameId()).asString()
  val composableFunction = FqName("androidx.compose.runtime.internal.ComposableFunction").asString()
  val kComposableFunction = FqName("androidx.compose.runtime.internal.KComposableFunction").asString()
  val Target = ClassId.topLevel(StandardNames.FqNames.target)
}
