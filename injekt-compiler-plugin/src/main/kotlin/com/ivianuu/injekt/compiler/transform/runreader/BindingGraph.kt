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
import com.ivianuu.injekt.compiler.flatMapFix
import com.ivianuu.injekt.compiler.getClassFromAnnotation
import com.ivianuu.injekt.compiler.getContext
import com.ivianuu.injekt.compiler.getContextValueParameter
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.implicit.ImplicitContextParamTransformer
import com.ivianuu.injekt.compiler.uniqueTypeName
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class BindingGraph(
    private val declarationGraph: DeclarationGraph,
    private val contextImpl: IrClass,
    val inputs: List<IrField>,
    private val implicitContextParamTransformer: ImplicitContextParamTransformer
) {

    private val instanceBindingNode = inputs
        .map { InstanceBindingNode(it) }
        .groupBy { it.key }

    private val modules = inputs
        .filter { it.type.classOrNull!!.owner.hasAnnotation(InjektFqNames.Module) }
        .associateWith { it.type.classOrNull!!.owner }

    val resolvedBindings = mutableMapOf<Key, BindingNode>()

    fun getBinding(request: BindingRequest): BindingNode {
        var binding = resolvedBindings[request.key]
        if (binding != null) return binding

        val allBindings = bindingsForKey(request.key)

        val instanceAndModuleBindings = allBindings
            .filter { it.key == request.key }
            .filter { it is InstanceBindingNode || it.module != null }

        if (instanceAndModuleBindings.size > 1) {
            error(
                "Multiple instance or module bindings found for '${request.key}' at:\n${
                instanceAndModuleBindings
                    .joinToString("\n") { "'${it.origin.orUnknown()}'" }
                }"
            )
        }

        binding = instanceAndModuleBindings.singleOrNull()
        binding?.let {
            resolvedBindings[request.key] = it
            return it
        }

        val (internalGlobalBindings, externalGlobalBindings) = allBindings
            .filterNot { it is InstanceBindingNode }
            .filter { it.module == null }
            .filter { it.key == request.key }
            .partition { !it.external }

        if (internalGlobalBindings.size > 1) {
            error(
                "Multiple internal bindings found for '${request.key}' at:\n${
                internalGlobalBindings
                    .joinToString("\n") { "'${it.origin.orUnknown()}'" }
                }"
            )
        }

        binding = internalGlobalBindings.singleOrNull()
        binding?.let {
            resolvedBindings[request.key] = it
            return it
        }

        if (externalGlobalBindings.size > 1) {
            error(
                "Multiple external bindings found for '${request.key}' at:\n${
                externalGlobalBindings
                    .joinToString("\n") { "'${it.origin.orUnknown()}'" }
                }.\nPlease specify a binding for the requested type in this module."
            )
        }

        binding = externalGlobalBindings.singleOrNull()
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
        instanceBindingNode[key]?.let { this += it }

        this += modules.values
            .flatMapFix { module ->
                (module.functions
                    .filter {
                        it.hasAnnotation(InjektFqNames.Given)
                    }
                    .toList() + module.properties
                    .filter { it.hasAnnotation(InjektFqNames.Given) }
                    .map { it.getter!! })
                    .filter { it.returnType.asKey() == key }
                    .map { implicitContextParamTransformer.getTransformedFunction(it) }
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
                            contexts = listOf(
                                function.getContext() ?: error("Wtf ${function.dump()}")
                            ),
                            external = function.isExternalDeclaration(),
                            explicitParameters = explicitParameters,
                            origin = function.descriptor.fqNameSafe,
                            function = function,
                            storage = storage,
                            module = module
                        )
                    }
            }

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
                    storage = storage,
                    module = null
                )
            }

        declarationGraph.mapEntries(key.type.uniqueTypeName().asString())
            .takeIf { it.isNotEmpty() }
            ?.let { entries ->
                MapBindingNode(
                    key,
                    entries.map { it.getContext()!! },
                    null,
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
                    null,
                    elements
                )
            }
            ?.let { this += it }
    }
}
