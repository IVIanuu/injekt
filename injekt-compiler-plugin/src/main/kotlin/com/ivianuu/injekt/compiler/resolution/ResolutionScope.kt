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
    produceContributions: () -> List<CallableRef>
) {
    val chain: MutableSet<GivenNode> = parent?.chain ?: mutableSetOf()
    val resultsByRequest = mutableMapOf<GivenRequest, ResolutionResult>()
    val resultsByCandidate = mutableMapOf<GivenNode, CandidateResolutionResult>()

    private val givens = mutableListOf<CallableRef>()
    private val setElements = mutableListOf<CallableRef>()

    private val constrainedContributions = mutableListOf<ConstrainedContributionNode>()
    private data class ConstrainedContributionNode(val callable: CallableRef) {
        val processedContributions = mutableSetOf<TypeRef>()
        fun copy() = ConstrainedContributionNode(callable).also {
            it.processedContributions += processedContributions
        }
    }

    val allParents: List<ResolutionScope> = parent?.allScopes ?: emptyList()
    val allScopes: List<ResolutionScope> = allParents + this

    private val givensByType = mutableMapOf<TypeRef, List<GivenNode>>()
    private val setElementsByType = mutableMapOf<TypeRef, List<TypeRef>>()

    private val initialize: Unit by unsafeLazy {
        if (parent != null) {
            parent.initialize
            constrainedContributions += parent.constrainedContributions
                .map { it.copy() }
        }

        var hasGivensOrConstrainedContributions = false

        produceContributions()
            .forEach { contribution ->
                contribution.collectContributions(
                    declarationStore = declarationStore,
                    substitutionMap = emptyMap(),
                    addGiven = { callable ->
                        hasGivensOrConstrainedContributions = true
                        givens += callable
                        val typeWithChain = callable.type
                            .copy(
                                constrainedContributionChain = listOf(callable.callable.fqNameSafe)
                            )
                        givens += callable.copy(type = typeWithChain)
                    },
                    addGivenSetElement = { setElements += it },
                    addConstrainedContribution = {
                        hasGivensOrConstrainedContributions = true
                        constrainedContributions += ConstrainedContributionNode(it)
                    }
                )
            }

        if (hasGivensOrConstrainedContributions) {
            allConstrainedContributionCandidates
                .forEach { collectConstrainedContributions(it.type) }
        }
    }

    private val setType = declarationStore.module.builtIns.set.defaultType
        .toTypeRef(declarationStore)

    fun givensForType(type: TypeRef): List<GivenNode> {
        initialize
        return givensByType.getOrPut(type) {
            buildList<GivenNode> {
                parent?.givensForType(type)
                    ?.filter { !it.isFrameworkGiven }
                    ?.let { this += it }

                this += givens
                    .filter { it.type.isAssignableTo(declarationStore, type) }
                    .map { it.toGivenNode(type, this@ResolutionScope) }

                if (type.constrainedContributionChain.isEmpty() &&
                    type.qualifiers.isEmpty() &&
                    (type.classifier.fqName.asString().startsWith("kotlin.Function")
                            || type.classifier.fqName.asString()
                        .startsWith("kotlin.coroutines.SuspendFunction")) &&
                    type.arguments.dropLast(1).all {
                        it.contributionKind != null
                    }
                ) {
                    this += ProviderGivenNode(
                        type = type,
                        ownerScope = this@ResolutionScope,
                        declarationStore = declarationStore
                    )
                }

                if (type.constrainedContributionChain.isEmpty() &&
                    type.setKey == null &&
                    type.isSubTypeOf(declarationStore, setType)) {
                    val setElementType = type.subtypeView(setType.classifier)!!.arguments.single()
                    var elementTypes = setElementsForType(setElementType)
                    if (elementTypes.isEmpty() &&
                        setElementType.qualifiers.isEmpty() &&
                        (setElementType.classifier.fqName.asString().startsWith("kotlin.Function")
                                || setElementType.classifier.fqName.asString()
                            .startsWith("kotlin.coroutines.SuspendFunction")) &&
                        setElementType.arguments.dropLast(1).all { it.contributionKind != null }) {
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
                    this += SetGivenNode(
                        type = type,
                        ownerScope = this@ResolutionScope,
                        dependencies = elements
                    )
                }
            }
        }
    }

    private fun setElementsForType(type: TypeRef): List<TypeRef> {
        initialize
        return setElementsByType.getOrPut(type) {
            (parent?.setElementsForType(type) ?: emptyList()) + setElements
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

    private val allConstrainedContributionCandidates get() = allScopes
        .flatMap { it.givens }
        .filter { it.type.constrainedContributionChain.isNotEmpty() }

    private fun collectConstrainedContributions(contribution: TypeRef) {
        for (constrainedContribution in constrainedContributions)
            collectConstrainedContributions(constrainedContribution, contribution)
    }

    private fun collectConstrainedContributions(
        constrainedContribution: ConstrainedContributionNode,
        contribution: TypeRef
    ) {
        if (contribution in constrainedContribution.processedContributions) return
        constrainedContribution.processedContributions += contribution

        val constraintType = constrainedContribution.callable.typeParameters.single {
            it.isGivenConstraint
        }.defaultType
        if (!contribution.copy(constrainedContributionChain = emptyList())
                .isSubTypeOf(declarationStore, constraintType)) return
        if (constrainedContribution.callable.callable.fqNameSafe in
            contribution.constrainedContributionChain) return

        val inputsSubstitutionMap = getSubstitutionMap(
            declarationStore,
            listOf(contribution to constraintType)
        )
        val outputsSubstitutionMap = getSubstitutionMap(
            declarationStore,
            listOf(contribution.copy(constrainedContributionChain = emptyList()) to constraintType)
        )
        val newContribution = constrainedContribution.callable.substituteInputs(inputsSubstitutionMap)
            .copy(
                fromGivenConstraint = true,
                typeArguments = inputsSubstitutionMap,
                type = constrainedContribution.callable.type.substitute(outputsSubstitutionMap)
            )

        newContribution.collectContributions(
            declarationStore = declarationStore,
            substitutionMap = outputsSubstitutionMap,
            addGiven = { newGiven ->
                givens += newGiven
                val newGivenWithChain = newGiven.copy(
                    type = newGiven.type.copy(
                        constrainedContributionChain = contribution.constrainedContributionChain + newContribution.callable.fqNameSafe
                    )
                )
                givens += newGivenWithChain
                collectConstrainedContributions(newGivenWithChain.type)
            },
            addGivenSetElement = { setElements += it },
            addConstrainedContribution = { newCallable ->
                val newConstrainedContribution = ConstrainedContributionNode(newCallable)
                constrainedContributions += newConstrainedContribution
                allConstrainedContributionCandidates.forEach {
                    collectConstrainedContributions(newConstrainedContribution, it.type)
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
    produceContributions = {
        declarationStore.globalContributions
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
    produceContributions = {
        declarationStore.globalContributions
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
    produceContributions = {
        descriptor.unsubstitutedMemberScope
            .collectContributions(
                declarationStore,
                descriptor.toClassifierRef(declarationStore).defaultType,
                emptyMap()
            ) + descriptor.thisAsReceiverParameter.toCallableRef(declarationStore)
            .copy(contributionKind = ContributionKind.VALUE)
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
    produceContributions = { descriptor.collectContributions(declarationStore) }
)

fun LocalDeclarationResolutionScope(
    declarationStore: DeclarationStore,
    parent: ResolutionScope,
    declaration: DeclarationDescriptor
): ResolutionScope {
    val declarations: List<CallableRef> = when (declaration) {
        is ClassDescriptor -> declaration.getContributionConstructors(declarationStore)
        is CallableDescriptor -> declaration
            .contributionKind(declarationStore)
            ?.let {
                listOf(
                    declaration.toCallableRef(declarationStore)
                        .copy(contributionKind = it)
                )
            }
        else -> null
    } ?: return parent
    return ResolutionScope(
        name = "LOCAL",
        declarationStore = declarationStore,
        callContext = parent.callContext,
        parent = parent,
        produceContributions = { declarations }
    )
}
