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

package com.ivianuu.injekt.compiler.transform.readercontext

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.getClassFromAnnotation
import com.ivianuu.injekt.compiler.getContext
import com.ivianuu.injekt.compiler.getContextValueParameter
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.implicit.ImplicitContextParamTransformer
import com.ivianuu.injekt.compiler.uniqueTypeName
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class GivensGraph(
    private val declarationGraph: DeclarationGraph,
    private val contextImpl: IrClass,
    val inputs: List<IrField>,
    private val implicitContextParamTransformer: ImplicitContextParamTransformer
) {

    private val instanceNodes = inputs
        .map { InstanceGivenNode(it) }
        .groupBy { it.key }

    private val inputFunctionNodes = mutableMapOf<Key, MutableSet<GivenNode>>()
    private val inputMapEntries = mutableMapOf<Key, MutableSet<IrFunction>>()
    private val inputSetElements = mutableMapOf<Key, MutableSet<IrFunction>>()

    val resolvedGivens = mutableMapOf<Key, GivenNode>()

    init {
        val givenSets = inputs
            .filter { it.type.classOrNull!!.owner.hasAnnotation(InjektFqNames.GivenSet) }
            .associateWith { it.type.classOrNull!!.owner }

        fun IrClass.collectGivens(
            parentAccessExpression: ContextExpression?,
            parentAccessFunction: IrFunction?
        ) {
            val functions = (functions
                .toList() + properties
                .map { it.getter!! })
                .map { implicitContextParamTransformer.getTransformedFunction(it) }

            val thisAccessExpression: ContextExpression = { c ->
                if (parentAccessExpression == null) {
                    irGetField(
                        c(),
                        contextImpl.fields
                            .single { it.type.classOrNull!!.owner == this@collectGivens }
                    )
                } else {
                    inputFunctionNodes
                    irCall(parentAccessFunction!!).apply {
                        dispatchReceiver = parentAccessExpression(c)
                    }
                }
            }

            for (function in functions) {
                fun functionOrPropertyHasAnnotation(fqName: FqName) =
                    function.hasAnnotation(fqName) ||
                            (function is IrSimpleFunction && function.correspondingPropertySymbol?.owner?.hasAnnotation(
                                fqName
                            ) == true)

                when {
                    functionOrPropertyHasAnnotation(InjektFqNames.Given) -> {
                        val storage = (function.getClassFromAnnotation(
                            InjektFqNames.Given, 0
                        )
                            ?: if (function is IrConstructor) function.constructedClass.getClassFromAnnotation(
                                InjektFqNames.Given, 0
                            ) else null)
                            ?.takeUnless { it.defaultType.isNothing() }

                        val explicitParameters = function.valueParameters
                            .filter { it != function.getContextValueParameter() }

                        inputFunctionNodes.getOrPut(function.returnType.asKey()) { mutableSetOf() } += FunctionGivenNode(
                            key = function.returnType.asKey(),
                            contexts = listOf(
                                function.getContext() ?: error("Wtf ${function.dump()}")
                            ),
                            external = function.isExternalDeclaration(),
                            explicitParameters = explicitParameters,
                            origin = function.descriptor.fqNameSafe,
                            function = function,
                            scopeContext = storage,
                            givenSetAccessExpression = thisAccessExpression
                        )
                    }
                    functionOrPropertyHasAnnotation(InjektFqNames.GivenSet) -> {
                        function.returnType.classOrNull!!.owner.collectGivens(
                            thisAccessExpression,
                            function
                        )
                    }
                    functionOrPropertyHasAnnotation(InjektFqNames.MapEntries) -> {
                        inputMapEntries.getOrPut(function.returnType.asKey()) { mutableSetOf() } += function
                    }
                    functionOrPropertyHasAnnotation(InjektFqNames.SetElements) -> {
                        inputSetElements.getOrPut(function.returnType.asKey()) { mutableSetOf() } += function
                    }
                }
            }
        }

        givenSets.values.forEach {
            it.collectGivens(null, null)
        }
    }

    fun getGivenNode(request: GivenRequest): GivenNode {
        var binding = resolvedGivens[request.key]
        if (binding != null) return binding

        val allBindings = givensForKey(request.key)

        val instanceAndGivenSetGivens = allBindings
            .filter { it.key == request.key }
            .filter { it is InstanceGivenNode || it.givenSetAccessExpression != null }

        if (instanceAndGivenSetGivens.size > 1) {
            error(
                "Multiple instance or given set givens found for '${request.key}' at:\n${
                instanceAndGivenSetGivens
                    .joinToString("\n") { "'${it.origin.orUnknown()}'" }
                }"
            )
        }

        binding = instanceAndGivenSetGivens.singleOrNull()
        binding?.let {
            resolvedGivens[request.key] = it
            return it
        }

        val (internalGlobalBindings, externalGlobalBindings) = allBindings
            .filterNot { it is InstanceGivenNode }
            .filter { it.givenSetAccessExpression == null }
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
            resolvedGivens[request.key] = it
            return it
        }

        if (externalGlobalBindings.size > 1) {
            error(
                "Multiple external bindings found for '${request.key}' at:\n${
                externalGlobalBindings
                    .joinToString("\n") { "'${it.origin.orUnknown()}'" }
                }.\nPlease specify a binding for the requested type in this project."
            )
        }

        binding = externalGlobalBindings.singleOrNull()
        binding?.let {
            resolvedGivens[request.key] = it
            return it
        }

        if (request.key.type.isMarkedNullable()) {
            binding =
                NullGivenNode(
                    request.key
                )
            resolvedGivens[request.key] = binding
            return binding
        }

        error(
            "No binding found for '${request.key}'\n" +
                    "required at '${request.requestingKey}' '${request.requestOrigin.orUnknown()}'\n" +
                    "in ${contextImpl.superTypes.first().render()}\n"
        )
    }

    private fun givensForKey(key: Key): List<GivenNode> = buildList<GivenNode> {
        instanceNodes[key]?.let { this += it }

        inputFunctionNodes[key]?.let { this += it }

        this += declarationGraph.givens(key.type.uniqueTypeName().asString())
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

                FunctionGivenNode(
                    key = key,
                    contexts = listOf(function.getContext()!!),
                    external = function.isExternalDeclaration(),
                    explicitParameters = explicitParameters,
                    origin = function.descriptor.fqNameSafe,
                    function = function,
                    scopeContext = storage,
                    givenSetAccessExpression = null
                )
            }

        (declarationGraph.mapEntries(key.type.uniqueTypeName().asString()) +
                inputMapEntries.getOrElse(key) { emptySet() })
            .takeIf { it.isNotEmpty() }
            ?.let { entries ->
                MapGivenNode(
                    key,
                    entries.map { it.getContext()!! },
                    null,
                    entries
                )
            }
            ?.let { this += it }

        (declarationGraph.setElements(key.type.uniqueTypeName().asString()) +
                inputSetElements.getOrElse(key) { emptySet() })
            .takeIf { it.isNotEmpty() }
            ?.let { elements ->
                SetGivenNode(
                    key,
                    elements.map { it.getContext()!! },
                    null,
                    elements
                )
            }
            ?.let { this += it }
    }
}
