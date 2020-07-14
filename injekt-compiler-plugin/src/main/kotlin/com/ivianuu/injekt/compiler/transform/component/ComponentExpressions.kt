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

import com.ivianuu.injekt.compiler.irLambda
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.typeWith
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName

class ComponentExpressions(
    private val graph: ComponentGraph,
    private val pluginContext: IrPluginContext,
    private val members: ComponentMembers,
    private val parent: ComponentExpressions?,
    private val component: ComponentImpl
) {

    private val bindingExpressions =
        mutableMapOf<BindingRequest, ComponentExpression>()

    fun getBindingExpression(request: BindingRequest): ComponentExpression {
        bindingExpressions[request]?.let { return it }

        val binding = graph.getBinding(request)

        if (binding.owner != component) {
            return parentExpression(request)
                .also { bindingExpressions[request] = it }
        }

        val expression = when (binding) {
            is ChildComponentFactoryBindingNode -> childComponentFactoryExpression(binding) to true
            is ComponentImplBindingNode -> componentExpression(binding) to false
            is GivenBindingNode -> givenExpression(binding) to true
            is InputParameterBindingNode -> inputParameterExpression(binding) to false
            is MapBindingNode -> mapBindingExpression(binding) to false
            is NullBindingNode -> nullExpression(binding) to false
            is ProviderBindingNode -> providerExpression(binding) to true
            is SetBindingNode -> setBindingExpression(binding) to false
        }.let { (expression, forceWrap) ->
            if (forceWrap || component.dependencyRequests.any {
                    it.second.key == binding.key
                }) expression.wrapInFunction(binding.key) else expression
        }

        return expression
            .also { bindingExpressions[request] = it }
    }

    private fun childComponentFactoryExpression(binding: ChildComponentFactoryBindingNode): ComponentExpression =
        { c -> binding.childComponentFactoryExpression(this, c) }

    private fun componentExpression(
        binding: ComponentImplBindingNode
    ): ComponentExpression = { c -> c[binding.component] }

    private fun inputParameterExpression(
        binding: InputParameterBindingNode
    ): ComponentExpression = { irGet(binding.inputParameter) }

    private fun mapBindingExpression(bindingNode: MapBindingNode): ComponentExpression {
        return { c ->
            irBlock {
                val tmpMap = irTemporary(
                    irCall(pluginContext.referenceFunctions(
                        FqName("kotlin.collections.mutableMapOf")
                    ).first { it.owner.valueParameters.isEmpty() })
                )
                val mapType = pluginContext.referenceClass(
                    FqName("kotlin.collections.Map")
                )!!
                bindingNode.functions.forEach { function ->
                    +irCall(
                        tmpMap.type.classOrNull!!
                            .functions
                            .map { it.owner }
                            .single {
                                it.name.asString() == "putAll" &&
                                        it.valueParameters.singleOrNull()?.type?.classOrNull == mapType
                            }
                    ).apply {
                        dispatchReceiver = irGet(tmpMap)
                        putValueArgument(
                            0,
                            irCall(function).apply {
                                if (function.dispatchReceiverParameter != null)
                                    dispatchReceiver =
                                        irGetObject(function.dispatchReceiverParameter!!.type.classOrNull!!)
                                function.valueParameters.forEach { valueParameter ->
                                    putValueArgument(
                                        valueParameter.index,
                                        getBindingExpression(
                                            bindingNode.dependencies.single {
                                                it.key == valueParameter.type.asKey()
                                            }
                                        )
                                            .invoke(this@irBlock, c)
                                    )
                                }
                            }
                        )
                    }
                }

                +irGet(tmpMap)
            }
        }
    }

    private fun setBindingExpression(bindingNode: SetBindingNode): ComponentExpression {
        return { c ->
            irBlock {
                val tmpSet = irTemporary(
                    irCall(pluginContext.referenceFunctions(
                        FqName("kotlin.collections.mutableSetOf")
                    ).first { it.owner.valueParameters.isEmpty() })
                )
                val collectionType = pluginContext.referenceClass(
                    FqName("kotlin.collections.Collection")
                )
                bindingNode.functions.forEach { function ->
                    +irCall(
                        tmpSet.type.classOrNull!!
                            .functions
                            .map { it.owner }
                            .single {
                                it.name.asString() == "addAll" &&
                                        it.valueParameters.singleOrNull()?.type?.classOrNull == collectionType
                            }
                    ).apply {
                        dispatchReceiver = irGet(tmpSet)
                        putValueArgument(
                            0,
                            irCall(function).apply {
                                if (function.dispatchReceiverParameter != null)
                                    dispatchReceiver =
                                        irGetObject(function.dispatchReceiverParameter!!.type.classOrNull!!)
                                function.valueParameters.forEach { valueParameter ->
                                    putValueArgument(
                                        valueParameter.index,
                                        getBindingExpression(
                                            bindingNode.dependencies.single {
                                                it.key == valueParameter.type.asKey()
                                            }
                                        )
                                            .invoke(this@irBlock, c)
                                    )
                                }
                            }
                        )
                    }
                }

                +irGet(tmpSet)
            }
        }
    }

    private fun nullExpression(binding: NullBindingNode): ComponentExpression =
        { irNull() }

    private fun providerExpression(binding: ProviderBindingNode): ComponentExpression {
        return { c ->
            val dependency = getBindingExpression(binding.dependencies.single())
            DeclarationIrBuilder(pluginContext, scope.scopeOwnerSymbol)
                .irLambda(binding.key.type) { dependency(this, c) }
        }
    }

    private fun givenExpression(binding: GivenBindingNode): ComponentExpression {
        val dependencies = binding.dependencies
            .map { getBindingExpression(it) }

        val instanceExpression: ComponentExpression = bindingExpression@{ c ->
            if (binding.parameters.any { it.assisted }) {
                irLambda(binding.key.type) { function ->
                    val (assistedParameters, nonAssistedParameters) = binding.parameters
                        .partition { it.assisted }
                    binding.createExpression(
                        this,
                        binding.parameters
                            .associateWith { parameter ->
                                if (parameter.assisted) {
                                    {
                                        irGet(
                                            function.valueParameters[assistedParameters.indexOf(
                                                parameter
                                            )]
                                        )
                                    }
                                } else {
                                    {
                                        dependencies[nonAssistedParameters.indexOf(parameter)](c)
                                    }
                                }
                            }
                    )
                }
            } else {
                binding.createExpression(
                    this,
                    binding.parameters
                        .mapIndexed { index, parameter ->
                            index to parameter
                        }
                        .associateWith { (index, parameter) ->
                            { dependencies[index](c) }
                        }
                        .mapKeys { it.key.second }
                )
            }
        }

        if (!binding.scoped) return instanceExpression

        // todo
        check(
            binding.parameters
                .filter { it.assisted }
                .size <= 1
        ) {
            "Scoped bindings with assisted parameters are unsupported ${binding.key}"
        }

        val lazy = pluginContext.referenceFunctions(FqName("kotlin.lazy"))
            .single { it.owner.valueParameters.size == 1 }
            .owner

        val lazyExpression = members.cachedValue(
            lazy.returnType.typeWith(binding.key.type).asKey()
        ) { c ->
            irCall(lazy).apply {
                putValueArgument(
                    0,
                    DeclarationIrBuilder(pluginContext, symbol)
                        .irLambda(
                            pluginContext.tmpFunction(0)
                                .typeWith(binding.key.type)
                        ) { instanceExpression(this, c) }
                )
            }
        }

        return bindingExpression@{ c ->
            irCall(
                lazy.returnType
                    .classOrNull!!
                    .owner
                    .properties
                    .single { it.name.asString() == "value" }
                    .getter!!
            ).apply {
                dispatchReceiver = lazyExpression(c)
            }
        }
    }

    private fun ComponentExpression.wrapInFunction(key: Key): ComponentExpression {
        val factoryExpression = this
        val function = members.getFunction(key) function@{ c ->
            factoryExpression(this, c)
        }
        return bindingExpression@{ c ->
            irCall(function).apply {
                dispatchReceiver = c[component]
            }
        }
    }

    private fun parentExpression(request: BindingRequest): ComponentExpression {
        val parentExpression = parent?.getBindingExpression(request)!!
        if (component.dependencyRequests.any {
                it.second.key == request.key
            }) {
            val function = members.getFunction(request.key, parentExpression)
            return { c ->
                irCall(function).apply {
                    dispatchReceiver = c[component]
                }
            }
        } else return parentExpression
    }

}

typealias ComponentExpression = IrBuilderWithScope.(ComponentExpressionContext) -> IrExpression

data class ComponentExpressionContext(
    val accessors: Map<ComponentImpl, () -> IrExpression>
) {
    operator fun get(factory: ComponentImpl) =
        accessors.getValue(factory)()
}

fun ComponentExpressionContext(
    thisComponent: ComponentImpl,
    thisAccessor: () -> IrExpression
): ComponentExpressionContext {
    val componentsWithAccessor = mutableMapOf<ComponentImpl, () -> IrExpression>()

    componentsWithAccessor[thisComponent] = thisAccessor

    var current: ComponentImpl? = thisComponent
    while (current != null) {
        val parent = current.factoryImpl.parent?.componentImpl ?: break
        componentsWithAccessor[parent] = current.factoryImpl.parentAccessor!!
        current = parent
    }

    return ComponentExpressionContext(componentsWithAccessor)
}
