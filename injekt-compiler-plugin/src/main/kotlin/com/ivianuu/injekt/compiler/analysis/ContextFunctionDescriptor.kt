/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.allParametersWithContext
import com.ivianuu.injekt.compiler.injektName
import com.ivianuu.injekt.compiler.resolution.isContext
import org.jetbrains.kotlin.builtins.contextFunctionTypeParamsCount
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ContextReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.utils.addToStdlib.cast

interface ContextFunctionDescriptor : FunctionDescriptor {
  val underlyingDescriptor: FunctionDescriptor
}

class ContextValueParameterDescriptor(
  parent: ContextFunctionDescriptor,
  val underlyingDescriptor: ValueParameterDescriptor,
  val ctx: Context
) : ValueParameterDescriptorImpl(
  parent,
  underlyingDescriptor,
  underlyingDescriptor.index,
  underlyingDescriptor.annotations,
  underlyingDescriptor.injektName(ctx),
  underlyingDescriptor.type,
  false,
  underlyingDescriptor.isCrossinline,
  underlyingDescriptor.isNoinline,
  underlyingDescriptor.varargElementType,
  underlyingDescriptor.source
) {
  private val declaresDefaultValue =
    underlyingDescriptor.isContext(ctx) || underlyingDescriptor.declaresDefaultValue()
  override fun declaresDefaultValue(): Boolean = declaresDefaultValue
}

val ValueParameterDescriptor.hasDefaultValueIgnoringContext: Boolean
  get() = (this as? ContextValueParameterDescriptor)?.underlyingDescriptor?.hasDefaultValue()
    ?: hasDefaultValue()

abstract class AbstractContextFunctionDescriptor(
  final override val underlyingDescriptor: FunctionDescriptor,
  private val ctx: Context
) : ContextFunctionDescriptor {
  private val valueParams = underlyingDescriptor
      .valueParameters
      .mapTo(mutableListOf()) { valueParameter ->
        ContextValueParameterDescriptor(this, valueParameter, ctx)
      }

  override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
    valueParams.cast()
}

fun FunctionDescriptor.toContextFunctionDescriptor(
  dispatchReceiverType: KotlinType?,
  ctx: Context
): ContextFunctionDescriptor? {
  if (this is ContextFunctionDescriptor) return this
  if (this is JavaMethodDescriptor) return null
  if (allParametersWithContext.none { it.isContext(ctx) } &&
      dispatchReceiverType?.contextFunctionTypeParamsCount().let {
        it == null || it == 0
      }) return null
  return when (this) {
    is ClassConstructorDescriptor -> ContextConstructorDescriptorImpl(this, ctx)
    is SimpleFunctionDescriptor -> ContextSimpleFunctionDescriptorImpl(
      this,
      dispatchReceiverType?.contextFunctionTypeParamsCount()?.takeIf { it > 0 },
      ctx
    )
    else -> ContextFunctionDescriptorImpl(
      this,
      dispatchReceiverType?.contextFunctionTypeParamsCount()?.takeIf { it > 0 },
      ctx
    )
  }
}

class ContextConstructorDescriptorImpl(
  underlyingDescriptor: ClassConstructorDescriptor,
  private val ctx: Context
) : AbstractContextFunctionDescriptor(underlyingDescriptor, ctx),
  ClassConstructorDescriptor by underlyingDescriptor {
  override fun substitute(substitutor: TypeSubstitutor): ClassConstructorDescriptor =
    ContextConstructorDescriptorImpl(
      underlyingDescriptor
        .substitute(substitutor) as ClassConstructorDescriptor,
      ctx
    )

  override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
    super.getValueParameters()
}

class ContextFunctionDescriptorImpl(
  underlyingDescriptor: FunctionDescriptor,
  private val contextFunctionTypeParametersCount: Int?,
  private val ctx: Context
) : AbstractContextFunctionDescriptor(underlyingDescriptor, ctx),
  FunctionDescriptor by underlyingDescriptor {
  override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor =
    ContextFunctionDescriptorImpl(
      underlyingDescriptor.substitute(substitutor) as FunctionDescriptor,
      contextFunctionTypeParametersCount,
      ctx
    )

  private val contextParams = if (contextFunctionTypeParametersCount != null)
    underlyingDescriptor.valueParameters.take(contextFunctionTypeParametersCount)
      .map {
        ReceiverParameterDescriptorImpl(
          this,
          ContextReceiver(this, it.type, null),
          Annotations.EMPTY
        )
      } else underlyingDescriptor.contextReceiverParameters

  override fun getContextReceiverParameters(): List<ReceiverParameterDescriptor> =
    contextParams

  private val valueParams = super.getValueParameters()
    .drop(contextFunctionTypeParametersCount ?: 0)
    .mapTo(mutableListOf()) {
      if (contextFunctionTypeParametersCount == null) it
      else ValueParameterDescriptorImpl(
        it.containingDeclaration,
        null,
        it.index - contextFunctionTypeParametersCount,
        it.annotations,
        it.name,
        it.type,
        it.declaresDefaultValue(),
        it.isCrossinline,
        it.isNoinline,
        it.varargElementType,
        it.source
      )
    }

  override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
    valueParams
}

class ContextSimpleFunctionDescriptorImpl(
  underlyingDescriptor: SimpleFunctionDescriptor,
  private val contextFunctionTypeParametersCount: Int?,
  private val ctx: Context
) : AbstractContextFunctionDescriptor(underlyingDescriptor, ctx),
  SimpleFunctionDescriptor by underlyingDescriptor {
  override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor =
    ContextSimpleFunctionDescriptorImpl(
      underlyingDescriptor
        .substitute(substitutor) as SimpleFunctionDescriptor,
      contextFunctionTypeParametersCount,
      ctx
    )

  private val contextParams = if (contextFunctionTypeParametersCount != null)
    underlyingDescriptor.valueParameters.take(contextFunctionTypeParametersCount)
      .map {
        ReceiverParameterDescriptorImpl(
          this,
          ContextReceiver(this, it.type, null),
          Annotations.EMPTY
        )
      } else underlyingDescriptor.contextReceiverParameters

  override fun getContextReceiverParameters(): List<ReceiverParameterDescriptor> =
    contextParams

  private val valueParams = super.getValueParameters()
    .drop(contextFunctionTypeParametersCount ?: 0)
    .mapTo(mutableListOf()) {
      if (contextFunctionTypeParametersCount == null) it
      else ValueParameterDescriptorImpl(
        it.containingDeclaration,
        null,
        it.index - contextFunctionTypeParametersCount,
        it.annotations,
        it.name,
        it.type,
        it.declaresDefaultValue(),
        it.isCrossinline,
        it.isNoinline,
        it.varargElementType,
        it.source
      )
    }

  override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
    valueParams
}
