/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*

data class CallableRef(
  val callable: CallableDescriptor,
  val type: TypeRef,
  val originalType: TypeRef,
  val typeParameters: List<ClassifierRef>,
  val parameterTypes: Map<Int, TypeRef>,
  val typeArguments: Map<ClassifierRef, TypeRef>,
  val import: ResolvedProviderImport?,
  val chainLength: Int
)

fun CallableRef.substitute(
  map: Map<ClassifierRef, TypeRef>,
  ctx: Context
): CallableRef {
  if (map == typeArguments) return this
  val substitutedTypeParameters = typeParameters.substitute(map)
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
    typeParameters = substitutedTypeParameters,
    typeArguments = typeArguments
      .mapValues {
        it.value
          .substitute(map)
          .substitute(typeParameterSubstitutionMap)
      }
  )
}

@OptIn(ExperimentalStdlibApi::class)
fun CallableDescriptor.toCallableRef(ctx: Context): CallableRef =
  ctx.trace!!.getOrPut(InjektWritableSlices.CALLABLE_REF, this) {
    val info = callableInfo(ctx)
    val typeParameters = typeParameters.map { it.toClassifierRef(ctx) }

    CallableRef(
      callable = this,
      type = info.type,
      originalType = info.type,
      typeParameters = typeParameters,
      parameterTypes = info.parameterTypes,
      typeArguments = buildMap {
        for (typeParameter in typeParameters)
          this[typeParameter] = typeParameter.defaultType
      },
      import = null,
      chainLength = 0
    )
  }
