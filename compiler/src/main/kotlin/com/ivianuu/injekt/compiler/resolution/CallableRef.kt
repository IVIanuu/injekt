/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.analysis.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

data class CallableRef(
  val callable: CallableDescriptor,
  val type: TypeRef,
  val originalType: TypeRef,
  val parameterTypes: Map<Int, TypeRef>,
  val typeArguments: Map<ClassifierRef, TypeRef>,
  val callableFqName: FqName,
  val injectParameters: Set<Int>
)

fun CallableRef.substitute(map: Map<ClassifierRef, TypeRef>): CallableRef {
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

fun CallableDescriptor.toCallableRef(ctx: Context): CallableRef =
  ctx.cached("callable_ref", this) {
    val info = callableInfo(ctx)
    CallableRef(
      callable = this,
      type = info.type,
      originalType = info.type,
      parameterTypes = info.parameterTypes,
      typeArguments = typeParameters.map { it.toClassifierRef(ctx) }
        .associateWith { it.defaultType },
      callableFqName = safeAs<ConstructorDescriptor>()?.constructedClass?.fqNameSafe ?:
      safeAs<LambdaInjectable.ParameterDescriptor>()?.let {
        it.lambdaInjectable.callableFqName.child(it.name)
      } ?: safeAs<ReceiverParameterDescriptor>()?.fqNameSafe?.parent() ?:
      fqNameSafe,
      injectParameters = info.injectParameters
    )
  }
