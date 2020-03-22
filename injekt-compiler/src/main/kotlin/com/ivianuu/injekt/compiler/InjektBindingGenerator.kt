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
import org.jetbrains.kotlin.ir.IrStatement
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
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.addToStdlib.cast

class InjektBindingGenerator(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val behavior = getClass(InjektClassNames.Behavior)
    private val boundBehavior = getClass(InjektClassNames.BoundBehavior)
    private val component = getClass(InjektClassNames.Component)
    private val componentBuilder = getClass(InjektClassNames.ComponentBuilder)
    private val parameters = getClass(InjektClassNames.Parameters)
    private val qualifier = getClass(InjektClassNames.Qualifier)

    override fun visitFile(declaration: IrFile): IrFile {
        super.visitFile(declaration)

        val injectableClasses = mutableListOf<IrClass>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.descriptor.getAnnotatedAnnotations(InjektClassNames.BehaviorMarker).isNotEmpty()) {
                    injectableClasses += declaration
                }

                return super.visitClass(declaration)
            }
        })

        injectableClasses.forEach {
            componentBuilderContributorFunction(declaration, it)
        }

        return declaration
    }

    private fun componentBuilderContributorFunction(
        file: IrFile,
        injectClass: IrClass
    ) {
        file.addFunction {
            name = Name.identifier("bind${injectClass.name.asString()}")
            returnType = pluginContext.irBuiltIns.unitType
            origin = InjektOrigin
        }.apply {
            addExtensionReceiver(componentBuilder.defaultType.toIrType())
            val extensionReceiver = this.extensionReceiverParameter!!

            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                +irCall(
                    callee = symbolTable.referenceSimpleFunction(
                        componentBuilder.unsubstitutedMemberScope.findFirstFunction("bind") {
                            it.typeParameters.singleOrNull()?.isReified ?: false
                        }
                    ),
                    type = pluginContext.irBuiltIns.unitType
                ).apply {
                    dispatchReceiver = irGet(extensionReceiver)

                    putTypeArgument(0, injectClass.descriptor.defaultType.toIrType())

                    val behaviors =
                        injectClass.descriptor.getAnnotatedAnnotations(InjektClassNames.BehaviorMarker)
                            .map {
                                val behaviorMarkerAnnotation = it.annotationClass!!
                                    .annotations.findAnnotation(InjektClassNames.BehaviorMarker)!!
                                irGetObject(
                                    symbolTable.referenceClass(
                                        behaviorMarkerAnnotation.argumentValue("type")!!
                                            .cast<KClassValue>()
                                            .getArgumentType(this@InjektBindingGenerator.pluginContext.moduleDescriptor)
                                            .constructor
                                            .declarationDescriptor as ClassDescriptor
                                    )
                                ) as IrExpression
                            }
                            .toMutableList()

                    val scopeAnnotation =
                        injectClass.descriptor.getAnnotatedAnnotations(InjektClassNames.ScopeMarker)
                            .singleOrNull()
                    if (scopeAnnotation != null) {
                        behaviors += irCall(
                            symbolTable.referenceConstructor(
                                boundBehavior.unsubstitutedPrimaryConstructor!!
                            ),
                            boundBehavior.defaultType.toIrType()
                        ).apply {
                            val scopeObject =
                                getClass(scopeAnnotation.fqName!!).companionObjectDescriptor!!
                            putValueArgument(
                                0,
                                irGetObject(symbolTable.referenceClass(scopeObject))
                            )
                        }
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

                    putValueArgument(3, bindingProvider(injectClass.descriptor))
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

                                    val qualifiers: List<IrExpression> = param
                                        .getAnnotatedAnnotations(InjektClassNames.QualifierMarker)
                                        .map { getClass(it.fqName!!).companionObjectDescriptor!! }
                                        .map {
                                            irGetObject(
                                                symbolTable.referenceClass(
                                                    it
                                                )
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
