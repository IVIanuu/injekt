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
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.FieldDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

class BindingModuleGenerator(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val behavior = getClass(InjektClassNames.Behavior)
    private val component = getClass(InjektClassNames.Component)
    private val module = getClass(InjektClassNames.Module)
    private val moduleMarker = getClass(InjektClassNames.ModuleMarker)
    private val parameters = getClass(InjektClassNames.Parameters)
    private val qualifier = getClass(InjektClassNames.Qualifier)
    private val scope = getClass(InjektClassNames.Scope)

    override fun visitFile(declaration: IrFile): IrFile {
        super.visitFile(declaration)

        val injectables = mutableListOf<IrClass>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.descriptor.getSyntheticAnnotationPropertiesOfType(behavior.defaultType)
                        .isNotEmpty()
                ) {
                    injectables += declaration
                }
                return super.visitDeclaration(declaration)
            }
        })

        injectables.forEach {
            declaration.addChild(
                DeclarationIrBuilder(
                    pluginContext,
                    it.symbol
                ).bindingModuleProperty(it)
            )
        }

        (declaration as IrFileImpl).metadata = MetadataSource.File(
            declaration.declarations
                .map { it.descriptor }
        )

        return declaration
    }

    private fun IrBuilderWithScope.bindingModuleProperty(injectable: IrClass): IrProperty {
        val descriptor = ModulePropertyDescriptor(injectable)
        return IrPropertyImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            InjektOrigin,
            IrPropertySymbolImpl(descriptor)
        ).apply property@{
            parent = injectable.parent
            metadata = MetadataSource.Property(descriptor)

            annotations += irCallConstructor(
                symbolTable.referenceConstructor(moduleMarker.constructors.first())
                    .ensureBound(),
                emptyList()
            )

            backingField = IrFieldImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                InjektOrigin,
                descriptor,
                module.defaultType.toIrType()
            ).apply {
                initializer = irExprBody(bindingModule(injectable))
            }

            addGetter {
                returnType = descriptor.type.toIrType()
            }.apply {
                body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                    +irReturn(irGetField(null, backingField!!))
                }
            }
        }
    }

    private inner class ModulePropertyDescriptor(injectable: IrDeclaration) :
        PropertyDescriptorImpl(
            injectable.descriptor.containingDeclaration!!,
            null,
            Annotations.create(
                listOf(
                    AnnotationDescriptorImpl(
                        moduleMarker.defaultType,
                        emptyMap(),
                        SourceElement.NO_SOURCE
                    )
                )
            ),
            Modality.FINAL,
            Visibilities.PRIVATE,
            false,
            Name.identifier("${injectable.descriptor.name.asString().removeIllegalChars()}Module"),
            CallableMemberDescriptor.Kind.DECLARATION,
            SourceElement.NO_SOURCE,
            false,
            false,
            false,
            false,
            false,
            false
        ) {
        init {
            initialize(
                null,
                null,
                FieldDescriptorImpl(
                    Annotations.EMPTY,
                    this
                ),
                null
            )
            setType(
                module.defaultType,
                emptyList(),
                null,
                null
            )
        }
    }

    private fun IrBuilderWithScope.bindingModule(
        injectable: IrClass
    ): IrExpression {
        return irCall(
            symbolTable.referenceSimpleFunction(
                injektInternalPackage.memberScope.findFirstFunction("BindingModule") {
                    it.valueParameters.firstOrNull()?.type == qualifier.defaultType
                }
            ),
            module.defaultType.toIrType()
        ).apply {
            putTypeArgument(0, injectable.defaultType)

            val qualifiers =
                injectable.descriptor.getSyntheticAnnotationsForType(qualifier.defaultType)
                    .map { syntheticAnnotationAccessor(it) }
                    .filterNot {
                        it.type.isSubtypeOfClass(
                            symbolTable.referenceClass(this@BindingModuleGenerator.scope)
                        )
                    }

            if (qualifiers.isNotEmpty()) {
                putValueArgument(
                    1,
                    qualifiers
                        .reduceRight { currentBehavior, acc ->
                            irCall(
                                symbolTable.referenceSimpleFunction(
                                    this@BindingModuleGenerator.qualifier.unsubstitutedMemberScope
                                        .findSingleFunction(
                                            Name.identifier(
                                                "plus"
                                            )
                                        )
                                ),
                                this@BindingModuleGenerator.qualifier.defaultType.toIrType()
                            ).apply {
                                dispatchReceiver = currentBehavior
                                putValueArgument(0, acc)
                            }
                        }
                )
            }

            val behaviors =
                injectable.descriptor.getSyntheticAnnotationsForType(
                        behavior.defaultType
                    )
                    .map { syntheticAnnotationAccessor(it) }

            if (behaviors.isNotEmpty()) {
                putValueArgument(
                    1,
                    behaviors
                        .reduceRight { currentBehavior, acc ->
                            irCall(
                                symbolTable.referenceSimpleFunction(
                                    this@BindingModuleGenerator.behavior.unsubstitutedMemberScope
                                        .findSingleFunction(
                                            Name.identifier(
                                                "plus"
                                            )
                                        )
                                ),
                                this@BindingModuleGenerator.behavior.defaultType.toIrType()
                            ).apply {
                                dispatchReceiver = currentBehavior
                                putValueArgument(0, acc)
                            }
                        }
                )
            }

            putValueArgument(
                2,
                irCall(
                    symbolTable.referenceSimpleFunction(
                        injektPackage.memberScope
                            .getContributedVariables(
                                Name.identifier("ApplicationScope"),
                                NoLookupLocation.FROM_BACKEND
                            )
                            .single()
                            .getter!!
                    ),
                    this@BindingModuleGenerator.scope.defaultType.toIrType()
                )
            )

            putValueArgument(3, bindingProvider(injectable))
        }
    }

    private fun IrBuilderWithScope.bindingProvider(
        injectable: IrClass
    ): IrExpression {
        val injectableType = injectable.defaultType

        val providerType = KotlinTypeFactory.simpleType(
            context.builtIns.getFunction(2).defaultType,
            arguments = listOf(
                component.defaultType.asTypeProjection(),
                parameters.defaultType.asTypeProjection(),
                injectableType.toKotlinType().asTypeProjection()
            )
        )

        return irLambdaExpression(
            createFunctionDescriptor(providerType),
            providerType.toIrType()
        ) { lambdaFn ->
            +irReturn(if (injectable.kind == ClassKind.OBJECT) {
                irGetObject(injectable.symbol)
            } else {
                injectableConstructorCall(
                    injectable.primaryConstructor!!,
                    component = { irGet(lambdaFn.valueParameters[0]) },
                    parameters = { irGet(lambdaFn.valueParameters[1]) }
                )
            })
        }
    }

    private fun IrBuilderWithScope.injectableConstructorCall(
        function: IrFunction,
        component: () -> IrExpression,
        parameters: () -> IrExpression
    ): IrExpression {
        val componentGet = injektPackage.memberScope
            .findFirstFunction("get") {
                it.annotations.hasAnnotation(InjektClassNames.KeyOverloadStub) &&
                        it.extensionReceiverParameter?.type == this@BindingModuleGenerator.component.defaultType
            }

        val parametersGet = this@BindingModuleGenerator.parameters.unsubstitutedMemberScope
            .findSingleFunction(Name.identifier("get"))

        return irCall(function).apply {
            var paramIndex = 0

            function.valueParameters
                .map { param ->
                    val paramExpr = if (param.annotations.hasAnnotation(
                            InjektClassNames.Param
                        )
                    ) {
                        irCall(
                            callee = symbolTable.referenceSimpleFunction(
                                parametersGet
                            ),
                            type = param.type
                        ).apply {
                            dispatchReceiver = parameters()
                            putTypeArgument(0, param.type)
                            putValueArgument(0, irInt(paramIndex))
                            ++paramIndex
                        }
                    } else {
                        irCall(
                            symbolTable.referenceSimpleFunction(
                                componentGet
                            ),
                            param.type
                        ).apply {
                            extensionReceiver = component()
                            putTypeArgument(0, param.type)

                            val qualifiers: List<IrExpression> = param
                                .descriptor
                                .getSyntheticAnnotationsForType(qualifier.defaultType)
                                .map { syntheticAnnotationAccessor(it) }

                            if (qualifiers.isNotEmpty()) {
                                putValueArgument(
                                    0,
                                    qualifiers
                                        .reduceRight { currentQualifier, acc ->
                                            irCall(
                                                symbolTable.referenceSimpleFunction(
                                                    qualifier.unsubstitutedMemberScope
                                                        .findSingleFunction(
                                                            Name.identifier(
                                                                "plus"
                                                            )
                                                        )
                                                ),
                                                qualifier.defaultType.toIrType()
                                            ).apply {
                                                dispatchReceiver =
                                                    currentQualifier
                                                putValueArgument(0, acc)
                                            }
                                        }
                                )
                            }
                        }
                    }

                    putValueArgument(param.index, paramExpr)
                }
        }
    }
}
