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
import com.ivianuu.injekt.compiler.getClassFromSingleValueAnnotationOrNull
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.render

class ComponentTree(
    private val pluginContext: IrPluginContext,
    private val allFactories: Map<IrType, List<ComponentFactory>>,
    private val allEntryPoints: Map<IrType, List<EntryPoint>>
) {

    val nodes = mutableListOf<ComponentNode>()

    private val chain = mutableSetOf<IrType>()

    init {
        allFactories.forEach { (_, factories) ->
            factories.forEach { getOrCreateNode(it) }
        }
    }

    private fun getNodesForComponent(component: IrType): List<ComponentNode> {
        return allFactories.getValue(component)
            .map { getOrCreateNode(it) }
    }

    private fun getOrCreateNode(
        factory: ComponentFactory
    ): ComponentNode {
        check(factory.factory.defaultType !in chain) {
            "Circular dependency for ${factory.factory.defaultType.render()} " +
                    "${chain.map { it.render() }}"
        }
        val existing = nodes.firstOrNull {
            it.factory == factory
        }
        if (existing != null) return existing

        chain += factory.factory.defaultType

        val node = ComponentNode(factory, pluginContext)

        if (node.parentComponent != null) {
            getNodesForComponent(node.parentComponent).forEach { parentNode ->
                parentNode.children += node
                node.parents += parentNode
            }
        }

        allEntryPoints[node.component.defaultType]?.forEach {
            node.entryPoints += it
        }

        nodes += node

        chain -= factory.factory.defaultType

        return node
    }
}

class ComponentNode(
    val factory: ComponentFactory,
    pluginContext: IrPluginContext
) {

    val component = factory.factory.functions
        .filterNot { it.isFakeOverride }
        .single()
        .returnType
        .classOrNull!!
        .owner

    val parentComponent = factory.factory.functions
        .filterNot { it.isFakeOverride }
        .single()
        .returnType
        .classOrNull!!
        .owner.getClassFromSingleValueAnnotationOrNull(
            InjektFqNames.Component, pluginContext
        ).takeUnless { it?.defaultType?.isNothing() == true }?.symbol?.defaultType

    val parents = mutableSetOf<ComponentNode>()
    val children = mutableSetOf<ComponentNode>()

    val entryPoints = mutableSetOf<EntryPoint>()

}
