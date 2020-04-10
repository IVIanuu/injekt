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
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.builtins.extractParameterNameFromFunctionTypeArgument
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.endOffset
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.startOffset
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType

abstract class AbstractInjektTransformer(
    protected val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        return super.visitModuleFragment(declaration)
            .also { it.patchDeclarationParents() }
    }

    protected val symbolTable = pluginContext.symbolTable
    protected val typeTranslator = pluginContext.typeTranslator
    protected fun KotlinType.toIrType() = typeTranslator.translateType(this)

    protected val injektPackage =
        pluginContext.moduleDescriptor.getPackage(InjektClassNames.InjektPackage)

    protected fun getClass(fqName: FqName) =
        pluginContext.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(fqName))!!

    protected fun List<IrConstructorCall>.hasAnnotation(fqName: FqName): Boolean =
        any { it.symbol.descriptor.constructedClass.fqNameSafe == fqName }

    protected fun <T : IrSymbol> T.ensureBound(): T {
        if (!this.isBound) pluginContext.irProvider.getDeclaration(this)
        check(this.isBound) { "$this is not bound" }
        return this
    }

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

    protected fun IrBuilderWithScope.irLambdaExpression(
        descriptor: FunctionDescriptor,
        type: IrType,
        body: IrBlockBodyBuilder.(IrFunction) -> Unit
    ) = irLambdaExpression(this.startOffset, this.endOffset, descriptor, type, body)

    protected fun IrBuilderWithScope.irLambdaExpression(
        startOffset: Int,
        endOffset: Int,
        descriptor: FunctionDescriptor,
        type: IrType,
        body: IrBlockBodyBuilder.(IrFunction) -> Unit
    ): IrExpression {
        val symbol = IrSimpleFunctionSymbolImpl(descriptor)

        val returnType = descriptor.returnType!!.toIrType()

        val lambda = IrFunctionImpl(
            startOffset, endOffset,
            IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA,
            symbol,
            returnType
        ).also {
            it.parent = scope.getLocalDeclarationParent()
            it.createParameterDeclarations(descriptor)
            it.body = DeclarationIrBuilder(this@AbstractInjektTransformer.pluginContext, symbol)
                .irBlockBody { body(it) }
        }

        return IrFunctionExpressionImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            type = type,
            origin = IrStatementOrigin.LAMBDA,
            function = lambda
        )
    }

    protected fun IrBuilderWithScope.syntheticAnnotationAccessor(
        annotation: AnnotationDescriptor
    ): IrExpression {
        val property =
            annotation.getPropertyForSyntheticAnnotation(pluginContext.moduleDescriptor)
        return irCall(
            symbolTable.referenceSimpleFunction(property.getter!!),
            property.type.toIrType()
        )
    }

    protected fun IrBuilderWithScope.createFunctionDescriptor(
        type: KotlinType,
        owner: DeclarationDescriptor = scope.scopeOwner
    ): FunctionDescriptor {
        return AnonymousFunctionDescriptor(
            owner,
            Annotations.EMPTY,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE,
            false
        ).apply {
            initialize(
                type.getReceiverTypeFromFunctionType()?.let {
                    DescriptorFactory.createExtensionReceiverParameterForCallable(
                        this,
                        it,
                        Annotations.EMPTY
                    )
                },
                null,
                emptyList(),
                type.getValueParameterTypesFromFunctionType().mapIndexed { i, t ->
                    ValueParameterDescriptorImpl(
                        containingDeclaration = this,
                        original = null,
                        index = i,
                        annotations = Annotations.EMPTY,
                        name = t.type.extractParameterNameFromFunctionTypeArgument()
                            ?: Name.identifier("p$i"),
                        outType = t.type,
                        declaresDefaultValue = false,
                        isCrossinline = false,
                        isNoinline = false,
                        varargElementType = null,
                        source = SourceElement.NO_SOURCE
                    )
                },
                type.getReturnTypeFromFunctionType(),
                Modality.FINAL,
                Visibilities.LOCAL,
                null
            )
            isOperator = false
            isInfix = false
            isExternal = false
            isInline = false
            isTailrec = false
            isSuspend = false
            isExpect = false
            isActual = false
        }
    }

}

object InjektOrigin : IrDeclarationOrigin
