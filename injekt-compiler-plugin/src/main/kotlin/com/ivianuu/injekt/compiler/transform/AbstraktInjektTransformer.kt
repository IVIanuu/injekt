package com.ivianuu.injekt.compiler.transform

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

import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.classOrFail
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.typeArguments
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.builtins.extractParameterNameFromFunctionTypeArgument
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.endOffset
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.util.startOffset
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType

abstract class AbstractInjektTransformer(
    val context: IrPluginContext
) : IrElementTransformerVoid(), ModuleLoweringPass {

    val symbols = InjektSymbols(context)

    protected val irProviders = context.irProviders
    protected val symbolTable = context.symbolTable
    val irBuiltIns = context.irBuiltIns
    protected val builtIns = context.builtIns
    protected val typeTranslator = context.typeTranslator
    protected fun KotlinType.toIrType() = typeTranslator.translateType(this)

    lateinit var moduleFragment: IrModuleFragment

    override fun lower(module: IrModuleFragment) {
        moduleFragment = module
        visitModuleFragment(module, null)
        module.patchDeclarationParents()
    }

    fun List<IrConstructorCall>.hasAnnotation(fqName: FqName): Boolean =
        any { it.symbol.descriptor.constructedClass.fqNameSafe == fqName }

    fun IrBuilderWithScope.irInjektIntrinsicUnit(): IrExpression {
        return irCall(
            symbolTable.referenceFunction(
                symbols.internalPackage.memberScope
                    .findSingleFunction(Name.identifier("injektIntrinsic"))
            ),
            this@AbstractInjektTransformer.context.irBuiltIns.unitType
        )
    }

    fun IrBuilderWithScope.noArgSingleConstructorCall(clazz: IrClassSymbol): IrConstructorCall =
        irCall(clazz.constructors.single())

    fun IrBuilderWithScope.fieldPathAnnotation(field: IrField): IrConstructorCall =
        irCall(symbols.astFieldPath.constructors.single()).apply {
            putValueArgument(0, irString(field.name.asString()))
        }

    fun IrBuilderWithScope.classPathAnnotation(clazz: IrClass): IrConstructorCall =
        irCall(symbols.astClassPath.constructors.single()).apply {
            putTypeArgument(0, clazz.defaultType)
        }

    fun IrFunction.createParameterDeclarations(descriptor: FunctionDescriptor) {
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

    fun IrBuilderWithScope.irLambdaExpression(
        descriptor: FunctionDescriptor,
        type: IrType,
        body: IrBlockBodyBuilder.(IrFunction) -> Unit
    ) = irLambdaExpression(this.startOffset, this.endOffset, descriptor, type, body)

    fun IrBuilderWithScope.irLambdaExpression(
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
            it.body = DeclarationIrBuilder(this@AbstractInjektTransformer.context, symbol)
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

    fun IrBuilderWithScope.createFunctionDescriptor(
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

    fun IrBlockBodyBuilder.initializeClassWithAnySuperClass(symbol: IrClassSymbol) {
        +IrDelegatingConstructorCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            irBuiltIns.unitType,
            symbolTable.referenceConstructor(
                builtIns.any
                    .unsubstitutedPrimaryConstructor!!
            )
        )
        +IrInstanceInitializerCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            symbol,
            irBuiltIns.unitType
        )
    }

    fun IrBuilderWithScope.irMapKeyConstructorForKey(
        expression: IrExpression
    ): IrConstructorCall {
        return when (expression) {
            is IrClassReference -> {
                irCall(symbols.astMapClassKey.constructors.single())
                    .apply {
                        putValueArgument(0, expression.deepCopyWithVariables())
                    }
            }
            is IrConst<*> -> {
                when (expression.kind) {
                    is IrConstKind.Int -> irCall(symbols.astMapIntKey.constructors.single())
                        .apply {
                            putValueArgument(0, expression.deepCopyWithVariables())
                        }
                    is IrConstKind.Long -> irCall(symbols.astMapLongKey.constructors.single())
                        .apply {
                            putValueArgument(0, expression.deepCopyWithVariables())
                        }
                    is IrConstKind.String -> irCall(symbols.astMapStringKey.constructors.single())
                        .apply {
                            putValueArgument(0, expression.deepCopyWithVariables())
                        }
                    else -> error("Unexpected expression ${expression.dump()}")
                }
            }
            else -> error("Unexpected expression ${expression.dump()}")
        }
    }

    data class ProviderParameter(
        val name: String,
        val type: IrType,
        val assisted: Boolean
    )

    fun IrBuilderWithScope.provider(
        name: Name,
        parameters: List<ProviderParameter>,
        returnType: IrType,
        createBody: IrBuilderWithScope.(IrFunction) -> IrBody
    ): IrClass {
        val (assistedParameters, nonAssistedParameters) = parameters
            .partition { it.assisted }
        return buildClass {
            kind = if (nonAssistedParameters.isNotEmpty()) ClassKind.CLASS else ClassKind.OBJECT
            this.name = name
        }.apply clazz@{
            val superType = symbols.getFunction(assistedParameters.size)
                .typeWith(
                    assistedParameters
                        .map { it.type } + returnType
                )
            superTypes += superType

            (this as IrClassImpl).metadata = MetadataSource.Class(descriptor)

            createImplicitParameterDeclarationWithWrappedDescriptor()

            val fieldsByNonAssistedParameter = nonAssistedParameters
                .toList()
                .associateWith { (name, type) ->
                    addField(
                        name,
                        symbols.getFunction(0).typeWith(type)
                    )
                }

            addConstructor {
                this.returnType = defaultType
                isPrimary = true
                visibility = Visibilities.PUBLIC
            }.apply {
                fieldsByNonAssistedParameter.forEach { (_, field) ->
                    addValueParameter(
                        field.name.asString(),
                        field.type
                    )
                }

                body = irBlockBody {
                    initializeClassWithAnySuperClass(this@clazz.symbol)
                    valueParameters
                        .forEach { valueParameter ->
                            +irSetField(
                                irGet(thisReceiver!!),
                                fieldsByNonAssistedParameter.values.toList()[valueParameter.index],
                                irGet(valueParameter)
                            )
                        }
                }
            }

            val companion = if (nonAssistedParameters.isNotEmpty()) {
                providerCompanion(parameters, returnType, createBody).also { addChild(it) }
            } else null

            val createFunction = if (nonAssistedParameters.isEmpty()) {
                providerCreateFunction(parameters, returnType, this, createBody)
            } else {
                null
            }

            addFunction {
                this.name = Name.identifier("invoke")
                this.returnType = returnType
                visibility = Visibilities.PUBLIC
            }.apply func@{
                dispatchReceiverParameter = thisReceiver?.copyTo(this, type = defaultType)

                overriddenSymbols += superType.classOrFail
                    .ensureBound(this@AbstractInjektTransformer.context.irProviders)
                    .owner
                    .functions
                    .single { it.name.asString() == "invoke" }
                    .symbol

                val valueParametersByAssistedParameter = assistedParameters.associateWith {
                    addValueParameter(
                        it.name,
                        it.type
                    )
                }

                body = irExprBody(
                    irCall(companion?.functions?.single() ?: createFunction!!).apply {
                        dispatchReceiver =
                            if (companion != null) irGetObject(companion.symbol) else irGet(
                                dispatchReceiverParameter!!
                            )

                        parameters.forEachIndexed { index, parameter ->
                            putValueArgument(
                                index,
                                if (parameter in assistedParameters) {
                                    irGet(valueParametersByAssistedParameter.getValue(parameter))
                                } else {
                                    irCall(
                                        symbols.getFunction(0)
                                            .functions
                                            .single { it.owner.name.asString() == "invoke" })
                                        .apply {
                                            dispatchReceiver = irGetField(
                                                irGet(dispatchReceiverParameter!!),
                                                fieldsByNonAssistedParameter.getValue(parameter)
                                            )
                                        }
                                }
                            )
                        }

                        fieldsByNonAssistedParameter.values.forEachIndexed { index, field ->
                            putValueArgument(
                                index,
                                irCall(
                                    symbols.getFunction(0)
                                        .functions
                                        .single { it.owner.name.asString() == "invoke" },
                                    field.type.typeArguments.single()
                                ).apply {
                                    dispatchReceiver = irGetField(
                                        irGet(dispatchReceiverParameter!!),
                                        field
                                    )
                                }
                            )
                        }
                    }
                )
            }
        }
    }

    private fun IrBuilderWithScope.providerCompanion(
        parameters: List<ProviderParameter>,
        returnType: IrType,
        createBody: IrBuilderWithScope.(IrFunction) -> IrBody
    ) = buildClass {
        kind = ClassKind.OBJECT
        name = SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
        isCompanion = true
    }.apply clazz@{
        createImplicitParameterDeclarationWithWrappedDescriptor()

        (this as IrClassImpl).metadata = MetadataSource.Class(descriptor)

        addConstructor {
            this.returnType = defaultType
            isPrimary = true
            visibility = Visibilities.PUBLIC
        }.apply {
            body = irBlockBody {
                initializeClassWithAnySuperClass(this@clazz.symbol)
            }
        }

        providerCreateFunction(parameters, returnType, this, createBody)
    }

    private fun IrBuilderWithScope.providerCreateFunction(
        parameters: List<ProviderParameter>,
        returnType: IrType,
        owner: IrClass,
        createBody: IrBuilderWithScope.(IrFunction) -> IrBody
    ): IrFunction {
        return owner.addFunction {
            name = Name.identifier("create")
            this.returnType = returnType
            visibility = Visibilities.PUBLIC
            isInline = true
        }.apply {
            dispatchReceiverParameter = owner.thisReceiver?.copyTo(this, type = owner.defaultType)

            parameters
                .forEach { (name, type) ->
                    addValueParameter(
                        name,
                        type
                    )
                }

            body = createBody(this@providerCreateFunction, this)
        }
    }
}
