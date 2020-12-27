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

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.utils.addToStdlib.cast

interface GivenFunctionDescriptor : FunctionDescriptor {
    val invokeDescriptor: FunctionDescriptor
}

class GivenValueParameterDescriptor(
    parent: GivenFunctionDescriptor,
    val underlyingDescriptor: ValueParameterDescriptor,
) : ValueParameterDescriptorImpl(
    parent,
    null,
    underlyingDescriptor.index,
    underlyingDescriptor.annotations,
    if (underlyingDescriptor.name.isSpecial) underlyingDescriptor.type.toTypeRef()
        .classifier.fqName.shortName().asString().decapitalize().asNameId() else underlyingDescriptor.name,
    underlyingDescriptor.type,
    underlyingDescriptor.hasAnnotation(InjektFqNames.Given) ||
            underlyingDescriptor.type.hasAnnotation(InjektFqNames.Given) ||
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
) : GivenFunctionDescriptor {
    private val valueParameters = invokeDescriptor
        .valueParameters
        .mapTo(mutableListOf()) { valueParameter ->
            GivenValueParameterDescriptor(this, valueParameter)
        }

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
        valueParameters.cast()
}

fun FunctionDescriptor.toGivenFunctionDescriptor() = when (this) {
    is GivenFunctionDescriptor -> this
    is ClassConstructorDescriptor -> GivenConstructorDescriptorImpl(this)
    is SimpleFunctionDescriptor -> GivenSimpleFunctionDescriptorImpl(this)
    else -> GivenFunctionDescriptorImpl(this)
}

class GivenConstructorDescriptorImpl(underlyingDescriptor: ClassConstructorDescriptor) :
    AbstractGivenFunctionDescriptor(underlyingDescriptor),
    ClassConstructorDescriptor by underlyingDescriptor {
    override fun substitute(substitutor: TypeSubstitutor): ClassConstructorDescriptor =
        GivenConstructorDescriptorImpl(invokeDescriptor
            .substitute(substitutor) as ClassConstructorDescriptor)

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
        super.getValueParameters()
}

class GivenFunctionDescriptorImpl(underlyingDescriptor: FunctionDescriptor) :
    AbstractGivenFunctionDescriptor(underlyingDescriptor),
    FunctionDescriptor by underlyingDescriptor {
    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor =
        GivenFunctionDescriptorImpl(invokeDescriptor.substitute(substitutor) as FunctionDescriptor)

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
        super.getValueParameters()
}

class GivenSimpleFunctionDescriptorImpl(
    underlyingDescriptor: SimpleFunctionDescriptor,
) : AbstractGivenFunctionDescriptor(underlyingDescriptor),
    SimpleFunctionDescriptor by underlyingDescriptor {
    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor =
        GivenSimpleFunctionDescriptorImpl(invokeDescriptor
            .substitute(substitutor) as SimpleFunctionDescriptor)

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
        super.getValueParameters()
}
