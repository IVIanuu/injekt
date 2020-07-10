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
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.render

class ComponentGraph(
    val parent: ComponentGraph?,
    val component: ComponentImpl,
    context: IrPluginContext,
    declarationGraph: DeclarationGraph,
    val symbols: InjektSymbols,
    inputParameters: List<IrValueParameter>
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
        InputParameterBindingResolver(inputParameters, component),
        GivenBindingResolver(context, declarationGraph, component),
        NoArgProviderBindingResolver(component),
        ComponentImplBindingResolver(component),
        ChildComponentFactoryBindingResolver(component),
        mapBindingResolver,
        setBindingResolver
    )
    val resolvedBindings = mutableMapOf<Key, BindingNode>()

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

        binding = bindings.singleOrNull()

        if (binding?.targetComponent != null &&
            binding.targetComponent != component.factoryImpl.node.component.defaultType
        ) {
            if (parent == null) {
                error(
                    "Component mismatch binding '${binding.key}' " +
                            "requires component '${binding.targetComponent?.render()}' but component is " +
                            "${component.factoryImpl.node.component.render()} '${component.origin}'"
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
            "No binding found for '${request.key}' " +
                    "required at '${request.requestOrigin.orUnknown()}' " +
                    "in '${component.origin}'"
        )
    }

}
