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

package com.ivianuu.injekt.compiler.transform.component

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ComponentTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private data class BuildComponentCall(
        val call: IrCall,
        val scope: ScopeWithIr,
        val file: IrFile
    )

    override fun lower() {
        val buildComponentCalls =
            mutableListOf<BuildComponentCall>()

        module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.descriptor.fqNameSafe.asString() ==
                    "com.ivianuu.injekt.buildComponents"
                ) {
                    buildComponentCalls += BuildComponentCall(
                        expression,
                        currentScope!!,
                        currentFile
                    )
                }
                return super.visitCall(expression)
            }
        })

        println("build component call $buildComponentCalls")

        if (buildComponentCalls.isEmpty()) return

        val declarationGraph = DeclarationGraph(module, pluginContext)
            .also { it.initialize() }

        println("factories ${declarationGraph.componentFactories}")

        if (declarationGraph.componentFactories.isEmpty()) return

        val callMap = buildComponentCalls.associateWith {
            transformBuildComponentCall(declarationGraph, it)
        }.mapKeys { it.key.call }

        module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                return callMap[expression] ?: super.visitCall(expression)
            }
        })
    }

    private fun transformBuildComponentCall(
        declarationGraph: DeclarationGraph,
        call: BuildComponentCall
    ): IrCall {
        println("process call ${declarationGraph.componentFactories
            .map { it.factory.render() }}"
        )

        val componentTree = ComponentTree(
            pluginContext, declarationGraph.componentFactories.groupBy {
                it.factory.functions
                    .single()
                    .returnType
            })

        val components = mutableMapOf<IrClassSymbol, IrClass>()

        componentTree.nodes.forEach { node ->
        }

        /*
        return DeclarationIrBuilder(pluginContext, expression.symbol).run {
            irBlock {
                components.forEach { (compositionType, factoryFunctionImpl) ->
                    if (factoryFunctionImpl.owner.hasAnnotation(InjektFqNames.ChildFactory)) return@forEach
                    +irCall(
                        compositionSymbols.compositionFactories
                            .functions
                            .single { it.owner.name.asString() == "register" }
                    ).apply {
                        dispatchReceiver =
                            irGetObject(compositionSymbols.compositionFactories)

                        putValueArgument(
                            0,
                            IrClassReferenceImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                irBuiltIns.kClassClass.typeWith(compositionType.defaultType),
                                compositionType,
                                compositionType.defaultType
                            )
                        )

                        putValueArgument(
                            1,
                            IrFunctionReferenceImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                factoryFunctionImpl.owner.getFunctionType(pluginContext),
                                factoryFunctionImpl,
                                0,
                                null
                            )
                        )
                    }
                }
            }
        }*/

        return call.call
    }

}
