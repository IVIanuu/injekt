package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
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
) : ValueParameterDescriptorImpl(
    parent,
    null,
    underlyingDescriptor.index,
    underlyingDescriptor.annotations,
    underlyingDescriptor.name,
    underlyingDescriptor.type,
    underlyingDescriptor.hasAnnotation(InjektFqNames.Given) ||
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
    override val underlyingDescriptor: FunctionDescriptor,
) : GivenFunctionDescriptor {
    private val valueParameters = underlyingDescriptor
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
        GivenConstructorDescriptorImpl(underlyingDescriptor
            .substitute(substitutor) as ClassConstructorDescriptor)

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
        super.getValueParameters()
}

class GivenFunctionDescriptorImpl(underlyingDescriptor: FunctionDescriptor) :
    AbstractGivenFunctionDescriptor(underlyingDescriptor),
    FunctionDescriptor by underlyingDescriptor {
    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor =
        GivenFunctionDescriptorImpl(underlyingDescriptor.substitute(substitutor) as FunctionDescriptor)

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
        super.getValueParameters()
}

class GivenSimpleFunctionDescriptorImpl(
    underlyingDescriptor: SimpleFunctionDescriptor,
) : AbstractGivenFunctionDescriptor(underlyingDescriptor),
    SimpleFunctionDescriptor by underlyingDescriptor {
    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor =
        GivenSimpleFunctionDescriptorImpl(underlyingDescriptor
            .substitute(substitutor) as SimpleFunctionDescriptor)

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
        super.getValueParameters()
}
