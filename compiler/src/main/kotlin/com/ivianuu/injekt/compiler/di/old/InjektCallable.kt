/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.di.old

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

data class InjektCallable(
  val callable: CallableDescriptor,
  val type: InjektType,
  val originalType: InjektType,
  val typeParameters: List<InjektClassifier>,
  val parameterTypes: Map<Int, InjektType>,
  val typeArguments: Map<InjektClassifier, InjektType>,
  val callableFqName: FqName
)

fun InjektCallable.substitute(map: Map<InjektClassifier, InjektType>): InjektCallable {
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

fun CallableDescriptor.toInjektCallable(ctx: Context): InjektCallable =
  ctx.cache.cached("callable_ref", this) {
    val type = run {
      val tags = if (this is ConstructorDescriptor)
        buildList {
          addAll(constructedClass.toInjektClassifier(ctx).tags)
          for (tagAnnotation in getTags())
            add(tagAnnotation.type.toTypeRef(ctx))
        }
      else emptyList()
      tags.wrap(returnType?.toTypeRef(ctx) ?: ctx.nullableAnyType)
    }

    val parameterTypes = buildMap {
      for (parameter in allParameters)
        this[parameter.injektIndex()] = parameter.type.toTypeRef(ctx)
    }
    val typeParameters = typeParameters.map { it.toInjektClassifier(ctx) }

    InjektCallable(
      callable = this,
      type = type,
      originalType = type,
      typeParameters = typeParameters,
      parameterTypes = parameterTypes,
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
