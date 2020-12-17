package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.resolution.GivenKind
import com.ivianuu.injekt.compiler.resolution.givenKind
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
) : SimpleFunctionDescriptor by invokeDescriptor {
    private val valueParameters = givenFunDescriptor.valueParameters
        .filterNot { it.givenKind() == GivenKind.VALUE }
        .mapIndexed { index, givenFunParameter ->
            val invokeParameter = invokeDescriptor.valueParameters[index]
            object : ValueParameterDescriptorImpl(
                this@GivenFunFunctionDescriptor,
                null,
                index,
                Annotations.EMPTY,
                givenFunParameter.name,
                invokeParameter.type,
                givenFunParameter.declaresDefaultValue(),
                false,
                false,
                null,
                SourceElement.NO_SOURCE
            ) {}
        }

    override fun getOriginal(): SimpleFunctionDescriptor = this

    override fun getValueParameters(): MutableList<ValueParameterDescriptor> =
        valueParameters.cast()

    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor =
        GivenFunFunctionDescriptor(
            invokeDescriptor
                .substitute(substitutor) as SimpleFunctionDescriptor,
            givenFunDescriptor
        )

    override fun hasStableParameterNames(): Boolean = true
}
