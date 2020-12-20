package com.ivianuu.injekt.compiler.analysis

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.utils.addToStdlib.cast

class GivenFunFunctionDescriptor(
    val invokeDescriptor: SimpleFunctionDescriptor,
    val givenFunDescriptor: FunctionDescriptor,
    val givenInvokeDescriptor: FunctionDescriptor,
) : SimpleFunctionDescriptor by invokeDescriptor {
    private val valueParameters = givenInvokeDescriptor
        .valueParameters
        .mapIndexed { index, givenInvokeParameter ->
            val invokeParameter = invokeDescriptor.valueParameters[index]
            object : ValueParameterDescriptorImpl(
                this@GivenFunFunctionDescriptor,
                null,
                index,
                Annotations.EMPTY,
                givenInvokeParameter.name,
                invokeParameter.type,
                givenInvokeParameter.declaresDefaultValue(),
                false,
                false,
                null,
                givenInvokeParameter.source
            ) {}
        }

    override fun getOriginal(): SimpleFunctionDescriptor = this

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
        valueParameters.cast()

    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor =
        GivenFunFunctionDescriptor(
            invokeDescriptor
                .substitute(substitutor) as SimpleFunctionDescriptor,
            givenFunDescriptor,
            givenInvokeDescriptor
        )

    override fun hasStableParameterNames(): Boolean = true

    override fun getSource(): SourceElement = givenInvokeDescriptor.source
}
