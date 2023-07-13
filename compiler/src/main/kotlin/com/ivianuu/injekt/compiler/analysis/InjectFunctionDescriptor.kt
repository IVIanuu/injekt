/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.allParametersWithContext
import com.ivianuu.injekt.compiler.injektName
import com.ivianuu.injekt.compiler.resolution.isInject
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast

interface InjectFunctionDescriptor : FunctionDescriptor {
  val underlyingDescriptor: FunctionDescriptor
}

class InjectValueParameterDescriptor(
  parent: InjectFunctionDescriptor,
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
    underlyingDescriptor.isInject() || underlyingDescriptor.declaresDefaultValue()
  override fun declaresDefaultValue(): Boolean = declaresDefaultValue
}

val ValueParameterDescriptor.hasDefaultValueIgnoringInject: Boolean
  get() = (this as? InjectValueParameterDescriptor)?.underlyingDescriptor?.hasDefaultValue()
    ?: hasDefaultValue()

abstract class AbstractInjectFunctionDescriptor(
  final override val underlyingDescriptor: FunctionDescriptor,
  private val ctx: Context
) : InjectFunctionDescriptor {
  private val valueParams = underlyingDescriptor
      .valueParameters
      .mapTo(mutableListOf()) { valueParameter ->
        InjectValueParameterDescriptor(this, valueParameter, ctx)
      }

  @OptIn(UnsafeCastFunction::class)
  override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
    valueParams.cast()
}

fun FunctionDescriptor.toInjectFunctionDescriptor(ctx: Context): InjectFunctionDescriptor? {
  if (this is InjectFunctionDescriptor) return this
  if (this is JavaMethodDescriptor) return null
  if (allParametersWithContext.none { it.isInject() }) return null
  return when (this) {
    is ClassConstructorDescriptor -> InjectConstructorDescriptorImpl(this, ctx)
    is SimpleFunctionDescriptor -> InjectSimpleFunctionDescriptorImpl(this, ctx)
    else -> InjectFunctionDescriptorImpl(this, ctx)
  }
}

class InjectConstructorDescriptorImpl(
  underlyingDescriptor: ClassConstructorDescriptor,
  private val ctx: Context
) : AbstractInjectFunctionDescriptor(underlyingDescriptor, ctx),
  ClassConstructorDescriptor by underlyingDescriptor {
  override fun substitute(substitutor: TypeSubstitutor): ClassConstructorDescriptor =
    InjectConstructorDescriptorImpl(
      underlyingDescriptor
        .substitute(substitutor) as ClassConstructorDescriptor,
      ctx
    )

  override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
    super.getValueParameters()
}

class InjectFunctionDescriptorImpl(
  underlyingDescriptor: FunctionDescriptor,
  private val ctx: Context
) : AbstractInjectFunctionDescriptor(underlyingDescriptor, ctx),
  FunctionDescriptor by underlyingDescriptor {
  override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor =
    InjectFunctionDescriptorImpl(
      underlyingDescriptor.substitute(substitutor) as FunctionDescriptor,
      ctx
    )

  override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
    super.getValueParameters()
}

class InjectSimpleFunctionDescriptorImpl(
  underlyingDescriptor: SimpleFunctionDescriptor,
  private val ctx: Context
) : AbstractInjectFunctionDescriptor(underlyingDescriptor, ctx),
  SimpleFunctionDescriptor by underlyingDescriptor {
  override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor =
    InjectSimpleFunctionDescriptorImpl(
      underlyingDescriptor
        .substitute(substitutor) as SimpleFunctionDescriptor,
      ctx
    )

  override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
    super.getValueParameters()
}
