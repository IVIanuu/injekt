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

package com.ivianuu.injekt.compiler.generator.componentimpl

import com.ivianuu.injekt.Assisted
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.generator.Callable
import com.ivianuu.injekt.compiler.generator.DeclarationStore
import com.ivianuu.injekt.compiler.generator.ModuleDescriptor
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.asNameId
import com.ivianuu.injekt.compiler.generator.getGivenConstructor
import com.ivianuu.injekt.compiler.generator.getSubstitutionMap
import com.ivianuu.injekt.compiler.generator.isAssignable
import com.ivianuu.injekt.compiler.generator.render
import com.ivianuu.injekt.compiler.generator.substitute
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@Given
class GivensGraph(
    @Assisted private val owner: ComponentImpl,
    collectionsFactory: (ComponentImpl, GivenCollections?) -> GivenCollections,
    private val declarationStore: DeclarationStore,
    private val componentFactoryImplFactory: (
        Name,
        TypeRef,
        List<TypeRef>,
        TypeRef,
        ComponentImpl?,
    ) -> ComponentFactoryImpl,
) {

    private val parent = owner.factoryImpl.parent?.graph

    private val inputs = owner.inputTypes

    private val contextType = owner.contextType

    private val instanceNodes = inputs
        .mapIndexed { index, inputType -> InputGivenNode(inputType, "p$index", owner) }
        .groupBy { it.type }

    private val moduleGivenCallables = mutableListOf<CallableWithReceiver>()

    private val collections: GivenCollections = collectionsFactory(owner, parent?.collections)

    val resolvedGivens = mutableMapOf<TypeRef, GivenNode>()

    private val chain = mutableListOf<GivenNode>()

    init {
        val modules = inputs
            .filter { it.isModule }
            .map { declarationStore.moduleForType(it) }

        fun ModuleDescriptor.collectGivens(
            parentCallable: Callable?,
            parentAccessStatement: ComponentStatement,
        ) {
            val thisAccessStatement: ComponentStatement = {
                parentAccessStatement()
                emit(".")
                if (parentCallable != null) {
                    emit("${parentCallable!!.name}")
                    if (parentCallable.isCall) emit("()")
                } else {
                    emit("p${inputs.indexOf(type)}")
                }
            }

            for (callable in callables) {
                if (callable.givenKind == null) continue
                when (callable.givenKind) {
                    Callable.GivenKind.GIVEN -> moduleGivenCallables += CallableWithReceiver(
                        callable,
                        thisAccessStatement,
                        emptyMap()
                    )
                    Callable.GivenKind.GIVEN_MAP_ENTRIES -> {
                        collections.addMapEntries(
                            CallableWithReceiver(
                                callable,
                                thisAccessStatement,
                                emptyMap()
                            )
                        )
                    }
                    Callable.GivenKind.GIVEN_SET_ELEMENTS -> {
                        collections.addSetElements(
                            CallableWithReceiver(
                                callable,
                                thisAccessStatement,
                                emptyMap()
                            )
                        )
                    }
                    Callable.GivenKind.MODULE -> {
                        declarationStore.moduleForType(callable.type)
                            .collectGivens(callable, thisAccessStatement)
                    }
                }.let {}
            }
        }

        modules.forEach {
            it.collectGivens(null) {
                emit("this@${owner.name}")
            }
        }
    }

    fun checkRequests(requests: List<GivenRequest>) {
        requests.forEach { check(it) }
    }

    private fun check(given: GivenNode) {
        if (given in chain) {
            val relevantSubchain = chain.subList(
                chain.indexOf(given), chain.lastIndex
            )
            if (relevantSubchain.any { it is ProviderGivenNode }) return
            error(
                "Circular dependency ${relevantSubchain.map { it.type.render() }} already contains ${given.type.render()}"
            )
        }
        chain.push(given)
        given
            .dependencies
            .forEach { check(it) }
        (given as? ChildFactoryGivenNode)?.childFactoryImpl?.initialize()
        chain.pop()
    }

    private fun check(request: GivenRequest) {
        check(getGiven(request))
    }

    fun getGiven(request: GivenRequest): GivenNode {
        var given = getGivenOrNull(request.type)
        if (given != null) return given

        if (request.type.isMarkedNullable) {
            given = NullGivenNode(request.type, owner)
            resolvedGivens[request.type] = given
            return given
        }

        error(
            buildString {
                var indendation = ""
                fun indent() {
                    indendation = "$indendation    "
                }
                appendLine("No given found for '${request.type.render()}' in '${contextType.render()}':")
                /*chain.push(ChainElement.Given(type))
                chain.forEachIndexed { index, element ->
                    if (index == 0) {
                        appendLine("${indendation}runReader call '${element}'")
                    } else {
                        when (element) {
                            is ChainElement.Call -> {
                                val lastElement = chain.getOrNull(index - 1)
                                if (lastElement is ChainElement.Given) {
                                    appendLine("${indendation}given by '${element}'")
                                } else {
                                    appendLine("${indendation}calls reader '${element}'")
                                }
                            }
                            is ChainElement.Given -> appendLine("${indendation}requires given '${element}'")
                        }

                    }
                    indent()
                }
                chain.pop()*/
                // todo
            }
        )
    }

    private fun getGivenOrNull(type: TypeRef): GivenNode? {
        var given = resolvedGivens[type]
        if (given != null) return given

        val givens = givensForType(type)

        if (givens.size > 1) {
            val mostSpecific = givens.getExact(type)
            if (mostSpecific != null) {
                given = mostSpecific
            } else {
                error(
                    "Multiple givens found for '${type.render()}' at:\n${
                    givens
                        .joinToString("\n") { "    '${it.origin.orUnknown()}'" }
                    }"
                )
            }
        } else {
            given = givens.singleOrNull()
        }

        given?.let {
            resolvedGivens[type] = it
            return it
        }

        if (type.isGiven || type.typeArguments.lastOrNull()?.isGiven == true) {
            val givenType = if (type.isGiven) type else type.typeArguments.last()
            given = declarationStore.callableForDescriptor(
                declarationStore.classDescriptorForFqName(givenType.classifier.fqName)
                    .getGivenConstructor()!!
            )
                .let { callable ->
                    val substitutionMap = type.getSubstitutionMap(callable.type)
                    CallableGivenNode(
                        type = type,
                        rawType = callable.type.substitute(substitutionMap),
                        owner = owner,
                        dependencies = callable.valueParameters
                            .filterNot { it.isAssisted }
                            .map {
                                GivenRequest(
                                    it.type.substitute(substitutionMap),
                                    callable.fqName.child(it.name)
                                )
                            },
                        valueParameters = callable.valueParameters.map {
                            it.copy(
                                type = it.type.substitute(substitutionMap)
                            )
                        },
                        origin = callable.fqName,
                        targetComponent = callable.targetComponent,
                        receiver = null,
                        callable = callable
                    )
                }

            given.let {
                resolvedGivens[type] = it
                return it
            }
        }

        parent?.getGivenOrNull(type)?.let {
            resolvedGivens[type] = it
            return it
        }

        return null
    }

    private fun givensForType(type: TypeRef): List<GivenNode> = buildList<GivenNode> {
        instanceNodes[type]?.let { this += it }

        if (type == contextType) {
            this += SelfGivenNode(
                type = type,
                component = owner
            )
        }

        if (type.isChildFactory) {
            val existingFactories = mutableSetOf<TypeRef>()
            var currentComponent: ComponentImpl? = owner
            while (currentComponent != null) {
                existingFactories += currentComponent.factoryImpl.factoryType
                currentComponent = currentComponent.factoryImpl.parent
            }
            if (type !in existingFactories) {
                val factoryDescriptor = declarationStore.factoryForType(type)
                val factoryImpl = componentFactoryImplFactory(
                    owner.factoryImpl.contextTreeNameProvider("F").asNameId(),
                    type,
                    factoryDescriptor.inputTypes,
                    factoryDescriptor.contextType,
                    owner
                )
                this += ChildFactoryGivenNode(
                    type = type,
                    owner = owner,
                    origin = null,
                    childFactoryImpl = factoryImpl
                )
            }
        }

        if (type.isFunctionAlias) {
            val callable = declarationStore.functionForAlias(type)
            val substitutionMap = callable.typeParameters
                .zip(type.typeArguments)
                .toMap()
            this += FunctionAliasGivenNode(
                type = type,
                rawType = callable.type,
                owner = owner,
                dependencies = callable.valueParameters
                    .filterNot { it.isAssisted }
                    .map {
                        GivenRequest(
                            it.type.substitute(substitutionMap),
                            callable.fqName.child(it.name)
                        )
                    },
                valueParameters = callable.valueParameters.map {
                    it.copy(
                        type = it.type.substitute(substitutionMap)
                    )
                },
                origin = callable.fqName,
                targetComponent = callable.targetComponent,
                receiver = null,
                callable = callable
            )
        }

        if (type.classifier.fqName.asString() == "kotlin.Function0") {
            this += ProviderGivenNode(
                type = type,
                owner = owner,
                dependencies = listOf(
                    GivenRequest(
                        type.typeArguments.single(),
                        FqName.ROOT // todo
                    )
                ),
                FqName.ROOT // todo
            )
        }

        this += moduleGivenCallables
            .filter { type.isAssignable(it.callable.type) }
            .map { (callable, receiver) ->
                val substitutionMap = type.getSubstitutionMap(callable.type)
                CallableGivenNode(
                    type = type,
                    rawType = callable.type,
                    owner = owner,
                    dependencies = callable.valueParameters
                        .filterNot { it.isAssisted }
                        .map {
                            GivenRequest(
                                it.type.substitute(substitutionMap),
                                callable.fqName.child(it.name)
                            )
                        },
                    valueParameters = callable.valueParameters.map {
                        it.copy(
                            type = it.type.substitute(substitutionMap)
                        )
                    },
                    origin = callable.fqName,
                    targetComponent = callable.targetComponent,
                    receiver = receiver,
                    callable = callable
                )
            }

        this += collections.getNodes(type)
    }

    private fun List<GivenNode>.getExact(requested: TypeRef): GivenNode? =
        singleOrNull { it.rawType == requested }
}

