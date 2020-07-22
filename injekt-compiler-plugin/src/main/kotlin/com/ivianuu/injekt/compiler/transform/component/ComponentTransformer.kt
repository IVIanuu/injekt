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
import com.ivianuu.injekt.compiler.transform.ImplicitTransformer
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ComponentTransformer(
    pluginContext: IrPluginContext,
    private val implicitTransformer: ImplicitTransformer
) : AbstractInjektTransformer(pluginContext) {

    private data class InitializeComponentsCall(
        val call: IrCall,
        val scope: ScopeWithIr,
        val file: IrFile
    )

    override fun lower() {
        val initializeComponentCalls =
            mutableListOf<InitializeComponentsCall>()

        module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.descriptor.fqNameSafe.asString() ==
                    "com.ivianuu.injekt.initializeComponents"
                ) {
                    initializeComponentCalls += InitializeComponentsCall(
                        expression,
                        currentScope!!,
                        currentFile
                    )
                }
                return super.visitCall(expression)
            }
        })

        if (initializeComponentCalls.isEmpty()) return

        val declarationGraph = DeclarationGraph(module, pluginContext, implicitTransformer)
            .also { it.initialize() }

        if (declarationGraph.rootComponentFactories.isEmpty()) return

        val callMap = initializeComponentCalls.associateWith {
            transformInitializeComponentsCall(declarationGraph, it)
        }.mapKeys { it.key.call }

        module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                return callMap[expression] ?: super.visitCall(expression)
            }
        })
    }

    private fun transformInitializeComponentsCall(
        declarationGraph: DeclarationGraph,
        call: InitializeComponentsCall
    ): IrExpression {
        val rootComponentFactories = declarationGraph.rootComponentFactories.groupBy {
            it.functions
                .filterNot { it.isFakeOverride }
                .single()
                .returnType
        }

        val entryPoints = declarationGraph.entryPoints.groupBy {
            it.getClassFromSingleValueAnnotation(
                InjektFqNames.EntryPoint,
                pluginContext
            )
        }

        return DeclarationIrBuilder(pluginContext, call.call.symbol).run {
            irBlock {
                rootComponentFactories.values
                    .flatten()
                    .forEach { componentFactory ->
                        val component = componentFactory.functions
                            .filterNot { it.isFakeOverride }
                            .single()
                            .returnType
                            .classOrNull!!
                            .owner

                        val componentFactoryImpl = ComponentFactoryImpl(
                            call.file,
                            componentFactory,
                            entryPoints.getOrElse(component) { emptyList() },
                            null,
                            pluginContext,
                            declarationGraph,
                            symbols
                        )

                        componentFactoryImpl.init()
                        call.file.addChild(componentFactoryImpl.clazz)

                        +irCall(
                            symbols.rootComponentFactories
                                .functions
                                .single { it.owner.name.asString() == "register" }
                        ).apply {
                            dispatchReceiver =
                                irGetObject(symbols.rootComponentFactories)

                            putValueArgument(
                                0,
                                IrClassReferenceImpl(
                                    UNDEFINED_OFFSET,
                                    UNDEFINED_OFFSET,
                                    irBuiltIns.kClassClass.typeWith(componentFactory.defaultType),
                                    componentFactory.symbol,
                                    componentFactory.defaultType
                                )
                            )

                            putValueArgument(
                                1,
                                irCall(
                                    componentFactoryImpl.clazz.constructors
                                        .single()
                                )
                            )
                        }
                    }
            }
        }
    }

}
