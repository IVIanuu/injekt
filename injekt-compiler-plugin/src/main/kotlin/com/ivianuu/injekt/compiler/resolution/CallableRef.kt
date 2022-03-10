/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.types.*

data class CallableRef(
  val callable: CallableDescriptor,
  val type: KotlinType,
  val originalType: KotlinType,
  val typeParameters: List<TypeParameterDescriptor>,
  val parameterTypes: Map<Int, KotlinType>,
  val typeArguments: Map<TypeParameterDescriptor, KotlinType>,
  val import: ResolvedProviderImport?,
  val chainLength: Int
)

fun CallableRef.substitute(map: Map<String, KotlinType>, ctx: Context): CallableRef {
  if (map.isEmpty()) return this
  return copy(
    type = type.substitute(map, ctx),
    parameterTypes = parameterTypes
      .mapValues { it.value.substitute(map, ctx) },
    typeArguments = typeArguments
      .mapValues { it.value.substitute(map, ctx) }
  )
}


@OptIn(ExperimentalStdlibApi::class)
fun CallableDescriptor.toCallableRef(ctx: Context): CallableRef {
  val info = callableInfo(ctx)
  val typeParameters = typeParameters.map { it.prepare() as TypeParameterDescriptor }
  return CallableRef(
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
