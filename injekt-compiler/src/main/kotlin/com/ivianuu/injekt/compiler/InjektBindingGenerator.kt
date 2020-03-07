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

import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.builtins.extractParameterNameFromFunctionTypeArgument
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
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
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.addMember
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.endOffset
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.startOffset
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

class InjektBindingGenerator(private val context: IrPluginContext) : IrElementVisitorVoid {

    private val symbolTable = context.symbolTable
    private val typeTranslator = context.typeTranslator
    private fun KotlinType.toIrType() = typeTranslator.translateType(this)

    private val abstractBindingFactory = getClass(InjektClassNames.AbstractBindingFactory)
    private val binding = getClass(InjektClassNames.Binding)
    private val boundProvider = getClass(InjektClassNames.BoundProvider)
    private val component = getClass(InjektClassNames.Component)
    private val key = getClass(InjektClassNames.Key)
    private val parameters = getClass(InjektClassNames.Parameters)
    private val singleProvider = getClass(InjektClassNames.SingleProvider)

    private fun getClass(fqName: FqName) =
        context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(fqName))!!

    override fun visitElement(element: IrElement) {
    }

    override fun visitClass(declaration: IrClass) {
        val descriptor = declaration.descriptor

        if (!descriptor.annotations.hasAnnotation(InjektClassNames.Factory) &&
            !descriptor.annotations.hasAnnotation(InjektClassNames.Single)
        ) return

        declaration.addMember(bindingFactory(declaration))

        declaration.patchDeclarationParents(declaration.parent)
    }

    private fun bindingFactory(declaration: IrClass): IrClass {
        val descriptor = declaration.descriptor

        val bindingDescriptor = ClassDescriptorImpl(
            descriptor,
            Name.identifier("BindingFactory"),
            Modality.FINAL,
            ClassKind.OBJECT,
            emptyList(),
            descriptor.source,
            false,
            LockBasedStorageManager.NO_LOCKS
        ).apply {
            initialize(
                MemberScope.Empty,
                emptySet(),
                null
            )
        }

        return IrClassImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            InjektOrigin,
            IrClassSymbolImpl(bindingDescriptor)
        ).apply clazz@ {
            createImplicitParameterDeclarationWithWrappedDescriptor()

            val bindingFactoryWithType = KotlinTypeFactory.simpleType(
                baseType = abstractBindingFactory.defaultType,
                arguments = listOf(descriptor.defaultType.asTypeProjection())
            ).toIrType()

            superTypes = superTypes + bindingFactoryWithType

            addConstructor {
                origin = InjektOrigin
                isPrimary = true
                visibility = Visibilities.PRIVATE
            }.apply {
                body = DeclarationIrBuilder(context, symbol).irBlockBody {
                    +IrDelegatingConstructorCallImpl(
                        startOffset, endOffset,
                        context.irBuiltIns.unitType,
                        symbolTable.referenceConstructor(
                            abstractBindingFactory.unsubstitutedPrimaryConstructor!!
                        )
                    ).apply {
                        putTypeArgument(0, descriptor.defaultType.toIrType())
                    }
                    +IrInstanceInitializerCallImpl(startOffset, endOffset, this@clazz.symbol, context.irBuiltIns.unitType)
                }
            }

            val bindingFactoryCreate = abstractBindingFactory.unsubstitutedMemberScope
                .findSingleFunction(Name.identifier("create"))

            val bindingWithType = KotlinTypeFactory.simpleType(
                baseType = binding.defaultType,
                arguments = listOf(descriptor.defaultType.asTypeProjection())
            ).toIrType()

            addFunction(
                name = "create",
                returnType = bindingWithType,
                modality = Modality.FINAL,
                isStatic = false,
                isSuspend = false,
                origin = InjektOrigin
            ).apply {
                overriddenSymbols =
                    overriddenSymbols + symbolTable.referenceSimpleFunction(bindingFactoryCreate)
                createParameterDeclarations(bindingFactoryCreate)
                dispatchReceiverParameter = thisReceiver!!.deepCopyWithVariables()
                body = DeclarationIrBuilder(context, symbol).irBlockBody {
                    val constructor = binding.unsubstitutedPrimaryConstructor!!
                    +irReturn(
                        irCall(
                            symbolTable.referenceConstructor(constructor),
                            bindingWithType
                        ).apply {
                            val keyOf =
                                this@InjektBindingGenerator.context.moduleDescriptor.getPackage(
                                    InjektClassNames.InjektPackage
                                )
                                    .memberScope.findFirstFunction("keyOf") {
                                    it.valueParameters.size == 1
                                }

                            putValueArgument(
                                0,
                                irCall(
                                    callee = symbolTable.referenceSimpleFunction(keyOf),
                                    type = key.defaultType.toIrType()
                                ).apply {
                                    putTypeArgument(0, descriptor.defaultType.toIrType())
                                }
                            )

                            val providerType = KotlinTypeFactory.simpleType(
                                context.builtIns.getFunction(2).defaultType,
                                arguments = listOf(
                                    component.defaultType.asTypeProjection(),
                                    parameters.defaultType.asTypeProjection(),
                                    descriptor.defaultType.asTypeProjection()
                                )
                            )

                            val providerLambda = irLambdaExpression(
                                createFunctionDescriptor(providerType),
                                providerType.toIrType()
                            ) { lambdaFn ->
                                val injektConstructor = descriptor.findInjektConstructor()

                                val componentGet = component.unsubstitutedMemberScope
                                    .findFirstFunction("get") {
                                        it.typeParameters.first().isReified &&
                                                it.valueParameters.size == 2
                                    }

                                val parametersGet = parameters.unsubstitutedMemberScope
                                    .findSingleFunction(Name.identifier("get"))

                                +irReturn(
                                    irCall(
                                        symbolTable.referenceConstructor(injektConstructor),
                                        descriptor.defaultType.toIrType()
                                    ).apply {
                                        var paramIndex = 0

                                        injektConstructor.valueParameters
                                            .map { param ->
                                                val paramExpr = if (param.annotations.hasAnnotation(
                                                        InjektClassNames.Param
                                                    )
                                                ) {
                                                    irCall(
                                                        callee = symbolTable.referenceSimpleFunction(
                                                            parametersGet
                                                        ),
                                                        type = param.type.toIrType()
                                                    ).apply {
                                                        dispatchReceiver =
                                                            irGet(lambdaFn.valueParameters[1])
                                                        putTypeArgument(0, param.type.toIrType())
                                                        putValueArgument(0, irInt(paramIndex))
                                                        ++paramIndex
                                                    }
                                                } else {
                                                    irCall(
                                                        symbolTable.referenceSimpleFunction(
                                                            componentGet
                                                        ),
                                                        param.type.toIrType()
                                                    ).apply {
                                                        dispatchReceiver =
                                                            irGet(lambdaFn.valueParameters[0])
                                                        putTypeArgument(0, param.type.toIrType())

                                                        val nameClass =
                                                            param.getAnnotatedAnnotations(
                                                                InjektClassNames.Name
                                                            )
                                                                .singleOrNull()
                                                                ?.let { nameAnnotation ->
                                                                    getClass(
                                                                        nameAnnotation.fqName!!
                                                                    )
                                                                }
                                                                ?.companionObjectDescriptor
                                                        if (nameClass != null) {
                                                            putValueArgument(
                                                                0,
                                                                irGetObject(
                                                                    symbolTable.referenceClass(
                                                                        nameClass
                                                                    )
                                                                )
                                                            )
                                                        }
                                                    }
                                                }

                                                putValueArgument(param.index, paramExpr)
                                            }
                                    }
                                )
                            }

                            val scopeAnnotation =
                                descriptor.getAnnotatedAnnotations(InjektClassNames.Scope)
                                    .singleOrNull()

                            val scopeExpr = scopeAnnotation?.let {
                                val scopeObject =
                                    getClass(scopeAnnotation.fqName!!).companionObjectDescriptor!!
                                irGetObject(symbolTable.referenceClass(scopeObject))
                            }

                            val providerExpr =
                                if (descriptor.annotations.hasAnnotation(InjektClassNames.Single)) {
                                    irCall(
                                        symbolTable.referenceConstructor(
                                            singleProvider.unsubstitutedPrimaryConstructor!!
                                        ),
                                        KotlinTypeFactory.simpleType(
                                            singleProvider.defaultType,
                                            arguments = listOf(
                                                descriptor.defaultType.asTypeProjection()
                                            )
                                        ).toIrType()
                                    ).apply {
                                        putValueArgument(0, scopeExpr!!)
                                        putValueArgument(1, providerLambda)
                                    }
                                } else if (scopeAnnotation != null) {
                                    irCall(
                                    symbolTable.referenceConstructor(
                                        boundProvider.unsubstitutedPrimaryConstructor!!
                                    ),
                                    KotlinTypeFactory.simpleType(
                                        boundProvider.defaultType,
                                        arguments = listOf(
                                            descriptor.defaultType.asTypeProjection()
                                        )
                                    ).toIrType()
                                ).apply {
                                        putValueArgument(0, scopeExpr!!)
                                        putValueArgument(1, providerLambda)
                                }
                                } else {
                                    providerLambda
                                }

                            putValueArgument(2, providerExpr)
                        }

                    )
                }
            }
        }
    }

    private fun IrBuilderWithScope.irLambdaExpression(
        descriptor: FunctionDescriptor,
        type: IrType,
        body: IrBlockBodyBuilder.(IrFunction) -> Unit
    ) = irLambdaExpression(this.startOffset, this.endOffset, descriptor, type, body)

    private fun IrBuilderWithScope.irLambdaExpression(
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
            it.body = DeclarationIrBuilder(this@InjektBindingGenerator.context, symbol)
                .irBlockBody { body(it) }
        }

        return irBlock(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = IrStatementOrigin.LAMBDA,
            resultType = type
        ) {
            +lambda
            +IrFunctionReferenceImpl(
                startOffset = startOffset,
                endOffset = endOffset,
                type = type,
                symbol = symbol,
                typeArgumentsCount = descriptor.typeParametersCount,
                origin = IrStatementOrigin.LAMBDA,
                reflectionTarget = null
            )
        }
    }

    private fun IrBuilderWithScope.createFunctionDescriptor(
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

    private fun ClassDescriptor.findInjektConstructor(): ClassConstructorDescriptor {
        return constructors.singleOrNull { it.annotations.hasAnnotation(InjektClassNames.InjektConstructor) }
            ?: unsubstitutedPrimaryConstructor!!
    }

    private fun IrFunction.createParameterDeclarations(descriptor: FunctionDescriptor) {
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

object InjektOrigin : IrDeclarationOrigin
