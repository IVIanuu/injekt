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
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.at
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class ModuleProviderFactory(
    private val moduleFunction: IrFunction,
    private val declarationStore: InjektDeclarationStore,
    private val pluginContext: IrPluginContext,
    private val symbols: InjektSymbols
) {

    fun providerForClass(clazz: IrClass): IrExpression {
        return InjektDeclarationIrBuilder(pluginContext, moduleFunction.symbol)
            .classFactoryLambda(clazz, declarationStore.getMembersInjectorForClassOrNull(clazz))
    }

    fun providerForDefinition(definition: IrFunctionExpression): IrExpression {
        val definitionFunction = definition.function

        val type = definition.function.returnType
            .remapTypeParameters(definitionFunction, moduleFunction)

        val assistedParameterCalls = mutableListOf<IrCall>()

        definitionFunction.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.dispatchReceiver?.type == definitionFunction.valueParameters.first().type) {
                    assistedParameterCalls += expression
                }
                return super.visitCall(expression)
            }
        })

        val parameterNameProvider = NameProvider()

        val parameters =
            mutableListOf<InjektDeclarationIrBuilder.FactoryParameter>()

        val parametersByFunctionParameters =
            mutableMapOf<IrValueParameter, InjektDeclarationIrBuilder.FactoryParameter>()

        definitionFunction.valueParameters
            .drop(1) // assisted
            .forEach { valueParameter ->
                parameters += InjektDeclarationIrBuilder.FactoryParameter(
                    name = parameterNameProvider.allocateForType(valueParameter.type)
                        .asString(), // todo use param name
                    type = valueParameter.type,
                    assisted = false // todo maybe transform assisted in another step and check here if it has @Assisted
                ).also {
                    parametersByFunctionParameters[valueParameter] = it
                }
            }

        val parametersByCall =
            mutableMapOf<IrCall, InjektDeclarationIrBuilder.FactoryParameter>()
        assistedParameterCalls.forEach { call ->
            val exprQualifiers =
                pluginContext.irTrace[InjektWritableSlices.QUALIFIERS, call] ?: emptyList()
            parameters += InjektDeclarationIrBuilder.FactoryParameter(
                name = parameterNameProvider.allocateForType(call.type).asString(),
                type = call.type
                    .withAnnotations(exprQualifiers),
                assisted = call in assistedParameterCalls
            ).also {
                parametersByCall[call] = it
            }
        }

        return InjektDeclarationIrBuilder(pluginContext, moduleFunction.symbol).factoryLambda(
            parameters = parameters,
            returnType = type,
            createExpr = { lambda, parametersMap ->
                val body = definitionFunction.body!!
                body.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitReturn(expression: IrReturn): IrExpression {
                        return if (expression.returnTargetSymbol != definitionFunction.symbol) {
                            super.visitReturn(expression)
                        } else {
                            at(expression.startOffset, expression.endOffset)
                            DeclarationIrBuilder(
                                pluginContext,
                                lambda.symbol
                            ).irReturn(expression.value.transform(this, null))
                        }
                    }

                    override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                        if (declaration.parent == definitionFunction)
                            declaration.parent = lambda
                        return super.visitDeclaration(declaration)
                    }

                    override fun visitCall(expression: IrCall): IrExpression {
                        super.visitCall(expression)
                        return when (expression) {
                            in assistedParameterCalls -> {
                                irGet(parametersMap.getValue(parametersByCall.getValue(expression)))
                            }
                            else -> expression
                        }
                    }

                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                        val valueParameter = definitionFunction.valueParameters
                            .singleOrNull { it.symbol == expression.symbol }

                        if (valueParameter != null) {
                            parametersMap[parametersByFunctionParameters[valueParameter]]
                                ?.let { return irGet(it) }
                        }

                        return super.visitGetValue(expression)
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
