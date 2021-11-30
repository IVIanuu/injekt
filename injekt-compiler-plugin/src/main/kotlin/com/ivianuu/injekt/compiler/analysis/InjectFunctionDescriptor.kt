/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import com.ivianuu.shaded_injekt.*
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.load.java.descriptors.*
import org.jetbrains.kotlin.resolve.calls.components.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.*

interface InjectFunctionDescriptor : FunctionDescriptor {
  val underlyingDescriptor: FunctionDescriptor
}

class InjectValueParameterDescriptor(
  parent: InjectFunctionDescriptor,
  val underlyingDescriptor: ValueParameterDescriptor,
  @Inject val ctx: Context
) : ValueParameterDescriptorImpl(
  parent,
  underlyingDescriptor,
  underlyingDescriptor.index,
  underlyingDescriptor.annotations,
  underlyingDescriptor.injektName(),
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
  @Inject private val ctx: Context
) : InjectFunctionDescriptor {
  private val valueParameters = underlyingDescriptor
      .valueParameters
      .mapTo(mutableListOf()) { valueParameter ->
        InjectValueParameterDescriptor(this, valueParameter)
      }

  override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
    valueParameters.cast()
}

fun FunctionDescriptor.toInjectFunctionDescriptor(
  @Inject ctx: Context
): InjectFunctionDescriptor? {
  if (this is InjectFunctionDescriptor) return this
  if (this is JavaMethodDescriptor) return null
  if (allParameters.none { it.isInject() }) return null
  return when (this) {
    is ClassConstructorDescriptor -> InjectConstructorDescriptorImpl(this)
    is SimpleFunctionDescriptor -> InjectSimpleFunctionDescriptorImpl(this)
    else -> InjectFunctionDescriptorImpl(this)
  }
}

class InjectConstructorDescriptorImpl(
  underlyingDescriptor: ClassConstructorDescriptor,
  @Inject private val ctx: Context
) : AbstractInjectFunctionDescriptor(underlyingDescriptor),
  ClassConstructorDescriptor by underlyingDescriptor {
  override fun substitute(substitutor: TypeSubstitutor): ClassConstructorDescriptor =
    InjectConstructorDescriptorImpl(
      underlyingDescriptor
        .substitute(substitutor) as ClassConstructorDescriptor
    )

  override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
    super.getValueParameters()
}

class InjectFunctionDescriptorImpl(
  underlyingDescriptor: FunctionDescriptor,
  @Inject private val ctx: Context
) : AbstractInjectFunctionDescriptor(underlyingDescriptor),
  FunctionDescriptor by underlyingDescriptor {
  override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor =
    InjectFunctionDescriptorImpl(underlyingDescriptor.substitute(substitutor) as FunctionDescriptor)

  override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
    super.getValueParameters()
}

class InjectSimpleFunctionDescriptorImpl(
  underlyingDescriptor: SimpleFunctionDescriptor,
  @Inject private val ctx: Context
) : AbstractInjectFunctionDescriptor(underlyingDescriptor),
  SimpleFunctionDescriptor by underlyingDescriptor {
  override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor =
    InjectSimpleFunctionDescriptorImpl(
      underlyingDescriptor
        .substitute(substitutor) as SimpleFunctionDescriptor
    )

  override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
    super.getValueParameters()
}
