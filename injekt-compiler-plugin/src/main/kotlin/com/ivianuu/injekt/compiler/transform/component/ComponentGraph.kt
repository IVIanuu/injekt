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
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName

class ComponentGraph(
    val parent: ComponentGraph?,
    val component: ComponentImpl,
    context: IrPluginContext,
    declarationGraph: DeclarationGraph,
    val symbols: InjektSymbols,
    inputs: List<IrField>
) {

    private val mapBindingResolver: MapBindingResolver = MapBindingResolver(
        parent?.mapBindingResolver,
        component.factoryImpl.pluginContext,
        component.factoryImpl.declarationGraph,
        component
    )
    private val setBindingResolver: SetBindingResolver = SetBindingResolver(
        parent?.setBindingResolver,
        component.factoryImpl.pluginContext,
        component.factoryImpl.declarationGraph,
        component
    )

    private val bindingsResolvers = listOf(
        InputsBindingResolver(inputs, component),
        GivenBindingResolver(context, declarationGraph, component),
        ComponentImplBindingResolver(component),
        ChildComponentFactoryBindingResolver(declarationGraph, component),
        mapBindingResolver,
        setBindingResolver,
        ProviderBindingResolver(component)
    )

    private val resolvedBindings = mutableMapOf<Key, BindingNode>()

    private val chain = mutableSetOf<Key>()

    fun validate(request: BindingRequest) {
        check(request.key !in chain || chain
            .toList()
            .let { it.subList(it.indexOf(request.key), it.size) }
            .any { it.type.isFunction() }
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

    fun getBinding(request: BindingRequest): BindingNode {
        var binding = resolvedBindings[request.key]
        if (binding != null) return binding

        val bindings = bindingsResolvers.flatMapFix { it(request.key) }

        if (bindings.filterNot { it is ProviderBindingNode }.size > 1) {
            error(
                "Multiple bindings found for '${request.key}' at:\n${
                    bindings
                        .filterNot { it is ProviderBindingNode }
                        .joinToString("\n") { "'${it.origin.orUnknown()}'" }
                } in '${component.origin}'"
            )
        }

        binding = bindings.firstOrNull()

        if (binding?.targetComponent != null &&
            binding.targetComponent != component.factoryImpl.component.defaultType
        ) {
            if (parent == null) {
                error(
                    "Component mismatch binding '${binding.key}' " +
                            "requires component '${binding.targetComponent?.render()}' but component is " +
                            "${component.factoryImpl.component.render()} '${component.origin}'"
                )
            } else {
                binding = null
            }
        }

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
            "No binding found for '${request.key}'\n" +
                    "required at '${request.requestingKey}' '${request.requestOrigin.orUnknown()}'\n" +
                    "in '${component.origin}'\n" +
                    "chain '$chain'\n" +
                    "all providers ${
                        component.factoryImpl.declarationGraph.bindings.map {
                            it.function.render()
                        }.joinToString("\n")
                    }\n\n\nindices${component.factoryImpl.declarationGraph.indices.joinToString("\n")}\n" +
                    "files ${component.factoryImpl.declarationGraph.module.files.joinToString("\n") { it.path }}\n\n" +
                    "${
                        component.factoryImpl.pluginContext.referenceClass(
                            FqName(
                                "com.ivianuu.injekt.internal.index.com_ivianuu_injekt_sample_Repo"
                            )
                        )?.owner?.render()
                    }\n" +
                    "${
                        component.factoryImpl.pluginContext.referenceClass(
                            FqName(
                                "com.ivianuu.injekt.sample.Repo"
                            )
                        )?.owner?.render()
                    }"
        )
    }

}
