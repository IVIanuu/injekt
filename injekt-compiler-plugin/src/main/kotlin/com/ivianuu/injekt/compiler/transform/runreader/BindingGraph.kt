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

package com.ivianuu.injekt.compiler.transform.runreader

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.getClassFromAnnotation
import com.ivianuu.injekt.compiler.getContext
import com.ivianuu.injekt.compiler.getContextValueParameter
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.InjektContext
import com.ivianuu.injekt.compiler.transform.implicit.ImplicitContextParamTransformer
import com.ivianuu.injekt.compiler.uniqueTypeName
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class BindingGraph(
    private val injektContext: InjektContext,
    private val declarationGraph: DeclarationGraph,
    private val contextImpl: IrClass,
    val inputs: List<IrField>,
    private val implicitContextParamTransformer: ImplicitContextParamTransformer
) {

    private val inputBindingNodes = inputs
        .map { InputBindingNode(it) }
        .groupBy { it.key }

    val resolvedBindings = mutableMapOf<Key, BindingNode>()

    fun getBinding(request: BindingRequest): BindingNode {
        var binding = resolvedBindings[request.key]
        if (binding != null) return binding

        val allBindings = bindingsForKey(request.key)

        val inputBindings = allBindings
            .filterIsInstance<InputBindingNode>()
            .filter { it.key == request.key }

        if (inputBindings.size > 1) {
            error(
                "Multiple inputs found for '${request.key}' at:\n${
                inputBindings
                    .joinToString("\n") { "'${it.origin.orUnknown()}'" }
                }"
            )
        }

        binding = inputBindings.singleOrNull()
        binding?.let {
            resolvedBindings[request.key] = it
            return it
        }

        val (internalBindings, externalBindings) = allBindings
            .filterNot { it is InputBindingNode }
            .filter { it.key == request.key }
            .partition { !it.external }

        if (internalBindings.size > 1) {
            error(
                "Multiple internal bindings found for '${request.key}' at:\n${
                internalBindings
                    .joinToString("\n") { "'${it.origin.orUnknown()}'" }
                }"
            )
        }

        binding = internalBindings.singleOrNull()
        binding?.let {
            resolvedBindings[request.key] = it
            return it
        }

        if (externalBindings.size > 1) {
            error(
                "Multiple external bindings found for '${request.key}' at:\n${
                externalBindings
                    .joinToString("\n") { "'${it.origin.orUnknown()}'" }
                }.\nPlease specify a binding for the requested type in this module."
            )
        }

        binding = externalBindings.singleOrNull()
        binding?.let {
            resolvedBindings[request.key] = it
            return it
        }

        if (request.key.type.isMarkedNullable()) {
            binding = NullBindingNode(request.key)
            resolvedBindings[request.key] = binding
            return binding
        }

        error(
            "No binding found for '${request.key}'\n" +
                    "required at '${request.requestingKey}' '${request.requestOrigin.orUnknown()}'\n" +
                    "in ${contextImpl.superTypes.first().render()}\n"
        )
    }

    private fun bindingsForKey(key: Key): List<BindingNode> = buildList<BindingNode> {
        inputBindingNodes[key]?.let { this += it }

        this += declarationGraph.bindings(key.type.uniqueTypeName().asString())
            .map { function ->
                val storage = (function.getClassFromAnnotation(
                    InjektFqNames.Given, 0
                )
                    ?: if (function is IrConstructor) function.constructedClass.getClassFromAnnotation(
                        InjektFqNames.Given, 0
                    ) else null)
                    ?.takeUnless { it.defaultType.isNothing() }

                val explicitParameters = function.valueParameters
                    .filter { it != function.getContextValueParameter() }

                GivenBindingNode(
                    key = key,
                    contexts = listOf(function.getContext()!!),
                    external = function.isExternalDeclaration(),
                    explicitParameters = explicitParameters,
                    origin = function.descriptor.fqNameSafe,
                    function = function,
                    storage = storage
                )
            }

        declarationGraph.mapEntries(key.type.uniqueTypeName().asString())
            .takeIf { it.isNotEmpty() }
            ?.let { entries ->
                MapBindingNode(
                    key,
                    entries.map { it.getContext()!! },
                    entries
                )
            }
            ?.let { this += it }

        declarationGraph.setElements(key.type.uniqueTypeName().asString())
            .takeIf { it.isNotEmpty() }
            ?.let { elements ->
                SetBindingNode(
                    key,
                    elements.map { it.getContext()!! },
                    elements
                )
            }
            ?.let { this += it }
    }
}
