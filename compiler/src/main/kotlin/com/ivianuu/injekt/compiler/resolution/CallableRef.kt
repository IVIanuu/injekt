/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.cached
import com.ivianuu.injekt.compiler.callableInfo
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

data class CallableRef(
  val callable: CallableDescriptor,
  val type: KotlinType,
  val originalType: KotlinType,
  val typeParameters: List<TypeParameterDescriptor>,
  val parameterTypes: Map<Int, KotlinType>,
  val typeArguments: Map<TypeParameterDescriptor, KotlinType>,
  val callableFqName: FqName
)

fun CallableRef.substitute(substitutor: TypeSubstitutor): CallableRef {
  if (substitutor.isEmpty) return this
  return copy(
    type = type.substitute(substitutor),
    parameterTypes = parameterTypes.mapValues { it.value.substitute(substitutor) },
    typeArguments = typeArguments.mapValues { it.value.substitute(substitutor) }
  )
}

fun CallableDescriptor.toCallableRef(ctx: Context): CallableRef =
  ctx.cached("callable_ref", this) {
    val info = callableInfo(ctx)
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
      callableFqName = safeAs<ConstructorDescriptor>()?.constructedClass?.fqNameSafe ?:
      safeAs<LambdaInjectable.ParameterDescriptor>()?.let {
        it.lambdaInjectable.callableFqName.child(it.name)
      } ?: safeAs<ReceiverParameterDescriptor>()?.fqNameSafe?.parent() ?:
      fqNameSafe
    )
  }
