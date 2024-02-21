/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.resolve.substitution.*
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*

data class InjektCallable(
  val callable: FirCallableSymbol<*>,
  val type: ConeKotlinType,
  val originalType: ConeKotlinType,
  val parameterTypes: Map<Int, ConeKotlinType>,
  val typeArguments: Map<FirTypeParameterSymbol, ConeKotlinType>,
  val chainFqName: FqName
)

fun InjektCallable.substitute(substitutor: ConeSubstitutor): InjektCallable {
  if (substitutor == ConeSubstitutor.Empty) return this
  return copy(
    type = substitutor.substituteOrSelf(type),
    parameterTypes = parameterTypes
      .mapValues { substitutor.substituteOrSelf(it.value) },
    typeArguments = typeArguments
      .mapValues { substitutor.substituteOrSelf(it.value) }
  )
}

fun FirCallableSymbol<*>.toInjektCallable(session: FirSession): InjektCallable {
  val parameterTypes = buildMap {
    dispatchReceiverType?.let { this[DISPATCH_RECEIVER_INDEX] = it.prepare(session) }
    receiverParameter?.let { this[EXTENSION_RECEIVER_INDEX] = it.typeRef.coneType.prepare(session) }
    if (this@toInjektCallable is FirFunctionSymbol<*>)
      valueParameterSymbols.forEachIndexed { index, parameterSymbol ->
        this[index] = try {
          parameterSymbol.resolvedReturnType.prepare(session)
        } catch (e: Throwable) {
          e.printStackTrace()
          throw e
        }
      }
  }

  val type = resolvedReturnType.prepare(session)

  return InjektCallable(
    callable = this,
    type = type,
    originalType = type,
    parameterTypes = parameterTypes,
    typeArguments = buildMap {
      for (typeParameter in typeParameterSymbols)
        this[typeParameter] = typeParameter.toConeType()
    },
    chainFqName = callableId.asSingleFqName()
  )
}

val DISPATCH_RECEIVER_NAME = Name.identifier("\$dispatchReceiver")
val EXTENSION_RECEIVER_NAME = Name.identifier("\$extensionReceiver")

const val DISPATCH_RECEIVER_INDEX = -2
const val EXTENSION_RECEIVER_INDEX = -1
