/*
 * Copyright 2020 Manuel Wrage
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

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.resolution.isGiven
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.utils.addToStdlib.cast

interface GivenFunctionDescriptor : FunctionDescriptor {
    val invokeDescriptor: FunctionDescriptor
}

class GivenValueParameterDescriptor(
    parent: GivenFunctionDescriptor,
    val underlyingDescriptor: ValueParameterDescriptor,
    val context: InjektContext,
    trace: BindingTrace
) : ValueParameterDescriptorImpl(
    parent,
    null,
    underlyingDescriptor.index,
    underlyingDescriptor.annotations,
    underlyingDescriptor.name,
    underlyingDescriptor.type,
    underlyingDescriptor.isGiven(context, trace) ||
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
    override val invokeDescriptor: FunctionDescriptor,
    val context: InjektContext,
    trace: BindingTrace
) : GivenFunctionDescriptor {
    private val valueParameters = invokeDescriptor
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
        GivenConstructorDescriptorImpl(invokeDescriptor
            .substitute(substitutor) as ClassConstructorDescriptor, context, trace)

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
            invokeDescriptor.substitute(substitutor) as FunctionDescriptor, context, trace)

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
        GivenSimpleFunctionDescriptorImpl(invokeDescriptor
            .substitute(substitutor) as SimpleFunctionDescriptor, context, trace)

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
        super.getValueParameters()
}
