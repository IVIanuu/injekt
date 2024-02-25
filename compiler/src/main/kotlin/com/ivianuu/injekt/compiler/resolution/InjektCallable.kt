/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.frontend.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.*

data class InjektCallable(
  val symbol: FirCallableSymbol<*>,
  val type: InjektType,
  val originalType: InjektType,
  val parameterTypes: Map<Int, InjektType>,
  val typeArguments: Map<InjektClassifier, InjektType>,
  val chainFqName: FqName,
  val injectParameters: Set<Int>
)

fun InjektCallable.substitute(map: Map<InjektClassifier, InjektType>): InjektCallable {
  if (map == typeArguments) return this
  val substitutedTypeParameters = typeArguments.keys.toList().substitute(map)
  val typeParameterSubstitutionMap = substitutedTypeParameters.associateWith {
    it.defaultType
  }
  return copy(
    type = type.substitute(map).substitute(typeParameterSubstitutionMap),
    parameterTypes = parameterTypes
      .mapValues {
        it.value
          .substitute(map)
          .substitute(typeParameterSubstitutionMap)
      },
    typeArguments = typeArguments
      .mapValues {
        it.value
          .substitute(map)
          .substitute(typeParameterSubstitutionMap)
      }
      .mapKeys { (typeParameter, _) ->
        substitutedTypeParameters.single { it.key == typeParameter.key }
      }
  )
}

fun FirCallableSymbol<*>.toInjektCallable(ctx: InjektContext, chainFqName: FqName = fqName): InjektCallable =
  ctx.cached("injekt_callable", this) {
    val info = callableInfo(ctx)
    InjektCallable(
      symbol = this,
      type = info.type,
      originalType = info.type,
      parameterTypes = info.parameterTypes,
      typeArguments = typeParameterSymbols.map { it.toInjektClassifier(ctx) }
        .associateWith { it.defaultType },
      chainFqName = chainFqName,
      injectParameters = info.injectParameters
    )
  }
