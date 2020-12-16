package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.GivenInfo
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.utils.addToStdlib.cast

interface GivenFunctionDescriptor : FunctionDescriptor {
    val underlyingDescriptor: FunctionDescriptor
}

class GivenValueParameterDescriptor(
    parent: GivenFunctionDescriptor,
    val underlyingDescriptor: ValueParameterDescriptor,
    val isGiven: Boolean,
) : ValueParameterDescriptorImpl(
    parent,
    null,
    underlyingDescriptor.index,
    underlyingDescriptor.annotations,
    underlyingDescriptor.name,
    underlyingDescriptor.type,
    isGiven || underlyingDescriptor.declaresDefaultValue(),
    underlyingDescriptor.isCrossinline,
    underlyingDescriptor.isNoinline,
    underlyingDescriptor.varargElementType,
    underlyingDescriptor.source
)

val ValueParameterDescriptor.hasDefaultValueIgnoringGiven: Boolean
    get() = (this as? GivenValueParameterDescriptor)?.underlyingDescriptor?.hasDefaultValue()
        ?: hasDefaultValue()

abstract class AbstractGivenFunctionDescriptor(
    override val underlyingDescriptor: FunctionDescriptor,
    private val givenInfo: GivenInfo,
) : GivenFunctionDescriptor {
    private val valueParameters = underlyingDescriptor
        .valueParameters
        .mapTo(mutableListOf()) { valueParameter ->
            GivenValueParameterDescriptor(
                this,
                valueParameter,
                valueParameter.name in givenInfo.givens
            )
        }

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
        valueParameters.cast()
}

fun FunctionDescriptor.toGivenFunctionDescriptor(givenInfo: GivenInfo) = when (this) {
    is GivenFunctionDescriptor -> this
    is ClassConstructorDescriptor -> GivenConstructorDescriptorImpl(this, givenInfo)
    is SimpleFunctionDescriptor -> GivenSimpleFunctionDescriptorImpl(this, givenInfo)
    else -> GivenFunctionDescriptorImpl(this, givenInfo)
}

class GivenConstructorDescriptorImpl(
    underlyingDescriptor: ClassConstructorDescriptor,
    private val givenInfo: GivenInfo,
) : AbstractGivenFunctionDescriptor(underlyingDescriptor, givenInfo),
    ClassConstructorDescriptor by underlyingDescriptor {
    override fun substitute(substitutor: TypeSubstitutor): ClassConstructorDescriptor =
        GivenConstructorDescriptorImpl(underlyingDescriptor
            .substitute(substitutor) as ClassConstructorDescriptor, givenInfo)

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
        super.getValueParameters()
}

class GivenFunctionDescriptorImpl(
    underlyingDescriptor: FunctionDescriptor,
    private val givenInfo: GivenInfo,
) : AbstractGivenFunctionDescriptor(underlyingDescriptor, givenInfo),
    FunctionDescriptor by underlyingDescriptor {
    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor =
        GivenFunctionDescriptorImpl(underlyingDescriptor.substitute(substitutor) as FunctionDescriptor,
            givenInfo)

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
        super.getValueParameters()
}

class GivenSimpleFunctionDescriptorImpl(
    underlyingDescriptor: SimpleFunctionDescriptor,
    private val givenInfo: GivenInfo,
) : AbstractGivenFunctionDescriptor(underlyingDescriptor, givenInfo),
    SimpleFunctionDescriptor by underlyingDescriptor {
    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor =
        GivenSimpleFunctionDescriptorImpl(underlyingDescriptor
            .substitute(substitutor) as SimpleFunctionDescriptor, givenInfo)

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
        super.getValueParameters()
}
