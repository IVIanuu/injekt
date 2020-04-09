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
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.constructors
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
    private val componentBuilder = getClass(InjektClassNames.ComponentBuilder)
    private val module = getClass(InjektClassNames.Module)
    private val moduleMarker = getClass(InjektClassNames.ModuleMarker)
    private val parameters = getClass(InjektClassNames.Parameters)
    private val qualifier = getClass(InjektClassNames.Qualifier)
    private val scope = getClass(InjektClassNames.Scope)

    override fun visitFile(declaration: IrFile): IrFile {
        super.visitFile(declaration)

        val injectableClasses = mutableListOf<IrClass>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.descriptor.getSyntheticAnnotationDeclarationsOfType(behavior.defaultType)
                        .isNotEmpty()
                ) {
                    injectableClasses += declaration
                }

                return super.visitClass(declaration)
            }
        })

        injectableClasses.forEach { declaration.addChild(moduleProperty(it)) }

        (declaration as IrFileImpl).metadata = MetadataSource.File(
            declaration.declarations
                .map { it.descriptor }
        )

        return declaration
    }

    private inner class ModulePropertyDescriptor(injectClass: IrClass) :
        PropertyDescriptorImpl(
            injectClass.descriptor.containingDeclaration,
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
            Name.identifier("${injectClass.name.asString()}Module"),
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

    private fun moduleProperty(injectClass: IrClass): IrProperty {
        val descriptor = ModulePropertyDescriptor(injectClass)
        return IrPropertyImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            InjektOrigin,
            IrPropertySymbolImpl(descriptor)
        ).apply property@{
            parent = injectClass.parent
            metadata = MetadataSource.Property(descriptor)

            val builder = DeclarationIrBuilder(pluginContext, symbol)
            annotations += builder.irCallConstructor(
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
                initializer = builder.irExprBody(
                    builder.irCall(
                        symbolTable.referenceSimpleFunction(
                            injektPackage.memberScope.findFirstFunction("Module") {
                                it.valueParameters.first().name.asString() == "scope"
                            }
                        ),
                        module.defaultType.toIrType()
                    ).apply {
                        val scopeAnnotation =
                            injectClass.descriptor
                                .getSyntheticAnnotationsForType(this@BindingModuleGenerator.scope.defaultType)
                                .singleOrNull()
                        if (scopeAnnotation != null) {
                            putValueArgument(
                                0,
                                builder.syntheticAnnotationAccessor(scopeAnnotation)
                            )
                        } else {
                            putValueArgument(
                                0,
                                builder.irCall(
                                    symbolTable.referenceSimpleFunction(
                                        injektPackage.memberScope
                                            .getContributedVariables(
                                                Name.identifier("ApplicationScope"),
                                                NoLookupLocation.FROM_BACKEND
                                            )
                                            .single()
                                            .getter!!
                                    ),
                                    scope.defaultType.toIrType()
                                )
                            )
                        }
                        val blockType = KotlinTypeFactory.simpleType(
                            pluginContext.builtIns.getFunction(1).defaultType,
                            arguments = listOf(
                                componentBuilder.defaultType.asTypeProjection(),
                                pluginContext.builtIns.unitType.asTypeProjection()
                            )
                        )
                        putValueArgument(
                            2,
                            builder.irLambdaExpression(
                                builder.createFunctionDescriptor(blockType),
                                blockType.toIrType()
                            ) { lambdaFn ->
                                +irCall(
                                    callee = symbolTable.referenceSimpleFunction(
                                        injektPackage.memberScope.findFirstFunction("bind") {
                                            it.annotations.hasAnnotation(InjektClassNames.KeyOverloadStub)
                                        }
                                    ),
                                    type = pluginContext.irBuiltIns.unitType
                                ).apply {
                                    this.extensionReceiver = irGet(lambdaFn.valueParameters[0])

                                    putTypeArgument(
                                        0,
                                        injectClass.defaultType
                                    )

                                    val behaviors =
                                        injectClass.descriptor.getSyntheticAnnotationsForType(
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
                                                            behavior.unsubstitutedMemberScope
                                                                .findSingleFunction(
                                                                    Name.identifier(
                                                                        "plus"
                                                                    )
                                                                )
                                                        ),
                                                        behavior.defaultType.toIrType()
                                                    ).apply {
                                                        dispatchReceiver = currentBehavior
                                                        putValueArgument(0, acc)
                                                    }
                                                }
                                        )
                                    }

                                    putValueArgument(4, bindingProvider(injectClass))
                                }
                            }
                        )
                    }
                )
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

    private fun IrBuilderWithScope.bindingProvider(
        injectClass: IrClass
    ): IrExpression {
        val providerType = KotlinTypeFactory.simpleType(
            context.builtIns.getFunction(2).defaultType,
            arguments = listOf(
                component.defaultType.asTypeProjection(),
                parameters.defaultType.asTypeProjection(),
                injectClass.defaultType.toKotlinType().asTypeProjection()
            )
        )

        return irLambdaExpression(
            createFunctionDescriptor(providerType),
            providerType.toIrType()
        ) { lambdaFn ->
            if (injectClass.kind == ClassKind.OBJECT) {
                +irReturn(irGetObject(injectClass.symbol))
                return@irLambdaExpression
            }

            val injektConstructor = injectClass.findInjektConstructor()!!

            val componentGet = injektPackage.memberScope
                .findFirstFunction("get") {
                    it.annotations.hasAnnotation(InjektClassNames.KeyOverloadStub)
                }

            val parametersGet = parameters.unsubstitutedMemberScope
                .findSingleFunction(Name.identifier("get"))

            +irReturn(
                irCall(injektConstructor).apply {
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
                                    type = param.type
                                ).apply {
                                    dispatchReceiver =
                                        irGet(lambdaFn.valueParameters[1])
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
                                    extensionReceiver =
                                        irGet(lambdaFn.valueParameters[0])
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
            )
        }
    }

    private fun IrClass.findInjektConstructor(): IrConstructor? {
        return if (kind == ClassKind.OBJECT) null
        else constructors.singleOrNull { it.annotations.hasAnnotation(InjektClassNames.InjektConstructor) }
            ?: primaryConstructor
    }

}
