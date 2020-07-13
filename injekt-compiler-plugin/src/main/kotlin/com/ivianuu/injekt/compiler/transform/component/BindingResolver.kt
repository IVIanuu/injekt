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
import com.ivianuu.injekt.compiler.flatMapFix
import com.ivianuu.injekt.compiler.getClassFromSingleValueAnnotation
import com.ivianuu.injekt.compiler.getClassFromSingleValueAnnotationOrNull
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.typeArguments
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

typealias BindingResolver = (Key) -> List<BindingNode>

class ChildComponentFactoryBindingResolver(
    private val parentComponent: ComponentImpl
) : BindingResolver {

    private val childComponents =
        mutableMapOf<Key, MutableList<Lazy<ChildComponentFactoryBindingNode>>>()

    init {
        parentComponent.factoryImpl.node.children
            .forEach { childNode ->
                val key = childNode.factory.factory.defaultType.asKey()
                childComponents.getOrPut(key) { mutableListOf() } += childComponentFactoryBindingNode(
                    key, childNode
                )
            }
    }

    override fun invoke(requestedKey: Key): List<BindingNode> =
        childComponents[requestedKey]?.map { it.value } ?: emptyList()

    private fun childComponentFactoryBindingNode(
        key: Key,
        node: ComponentNode
    ) = lazy {
        return@lazy ChildComponentFactoryBindingNode(
            key = key,
            owner = parentComponent,
            origin = node.factory.factory.descriptor.fqNameSafe,
            parent = parentComponent.clazz,
            childComponentFactoryExpression = { c ->
                irBlock {
                    val childComponentFactoryImpl = ComponentFactoryImpl(
                        parentComponent.clazz,
                        { c[parentComponent] },
                        node,
                        parentComponent.factoryImpl,
                        parentComponent.factoryImpl.pluginContext,
                        parentComponent.factoryImpl.declarationGraph,
                        parentComponent.factoryImpl.symbols,
                    )

                    +childComponentFactoryImpl.getClass()
                    +irCall(
                        childComponentFactoryImpl.factoryClass.constructors
                            .single()
                    )
                }
            }
        )
    }
}

class GivenBindingResolver(
    private val pluginContext: IrPluginContext,
    private val declarationGraph: DeclarationGraph,
    private val component: ComponentImpl
) : BindingResolver {

    private val bindings = declarationGraph.bindings
        .map { binding ->
            val function = binding.function
            val targetComponent = function.getClassFromSingleValueAnnotationOrNull(
                InjektFqNames.Given, pluginContext
            )
                ?: if (function is IrConstructor) function.constructedClass.getClassFromSingleValueAnnotationOrNull(
                    InjektFqNames.Given, pluginContext
                ) else null

            val readerInfo = pluginContext.getReaderInfo(function)

            val parameters = mutableListOf<BindingParameter>()

            parameters += function.valueParameters.map { valueParameter ->
                BindingParameter(
                    name = valueParameter.name.asString(),
                    key = valueParameter.type.asKey(),
                    assisted = readerInfo?.declarations
                        ?.filterIsInstance<IrDeclarationWithName>()
                        ?.none { it.name == valueParameter.name } ?: true,
                    origin = valueParameter.descriptor.fqNameSafe
                )
            }

            val assistedParameters = parameters.filter { it.assisted }

            val key = if (assistedParameters.isEmpty()) binding.function.returnType.asKey()
            else pluginContext.tmpFunction(assistedParameters.size)
                .typeWith(
                    assistedParameters.map { it.key.type } + binding.function.returnType
                )
                .asKey()

            GivenBindingNode(
                key = key,
                dependencies = parameters
                    .filterNot { it.assisted }
                    .map {
                        BindingRequest(it.key, key, it.origin) // todo
                    },
                targetComponent = targetComponent?.defaultType,
                scoped = targetComponent != null,
                createExpression = { parametersMap ->
                    val call = if (function is IrConstructor) {
                        IrConstructorCallImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            function.returnType,
                            function.symbol,
                            function.constructedClass.typeParameters.size,
                            function.typeParameters.size,
                            function.valueParameters.size
                        )
                    } else {
                        IrCallImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            function.returnType,
                            function.symbol,
                            function.typeParameters.size,
                            function.valueParameters.size
                        )
                    }
                    call.apply {
                        if (function.dispatchReceiverParameter != null) {
                            dispatchReceiver = irGetObject(
                                function.dispatchReceiverParameter!!.type.classOrNull!!
                            )
                        }

                        parametersMap.values.forEachIndexed { index, expression ->
                            putValueArgument(
                                index,
                                expression()
                            )
                        }
                    }
                },
                parameters = parameters,
                owner = component,
                origin = function.descriptor.fqNameSafe
            )
        }

    override fun invoke(requestedKey: Key): List<BindingNode> =
        bindings.filter { it.key == requestedKey }
}

class ProviderBindingResolver(
    private val component: ComponentImpl
) : BindingResolver {
    override fun invoke(requestedKey: Key): List<BindingNode> {
        val requestedType = requestedKey.type
        return when {
            requestedType.isFunction() &&
                    requestedType.typeArguments.size == 1 ->
                listOf(
                    ProviderBindingNode(
                        requestedKey,
                        component,
                        null
                    )
                )
            else -> emptyList()
        }
    }
}

