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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
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

    override fun getContainingDeclaration(): DeclarationDescriptor =
        givenInvokeDescriptor.containingDeclaration

    override fun copy(
        p0: DeclarationDescriptor?,
        p1: Modality?,
        p2: DescriptorVisibility?,
        p3: CallableMemberDescriptor.Kind?,
        p4: Boolean
    ): SimpleFunctionDescriptor {
        TODO()
    }

    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<out SimpleFunctionDescriptor> {
        TODO("Not yet implemented")
    }

    override fun <V : Any?> getUserData(p0: CallableDescriptor.UserDataKey<V>?): V? = null

    override fun getInitialSignatureDescriptor(): FunctionDescriptor? = null

    override fun getName(): Name = givenInvokeDescriptor.name

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

    override fun <R : Any?, D : Any?> accept(p0: DeclarationDescriptorVisitor<R, D>, p1: D): R =
        p0.visitFunctionDescriptor(this, p1)

    override fun acceptVoid(p0: DeclarationDescriptorVisitor<Void, Void>) {
        p0.visitFunctionDescriptor(this, null)
    }
}
