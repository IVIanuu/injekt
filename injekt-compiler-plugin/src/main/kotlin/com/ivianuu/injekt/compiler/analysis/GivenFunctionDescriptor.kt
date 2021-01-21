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

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.resolution.ContributionKind
import com.ivianuu.injekt.compiler.resolution.contributionKind
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
    val declarationStore: DeclarationStore
) : ValueParameterDescriptorImpl(
    parent,
    null,
    underlyingDescriptor.index,
    underlyingDescriptor.annotations,
    if (underlyingDescriptor.name.isSpecial) underlyingDescriptor.type.toTypeRef(declarationStore)
        .classifier.fqName.shortName().asString().decapitalize().asNameId() else underlyingDescriptor.name,
    underlyingDescriptor.type,
    underlyingDescriptor.contributionKind(declarationStore) == ContributionKind.VALUE ||
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
    val declarationStore: DeclarationStore
) : GivenFunctionDescriptor {
    private val valueParameters = invokeDescriptor
        .valueParameters
        .mapTo(mutableListOf()) { valueParameter ->
            GivenValueParameterDescriptor(this, valueParameter, declarationStore)
        }

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
        valueParameters.cast()
}

fun FunctionDescriptor.toGivenFunctionDescriptor(
    declarationStore: DeclarationStore
) = when (this) {
    is GivenFunctionDescriptor -> this
    is ClassConstructorDescriptor -> GivenConstructorDescriptorImpl(this, declarationStore)
    is SimpleFunctionDescriptor -> GivenSimpleFunctionDescriptorImpl(this, declarationStore)
    else -> GivenFunctionDescriptorImpl(this, declarationStore)
}

class GivenConstructorDescriptorImpl(
    underlyingDescriptor: ClassConstructorDescriptor,
    declarationStore: DeclarationStore
) : AbstractGivenFunctionDescriptor(underlyingDescriptor, declarationStore),
    ClassConstructorDescriptor by underlyingDescriptor {
    override fun substitute(substitutor: TypeSubstitutor): ClassConstructorDescriptor =
        GivenConstructorDescriptorImpl(invokeDescriptor
            .substitute(substitutor) as ClassConstructorDescriptor, declarationStore)

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
        super.getValueParameters()
}

class GivenFunctionDescriptorImpl(
    underlyingDescriptor: FunctionDescriptor,
    declarationStore: DeclarationStore
) : AbstractGivenFunctionDescriptor(underlyingDescriptor, declarationStore),
    FunctionDescriptor by underlyingDescriptor {
    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor =
        GivenFunctionDescriptorImpl(
            invokeDescriptor.substitute(substitutor) as FunctionDescriptor, declarationStore)

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
        super.getValueParameters()
}

class GivenSimpleFunctionDescriptorImpl(
    underlyingDescriptor: SimpleFunctionDescriptor,
    declarationStore: DeclarationStore,
) : AbstractGivenFunctionDescriptor(underlyingDescriptor, declarationStore),
    SimpleFunctionDescriptor by underlyingDescriptor {
    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor =
        GivenSimpleFunctionDescriptorImpl(invokeDescriptor
            .substitute(substitutor) as SimpleFunctionDescriptor, declarationStore)

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
        super.getValueParameters()
}