private fun FqName?.orUnknown(): String = this?.asString() ?: "unknown origin"

@Given
class GivenCollections(
    @Assisted private val owner: ComponentImpl,
    @Assisted private val parent: GivenCollections?,
) {

    private val thisMapEntries = mutableMapOf<TypeRef, MutableList<CallableWithReceiver>>()
    private val thisSetElements = mutableMapOf<TypeRef, MutableList<CallableWithReceiver>>()

    fun addMapEntries(entries: CallableWithReceiver) {
        thisMapEntries.getOrPut(entries.callable.type) { mutableListOf() } += entries
    }

    fun addSetElements(elements: CallableWithReceiver) {
        thisSetElements.getOrPut(elements.callable.type) { mutableListOf() } += elements
    }

    private val mapEntriesByType = mutableMapOf<TypeRef, List<CallableWithReceiver>>()
    private fun getMapEntries(type: TypeRef): List<CallableWithReceiver> {
        return mapEntriesByType.getOrPut(type) {
            (parent?.getMapEntries(type) ?: emptyList()) +
                (thisMapEntries[type] ?: emptyList())
        }
    }

    private val setElementsByType = mutableMapOf<TypeRef, List<CallableWithReceiver>>()
    private fun getSetElements(type: TypeRef): List<CallableWithReceiver> {
        return setElementsByType.getOrPut(type) {
            (parent?.getSetElements(type) ?: emptyList()) +
                (thisSetElements[type] ?: emptyList())
        }
    }

    fun getNodes(type: TypeRef): List<GivenNode> {
        return listOfNotNull(
            getMapEntries(type)
                .takeIf { it.isNotEmpty() }
                ?.let { entries ->
                    MapGivenNode(
                        type = type,
                        owner = owner,
                        dependencies = entries.flatMap { (entry, _, substitutionMap) ->
                            entry.valueParameters
                                .filterNot { it.isAssisted }
                                .map {
                                    GivenRequest(
                                        it.type.substitute(substitutionMap),
                                        entry.fqName.child(it.name)
                                    )
                                }
                        },
                        entries = entries
                    )
                },
            getSetElements(type)
                .takeIf { it.isNotEmpty() }
                ?.let { elements ->
                    SetGivenNode(
                        type = type,
                        owner = owner,
                        dependencies = elements.flatMap { (element, _, substitutionMap) ->
                            element.valueParameters
                                .filterNot { it.isAssisted }
                                .map {
                                    GivenRequest(
                                        it.type.substitute(substitutionMap),
                                        element.fqName.child(it.name)
                                    )
                                }
                        },
                        elements = elements
                    )
                }
        )
    }
}
