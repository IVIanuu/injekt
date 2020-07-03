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

package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.NameProvider
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.Name

class FactoryMembers(
    private val pluginContext: IrPluginContext,
    private val symbols: InjektSymbols
) {

    lateinit var blockBuilder: IrBlockBuilder

    private val getFunctions = mutableListOf<IrFunction>()
    private val getFunctionsNameProvider = NameProvider()

    private val membersNameProvider = NameProvider()

    enum class CachedValueState {
        Uninitialized,
        Initializing,
        Delegated,
        Initialized
    }

    private val initStates = mutableMapOf<Key, CachedValueState>()
    private val delegateVariables = mutableMapOf<Key, IrVariable>()

    fun cachedValue(
        key: Key,
        initializer: IrBuilderWithScope.() -> IrExpression
    ): FactoryExpression {
        fun getState() = initStates.getOrPut(key) { CachedValueState.Uninitialized }
        val initializerExpression: IrExpression = when (getState()) {
            CachedValueState.Uninitialized -> {
                initStates[key] = CachedValueState.Initializing

                val expression = initializer(blockBuilder)

                val finalExpression = if (getState() == CachedValueState.Delegated) {
                    val delegateVariable = delegateVariables.getValue(key)

                    blockBuilder.irBlock {
                        +irCall(
                            symbols.delegateFactory.owner
                                .properties
                                .single()
                                .setter!!
                        ).apply {
                            dispatchReceiver = irGet(delegateVariable)
                            putValueArgument(0, expression)
                        }
                        +irGet(delegateVariable)
                    }
                } else {
                    expression
                }

                initStates[key] = CachedValueState.Initialized

                finalExpression
            }
            CachedValueState.Initializing -> {
                initStates[key] = CachedValueState.Delegated
                blockBuilder.irCall(symbols.delegateFactory.constructors.single()).apply {
                    putTypeArgument(0, key.type) // todo might be wrong
                }
            }
            CachedValueState.Delegated -> error("Unexpected state")
            CachedValueState.Initialized -> error("Unexpected state")
        }
        val tmp = blockBuilder.irTemporary(
            value = initializerExpression,
            nameHint = membersNameProvider.allocateForType(key.type).asString()
        )

        val expression: FactoryExpression = { irGet(tmp) }

        if (getState() == CachedValueState.Delegated) {
            delegateVariables[key] = tmp
        }

        return expression
    }

    fun getGetFunction(
        key: Key,
        body: IrBuilderWithScope.(IrFunction) -> IrExpression
    ): IrFunction {
        return buildFun {
            this.name = Name.identifier(
                getFunctionsNameProvider.allocateForGroup(
                    key.type.classifierOrFail.descriptor.name.asString()
                )
            )
            returnType = key.type
            visibility = Visibilities.LOCAL
        }.apply {
            with(blockBuilder) {
                +this@apply
                this@apply.parent = scope.getLocalDeclarationParent()
            }
            this.body = DeclarationIrBuilder(pluginContext, symbol).run {
                irExprBody(body(this, this@apply))
            }
        }.also { getFunctions += it }
    }

}
