/*
 * Copyright 2021 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.components.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.*

interface GivenFunctionDescriptor : FunctionDescriptor {
  val underlyingDescriptor: FunctionDescriptor
}

class GivenValueParameterDescriptor(
  parent: GivenFunctionDescriptor,
  val underlyingDescriptor: ValueParameterDescriptor,
  val context: InjektContext,
  trace: BindingTrace
) : ValueParameterDescriptorImpl(
  parent,
  underlyingDescriptor,
  underlyingDescriptor.index,
  underlyingDescriptor.annotations,
  underlyingDescriptor.injektName().asNameId(),
  underlyingDescriptor.type,
  underlyingDescriptor.givenKind(context, trace) ||
      underlyingDescriptor.declaresDefaultValue(),
  underlyingDescriptor.isCrossinline,
  underlyingDescriptor.isNoinline,
  underlyingDescriptor.varargElementType,
  underlyingDescriptor.source
)

val ValueParameterDescriptor.hasDefaultValueIgnoringGiven: Boolean
  get() = (this as? GivenValueParameterDescriptor)?.underlyingDescriptor?.hasDefaultValue()
    ?: hasDefaultValue()

abstract class AbstractGivenFunctionDescriptor(
  final override val underlyingDescriptor: FunctionDescriptor,
  val context: InjektContext,
  trace: BindingTrace
) : GivenFunctionDescriptor {
  private val valueParameters = underlyingDescriptor
    .valueParameters
    .mapTo(mutableListOf()) { valueParameter ->
      GivenValueParameterDescriptor(this, valueParameter, context, trace)
    }

  override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
    valueParameters.cast()
}

fun FunctionDescriptor.toGivenFunctionDescriptor(
  context: InjektContext,
  trace: BindingTrace
) = when (this) {
  is GivenFunctionDescriptor -> this
  is ClassConstructorDescriptor -> GivenConstructorDescriptorImpl(this, context, trace)
  is SimpleFunctionDescriptor -> GivenSimpleFunctionDescriptorImpl(this, context, trace)
  else -> GivenFunctionDescriptorImpl(this, context, trace)
}

class GivenConstructorDescriptorImpl(
  underlyingDescriptor: ClassConstructorDescriptor,
  context: InjektContext,
  private val trace: BindingTrace
) : AbstractGivenFunctionDescriptor(underlyingDescriptor, context, trace),
  ClassConstructorDescriptor by underlyingDescriptor {
  override fun substitute(substitutor: TypeSubstitutor): ClassConstructorDescriptor =
    GivenConstructorDescriptorImpl(
      underlyingDescriptor
        .substitute(substitutor) as ClassConstructorDescriptor, context, trace
    )

  override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
    super.getValueParameters()
}

class GivenFunctionDescriptorImpl(
  underlyingDescriptor: FunctionDescriptor,
  context: InjektContext,
  private val trace: BindingTrace
) : AbstractGivenFunctionDescriptor(underlyingDescriptor, context, trace),
  FunctionDescriptor by underlyingDescriptor {
  override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor =
    GivenFunctionDescriptorImpl(
      underlyingDescriptor.substitute(substitutor) as FunctionDescriptor, context, trace
    )

  override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
    super.getValueParameters()
}

class GivenSimpleFunctionDescriptorImpl(
  underlyingDescriptor: SimpleFunctionDescriptor,
  context: InjektContext,
  private val trace: BindingTrace
) : AbstractGivenFunctionDescriptor(underlyingDescriptor, context, trace),
  SimpleFunctionDescriptor by underlyingDescriptor {
  override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor =
    GivenSimpleFunctionDescriptorImpl(
      underlyingDescriptor
        .substitute(substitutor) as SimpleFunctionDescriptor, context, trace
    )

  override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
    super.getValueParameters()
}
