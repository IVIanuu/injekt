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
import com.ivianuu.injekt.compiler.lookupTracker
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.typeWith
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.incremental.components.LocationInfo
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.record
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getPackageFragment
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
            is InputBindingNode -> inputExpression(binding) to false
            is MapBindingNode -> mapBindingExpression(binding) to true
            is NullBindingNode -> nullExpression(binding) to false
            is ProviderBindingNode -> providerExpression(binding) to true
            is SetBindingNode -> setBindingExpression(binding) to true
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
                bindingNode.functions.forEach {
                    recordLookup(it.signature)
                }
                bindingNode.functions.forEach { (function) ->
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
                bindingNode.functions.forEach {
                    recordLookup(it.signature)
                }
                bindingNode.functions.forEach { (function) ->
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
        recordLookup(binding.implicitPair.signature)
        val dependencies = binding.dependencies
            .map { getBindingExpression(it) }

        val instanceExpression: ComponentExpression = bindingExpression@{ c ->
            if (binding.parameters.any { it.explicit }) {
                irLambda(binding.key.type) { function ->
                    val (explicitParameters, implicitParameters) = binding.parameters
                        .partition { it.explicit }
                    binding.createExpression(
                        this,
                        binding.parameters
                            .associateWith { parameter ->
                                if (parameter.explicit) {
                                    {
                                        irGet(
                                            function.valueParameters[explicitParameters.indexOf(
                                                parameter
                                            )]
                                        )
                                    }
                                } else {
                                    {
                                        dependencies[implicitParameters.indexOf(parameter)](c)
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
                .filter { it.explicit }
                .size <= 1
        ) {
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

    private fun recordLookup(signature: IrFunction) {
        val location = object : LookupLocation {
            override val location: LocationInfo? = object : LocationInfo {
                override val filePath: String
                    get() = component.clazz.file.path
                override val position: Position
                    get() = Position.NO_POSITION
            }
        }
        lookupTracker!!.record(
            location,
            signature.getPackageFragment()!!.packageFragmentDescriptor,
            signature.name
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
