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
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.cast

class ResolutionScope(
    val name: String,
    val parent: ResolutionScope?,
    val declarationStore: DeclarationStore,
    val callContext: CallContext,
    produceContributions: () -> List<CallableRef>
) {
    val chain: MutableSet<CandidateKey> = parent?.chain ?: mutableSetOf()
    val resultsByRequest = mutableMapOf<GivenRequest, ResolutionResult>()
    val resultsByCandidate = mutableMapOf<CandidateKey, CandidateResolutionResult>()

    private val givens = mutableListOf<Pair<CallableRef, ResolutionScope>>()
    private val givenSetElements = mutableListOf<CallableRef>()
    private val macros = mutableListOf<CallableRef>()

    private val givenNodesByType = mutableMapOf<TypeRef, List<GivenNode>>()
    private val givenSetElementsByType = mutableMapOf<TypeRef, List<CallableRef>>()

    private val processedContributions = mutableSetOf<Pair<CallableRef, TypeRef>>()

    private val initialize: Unit by unsafeLazy {
        parent?.initialize

        parent?.processedContributions?.let { processedContributions += it }
        parent?.givens?.forEach { givens += it }
        parent?.givenSetElements?.forEach { givenSetElements += it }
        parent?.macros?.forEach { macros += it }

        produceContributions()
            .distinctBy { it.type to it.callable.original }
            .forEach { contribution ->
                contribution.collectContributions(
                    declarationStore = declarationStore,
                    substitutionMap = emptyMap(),
                    addGiven = { callable ->
                        givens += callable to this
                        val typeWithPath = callable.type
                            .copy(additionalKey = listOf(callable.callable.fqNameSafe))
                        givens += callable.copy(type = typeWithPath) to this
                    },
                    addGivenSetElement = { givenSetElements += it },
                    addMacro = { macros += it }
                )
            }

        givens
            .filter { it.first.type.additionalKey != null }
            .forEach { runMacros(it.first.type) }
    }

    private val setType = declarationStore.module.builtIns.set.defaultType
        .toTypeRef(declarationStore)

    fun givensForType(type: TypeRef): List<GivenNode> {
        initialize
        return givenNodesByType.getOrPut(type) {
            buildList<GivenNode> {
                this += givens
                    .filter { it.first.type.isAssignableTo(declarationStore, type) }
                    .map { it.first.toGivenNode(type, it.second, this@ResolutionScope) }

                if (type.additionalKey == null &&
                    type.qualifiers.isEmpty() &&
                    (type.classifier.fqName.asString().startsWith("kotlin.Function")
                            || type.classifier.fqName.asString()
                        .startsWith("kotlin.coroutines.SuspendFunction")) &&
                    type.arguments.dropLast(1).all {
                        it.contributionKind != null
                    }
                ) {
                    this += ProviderGivenNode(
                        type,
                        this@ResolutionScope,
                        declarationStore
                    )
                }

                if (type.isSubTypeOf(declarationStore, setType)) {
                    val setElementType = type.subtypeView(setType.classifier)!!.arguments.single()
                    val elements = givenSetElementsForType(setElementType)
                    this += SetGivenNode(
                        type,
                        this@ResolutionScope,
                        elements,
                        elements.flatMap { element ->
                            element.getGivenRequests(declarationStore)
                        }
                    )
                }
            }
        }
    }

    fun givenSetElementsForType(type: TypeRef): List<CallableRef> {
        initialize
        return givenSetElementsByType.getOrPut(type) {
            givenSetElements
                .filter { it.type.isAssignableTo(declarationStore, type) }
                .map { it.substitute(getSubstitutionMap(declarationStore, listOf(type to it.type))) }
        }
    }

    private fun runMacros(contribution: TypeRef) {
        for (macro in macros) {
            val key = macro to contribution
            if (key in processedContributions) continue
            processedContributions += key

            val macroType = macro.typeParameters.first().defaultType
            if (!contribution.copy(additionalKey = null).isSubTypeOf(declarationStore, macroType)) continue
            if (macro.callable.fqNameSafe in contribution.additionalKey.cast<List<Any>>()) continue

            val inputsSubstitutionMap = getSubstitutionMap(
                declarationStore,
                listOf(contribution to macroType)
            )
            val outputsSubstitutionMap = getSubstitutionMap(
                declarationStore,
                listOf(contribution.copy(additionalKey = null) to macroType)
            )
            val newContribution = macro.substituteInputs(inputsSubstitutionMap)
                .copy(
                    isMacro = false,
                    isFromMacro = true,
                    typeArguments = inputsSubstitutionMap,
                    type = macro.type.substitute(outputsSubstitutionMap)
                )

            newContribution.collectContributions(
                declarationStore = declarationStore,
                substitutionMap = outputsSubstitutionMap,
                addGiven = { givens += it to this },
                addGivenSetElement = { givenSetElements += it },
                addMacro = { macros += it }
            )

            if (newContribution.contributionKind == ContributionKind.VALUE) {
                val newContributionWithPath = newContribution.copy(
                    type = newContribution.type.copy(
                        additionalKey = contribution.additionalKey!!.cast<List<Any>>() + newContribution.callable.fqNameSafe
                    )
                )
                newContributionWithPath.collectContributions(
                    declarationStore = declarationStore,
                    substitutionMap = newContributionWithPath.typeArguments,
                    addGiven = { givens += it to this },
                    addGivenSetElement = { givenSetElements += it },
                    addMacro = { macros += it }
                )
                runMacros(newContributionWithPath.type)
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
