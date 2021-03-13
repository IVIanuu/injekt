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

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.parents
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class ResolutionScope(
    val name: String,
    val parent: ResolutionScope?,
    val declarationStore: DeclarationStore,
    val callContext: CallContext,
    val ownerDescriptor: DeclarationDescriptor?,
    var depth: Int = -1,
    produceGivens: () -> List<CallableRef>
) {
    val chain: MutableList<GivenNode> = parent?.chain ?: mutableListOf()
    val resultsByType = mutableMapOf<TypeRef, ResolutionResult>()
    val resultsByCandidate = mutableMapOf<GivenNode, ResolutionResult>()

    private val givens = mutableListOf<CallableRef>()

    private val constrainedGivens = mutableListOf<ConstrainedGivenNode>()
    private val constrainedGivenCandidates = mutableListOf<ConstrainedGivenCandidate>()

    private data class ConstrainedGivenNode(val callable: CallableRef) {
        val processedCandidateTypes = mutableSetOf<TypeRef>()
        fun copy() = ConstrainedGivenNode(callable).also {
            it.processedCandidateTypes += processedCandidateTypes
        }
    }
    private data class ConstrainedGivenCandidate(
        val type: TypeRef,
        val typeWithoutChain: TypeRef
    )

    val allParents: List<ResolutionScope> = parent?.allScopes ?: emptyList()
    val allScopes: List<ResolutionScope> = allParents + this

    private val givensByType = mutableMapOf<TypeRef, List<GivenNode>>()
    private val setElementsByType = mutableMapOf<TypeRef, List<TypeRef>>()

    private val initialize: Unit by unsafeLazy {
        if (parent != null) {
            parent.initialize
            constrainedGivens += parent.constrainedGivens
                .map { it.copy() }
            constrainedGivenCandidates += parent.constrainedGivenCandidates
        }

        var hasGivens = false

        produceGivens()
            .forEach { given ->
                given.collectGivens(
                    declarationStore = declarationStore,
                    ownerDescriptor = ownerDescriptor,
                    depth = given.depth,
                    substitutionMap = emptyMap(),
                    addGiven = { callable ->
                        hasGivens = true
                        givens += callable
                        val typeWithFrameworkKey = callable.type
                            .copy(frameworkKey = generateFrameworkKey())
                        givens += callable.copy(type = typeWithFrameworkKey)
                        constrainedGivenCandidates += ConstrainedGivenCandidate(
                            type = typeWithFrameworkKey,
                            typeWithoutChain = callable.type
                        )
                    },
                    addConstrainedGiven = {
                        hasGivens = true
                        constrainedGivens += ConstrainedGivenNode(it)
                    }
                )
            }

        if (hasGivens) {
            constrainedGivenCandidates
                .toList()
                .forEach { collectConstrainedGivens(it) }
        }

        if (depth == -1) {
            depth = givens.maxByOrNull { it.depth }?.depth ?: -1
        }
    }

    fun givensForType(type: TypeRef): List<GivenNode> {
        initialize
        return givensByType.getOrPut(type) {
            buildList<GivenNode> {
                parent?.givensForType(type)
                    ?.filterNot { it.isFrameworkGiven }
                    ?.let { this += it }
                this += givens
                    .filter { it.type.isAssignableTo(declarationStore, type) }
                    .map { it.toGivenNode(type, this@ResolutionScope) }

                if (type.qualifiers.isEmpty() &&
                    type.frameworkKey == null) {
                    if (type.isFunctionType && type.arguments.dropLast(1).all { it.isGiven }) {
                        this += ProviderGivenNode(
                            type = type,
                            ownerScope = this@ResolutionScope,
                            declarationStore = declarationStore
                        )
                    } else if (type.classifier == declarationStore.setType.classifier) {
                        val setElementType = type.arguments.single()
                        var elementTypes = setElementsForType(setElementType)
                        if (elementTypes.isEmpty() &&
                            setElementType.qualifiers.isEmpty() &&
                            setElementType.isFunctionType &&
                            setElementType.arguments.dropLast(1).all { it.isGiven }) {
                            val providerReturnType = setElementType.arguments.last()
                            elementTypes = setElementsForType(providerReturnType)
                                .map { elementType ->
                                    setElementType.copy(
                                        arguments = setElementType.arguments
                                            .dropLast(1) + elementType
                                    )
                                }
                        }

                        val elements = elementTypes
                            .mapIndexed { index, element ->
                                GivenRequest(
                                    type = element,
                                    required = true,
                                    callableFqName = FqName("com.ivianuu.injekt.givenSetOf"),
                                    parameterName = "element$index".asNameId()
                                )
                            }
                        this += SetGivenNode(
                            type = type,
                            ownerScope = this@ResolutionScope,
                            dependencies = elements
                        )
                    }
                }
            }
        }
    }

    private fun setElementsForType(type: TypeRef): List<TypeRef> {
        initialize
        val parentSetElements = parent?.setElementsForType(type) ?: emptyList()
        if (givens.isEmpty()) return parentSetElements
        return setElementsByType.getOrPut(type) {
            parentSetElements + givens
                .filter { it.type.isAssignableTo(declarationStore, type) }
                .map { it.substitute(getSubstitutionMap(declarationStore, listOf(type to it.type))) }
                .map { callable ->
                    val typeWithFrameworkKey = type.copy(
                        frameworkKey = generateFrameworkKey()
                    )
                    givens += callable.copy(type = typeWithFrameworkKey)
                    typeWithFrameworkKey
                }
        }
    }

    private fun collectConstrainedGivens(candidate: ConstrainedGivenCandidate) {
        for (constrainedGiven in constrainedGivens)
            collectConstrainedGivens(constrainedGiven, candidate)
    }

    private fun collectConstrainedGivens(
        constrainedGiven: ConstrainedGivenNode,
        candidate: ConstrainedGivenCandidate
    ) {
        if (candidate.type in constrainedGiven.processedCandidateTypes) return
        constrainedGiven.processedCandidateTypes += candidate.type

        val constraintType = constrainedGiven.callable.typeParameters.single {
            it.isGivenConstraint
        }.defaultType
        if (!candidate.typeWithoutChain
                .isSubTypeOf(declarationStore, constraintType)) return

        val inputsSubstitutionMap = getSubstitutionMap(
            declarationStore,
            listOf(candidate.type to constraintType)
        )
        val outputsSubstitutionMap = getSubstitutionMap(
            declarationStore,
            listOf(candidate.typeWithoutChain to constraintType)
        )
        val newGiven = constrainedGiven.callable.substituteInputs(inputsSubstitutionMap)
            .copy(
                fromGivenConstraint = true,
                typeArguments = inputsSubstitutionMap,
                depth = depth,
                type = constrainedGiven.callable.type.substitute(outputsSubstitutionMap)
            )

        newGiven.collectGivens(
            declarationStore = declarationStore,
            ownerDescriptor = ownerDescriptor,
            depth = depth,
            substitutionMap = outputsSubstitutionMap,
            addGiven = { newInnerGiven ->
                givens += newInnerGiven
                val newInnerGivenWithChain = newInnerGiven.copy(
                    type = newInnerGiven.type.copy(
                        frameworkKey = generateFrameworkKey()
                    )
                )
                givens += newInnerGivenWithChain
                val newCandidate = ConstrainedGivenCandidate(
                    type = newInnerGivenWithChain.type,
                    typeWithoutChain = newInnerGiven.type
                )
                constrainedGivenCandidates += newCandidate
                collectConstrainedGivens(newCandidate)
            },
            addConstrainedGiven = { newCallable ->
                val newConstrainedGiven = ConstrainedGivenNode(newCallable)
                constrainedGivens += newConstrainedGiven
                constrainedGivenCandidates
                    .toList()
                    .forEach {
                        collectConstrainedGivens(newConstrainedGiven, it)
                    }
            }
        )
    }

    override fun toString(): String = "ResolutionScope($name)"

}

fun HierarchicalResolutionScope(
    declarationStore: DeclarationStore,
    scope: HierarchicalScope,
    bindingContext: BindingContext
) = ResolutionScope(
    name = "Hierarchical $scope",
    declarationStore = declarationStore,
    callContext = scope.callContext(bindingContext),
    parent = null,
    ownerDescriptor = scope.parentsWithSelf
        .firstIsInstance<LexicalScope>()
        .ownerDescriptor,
    produceGivens = { scope.collectGivens(declarationStore) }
)
