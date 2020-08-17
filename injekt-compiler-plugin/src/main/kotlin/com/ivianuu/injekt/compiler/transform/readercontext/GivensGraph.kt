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
    private val parent: GivensGraph?,
    private val declarationGraph: DeclarationGraph,
    private val contextImpl: IrClass,
    val inputs: List<IrField>,
    private val implicitContextParamTransformer: ImplicitContextParamTransformer
) {

    private val instanceNodes = inputs
        .map { GivenInstance(it) }
        .groupBy { it.key }

    private val inputFunctionNodes = mutableMapOf<Key, MutableSet<Given>>()
    private val inputGivenMapEntries = mutableMapOf<Key, MutableSet<IrFunction>>()
    private val inputGivenSetElements = mutableMapOf<Key, MutableSet<IrFunction>>()

    val resolvedGivens = mutableMapOf<Key, Given>()

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
                        c[contextImpl],
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
                        val targetContext = (function.getClassFromAnnotation(
                            InjektFqNames.Given, 0
                        )
                            ?: (if (function is IrConstructor) function.constructedClass.getClassFromAnnotation(
                                InjektFqNames.Given, 0
                            ) else null)
                            ?: if (function is IrSimpleFunction) function.correspondingPropertySymbol
                                ?.owner?.getClassFromAnnotation(InjektFqNames.Given, 0) else null)
                            ?.takeUnless { it.defaultType.isNothing() }

                        val explicitParameters = function.valueParameters
                            .filter { it != function.getContextValueParameter() }

                        inputFunctionNodes.getOrPut(function.returnType.asKey()) { mutableSetOf() } += GivenFunction(
                            key = function.returnType.asKey(),
                            contexts = listOf(
                                function.getContext() ?: error("Wtf ${function.dump()}")
                            ),
                            external = function.isExternalDeclaration(),
                            explicitParameters = explicitParameters,
                            origin = function.descriptor.fqNameSafe,
                            function = function,
                            targetContext = targetContext,
                            givenSetAccessExpression = thisAccessExpression
                        )
                    }
                    functionOrPropertyHasAnnotation(InjektFqNames.GivenSet) -> {
                        function.returnType.classOrNull!!.owner.collectGivens(
                            thisAccessExpression,
                            function
                        )
                    }
                    functionOrPropertyHasAnnotation(InjektFqNames.GivenMapEntries) -> {
                        inputGivenMapEntries.getOrPut(function.returnType.asKey()) { mutableSetOf() } += function
                    }
                    functionOrPropertyHasAnnotation(InjektFqNames.GivenSetElements) -> {
                        inputGivenSetElements.getOrPut(function.returnType.asKey()) { mutableSetOf() } += function
                    }
                }
            }
        }

        givenSets.values.forEach {
            it.collectGivens(null, null)
        }
    }

    fun getGiven(request: GivenRequest): Given {
        var given = resolvedGivens[request.key]
        if (given != null) return given

        fun Given.check(): Given? {
            if (targetContext != null && targetContext != contextImpl.superTypes.first().classOrNull!!.owner) {
                if (parent == null) {
                    error(
                        "Context mismatch, given '${key}' " +
                                "requires context '${targetContext.defaultType.render()}' but actual context is " +
                                contextImpl.superTypes.first().render()
                    )
                } else {
                    return null
                }
            }

            return this
        }

        val allGivens = givensForKey(request.key)

        val instanceAndGivenSetGivens = allGivens
            .filter { it.key == request.key }
            .filter { it is GivenInstance || it.givenSetAccessExpression != null }

        if (instanceAndGivenSetGivens.size > 1) {
            error(
                "Multiple instance or given set givens found for '${request.key}' at:\n${
                instanceAndGivenSetGivens
                    .joinToString("\n") { "'${it.origin.orUnknown()}'" }
                }"
            )
        }

        given = instanceAndGivenSetGivens.singleOrNull()
        given?.check()?.let {
            resolvedGivens[request.key] = it
            return it
        }

        val (internalGlobalGivens, externalGlobalGivens) = allGivens
            .filterNot { it is GivenInstance }
            .filter { it.givenSetAccessExpression == null }
            .filter { it.key == request.key }
            .partition { !it.external }

        if (internalGlobalGivens.size > 1) {
            error(
                "Multiple internal givens found for '${request.key}' at:\n${
                internalGlobalGivens
                    .joinToString("\n") { "'${it.origin.orUnknown()}'" }
                }"
            )
        }

        given = internalGlobalGivens.singleOrNull()
        given?.check()?.let {
            resolvedGivens[request.key] = it
            return it
        }

        if (externalGlobalGivens.size > 1) {
            error(
                "Multiple external givens found for '${request.key}' at:\n${
                externalGlobalGivens
                    .joinToString("\n") { "'${it.origin.orUnknown()}'" }
                }.\nPlease specify a given for the requested type in this project."
            )
        }

        given = externalGlobalGivens.singleOrNull()
        given?.check()?.let {
            resolvedGivens[request.key] = it
            return it
        }

        parent?.getGiven(request)?.let { return it }

        if (request.key.type.isMarkedNullable()) {
            given = GivenNull(request.key)
            resolvedGivens[request.key] = given
            return given
        }

        error(
            "No given found for '${request.key}'\n" +
                    "required at '${request.requestingKey}' '${request.requestOrigin.orUnknown()}'\n" +
                    "in ${contextImpl.superTypes.first().render()}\n"
        )
    }

    private fun givensForKey(key: Key): List<Given> = buildList<Given> {
        instanceNodes[key]?.let { this += it }

        inputFunctionNodes[key]?.let { this += it }

        if (key.type.classOrNull!!.owner.hasAnnotation(InjektFqNames.ChildContextFactory)) {
            // todo
            this += GivenChildContext(
                key = key,
                contexts = emptyList(),
                origin = null,
                factory = key.type.classOrNull!!.owner
            )
        }

        this += declarationGraph.givens(key.type.uniqueTypeName().asString())
            .map { function ->
                val targetContext = (function.getClassFromAnnotation(
                    InjektFqNames.Given, 0
                )
                    ?: (if (function is IrConstructor) function.constructedClass.getClassFromAnnotation(
                        InjektFqNames.Given, 0
                    ) else null)
                    ?: if (function is IrSimpleFunction) function.correspondingPropertySymbol
                        ?.owner?.getClassFromAnnotation(InjektFqNames.Given, 0) else null)
                    ?.takeUnless { it.defaultType.isNothing() }

                val explicitParameters = function.valueParameters
                    .filter { it != function.getContextValueParameter() }

                GivenFunction(
                    key = key,
                    contexts = listOf(function.getContext()!!),
                    external = function.isExternalDeclaration(),
                    explicitParameters = explicitParameters,
                    origin = function.descriptor.fqNameSafe,
                    function = function,
                    targetContext = targetContext,
                    givenSetAccessExpression = null
                )
            }

        (declarationGraph.givenMapEntries(key.type.uniqueTypeName().asString()) +
                inputGivenMapEntries.getOrElse(key) { emptySet() })
            .takeIf { it.isNotEmpty() }
            ?.let { entries ->
                GivenMap(
                    key = key,
                    contexts = entries.map { it.getContext()!! },
                    givenSetAccessExpression = null,
                    functions = entries
                )
            }
            ?.let { this += it }

        (declarationGraph.givenSetElements(key.type.uniqueTypeName().asString()) +
                inputGivenSetElements.getOrElse(key) { emptySet() })
            .takeIf { it.isNotEmpty() }
            ?.let { elements ->
                GivenSet(
                    key = key,
                    contexts = elements.map { it.getContext()!! },
                    givenSetAccessExpression = null,
                    functions = elements
                )
            }
            ?.let { this += it }
    }
}
