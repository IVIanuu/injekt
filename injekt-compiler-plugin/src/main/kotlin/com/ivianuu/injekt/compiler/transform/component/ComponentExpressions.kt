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

import com.ivianuu.injekt.compiler.getContext
import com.ivianuu.injekt.compiler.irLambda
import com.ivianuu.injekt.compiler.recordLookup
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.typeWith
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fields
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
            is ChildComponentFactoryBindingNode -> childComponentFactoryExpression(binding)
            is ComponentImplBindingNode -> componentExpression(binding)
            is GivenBindingNode -> givenExpression(binding)
            is InputBindingNode -> inputExpression(binding)
            is MapBindingNode -> mapBindingExpression(binding)
            is NullBindingNode -> nullExpression(binding)
            is SetBindingNode -> setBindingExpression(binding)
        }.wrapInFunction(binding.key)

        return expression.also { bindingExpressions[request] = it }
    }

    private fun childComponentFactoryExpression(binding: ChildComponentFactoryBindingNode): ComponentExpression =
        { c -> binding.childComponentFactoryExpression(this, c) }

    private fun componentExpression(
        binding: ComponentImplBindingNode
    ): ComponentExpression = { c -> c[binding.component] }

    private fun inputExpression(
        binding: InputBindingNode
    ): ComponentExpression = { c ->
        irGetField(
            c[component],
            binding.inputField
        )
    }

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
                bindingNode.contexts.forEach { recordLookup(it) }
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
                                putValueArgument(
                                    valueArgumentsCount - 1,
                                    getBindingExpression(
                                        BindingRequest(
                                            component.clazz.defaultType.asKey(),
                                            bindingNode.key,
                                            null
                                        )
                                    )(c)
                                )
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
                bindingNode.contexts.forEach { recordLookup(it) }
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
                                putValueArgument(
                                    valueArgumentsCount - 1,
                                    getBindingExpression(
                                        BindingRequest(
                                            component.clazz.defaultType.asKey(),
                                            bindingNode.key,
                                            null
                                        )
                                    )(c)
                                )
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

    private fun givenExpression(binding: GivenBindingNode): ComponentExpression {
        recordLookup(binding.function)
        val componentExpression = getBindingExpression(
            BindingRequest(
                component.clazz.defaultType.asKey(),
                binding.key,
                null
            )
        )

        val instanceExpression: ComponentExpression = bindingExpression@{ c ->
            if (binding.explicitParameters.isNotEmpty()) {
                irLambda(binding.key.type) { function ->
                    binding.createExpression(
                        this,
                        binding.explicitParameters
                            .associateWith { parameter ->
                                {
                                    irGet(
                                        function.valueParameters[parameter.index]
                                    )
                                }
                            },
                        {
                            componentExpression(this, c)
                        }
                    )
                }
            } else {
                binding.createExpression(
                    this,
                    emptyMap(),
                    {
                        componentExpression(this, c)
                    }
                )
            }
        }

        if (!binding.scoped) return instanceExpression

        // todo
        check(binding.explicitParameters.isEmpty()) {
            "Scoped bindings with explicit parameters are unsupported ${binding.key}"
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
        val function = members.getFunction(request.key, parentExpression)
        return { c ->
            irCall(function).apply {
                dispatchReceiver = c[component]
            }
        }
    }

    private fun recordLookup(function: IrFunction) {
        val context = function.getContext()!!
        recordLookup(
            component.clazz,
            context
        )
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
    val allImplementations = mutableListOf<ComponentImpl>()
    var current: ComponentImpl? = thisComponent
    if (current != null) {
        while (current != null) {
            allImplementations += current
            current = current.factoryImpl.parent?.componentImpl
        }
    } else {
        allImplementations += thisComponent
    }
    val implementationWithAccessor =
        mutableMapOf<ComponentImpl, () -> IrExpression>()

    allImplementations.fold(thisAccessor) { acc: () -> IrExpression, component: ComponentImpl ->
        implementationWithAccessor[component] = acc
        {
            DeclarationIrBuilder(component.factoryImpl.pluginContext, component.clazz.symbol)
                .irGetField(acc(), component.clazz.fields.single { it.name.asString() == "parent" })
        }
    }

    return ComponentExpressionContext(implementationWithAccessor)
}
