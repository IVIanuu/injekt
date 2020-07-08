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
import com.ivianuu.injekt.compiler.flatMapFix
import com.ivianuu.injekt.compiler.isProvider
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.render

class Graph(
    val parent: Graph?,
    val component: ComponentImpl,
    context: IrPluginContext,
    declarationGraph: DeclarationGraph,
    val symbols: InjektSymbols
) {

    private val bindingsResolvers = listOf(
        NoArgProviderBindingResolver(component),
        ComponentImplBindingResolver(
            ComponentRequirementNode(component, component.clazz.defaultType.asKey(), { error("") })
        ),
        ProvideBindingResolver(context, declarationGraph, component)
    )
    val resolvedBindings = mutableMapOf<Key, BindingNode>()

    private val chain = mutableSetOf<Key>()

    fun getBinding(request: BindingRequest): BindingNode {
        var binding = resolvedBindings[request.key]
        if (binding != null) return binding

        val bindings = bindingsResolvers.flatMapFix { it(request.key) }
        if (bindings.size > 1) {
            error(
                "Multiple bindings found for '${request.key}' at:\n${bindings.joinToString(
                    "\n"
                ) {
                    "'${it.origin.orUnknown()}'"
                }} in '${component.origin}'"
            )
        }
        if (binding?.targetComponent != null &&
            binding.targetComponent?.classifierOrFail != component.factoryImpl.node.component.symbol
        ) {
            if (parent == null) {
                error(
                    "Scope mismatch binding '${binding.key}' " +
                            "with scope '${binding.targetComponent?.render()}' is not compatible with this component " +
                            "${component.factoryImpl.node.component.render()} '${component.origin}'"
                )
            } else {
                binding = null
            }
        }

        binding = bindings.singleOrNull()

        binding?.let {
            resolvedBindings[request.key] = it
            return it
        }

        parent?.getBinding(request)?.let { return it }

        if (request.key.type.isMarkedNullable()) {
            binding = NullBindingNode(request.key, component)
            resolvedBindings[request.key] = binding
            return binding
        }

        error(
            "No binding found for '${request.key}' " +
                    "required at '${request.requestOrigin.orUnknown()}' " +
                    "in '${component.origin}'"
        )
    }

    fun validate(request: BindingRequest) {
        check(request.key !in chain || chain
            .toList()
            .let { it.subList(it.indexOf(request.key), it.size) }
            .any { it.type.isProvider() }
        ) {
            val chain = (chain.toList() + request.key)
                .let { it.subList(it.indexOf(request.key), it.size) }

            // todo pretty print
            "Circular dependency for '${request.key}' in '${component.origin}' chain $chain"
        }

        // we don't have to check further because it's a legal cycle
        if (request.key in chain) return

        chain += request.key
        val binding = getBinding(request)
        binding.dependencies
            .forEach { validate(it) }
        chain -= request.key
    }

}