class MapBindingResolver(
    parent: MapBindingResolver?,
    private val pluginContext: IrPluginContext,
    declarationGraph: DeclarationGraph,
    private val component: ComponentImpl
) : BindingResolver {

    private val maps: Map<Key, List<MapEntries>> =
        mutableMapOf<Key, List<MapEntries>>().also { mergedMap ->
            if (parent != null) mergedMap += parent.maps

            val thisMaps = declarationGraph.mapEntries
                .filter {
                    it.function.getClassFromSingleValueAnnotation(
                        InjektFqNames.MapEntries,
                        pluginContext
                    ) == component.factoryImpl.node.component
                }
                .groupBy { it.function.returnType.asKey() }

            thisMaps.forEach { (mapKey, entries) ->
                val existingEntries = mergedMap[mapKey] ?: emptyList()
                mergedMap[mapKey] = existingEntries + entries
            }
        }

    private val mapBindings = maps.map { (key, entries) ->
        MapBindingNode(
            key,
            entries
                .flatMapFix { mapEntries ->
                    val function = mapEntries.function
                    val readerInfo = pluginContext.getReaderInfo(function)
                    readerInfo?.declarations
                        ?.filterIsInstance<IrFunction>()
                        ?.filterNot { it is IrConstructor }
                        ?.filterNot { it.isFakeOverride }
                        ?.filterNot { it.dispatchReceiverParameter?.type == pluginContext.irBuiltIns.anyType }
                        ?.map { it.returnType }
                        ?.distinct()
                        ?.map { BindingRequest(it.asKey(), null, null) }
                        ?: emptyList()
                },
            component,
            entries.map { it.function }
        )
    }

    override fun invoke(requestedKey: Key): List<BindingNode> {
        return mapBindings
            .filter { it.key == requestedKey }
            .takeIf { it.isNotEmpty() }
            ?: if (requestedKey.type.classOrNull == pluginContext.referenceClass(
                    FqName("kotlin.collections.Map")
                )
            ) listOf(
                MapBindingNode(
                    requestedKey,
                    emptyList(),
                    component,
                    emptyList()
                )
            ) else emptyList()
    }

}

class SetBindingResolver(
    parent: SetBindingResolver?,
    private val pluginContext: IrPluginContext,
    declarationGraph: DeclarationGraph,
    private val component: ComponentImpl
) : BindingResolver {

    private val sets: Map<Key, List<SetElements>> =
        mutableMapOf<Key, List<SetElements>>().also { mergedSet ->
            if (parent != null) mergedSet += parent.sets

            val thisMaps = declarationGraph.setElements
                .filter {
                    it.function.getClassFromSingleValueAnnotation(
                        InjektFqNames.SetElements,
                        pluginContext
                    ) == component.factoryImpl.node.component
                }
                .groupBy { it.function.returnType.asKey() }

            thisMaps.forEach { (mapKey, elements) ->
                val existingElements = mergedSet[mapKey] ?: emptyList()
                mergedSet[mapKey] = existingElements + elements
            }
        }

    private val setBindings = sets.map { (key, elements) ->
        SetBindingNode(
            key,
            elements
                .flatMapFix { setElements ->
                    val function = setElements.function
                    val readerInfo = pluginContext.getReaderInfo(function)
                    readerInfo?.declarations
                        ?.filterIsInstance<IrFunction>()
                        ?.filterNot { it is IrConstructor }
                        ?.filterNot { it.isFakeOverride }
                        ?.filterNot { it.dispatchReceiverParameter?.type == pluginContext.irBuiltIns.anyType }
                        ?.map { it.returnType }
                        ?.distinct()
                        ?.map { BindingRequest(it.asKey(), null, null) }
                        ?: emptyList()
                },
            component,
            elements.map { it.function }
        )
    }

    override fun invoke(requestedKey: Key): List<BindingNode> {
        return setBindings
            .filter { it.key == requestedKey }
            .takeIf { it.isNotEmpty() }
            ?: if (requestedKey.type.classOrNull == pluginContext.referenceClass(
                    FqName("kotlin.collections.Set")
                )
            ) listOf(
                SetBindingNode(
                    requestedKey,
                    emptyList(),
                    component,
                    emptyList()
                )
            ) else emptyList()
    }

}

class ComponentImplBindingResolver(
    private val component: ComponentImpl
) : BindingResolver {
    override fun invoke(requestedKey: Key): List<BindingNode> {
        if (requestedKey != component.factoryImpl.node.component.defaultType.asKey() &&
            requestedKey != component.clazz.defaultType.asKey() &&
            component.factoryImpl.node.entryPoints.none {
                it.entryPoint.defaultType.asKey() == requestedKey
            }
        ) return emptyList()
        return listOf(ComponentImplBindingNode(component))
    }
}

class InputParameterBindingResolver(
    inputParameters: List<IrValueParameter>,
    private val component: ComponentImpl
) : BindingResolver {

    private val bindings = inputParameters.map {
        InputParameterBindingNode(component, it)
    }

    override fun invoke(requestedKey: Key): List<BindingNode> {
        return bindings
            .filter { it.key == requestedKey }
    }
}
