/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.compiler.resolution

import injekt.compiler.*
import injekt.compiler.fir.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.*

data class InjektCallable(
  val symbol: FirCallableSymbol<*>,
  val type: InjektType,
  val originalType: InjektType,
  val parameterTypes: Map<Name, InjektType>,
  val typeArguments: Map<InjektClassifier, InjektType>,
  val chainFqName: FqName,
  val injectParameters: Set<Name>,
  val callContext: CallContext
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

context(ctx: InjektContext)
fun FirCallableSymbol<*>.toInjektCallable(
  chainFqName: FqName = FqName(fqName.pathSegments().filter { !it.isSpecial }.joinToString("."))
): InjektCallable = cached("injekt_callable", this) {
  val metadata = callableMetadata()
  InjektCallable(
    symbol = this,
    type = metadata.type,
    originalType = metadata.type,
    parameterTypes = metadata.parameterTypes,
    typeArguments = typeParameterSymbols.map { it.toInjektClassifier() }
      .associateWith { it.defaultType },
    chainFqName = chainFqName,
    injectParameters = metadata.injectParameters,
    callContext = callContext()
  )
}
