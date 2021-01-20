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
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ResolutionScope(
    val name: String,
    val parent: ResolutionScope?,
    val declarationStore: DeclarationStore,
    val callContext: CallContext,
    contributions: () -> List<CallableRef>,
) {
    val chain: MutableSet<GivenNode> = parent?.chain ?: mutableSetOf()
    val resultsByRequest = mutableMapOf<GivenRequest, ResolutionResult>()
    val resultsByCandidate = mutableMapOf<GivenNode, CandidateResolutionResult>()

    private val givens = mutableListOf<Pair<CallableRef, ResolutionScope>>()
    private val givenSetElements = mutableListOf<CallableRef>()
    private val interceptors = mutableListOf<CallableRef>()
    private val macros = mutableListOf<CallableRef>()

    private val givenNodesByType = mutableMapOf<TypeRef, List<GivenNode>>()
    private val givenSetElementsByType = mutableMapOf<TypeRef, List<CallableRef>>()
    private val interceptorsByType = mutableMapOf<TypeRef, List<InterceptorNode>>()

    private val initialize: Unit by unsafeLazy {
        parent?.initialize

        parent?.givens
            ?.filterNot { it.first.isFromMacro }
            ?.forEach { givens += it }
        parent?.givenSetElements
            ?.filterNot { it.isFromMacro }
            ?.forEach { givenSetElements += it }
        parent?.macros?.forEach { macros += it }
        contributions().forEach { contribution ->
            contribution.collectContributions(
                declarationStore = declarationStore,
                path = listOf(contribution.callable.fqNameSafe),
                addGiven = { givens += it to this },
                addGivenSetElement = { givenSetElements += it },
                addInterceptor = { interceptors += it },
                addMacro = { macros += it }
            )
        }
        parent?.interceptors
            ?.filterNot { it.isFromMacro }
            ?.forEach { interceptors += it }
        Unit
    }

    private val setType = declarationStore.module.builtIns.set.defaultType
        .toTypeRef(declarationStore)

    private val initializeMacros by unsafeLazy {
        initialize
        collectMacroContributions(
            givens
                .filterNot { it.first.isFromMacro }
                .map { (callable, scope) ->
                    val typeWithPath = callable.type
                        .copy(path = listOf(callable.callable.fqNameSafe))
                    givens += callable.copy(type = typeWithPath) to scope
                    typeWithPath
                } +
                    declarationStore.givenFuns
                        .map { (givenFun, givenFunType) ->
                            givenFunType.defaultType
                                .copy(path = listOf(givenFun.callable))
                        }
        )
    }

    fun givensForType(type: TypeRef): List<GivenNode> {
        initializeMacros
        return givenNodesByType.getOrPut(type) {
            buildList<GivenNode> {
                this += givens
                    .filter { it.first.type.isAssignableTo(type) }
                    .map { it.first.toGivenNode(type, it.second, this@ResolutionScope) }

                if (type.classifier.descriptor?.safeAs<ClassDescriptor>()
                        ?.kind == ClassKind.OBJECT)
                    this += ObjectGivenNode(type, this@ResolutionScope)

                if (type.classifier.isGivenFunAlias) this += FunGivenNode(
                    type,
                    this@ResolutionScope,
                    interceptorsForType(type),
                    declarationStore.functionDescriptorForFqName(type.classifier.fqName)
                        .filter { it.hasAnnotation(InjektFqNames.GivenFun) }
                        .let {
                            it.singleOrNull() ?: error("Wtf ${it.joinToString("\n")}")
                        }
                        .toCallableRef(declarationStore)
                )

                if (type.path == null &&
                    type.qualifiers.isEmpty() &&
                    (type.classifier.fqName.asString().startsWith("kotlin.Function")
                            || type.classifier.fqName.asString()
                        .startsWith("kotlin.coroutines.SuspendFunction")) &&
                    type.arguments.dropLast(1).all {
                        it.contributionKind != null
                    }
                ) this += ProviderGivenNode(
                    type,
                    this@ResolutionScope,
                    interceptorsForType(type),
                    declarationStore
                )


                if (type.isSubTypeOf(setType)) {
                    val setElementType = type.subtypeView(setType.classifier)!!.arguments.single()
                    val elements = givenSetElementsForType(setElementType)
                    this += SetGivenNode(
                        type,
                        this@ResolutionScope,
                        interceptorsForType(type),
                        elements,
                        elements.flatMap { element ->
                            element.getGivenRequests(declarationStore)
                        }
                    )
                }
            }.distinct()
        }
    }

    fun givenSetElementsForType(type: TypeRef): List<CallableRef> {
        initializeMacros
        return givenSetElementsByType.getOrPut(type) {
            givenSetElements
                .filter { it.type.isAssignableTo(type) }
                .map { it.substitute(getSubstitutionMap(listOf(type to it.type))) }
                .onEach { element ->
                    element.typeArguments.values.forEach { type ->
                        type.qualifiers.forEach { q ->
                            q.type.arguments.forEach { arg ->
                                if (arg.classifier.isTypeParameter) {
                                    error("Wtf $arg $q $element $type")
                                }
                            }
                        }
                    }
                }
        }
    }

    fun interceptorsForType(type: TypeRef): List<InterceptorNode> {
        initializeMacros
        return interceptorsByType.getOrPut(type) {
            interceptors
                .filter { callContext.canCall(it.callContext) }
                .filter { it.type.isAssignableTo(type) }
                .map { it.substitute(getSubstitutionMap(listOf(type to it.type))) }
                .filter { interceptor ->
                    interceptor.parameterTypes
                        .values
                        .none { it == type }
                }
                .map {
                    InterceptorNode(
                        it,
                        it.getGivenRequests(declarationStore)
                    )
                }
        }
    }

    private fun collectMacroContributions(initialContributions: List<TypeRef>) {
        if (macros.isEmpty() || initialContributions.isEmpty()) return

        val allContributions = mutableListOf<CallableRef>()

        val processedContributions = mutableSetOf<TypeRef>()
        var contributionsToProcess = initialContributions

        while (contributionsToProcess.isNotEmpty()) {
            val nextContributions = mutableListOf<TypeRef>()

            for (contribution in contributionsToProcess) {
                if (contribution in processedContributions) continue
                processedContributions += contribution
                for (macro in macros) {
                    val macroType = macro.typeParameters.first().defaultType

                    if (!contribution.copy(path = null).isSubTypeOf(macroType)) continue
                    if (macro.callable.fqNameSafe in contribution.path!!) continue
                    val inputsSubstitutionMap = getSubstitutionMap(
                        listOf(contribution to macroType),
                        macro.typeParameters
                    )
                    val outputsSubstitutionMap = getSubstitutionMap(
                        listOf(contribution.copy(path = null) to macroType),
                        macro.typeParameters
                    )
                    val newContribution = macro.substituteInputs(inputsSubstitutionMap)
                        .copy(
                            isMacro = false,
                            isFromMacro = true,
                            typeArguments = inputsSubstitutionMap,
                            type = macro.type.substitute(outputsSubstitutionMap)
                        )

                    allContributions += newContribution
                    if (newContribution.contributionKind == ContributionKind.VALUE) {
                        val newContributionWithPath = newContribution.copy(
                            type = newContribution.type.copy(
                                path = contribution.path!! + newContribution.callable.fqNameSafe
                            )
                        )
                        allContributions += newContributionWithPath
                        nextContributions += newContributionWithPath.type
                    }
                }
            }

            contributionsToProcess = nextContributions
        }

        allContributions.forEach { contribution ->
            contribution.collectContributions(
                declarationStore = declarationStore,
                path = listOf(contribution.callable.fqNameSafe),
                addGiven = { givens += it to this },
                addGivenSetElement = { givenSetElements += it },
                addInterceptor = { interceptors += it },
                addMacro = { macros += it }
            )
        }
    }

    override fun toString(): String = "ResolutionScope($name)"
}

fun ExternalResolutionScope(declarationStore: DeclarationStore): ResolutionScope = ResolutionScope(
    name = "EXTERNAL",
    declarationStore = declarationStore,
    callContext = CallContext.DEFAULT,
    parent = null,
    contributions = {
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
    contributions = {
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
    contributions = {
        descriptor.unsubstitutedMemberScope
            .collectContributions(
                declarationStore,
                descriptor.toClassifierRef(declarationStore).defaultType
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
    contributions = { descriptor.collectContributions(declarationStore) }
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
        contributions = { declarations }
    )
}
