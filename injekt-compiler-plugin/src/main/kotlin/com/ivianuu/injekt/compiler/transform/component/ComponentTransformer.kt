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
import com.ivianuu.injekt.compiler.getClassFromSingleValueAnnotation
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.ReaderTransformer
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ComponentTransformer(
    pluginContext: IrPluginContext,
    private val readerTransformer: ReaderTransformer
) : AbstractInjektTransformer(pluginContext) {

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
                    "com.ivianuu.injekt.initializeComponents"
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

        if (buildComponentCalls.isEmpty()) return

        val declarationGraph = DeclarationGraph(module, pluginContext, readerTransformer)
            .also { it.initialize() }

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
    ): IrExpression {
        val componentTree = ComponentTree(
            pluginContext, declarationGraph.componentFactories.groupBy {
                it.factory.functions
                    .filterNot { it.isFakeOverride }
                    .single()
                    .returnType
            }, declarationGraph.entryPoints.groupBy {
                it.entryPoint.getClassFromSingleValueAnnotation(
                    InjektFqNames.EntryPoint,
                    pluginContext
                ).defaultType
            }
        )

        return DeclarationIrBuilder(pluginContext, call.call.symbol).run {
            irBlock {
                componentTree.nodes
                    .filter { it.parentComponent == null }
                    .forEach { node ->
                        val componentFactoryImpl = ComponentFactoryImpl(
                            scope.getLocalDeclarationParent(),
                            null,
                            node,
                            null,
                            pluginContext,
                            declarationGraph,
                            symbols
                        )
                        +componentFactoryImpl.getClass()
                        +irCall(
                            symbols.componentFactories
                                .functions
                                .single { it.owner.name.asString() == "register" }
                        ).apply {
                            dispatchReceiver =
                                irGetObject(symbols.componentFactories)

                            putValueArgument(
                                0,
                                IrClassReferenceImpl(
                                    UNDEFINED_OFFSET,
                                    UNDEFINED_OFFSET,
                                    irBuiltIns.kClassClass.typeWith(node.factory.factory.defaultType),
                                    node.factory.factory.symbol,
                                    node.factory.factory.defaultType
                                )
                            )

                            putValueArgument(
                                1,
                                irCall(
                                    componentFactoryImpl.factoryClass.constructors
                                        .single()
                                )
                            )
                        }
                    }
            }
        }
    }

}
