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
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addExtensionReceiver
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

class BindingGenerator(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val behavior = getClass(InjektClassNames.Behavior)
    private val component = getClass(InjektClassNames.Component)
    private val componentBuilder = getClass(InjektClassNames.ComponentBuilder)
    private val duplicateStrategy = getClass(InjektClassNames.DuplicateStrategy)
    private val parameters = getClass(InjektClassNames.Parameters)
    private val qualifier = getClass(InjektClassNames.Qualifier)
    private val scope = getClass(InjektClassNames.Scope)

    override fun visitFile(declaration: IrFile): IrFile {
        super.visitFile(declaration)

        val injectableClasses = mutableListOf<IrClass>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.descriptor.getSyntheticAnnotationPropertiesOfType(behavior.defaultType)
                        .isNotEmpty()
                ) {
                    injectableClasses += declaration
                }

                return super.visitClass(declaration)
            }
        })

        injectableClasses.forEach {
            module(declaration, it)
        }

        return declaration
    }

    private fun module(
        file: IrFile,
        injectClass: IrClass
    ) {
        file.addFunction {
            name = Name.identifier("${injectClass.name.asString().decapitalize()}Module")
            returnType = pluginContext.irBuiltIns.unitType
            origin = InjektOrigin
        }.apply func@{
            addExtensionReceiver(componentBuilder.defaultType.toIrType())
            val extensionReceiver = this.extensionReceiverParameter!!

            pluginContext.irTrace.record(
                InjektWritableSlices.IS_MODULE, this, Unit
            )

            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                +irCall(
                    callee = symbolTable.referenceSimpleFunction(
                        injektPackage.memberScope.findFirstFunction("bind") {
                            it.annotations.hasAnnotation(InjektClassNames.KeyOverloadStub)
                        }
                    ),
                    type = pluginContext.irBuiltIns.unitType
                ).apply {
                    this.extensionReceiver = irGet(extensionReceiver)

                    putTypeArgument(0, injectClass.descriptor.defaultType.toIrType())

                    val behaviors =
                        injectClass.descriptor.getSyntheticAnnotationPropertiesOfType(behavior.defaultType)
                            .toSet()
                            .map { behaviorProperty ->
                                irCall(
                                    symbolTable.referenceSimpleFunction(behaviorProperty.getter!!),
                                    behaviorProperty.type.toIrType()
                                )
                            }

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

                    val scopeAnnotationProperty =
                        injectClass.descriptor
                            .getSyntheticAnnotationPropertiesOfType(this@BindingGenerator.scope.defaultType)
                            .singleOrNull()
                    if (scopeAnnotationProperty != null) {
                        putValueArgument(
                            2,
                            irCall(
                                symbolTable.referenceSimpleFunction(scopeAnnotationProperty.getter!!),
                                scopeAnnotationProperty.type.toIrType()
                            )
                        )
                        pluginContext.irTrace.record(
                            InjektWritableSlices.SCOPE,
                            this@func, scopeAnnotationProperty
                        )
                    }

                    putValueArgument(
                        3,
                        IrGetEnumValueImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            duplicateStrategy.defaultType.toIrType(),
                            symbolTable.referenceEnumEntry(
                                duplicateStrategy.unsubstitutedMemberScope
                                    .getContributedClassifier(
                                        Name.identifier("Drop"),
                                        NoLookupLocation.FROM_BACKEND
                                    ) as ClassDescriptor
                            )
                        )
                    )

                    putValueArgument(4, bindingProvider(injectClass.descriptor))
                }
            }
        }
    }

    private fun IrBuilderWithScope.bindingProvider(
        descriptor: ClassDescriptor
    ): IrExpression {
        val providerType = KotlinTypeFactory.simpleType(
            context.builtIns.getFunction(2).defaultType,
            arguments = listOf(
                component.defaultType.asTypeProjection(),
                parameters.defaultType.asTypeProjection(),
                descriptor.defaultType.asTypeProjection()
            )
        )

        return irLambdaExpression(
            createFunctionDescriptor(providerType),
            providerType.toIrType()
        ) { lambdaFn ->
            if (descriptor.kind == ClassKind.OBJECT) {
                +irReturn(irGetObject(symbolTable.referenceClass(descriptor)))
                return@irLambdaExpression
            }

            val injektConstructor = descriptor.findInjektConstructor()!!

            val componentGet = injektPackage.memberScope
                .findFirstFunction("get") {
                    it.annotations.hasAnnotation(InjektClassNames.KeyOverloadStub)
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
                                    extensionReceiver =
                                        irGet(lambdaFn.valueParameters[0])
                                    putTypeArgument(0, param.type.toIrType())

                                    val qualifiers: List<IrExpression> = param
                                        .getSyntheticAnnotationPropertiesOfType(qualifier.defaultType)
                                        .map { qualifierProperty ->
                                            irCall(
                                                symbolTable.referenceSimpleFunction(
                                                    qualifierProperty.getter!!
                                                ),
                                                qualifierProperty.type.toIrType()
                                            )
                                        }

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

    private fun ClassDescriptor.findInjektConstructor(): ClassConstructorDescriptor? {
        return if (kind == ClassKind.OBJECT) null
        else constructors.singleOrNull { it.annotations.hasAnnotation(InjektClassNames.InjektConstructor) }
            ?: unsubstitutedPrimaryConstructor!!
    }

}
