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

import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.isProvider
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions

class ComponentExpressions(
    private val graph: Graph,
    private val pluginContext: IrPluginContext,
    private val symbols: InjektSymbols,
    private val members: ComponentMembers,
    private val parent: ComponentExpressions?,
    private val component: ComponentImpl
) {

    private val bindingExpressions = mutableMapOf<BindingRequest, ComponentExpression>()
    private val requirementExpressions = mutableMapOf<RequirementNode, ComponentExpression>()

    fun getRequirementExpression(node: RequirementNode): ComponentExpression {
        requirementExpressions[node]?.let { return it }
        val expression = members.cachedValue(node.key) {
            node.accessor(this)!!
        }
        requirementExpressions[node] = expression
        return expression
    }

    fun getBindingExpression(request: BindingRequest): ComponentExpression {
        val binding = graph.getBinding(request)

        if (binding.owner != component) {
            return parent?.getBindingExpression(request)!!
        }

        bindingExpressions[request]?.let { return it }

        val expression = when (request.requestType) {
            RequestType.Instance -> {
                if (bindingExpressions.containsKey(
                        BindingRequest(
                            binding.key,
                            request.requestingKey,
                            binding.origin,
                            RequestType.Provider
                        )
                    )
                ) {
                    if (request.key.type.isProvider()) {
                        getBindingExpression(
                            BindingRequest(
                                binding.key,
                                request.requestingKey,
                                binding.origin,
                                RequestType.Provider
                            )
                        )
                    } else {
                        invokeProviderInstanceExpression(binding)
                    }
                } else {
                    when (binding) {
                        is ChildComponentFactoryBindingNode -> instanceExpressionForChildComponent(
                            binding
                        )
                        is ComponentImplBindingNode -> instanceExpressionForComponentImpl(
                            binding
                        )
                        is NullBindingNode -> instanceExpressionForNull(binding)
                        is ProviderBindingNode -> instanceExpressionForProvider(binding)
                        is ProvisionBindingNode -> instanceExpressionForProvision(binding)
                        else -> error("")
                    }.wrapInFunction(binding.key)
                }
            }
            RequestType.Provider -> {
                when (binding) {
                    is ChildComponentFactoryBindingNode -> providerExpressionForChildComponent(
                        binding
                    )
                    is ComponentImplBindingNode -> providerExpressionForComponentImpl(
                        binding
                    )
                    is NullBindingNode -> providerExpressionForNull(binding)
                    is ProviderBindingNode -> providerExpressionForProvider(binding)
                    is ProvisionBindingNode -> providerExpressionForProvision(binding)
                    else -> error("")
                }
            }
        }

        bindingExpressions[request] = expression
        return expression
    }

    /*private fun instanceExpressionForAssistedProvision(binding: AssistedProvisionBindingNode): FactoryExpression {
        return {
            val (assistedParameters, nonAssistedParameters) = binding.parameters
                .partition { it.assisted }

            val dependencyExpressions = binding.dependencies
                .map { getBindingExpression(it) }

            InjektDeclarationIrBuilder(pluginContext, component.factoryFunction.symbol)
                .irLambda(
                    pluginContext.tmpFunction(
                        assistedParameters
                            .size
                    ).typeWith(
                        assistedParameters
                            .map { it.type } +
                                binding.key.type.typeArguments.last().typeOrFail
                    )
                ) { lambda ->
                    +irReturn(
                        binding.createExpression(
                            this,
                            binding.parameters
                                .associateWith { parameter ->
                                    val expr: () -> IrExpression? = if (parameter.assisted) {
                                        {
                                            irGet(
                                                lambda.valueParameters[assistedParameters.indexOf(
                                                    parameter
                                                )]
                                            )
                                        }
                                    } else {
                                        {
                                            dependencyExpressions[nonAssistedParameters.indexOf(
                                                parameter
                                            )](this)
                                        }
                                    }
                                    expr
                                }
                        )
                    )
                }
        }
    }*/

    private fun instanceExpressionForChildComponent(binding: ChildComponentFactoryBindingNode): ComponentExpression {
        return { binding.childComponentExpression(this) }
    }

    private fun instanceExpressionForComponentImpl(
        binding: ComponentImplBindingNode
    ): ComponentExpression {
        return invokeProviderInstanceExpression(binding)
    }

    private fun instanceExpressionForNull(binding: NullBindingNode): ComponentExpression {
        return { irNull() }
    }

    private fun instanceExpressionForProvider(binding: ProviderBindingNode): ComponentExpression {
        return getBindingExpression(
            BindingRequest(
                key = binding.key.type.typeArguments.single().typeOrFail.asKey(),
                requestingKey = binding.key,
                requestOrigin = binding.origin,
                requestType = RequestType.Provider
            )
        )
    }

    private fun instanceExpressionForProvision(binding: ProvisionBindingNode): ComponentExpression {
        return if (binding.scoped) {
            invokeProviderInstanceExpression(binding)
        } else {
            val expression: ComponentExpression = bindingExpression@{
                val dependencies = binding.dependencies
                    .map { getBindingExpression(it) }

                binding.createExpression(
                    this,
                    binding.parameters
                        .mapIndexed { index, parameter ->
                            index to parameter
                        }
                        .associateWith { (index, parameter) ->
                            { dependencies[index]() }
                        }
                        .mapKeys { it.key.second }
                )
            }
            expression
        }
    }

    /*private fun providerExpressionForAssistedProvision(binding: AssistedProvisionBindingNode): FactoryExpression {
        return cachedFactoryExpression(binding.key) {
            singleInstanceFactory(
                getBindingExpression(
                    BindingRequest(
                        key = binding.key,
                        requestingKey = binding.key,
                        requestOrigin = binding.origin,
                        requestType = RequestType.Instance
                    )
                )()!!
            )
        }
    }*/

    private fun providerExpressionForChildComponent(binding: ChildComponentFactoryBindingNode): ComponentExpression {
        return cachedExpression(binding.key) {
            singleInstanceFactory(binding.childComponentExpression(this)!!)
        }
    }

    private fun providerExpressionForComponentImpl(
        binding: ComponentImplBindingNode
    ): ComponentExpression {
        return cachedExpression(binding.key) {
            irCall(symbols.lateinitFactory.constructors.single()).apply {
                putTypeArgument(0, component.factoryImpl.node.component.defaultType)
            }
        }.also { component.componentLateinitProvider = it }
    }

    private fun providerExpressionForNull(binding: NullBindingNode): ComponentExpression {
        return { singleInstanceFactory(irNull()) }
    }

    private fun providerExpressionForProvider(binding: ProviderBindingNode): ComponentExpression {
        return getBindingExpression(
            BindingRequest(
                key = binding.dependencies.single().key,
                requestingKey = binding.key,
                requestOrigin = binding.origin,
                requestType = RequestType.Provider
            )
        )
    }

    private fun providerExpressionForProvision(binding: ProvisionBindingNode): ComponentExpression {
        return cachedExpression(binding.key) providerFieldExpression@{
            val dependencyExpressions = binding.dependencies
                .map { getBindingExpression(it) }

            val newProvider =
                InjektDeclarationIrBuilder(pluginContext, component.factoryImpl.factoryClass.symbol)
                    .irLambda(
                        pluginContext.tmpFunction(0)
                            .typeWith(binding.key.type)
                    ) {
                        +irReturn(
                            binding.createExpression(
                                this,
                                binding.parameters
                                    .mapIndexed { index, parameter ->
                                        index to parameter
                                    }
                                    .associateWith { (index, parameter) ->
                                        { dependencyExpressions[index]() }
                                    }
                                    .mapKeys { it.key.second }
                            )
                        )
                    }

            if (binding.scoped) {
                doubleCheck(newProvider)
            } else {
                newProvider
            }
        }
    }

    private fun cachedExpression(
        key: Key,
        factoryInitializer: IrBuilderWithScope.() -> IrExpression
    ): ComponentExpression {
        return members.cachedValue(
            pluginContext.tmpFunction(0)
                .typeWith(key.type)
                .asKey(),
            factoryInitializer
        )
    }

    private fun IrBuilderWithScope.singleInstanceFactory(instance: IrExpression): IrExpression {
        val instanceProviderCompanion = symbols.singleInstanceFactory.owner
            .companionObject() as IrClass
        return irCall(
            instanceProviderCompanion
                .declarations
                .filterIsInstance<IrFunction>()
                .single { it.name.asString() == "create" }
        ).apply {
            dispatchReceiver = irGetObject(instanceProviderCompanion.symbol)
            putValueArgument(0, instance)
        }
    }

    private fun IrBuilderWithScope.doubleCheck(provider: IrExpression): IrExpression {
        return irCall(
            symbols.doubleCheck
                .constructors
                .single()
        ).apply { putValueArgument(0, provider) }
    }

    private fun ComponentExpression.wrapInFunction(key: Key): ComponentExpression {
        val factoryExpression = this
        val function = members.getGetFunction(key) function@{
            factoryExpression()!!
        }
        return bindingExpression@{ irCall(function) }
    }

    private fun invokeProviderInstanceExpression(binding: BindingNode): ComponentExpression {
        val providerExpression = getBindingExpression(
            BindingRequest(
                key = binding.key,
                requestingKey = binding.key,
                requestOrigin = binding.origin,
                requestType = RequestType.Provider
            )
        )
        return bindingExpression@{
            irCall(
                pluginContext.tmpFunction(0)
                    .functions
                    .single { it.owner.name.asString() == "invoke" }
            ).apply {
                dispatchReceiver = providerExpression()
            }
        }
    }

}

typealias ComponentExpression = IrBuilderWithScope.() -> IrExpression?
