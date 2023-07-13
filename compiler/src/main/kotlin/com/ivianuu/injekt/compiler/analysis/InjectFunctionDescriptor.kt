/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.Context
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.types.TypeSubstitutor

interface InjectFunctionDescriptor : FunctionDescriptor {
  val underlyingDescriptor: FunctionDescriptor
}

abstract class AbstractInjectFunctionDescriptor(
  override val underlyingDescriptor: FunctionDescriptor
) : InjectFunctionDescriptor {
  override fun getContextReceiverParameters(): List<ReceiverParameterDescriptor> =
    emptyList()
}

fun FunctionDescriptor.toInjectFunctionDescriptor(ctx: Context): InjectFunctionDescriptor? {
  if (this is InjectFunctionDescriptor) return this
  if (this is JavaMethodDescriptor) return null
  if (contextReceiverParameters.isEmpty()) return null
  return when (this) {
    is ClassConstructorDescriptor -> InjectConstructorDescriptorImpl(this, ctx)
    is SimpleFunctionDescriptor -> InjectSimpleFunctionDescriptorImpl(this, ctx)
    else -> InjectFunctionDescriptorImpl(this, ctx)
  }
}

class InjectConstructorDescriptorImpl(
  underlyingDescriptor: ClassConstructorDescriptor,
  private val ctx: Context
) : AbstractInjectFunctionDescriptor(underlyingDescriptor),
  ClassConstructorDescriptor by underlyingDescriptor {
  override fun substitute(substitutor: TypeSubstitutor): ClassConstructorDescriptor =
    InjectConstructorDescriptorImpl(
      underlyingDescriptor
        .substitute(substitutor) as ClassConstructorDescriptor,
      ctx
    )

  override fun getContextReceiverParameters(): List<ReceiverParameterDescriptor> =
    underlyingDescriptor.contextReceiverParameters
}

class InjectFunctionDescriptorImpl(
  underlyingDescriptor: FunctionDescriptor,
  private val ctx: Context
) : AbstractInjectFunctionDescriptor(underlyingDescriptor),
  FunctionDescriptor by underlyingDescriptor {
  override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor =
    InjectFunctionDescriptorImpl(
      underlyingDescriptor.substitute(substitutor) as FunctionDescriptor,
      ctx
    )

  override fun getContextReceiverParameters(): List<ReceiverParameterDescriptor> =
    underlyingDescriptor.contextReceiverParameters
}

class InjectSimpleFunctionDescriptorImpl(
  underlyingDescriptor: SimpleFunctionDescriptor,
  private val ctx: Context
) : AbstractInjectFunctionDescriptor(underlyingDescriptor),
  SimpleFunctionDescriptor by underlyingDescriptor {
  override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor =
    InjectSimpleFunctionDescriptorImpl(
      underlyingDescriptor
        .substitute(substitutor) as SimpleFunctionDescriptor,
      ctx
    )

  override fun getContextReceiverParameters(): List<ReceiverParameterDescriptor> =
    underlyingDescriptor.contextReceiverParameters
}
