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
