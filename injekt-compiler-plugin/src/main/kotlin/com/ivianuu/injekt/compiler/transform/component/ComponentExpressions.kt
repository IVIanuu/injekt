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
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions

class ComponentExpressions(
    private val graph: Graph,
    private val pluginContext: IrPluginContext,
    private val symbols: InjektSymbols,
    private val members: ComponentMembers,
    private val parent: ComponentExpressions?,
    private val component: ComponentImpl
) {

    private val bindingExpressions =
        mutableMapOf<BindingRequest, ComponentExpression>()

    fun getBindingExpression(request: BindingRequest): ComponentExpression {
        val binding = graph.getBinding(request)

        if (binding.owner != component) {
            return parent?.getBindingExpression(request)!!
        }

        bindingExpressions[request]?.let { return it }

        val expression = when (binding) {
            is ChildComponentFactoryBindingNode -> childComponentFactoryExpression(
                binding
            )
            is ComponentImplBindingNode -> componentExpression(
                binding
            )
            is NullBindingNode -> nullExpression(binding)
            is ProviderBindingNode -> providerExpression(binding)
            is ProvisionBindingNode -> provisionExpression(binding)
        }.wrapInFunction(binding.key)

        bindingExpressions[request] = expression
        return expression
    }

    private fun childComponentFactoryExpression(binding: ChildComponentFactoryBindingNode): ComponentExpression =
        { c -> binding.childComponentFactoryExpression(this, c) }

    private fun componentExpression(
        binding: ComponentImplBindingNode
    ): ComponentExpression = { c -> c[binding.component] }

    private fun nullExpression(binding: NullBindingNode): ComponentExpression =
        { irNull() }

    private fun providerExpression(binding: ProviderBindingNode): ComponentExpression {
        return /*getBindingExpression(
            BindingRequest(
                key = binding.key.type.typeArguments.single().typeOrFail.asKey(),
                requestingKey = binding.key,
                requestOrigin = binding.origin
            )
        )*/ error("todo")
    }

    private fun provisionExpression(binding: ProvisionBindingNode): ComponentExpression {
        val instanceExpression: ComponentExpression = bindingExpression@{ c ->
            val dependencies = binding.dependencies
                .map { getBindingExpression(it) }

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

        if (!binding.scoped) return instanceExpression

        val providerExpression = members.cachedValue(
            pluginContext.tmpFunction(0).typeWith(binding.key.type).asKey()
        ) { c ->
            irCall(
                symbols.doubleCheck
                    .constructors
                    .single()
            ).apply {
                putValueArgument(
                    0,
                    InjektDeclarationIrBuilder(pluginContext, symbol)
                        .irLambda(
                            pluginContext.tmpFunction(0)
                                .typeWith(binding.key.type)
                        ) {
                            +irReturn(instanceExpression(this, c))
                        }
                )
            }
        }

        return bindingExpression@{ c ->
            irCall(
                pluginContext.tmpFunction(0)
                    .functions
                    .single { it.owner.name.asString() == "invoke" }
            ).apply {
                dispatchReceiver = providerExpression(c)
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

}

typealias ComponentExpression = IrBuilderWithScope.(ComponentExpressionContext) -> IrExpression

data class ComponentExpressionContext(
    val componentAccessor: Map<ComponentImpl, () -> IrExpression>
) {
    operator fun get(factory: ComponentImpl) =
        componentAccessor.getValue(factory)()
}

fun IrBuilderWithScope.ComponentExpressionContext(
    thisComponent: ComponentImpl,
    thisAccessor: () -> IrExpression = { irGet(thisComponent.clazz.thisReceiver!!) }
): ComponentExpressionContext {
    val allComponents = mutableListOf<ComponentImpl>()
    var current: ComponentImpl? = thisComponent
    if (current != null) {
        while (current != null) {
            allComponents += current
            current = current.factoryImpl.parent?.componentImpl
        }
    } else {
        allComponents += thisComponent
    }

    return ComponentExpressionContext(
        allComponents.associateWith { component ->
            if (component == thisComponent) thisAccessor else ({
                irGet(component.clazz.thisReceiver!!)
            })
        }
    )
}
