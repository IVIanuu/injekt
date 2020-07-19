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
import com.ivianuu.injekt.compiler.distinctedType
import com.ivianuu.injekt.compiler.flatMapFix
import com.ivianuu.injekt.compiler.getClassFromSingleValueAnnotation
import com.ivianuu.injekt.compiler.getClassFromSingleValueAnnotationOrNull
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.typeArguments
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

typealias BindingResolver = (Key) -> List<BindingNode>

class ChildComponentFactoryBindingResolver(
    private val declarationGraph: DeclarationGraph,
    private val parentComponent: ComponentImpl
) : BindingResolver {

    private val childComponents =
        mutableMapOf<Key, ChildComponentFactoryBindingNode>()

    override fun invoke(requestedKey: Key): List<BindingNode> {
        childComponents[requestedKey]?.let { return listOf(it) }

        val factory = requestedKey.type.classOrNull?.owner

        if (factory?.hasAnnotation(InjektFqNames.ChildComponentFactory) != true) return emptyList()

        val component = factory.functions
            .filterNot { it.isFakeOverride }
            .single()
            .returnType
            .classOrNull!!
            .owner

        val node = ChildComponentFactoryBindingNode(
            key = requestedKey,
            owner = parentComponent,
            origin = factory.descriptor.fqNameSafe,
            parent = parentComponent.clazz,
            childComponentFactoryExpression = { c ->
                irBlock {
                    val childComponentFactoryImpl = ComponentFactoryImpl(
                        parentComponent.clazz,
                        factory,
                        declarationGraph.entryPoints
                            .filter {
                                it.getClassFromSingleValueAnnotation(
                                    InjektFqNames.EntryPoint,
                                    parentComponent.factoryImpl.pluginContext
                                ) == component
                            },
                        parentComponent.factoryImpl,
                        parentComponent.factoryImpl.pluginContext,
                        parentComponent.factoryImpl.declarationGraph,
                        parentComponent.factoryImpl.symbols,
                    )

                    childComponentFactoryImpl.init()
                    parentComponent.clazz.addChild(childComponentFactoryImpl.clazz)

                    +irCall(
                        childComponentFactoryImpl.clazz.constructors
                            .single()
                    ).apply {
                        putValueArgument(0, c[parentComponent])
                    }
                }
            }
        )

        childComponents[requestedKey] = node

        return listOf(node)
    }
}

class GivenBindingResolver(
    private val pluginContext: IrPluginContext,
    declarationGraph: DeclarationGraph,
    private val component: ComponentImpl
) : BindingResolver {

    private val bindings = declarationGraph.bindings
        .map { function ->
            val targetComponent = function.getClassFromSingleValueAnnotationOrNull(
                InjektFqNames.Given, pluginContext
            )
                ?: if (function is IrConstructor) function.constructedClass.getClassFromSingleValueAnnotationOrNull(
                    InjektFqNames.Given, pluginContext
                ) else null

            val parameters = mutableListOf<BindingParameter>()

            parameters += function.valueParameters
                .map { valueParameter ->
                    BindingParameter(
                        name = valueParameter.name.asString(),
                        key = valueParameter.type.asKey(),
                        explicit = !valueParameter.hasAnnotation(InjektFqNames.Implicit),
                        origin = valueParameter.descriptor.fqNameSafe
                    )
                }

            val explicitParameters = parameters.filter { it.explicit }

            val key = if (explicitParameters.isEmpty()) function.returnType
                .asKey()
            else pluginContext.tmpFunction(explicitParameters.size)
                .typeWith(
                    explicitParameters.map { it.key.type } + function.returnType
                )
                .asKey()

            GivenBindingNode(
                key = key,
                dependencies = parameters
                    .filterNot { it.explicit }
                    .map { BindingRequest(it.key, key, it.origin) },
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
            requestedType == requestedType.distinctedType &&
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

    private val maps: Map<Key, List<IrFunction>> =
        mutableMapOf<Key, List<IrFunction>>().also { mergedMap ->
            if (parent != null) mergedMap += parent.maps

            val thisMaps = declarationGraph.mapEntries
                .filter {
                    it.getClassFromSingleValueAnnotation(
                        InjektFqNames.MapEntries,
                        pluginContext
                    ) == component.factoryImpl.component
                }
                .groupBy { it.returnType.asKey() }

            thisMaps.forEach { (mapKey, entries) ->
                val existingEntries = mergedMap[mapKey] ?: emptyList()
                mergedMap[mapKey] = existingEntries + entries
            }
        }

    private val mapBindings = maps.map { (key, entries) ->
        MapBindingNode(
            key,
            entries
                .flatMapFix { function ->
                    function
                        .valueParameters
                        .filter { it.hasAnnotation(InjektFqNames.Implicit) }
                        .map { it.type }
                        .distinct()
                        .map { BindingRequest(it.asKey(), key, null) }
                },
            component,
            entries
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

    private val sets: Map<Key, List<IrFunction>> =
        mutableMapOf<Key, List<IrFunction>>().also { mergedSet ->
            if (parent != null) mergedSet += parent.sets

            val thisSets = declarationGraph.setElements
                .filter {
                    it.getClassFromSingleValueAnnotation(
                        InjektFqNames.SetElements,
                        pluginContext
                    ) == component.factoryImpl.component
                }
                .groupBy { it.returnType.asKey() }

            thisSets.forEach { (mapKey, elements) ->
                val existingElements = mergedSet[mapKey] ?: emptyList()
                mergedSet[mapKey] = existingElements + elements
            }
        }

    private val setBindings = sets.map { (key, elements) ->
        SetBindingNode(
            key,
            elements
                .flatMapFix { function ->
                    function
                        .valueParameters
                        .filter { it.hasAnnotation(InjektFqNames.Implicit) }
                        .map { it.type }
                        .distinct()
                        .map { BindingRequest(it.asKey(), key, null) }
                },
            component,
            elements
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
        if (requestedKey != component.factoryImpl.component.defaultType.asKey() &&
            requestedKey != component.clazz.defaultType.asKey() &&
            component.factoryImpl.entryPoints.none {
                it.defaultType.asKey() == requestedKey
            }
        ) return emptyList()
        return listOf(ComponentImplBindingNode(component))
    }
}

class InputsBindingResolver(
    inputParameters: List<IrField>,
    private val component: ComponentImpl
) : BindingResolver {

    private val bindings = inputParameters.map {
        InputBindingNode(component, it)
    }

    override fun invoke(requestedKey: Key): List<BindingNode> {
        return bindings
            .filter { it.key == requestedKey }
    }
}
