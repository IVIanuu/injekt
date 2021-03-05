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

    private val macros = mutableListOf<MacroNode>()
    private data class MacroNode(val callable: CallableRef) {
        val processedContributions = mutableSetOf<TypeRef>()
        fun copy() = MacroNode(callable).also {
            it.processedContributions += processedContributions
        }
    }

    private val allParents: List<ResolutionScope> = parent?.allScopes ?: emptyList()
    private val allScopes: List<ResolutionScope> = allParents + this

    private val givensByType = mutableMapOf<TypeRef, List<GivenNode>>()
    private val setElementsByType = mutableMapOf<TypeRef, List<TypeRef>>()
    private val syntheticProviderSetElementsByType = mutableMapOf<TypeRef, List<TypeRef>>()

    private val initialize: Unit by unsafeLazy {
        if (parent != null) {
            parent.initialize
            macros += parent.macros
                .map { it.copy() }
        }

        produceContributions()
            .distinctBy { it.type to it.callable.original }
            .forEach { contribution ->
                contribution.collectContributions(
                    declarationStore = declarationStore,
                    substitutionMap = emptyMap(),
                    addGiven = { callable ->
                        givens += callable
                        val typeWithMacroChain = callable.type
                            .copy(macroChain = listOf(callable.callable.fqNameSafe))
                        givens += callable.copy(type = typeWithMacroChain)
                    },
                    addGivenSetElement = { setElements += it },
                    addMacro = { macros += MacroNode(it) }
                )
            }

        allScopes
            .flatMap { it.givens }
            .filter { it.type.macroChain.isNotEmpty() }
            .forEach { runMacros(it.type) }
    }

    private val setType = declarationStore.module.builtIns.set.defaultType
        .toTypeRef(declarationStore)

    fun givensForType(type: TypeRef): List<GivenNode> {
        initialize
        return givensByType.getOrPut(type) {
            buildList<GivenNode> {
                if (parent != null) {
                    this += parent.givensForType(type)
                        .filter {
                            !it.isFrameworkGiven ||
                                    it.type.setKey != null
                        }
                }

                this += givens
                    .filter { it.type.isAssignableTo(declarationStore, type) }
                    .map { it.toGivenNode(type, this@ResolutionScope) }

                if (type.macroChain.isEmpty() &&
                    type.setKey == null &&
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

                if (type.macroChain.isEmpty() &&
                    type.setKey == null &&
                    type.isSubTypeOf(declarationStore, setType)) {
                    val setElementType = type.subtypeView(setType.classifier)!!.arguments.single()
                    val elements = (setElementsForType(setElementType)
                        .takeIf { it.isNotEmpty() } ?: syntheticProviderGivensForType(setElementType))
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

    private fun syntheticProviderGivensForType(type: TypeRef): List<TypeRef> {
        initialize
        return syntheticProviderSetElementsByType.getOrPut(type) {
            if (type.qualifiers.isEmpty() &&
                (type.classifier.fqName.asString().startsWith("kotlin.Function")
                        || type.classifier.fqName.asString()
                    .startsWith("kotlin.coroutines.SuspendFunction")) &&
                type.arguments.dropLast(1).all { it.contributionKind != null }) {
                val providerReturnType = type.arguments.last()
                val thisElements = setElements
                    .filter { it.type.isAssignableTo(declarationStore, providerReturnType) }
                    .map { it.substitute(getSubstitutionMap(declarationStore, listOf(providerReturnType to it.type))) }
                    .map { callable ->
                        val setKey = SetKey(type, callable)
                        val providerReturnTypeWithSetKey = providerReturnType.copy(setKey = setKey)

                        val typeWithSetKey = type.copy(
                            setKey = setKey,
                            arguments = type
                                .arguments
                                .dropLast(1) + providerReturnTypeWithSetKey
                        )

                        givensByType[typeWithSetKey] = listOf(
                            ProviderGivenNode(
                                type = typeWithSetKey,
                                ownerScope = this@ResolutionScope,
                                declarationStore = declarationStore,
                                additionalContributions = listOf(
                                    callable.copy(
                                        type = providerReturnTypeWithSetKey,
                                        contributionKind = ContributionKind.VALUE
                                    )
                                )
                            )
                        )

                        typeWithSetKey
                    }

                val parentElements = parent?.syntheticProviderGivensForType(type)
                if (parentElements != null && parentElements.isNotEmpty()) {
                    parentElements + thisElements
                } else thisElements
            } else {
                emptyList()
            }
        }
    }

    private fun setElementsForType(type: TypeRef): List<TypeRef> {
        initialize
        return setElementsByType.getOrPut(type) {
            val thisElements = setElements
                .filter { it.type.isAssignableTo(declarationStore, type) }
                .map { it.substitute(getSubstitutionMap(declarationStore, listOf(type to it.type))) }
                .map { callable ->
                    val typeWithSetKey = type.copy(
                        setKey = SetKey(type, callable)
                    )
                    givens += callable.copy(type = typeWithSetKey)
                    typeWithSetKey
                }
            val parentElements = parent?.setElementsForType(type)
            if (parentElements != null && parentElements.isNotEmpty()) {
                parentElements + thisElements
            } else thisElements
        }
    }

    private fun runMacros(contribution: TypeRef) {
        for (macro in macros) {
            if (contribution in macro.processedContributions) continue
            macro.processedContributions += contribution

            val macroType = macro.callable.typeParameters.first().defaultType
            if (!contribution.copy(macroChain = emptyList()).isSubTypeOf(declarationStore, macroType)) continue
            if (macro.callable.callable.fqNameSafe in contribution.macroChain) continue

            val inputsSubstitutionMap = getSubstitutionMap(
                declarationStore,
                listOf(contribution to macroType)
            )
            val outputsSubstitutionMap = getSubstitutionMap(
                declarationStore,
                listOf(contribution.copy(macroChain = emptyList()) to macroType)
            )
            val newContribution = macro.callable.substituteInputs(inputsSubstitutionMap)
                .copy(
                    isMacro = false,
                    isFromMacro = true,
                    typeArguments = inputsSubstitutionMap,
                    type = macro.callable.type.substitute(outputsSubstitutionMap)
                )

            newContribution.collectContributions(
                declarationStore = declarationStore,
                substitutionMap = outputsSubstitutionMap,
                addGiven = { givens += it },
                addGivenSetElement = { setElements += it },
                addMacro = { macros += MacroNode(it) }
            )

            if (newContribution.contributionKind == ContributionKind.VALUE) {
                val newContributionWithChain = newContribution.copy(
                    type = newContribution.type.copy(
                        macroChain = contribution.macroChain + newContribution.callable.fqNameSafe
                    )
                )
                newContributionWithChain.collectContributions(
                    declarationStore = declarationStore,
                    substitutionMap = newContributionWithChain.typeArguments,
                    addGiven = { givens += it },
                    addGivenSetElement = { setElements += it },
                    addMacro = { macros += MacroNode(it) }
                )
                runMacros(newContributionWithChain.type)
            }
        }
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
