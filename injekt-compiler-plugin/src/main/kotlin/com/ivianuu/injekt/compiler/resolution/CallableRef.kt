/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.cached
import com.ivianuu.injekt.compiler.callableInfo
import org.jetbrains.kotlin.descriptors.CallableDescriptor

data class CallableRef(
  val callable: CallableDescriptor,
  val type: TypeRef,
  val originalType: TypeRef,
  val typeParameters: List<ClassifierRef>,
  val parameterTypes: Map<Int, TypeRef>,
  val typeArguments: Map<ClassifierRef, TypeRef>,
  val chainLength: Int
)

fun CallableRef.substitute(map: Map<ClassifierRef, TypeRef>): CallableRef {
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

context(Context) fun CallableDescriptor.toCallableRef(): CallableRef =
  cached("callable_ref", this) {
    val info = callableInfo()
    val typeParameters = typeParameters.map { it.toClassifierRef() }

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
      chainLength = 0
    )
  }
