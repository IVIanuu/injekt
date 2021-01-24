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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.analysis.GivenFunFunctionDescriptor
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.propertyIfAccessor

class InjektIrGenerationExtension : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transform(InfoTransformer(
            DeclarationStore(pluginContext.moduleDescriptor),
            pluginContext
        ), null)
        moduleFragment.transform(GivenCallTransformer(pluginContext), null)
        moduleFragment.transform(GivenOptimizationTransformer(), null)
        moduleFragment.transform(GivenFunCallTransformer(pluginContext), null)
        moduleFragment.transform(KeyTypeParameterTransformer(pluginContext), null)
        moduleFragment.patchDeclarationParents()
    }

    override fun resolveSymbol(
        symbol: IrSymbol,
        context: TranslationPluginContext
    ): IrDeclaration? = if (symbol.descriptor is GivenFunFunctionDescriptor) {
        symbol as IrSimpleFunctionSymbol
        context.declareFunctionStub(symbol.descriptor).also { func ->
            symbol.bind(func)
            (context.symbolTable as SymbolTable)
                .declareSimpleFunction(symbol.descriptor) { func }
        }
    } else super.resolveSymbol(symbol, context)

}

private fun TranslationPluginContext.declareTypeParameterStub(typeParameterDescriptor: TypeParameterDescriptor): IrTypeParameter {
    val symbol = IrTypeParameterSymbolImpl(typeParameterDescriptor)
    return irFactory.createTypeParameter(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.DEFINED, symbol, typeParameterDescriptor.name,
        typeParameterDescriptor.index, typeParameterDescriptor.isReified, typeParameterDescriptor.variance
    )
}

private fun TranslationPluginContext.declareParameterStub(parameterDescriptor: ParameterDescriptor): IrValueParameter {
    val symbol = IrValueParameterSymbolImpl(parameterDescriptor)
    val type = typeTranslator.translateType(parameterDescriptor.type)
    val varargElementType = parameterDescriptor.varargElementType?.let { typeTranslator.translateType(it) }
    return irFactory.createValueParameter(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.DEFINED, symbol, parameterDescriptor.name,
        parameterDescriptor.indexOrMinusOne, type, varargElementType, parameterDescriptor.isCrossinline,
        parameterDescriptor.isNoinline, isHidden = false, isAssignable = false
    )
}

private fun TranslationPluginContext.declareFunctionStub(descriptor: FunctionDescriptor): IrSimpleFunction =
    irFactory.buildFun {
        name = descriptor.name
        visibility = descriptor.visibility
        returnType = typeTranslator.translateType(descriptor.returnType!!)
        modality = descriptor.modality
    }.also {
        it.typeParameters = descriptor.propertyIfAccessor.typeParameters.map(this::declareTypeParameterStub)
        it.dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.let(this::declareParameterStub)
        it.extensionReceiverParameter = descriptor.extensionReceiverParameter?.let(this::declareParameterStub)
        it.valueParameters = descriptor.valueParameters.map(this::declareParameterStub)
    }
