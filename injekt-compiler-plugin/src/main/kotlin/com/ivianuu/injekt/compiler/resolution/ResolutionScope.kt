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
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ResolutionScope(
    val name: String,
    val parent: ResolutionScope?,
    val declarationStore: DeclarationStore,
    val callContext: CallContext,
    val owningDescriptor: DeclarationDescriptor?,
    produceGivens: () -> List<CallableRef>
) {
    val chain: MutableSet<GivenNode> = parent?.chain ?: mutableSetOf()
    val resultsByRequest = mutableMapOf<GivenRequest, ResolutionResult>()
    val resultsByCandidate = mutableMapOf<GivenNode, CandidateResolutionResult>()

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
                    owningDescriptor = owningDescriptor,
                    substitutionMap = emptyMap(),
                    addGiven = { callable ->
                        hasGivens = true
                        givens += callable
                        val typeWithChain = callable.type
                            .copy(
                                constrainedGivenChain = listOf(callable.callable.fqNameSafe)
                            )
                        givens += callable.copy(type = typeWithChain)
                        constrainedGivenCandidates += ConstrainedGivenCandidate(
                            type = typeWithChain,
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
    }

    fun givensForType(type: TypeRef): List<GivenNode> {
        initialize
        if (givens.isEmpty()) return emptyList()
        return givensByType.getOrPut(type) {
            givens
                .filter { it.type.isAssignableTo(declarationStore, type) }
                .map { it.toGivenNode(type, this@ResolutionScope) }
        }
    }

    fun frameworkGivensForType(type: TypeRef): GivenNode? {
        if (type.constrainedGivenChain.isNotEmpty() ||
            type.qualifiers.isNotEmpty() ||
            type.setKey != null) return null
        initialize
        return if (type.isFunctionType && type.arguments.dropLast(1).all { it.isGiven }) {
            ProviderGivenNode(
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
                        callableFqName = FqName("GivenSet"),
                        parameterName = "element$index".asNameId()
                    )
                }
            SetGivenNode(
                type = type,
                ownerScope = this@ResolutionScope,
                dependencies = elements
            )
        } else {
            null
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
                    val typeWithSetKey = type.copy(
                        setKey = SetKey(type, callable)
                    )
                    givens += callable.copy(type = typeWithSetKey)
                    typeWithSetKey
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
        if (constrainedGiven.callable.callable.fqNameSafe in candidate.type.constrainedGivenChain) return

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
                type = constrainedGiven.callable.type.substitute(outputsSubstitutionMap)
            )

        newGiven.collectGivens(
            declarationStore = declarationStore,
            owningDescriptor = owningDescriptor,
            substitutionMap = outputsSubstitutionMap,
            addGiven = { newInnerGiven ->
                givens += newInnerGiven
                val newInnerGivenWithChain = newInnerGiven.copy(
                    type = newInnerGiven.type.copy(
                        constrainedGivenChain = candidate.type.constrainedGivenChain +
                                newInnerGiven.callable.fqNameSafe
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

fun ExternalResolutionScope(declarationStore: DeclarationStore): ResolutionScope = ResolutionScope(
    name = "EXTERNAL",
    declarationStore = declarationStore,
    callContext = CallContext.DEFAULT,
    parent = null,
    owningDescriptor = null,
    produceGivens = {
        declarationStore.globalGivens
            .filter { it.callable.isExternalDeclaration() }
            .filter { it.callable.visibility == DescriptorVisibilities.PUBLIC }
    }
)

fun InternalResolutionScope(
    parent: ResolutionScope,
    declarationStore: DeclarationStore,
): ResolutionScope = ResolutionScope(
    name = "INTERNAL",
    declarationStore = declarationStore,
    callContext = CallContext.DEFAULT,
    parent = parent,
    owningDescriptor = null,
    produceGivens = {
        declarationStore.globalGivens
            .filterNot { it.callable.isExternalDeclaration() }
    }
)

fun ClassResolutionScope(
    declarationStore: DeclarationStore,
    descriptor: ClassDescriptor,
    parent: ResolutionScope,
): ResolutionScope = ResolutionScope(
    name = "CLASS(${descriptor.fqNameSafe})",
    declarationStore = declarationStore,
    callContext = CallContext.DEFAULT,
    parent = parent,
    owningDescriptor = descriptor,
    produceGivens = {
        listOf(
            descriptor.thisAsReceiverParameter.toCallableRef(declarationStore)
                .copy(isGiven = true)
        )
    }
)

fun FunctionResolutionScope(
    declarationStore: DeclarationStore,
    parent: ResolutionScope,
    descriptor: FunctionDescriptor,
    lambdaType: TypeRef?,
) = ResolutionScope(
    name = "FUN(${descriptor.fqNameSafe})",
    declarationStore = declarationStore,
    callContext = lambdaType?.callContext ?: descriptor.callContext,
    parent = parent,
    owningDescriptor = descriptor,
    produceGivens = { descriptor.collectGivens(declarationStore) }
)

fun LocalDeclarationResolutionScope(
    declarationStore: DeclarationStore,
    parent: ResolutionScope,
    declaration: DeclarationDescriptor
): ResolutionScope {
    val declarations: List<CallableRef> = when (declaration) {
        is ClassDescriptor -> declaration.getGivenConstructors(declarationStore)
        is CallableDescriptor -> if (declaration.isGiven(declarationStore)) {
            listOf(
                declaration.toCallableRef(declarationStore)
                    .copy(isGiven = true)
            )
        } else emptyList()
        else -> null
    } ?: return parent
    return ResolutionScope(
        name = "LOCAL",
        declarationStore = declarationStore,
        callContext = parent.callContext,
        parent = parent,
        owningDescriptor = parent.owningDescriptor,
        produceGivens = { declarations }
    )
}
