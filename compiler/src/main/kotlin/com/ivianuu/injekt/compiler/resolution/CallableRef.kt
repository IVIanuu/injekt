/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.cached
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

data class CallableRef(
  val callable: FirCallableDeclaration,
  val type: ConeKotlinType,
  val originalType: ConeKotlinType,
  val typeParameters: List<FirTypeParameterSymbol>,
  val parameterTypes: Map<Int, ConeKotlinType>,
  val typeArguments: Map<FirTypeParameterSymbol, ConeKotlinType>,
  val callableFqName: FqName
)

fun CallableRef.substitute(
  map: Map<FirTypeParameterSymbol, ConeKotlinType>,
  session: FirSession
): CallableRef {
  if (map == typeArguments) return this
  val substitutor = substitutorByMap(map, session)
  return copy(
    type = substitutor.substituteOrSelf(type),
    parameterTypes = parameterTypes.mapValues { substitutor.substituteOrSelf(it.value) },
    typeParameters = typeParameters,
    typeArguments = typeArguments.mapValues { substitutor.substituteOrSelf(it.value) }
  )
}

fun

fun FirCallableDeclaration.toCallableRef(session: FirSession): CallableRef =
  ctx.cached("callable_ref", this) {
    CallableRef(
      callable = this,
      type = returnTypeRef.coneType,
      originalType = returnTypeRef.coneType,
      typeParameters = typeParameters.map { it.symbol },
      parameterTypes = buildMap {
        receiverParameter
      },
      typeArguments = buildMap {
        for (typeParameter in typeParameters)
          this[typeParameter.symbol] = typeParameter.symbol.toConeType()
      },
      callableFqName = safeAs<FirConstructor>()?.getContainingClass(session)?.symbol?.classId?.asSingleFqName() ?:
      safeAs<LambdaInjectable.ParameterDescriptor>()?.let {
        it.lambdaInjectable.callableFqName.child(it.name)
      } ?: safeAs<ReceiverParameterDescriptor>()?.fqNameSafe?.parent() ?:
      symbol.callableId.asSingleFqName()
    )
  }
