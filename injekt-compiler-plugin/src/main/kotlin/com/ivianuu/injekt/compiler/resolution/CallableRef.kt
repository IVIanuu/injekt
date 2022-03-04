/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.calls.inference.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*

data class CallableRef(
  val callable: CallableDescriptor,
  val type: KotlinType,
  val originalType: KotlinType,
  val parameterTypes: Map<Int, KotlinType>,
  val typeArguments: Map<TypeParameterDescriptor, TypeProjection>,
  val import: ResolvedProviderImport?,
  val chainLength: Int
)

fun CallableRef.substitute(substitutor: TypeSubstitutor): CallableRef {
  if (substitutor.isEmpty) return this
  return copy(
    type = substitutor.substitute(type.unwrap()),
    parameterTypes = parameterTypes
      .mapValues { substitutor.substitute(it.value.unwrap()) },
    typeArguments = typeArguments
      .mapValues { substitutor.substitute(it.value) ?: it.value }
  )
}

@OptIn(ExperimentalStdlibApi::class)
fun CallableDescriptor.toCallableRef(ctx: Context): CallableRef =
  ctx.trace!!.getOrPut(InjektWritableSlices.CALLABLE_REF, this) {
    val type = returnType ?: ctx.module.builtIns.nullableAnyType

    CallableRef(
      callable = this,
      type = type,
      originalType = type,
      parameterTypes = buildMap {
        for (parameter in allParameters)
          this[parameter.injektIndex()] = parameter.type
      },
      typeArguments = buildMap {
        for (typeParameter in typeParameters)
          this[typeParameter] = typeParameter.defaultType.asTypeProjection()
      },
      import = null,
      chainLength = 0
    )
  }
