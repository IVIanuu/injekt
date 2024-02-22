/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.calls.inference.components.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

data class CallableRef(
  val callable: CallableDescriptor,
  val type: KotlinType,
  val originalType: KotlinType,
  val parameterTypes: Map<Int, KotlinType>,
  val typeArguments: Map<TypeParameterDescriptor, TypeProjection>,
  val callableFqName: FqName,
  val injectParameters: Set<Int>
)

fun CallableRef.substitute(substitutor: NewTypeSubstitutor): CallableRef {
  if (substitutor.isEmpty) return this
  return copy(
    type = type.substitute(substitutor),
    parameterTypes = parameterTypes.mapValues { it.value.substitute(substitutor) },
    typeArguments = typeArguments.mapValues {
      it.value.substitute { it.substitute(substitutor) }
    }
  )
}

fun CallableDescriptor.toCallableRef(ctx: Context): CallableRef =
  ctx.cached("callable_ref", this) {
    val info = callableInfo(ctx)

    CallableRef(
      callable = this,
      type = info.type,
      originalType = info.type,
      parameterTypes = info.parameterTypes,
      typeArguments = buildMap {
        for (typeParameter in typeParameters)
          this[typeParameter] = typeParameter.defaultType.prepareForInjekt().asTypeProjection()
      },
      callableFqName = safeAs<ConstructorDescriptor>()?.constructedClass?.fqNameSafe ?:
      safeAs<LambdaInjectable.ParameterDescriptor>()?.let {
        it.lambdaInjectable.callableFqName.child(it.name)
      } ?: safeAs<ReceiverParameterDescriptor>()?.fqNameSafe?.parent() ?:
      fqNameSafe,
      injectParameters = info.injectParameters
    )
  }
