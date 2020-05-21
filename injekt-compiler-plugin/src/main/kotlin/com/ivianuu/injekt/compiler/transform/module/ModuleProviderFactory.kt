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

package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.irTrace
import com.ivianuu.injekt.compiler.remapTypeParameters
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.withAnnotations
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.at
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class ModuleProviderFactory(
    private val declarationStore: InjektDeclarationStore,
    private val module: ModuleImpl,
    private val pluginContext: IrPluginContext,
    private val symbols: InjektSymbols
) {

    fun providerForClass(
        name: Name,
        clazz: IrClass,
        visibility: Visibility
    ): IrClass {
        val constructor = clazz.constructors.singleOrNull()
        return InjektDeclarationIrBuilder(pluginContext, module.clazz.symbol).factory(
            name = name,
            visibility = visibility,
            typeParametersContainer = module.function,
            parameters = constructor?.valueParameters
                ?.map { valueParameter ->
                    InjektDeclarationIrBuilder.FactoryParameter(
                        name = valueParameter.name.asString(),
                        type = valueParameter.type,
                        assisted = valueParameter.hasAnnotation(InjektFqNames.Assisted),
                        requirement = false
                    )
                } ?: emptyList(),
            membersInjector = declarationStore.getMembersInjectorForClassOrNull(clazz),
            returnType = clazz.defaultType,
            createExpr = { createFunction ->
                if (clazz.kind == ClassKind.OBJECT) {
                    irGetObject(clazz.symbol)
                } else {
                    irCall(constructor!!).apply {
                        constructor.valueParameters.indices
                            .map { createFunction.valueParameters[it] }
                            .forEach {
                                putValueArgument(
                                    it.index,
                                    irGet(it)
                                )
                            }
                    }
                }
            }
        )
    }

    fun providerForDefinition(
        name: Name,
        definition: IrFunctionExpression,
        visibility: Visibility,
        moduleFieldsByParameter: Map<IrValueDeclaration, IrField>
    ): IrClass {
        val definitionFunction = definition.function

        val type = definition.function.returnType
            .remapTypeParameters(definitionFunction, module.clazz)

        val assistedParameterCalls = mutableListOf<IrCall>()
        val dependencyCalls = mutableListOf<IrCall>()
        val providerDslFunctionCalls = mutableListOf<IrCall>()
        val capturedModuleValueParameters = mutableListOf<IrValueDeclaration>()

        definitionFunction.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                when {
                    expression.dispatchReceiver?.type?.classOrNull == symbols.providerDsl &&
                            expression.symbol.owner.name.asString() == "get" -> {
                        dependencyCalls += expression
                    }
                    expression.symbol.owner.hasAnnotation(InjektFqNames.AstProviderDsl) -> {
                        providerDslFunctionCalls += expression
                    }
                    expression.dispatchReceiver?.type == definitionFunction.valueParameters.single().type -> {
                        assistedParameterCalls += expression
                    }
                }
                return super.visitCall(expression)
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                super.visitGetValue(expression)

                if (moduleFieldsByParameter.keys.any { it.symbol == expression.symbol }) {
                    capturedModuleValueParameters += expression.symbol.owner
                }

                return expression
            }
        })

        val parameters = mutableListOf<InjektDeclarationIrBuilder.FactoryParameter>()

        if (capturedModuleValueParameters.isNotEmpty()) {
            parameters += InjektDeclarationIrBuilder.FactoryParameter(
                name = "module",
                type = module.clazz.defaultType.remapTypeParameters(
                    definitionFunction,
                    module.clazz
                ),
                assisted = false,
                requirement = true
            )
        }

        val parameterNameProvider = NameProvider()

        val parametersByCall =
            mutableMapOf<IrCall, List<InjektDeclarationIrBuilder.FactoryParameter>>()
        (assistedParameterCalls + dependencyCalls).forEach { call ->
            val depQualifiers =
                pluginContext.irTrace[InjektWritableSlices.QUALIFIERS, call] ?: emptyList()
            parameters += InjektDeclarationIrBuilder.FactoryParameter(
                name = parameterNameProvider.allocateForType(call.type).asString(),
                type = call.type
                    .withAnnotations(depQualifiers)
                    .remapTypeParameters(definitionFunction, module.clazz),
                assisted = call in assistedParameterCalls,
                requirement = false
            ).also {
                parametersByCall[call] = listOf(it)
            }
        }

        providerDslFunctionCalls.forEach { call ->
            parameters += call.symbol.owner
                .valueParameters
                .filter { it.name.asString().startsWith("dsl_provider\$") }
                .map {
                    InjektDeclarationIrBuilder.FactoryParameter(
                        name = "p${parameters.size}",
                        type = call.type
                            .remapTypeParameters(definitionFunction, module.clazz),
                        assisted = call in assistedParameterCalls,
                        requirement = false
                    )
                }
                .also {
                    parametersByCall[call] = it
                }
        }

        return InjektDeclarationIrBuilder(pluginContext, module.clazz.symbol).factory(
            name = name,
            visibility = visibility,
            typeParametersContainer = module.function,
            parameters = parameters,
            membersInjector = null,
            returnType = type,
            createExpr = { createFunction ->
                val body = definitionFunction.body!!
                body.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitReturn(expression: IrReturn): IrExpression {
                        return if (expression.returnTargetSymbol != definitionFunction.symbol) {
                            super.visitReturn(expression)
                        } else {
                            at(expression.startOffset, expression.endOffset)
                            DeclarationIrBuilder(
                                pluginContext,
                                createFunction.symbol
                            ).irReturn(expression.value.transform(this, null))
                        }
                    }

                    override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                        if (declaration.parent == definitionFunction)
                            declaration.parent = createFunction
                        return super.visitDeclaration(declaration)
                    }

                    override fun visitCall(expression: IrCall): IrExpression {
                        super.visitCall(expression)
                        return when (expression) {
                            in assistedParameterCalls, in dependencyCalls -> {
                                irGet(createFunction.valueParameters.single {
                                    it.name.asString() == parametersByCall.getValue(expression)
                                        .singleOrNull()?.name
                                })
                            }
                            in providerDslFunctionCalls -> {
                                expression.apply {
                                    expression.symbol.owner.valueParameters
                                        .filter {
                                            it.name.asString().startsWith("dsl_provider\$")
                                        }
                                        .forEachIndexed { dslProvidersIndex, valueParameter ->
                                            putValueArgument(
                                                valueParameter.index,
                                                irGet(createFunction.valueParameters.single {
                                                    it.name.asString() ==
                                                            parametersByCall.getValue(expression)[dslProvidersIndex]?.name
                                                })

                                            )
                                        }
                                }
                            }
                            else -> expression
                        }
                    }

                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                        return if (moduleFieldsByParameter.keys.none { it.symbol == expression.symbol }) {
                            super.visitGetValue(expression)
                        } else {
                            val field = moduleFieldsByParameter[expression.symbol.owner]!!
                            return irGetField(
                                irGet(createFunction.valueParameters.first()),
                                field
                            )
                        }
                    }

                })

                irBlock {
                    body.statements.forEach {
                        +it
                    }
                }
            }
        )
    }

}
