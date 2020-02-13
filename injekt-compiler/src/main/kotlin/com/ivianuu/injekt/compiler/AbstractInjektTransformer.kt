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

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.util.endOffset
import org.jetbrains.kotlin.ir.util.startOffset
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.KotlinType

abstract class AbstractInjektTransformer(
    protected val context: IrPluginContext
) : IrElementTransformerVoid() {

    protected val symbolTable = context.symbolTable
    protected val typeTranslator = context.typeTranslator
    protected fun KotlinType.toIrType() = typeTranslator.translateType(this)

    protected fun getTopLevelClass(fqName: FqName) =
        context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(fqName))!!

    protected fun IrFunction.createParameterDeclarations(descriptor: FunctionDescriptor) {
        fun ParameterDescriptor.irValueParameter() = IrValueParameterImpl(
            this.startOffset ?: UNDEFINED_OFFSET,
            this.endOffset ?: UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            this,
            type.toIrType(),
            (this as? ValueParameterDescriptor)?.varargElementType?.toIrType()
        ).also {
            it.parent = this@createParameterDeclarations
        }

        fun TypeParameterDescriptor.irTypeParameter() = IrTypeParameterImpl(
            this.startOffset ?: UNDEFINED_OFFSET,
            this.endOffset ?: UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            IrTypeParameterSymbolImpl(this)
        ).also {
            it.parent = this@createParameterDeclarations
        }

        dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.irValueParameter()
        extensionReceiverParameter = descriptor.extensionReceiverParameter?.irValueParameter()

        assert(valueParameters.isEmpty()) { "params ${valueParameters.map { it.name }}" }
        valueParameters = descriptor.valueParameters.map { it.irValueParameter() }

        assert(typeParameters.isEmpty()) { "types ${typeParameters.map { it.name }}" }
        typeParameters + descriptor.typeParameters.map { it.irTypeParameter() }
    }
}