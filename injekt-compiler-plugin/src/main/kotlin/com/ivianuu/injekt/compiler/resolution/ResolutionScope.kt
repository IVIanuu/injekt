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

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.isExternalDeclaration
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
    contributions: List<CallableRef>,
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

    init {
        parent?.givens
            ?.filterNot { it.first.isFromMacro }
            ?.forEach { givens += it }
        parent?.givenSetElements
            ?.filterNot { it.isFromMacro }
            ?.forEach { givenSetElements += it }
        parent?.macros?.forEach { macros += it }
        contributions.forEach { contribution ->
            contribution.collectContributions(
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

        collectMacroContributions(
            givens
                .filterNot { it.first.isFromMacro }
                .map { it.first.type } +
                    declarationStore.givenFuns
                        .map { (givenFun, givenFunType) ->
                            givenFunType.defaultType
                                .copy(
                                    qualifiers = givenFun.callable
                                        .getAnnotatedAnnotations(InjektFqNames.Qualifier)
                                )
                        }
        )
    }

    fun givensForType(type: TypeRef): List<GivenNode> = givenNodesByType.getOrPut(type) {
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
                CallableRef(
                    declarationStore.functionDescriptorForFqName(type.classifier.fqName)
                        .single()
                )
            )

            if (type.path == null &&
                type.qualifiers.isEmpty() &&
                (type.classifier.fqName.asString().startsWith("kotlin.Function")
                        || type.classifier.fqName.asString()
                    .startsWith("kotlin.coroutines.SuspendFunction")) &&
                type.typeArguments.dropLast(1).all {
                    it.contributionKind != null
                }
            ) this += ProviderGivenNode(
                type,
                this@ResolutionScope,
                interceptorsForType(type),
                declarationStore
            )

            val setType = declarationStore.module.builtIns.set.defaultType.toTypeRef()
            if (type.isSubTypeOf(setType)) {
                val setElementType = type.subtypeView(setType.classifier)!!.typeArguments.single()
                val elements = givenSetElementsForType(setElementType)
                this += SetGivenNode(
                    type,
                    this@ResolutionScope,
                    interceptorsForType(type),
                    elements,
                    elements.flatMap { element -> element.getGivenRequests(false) }
                )
            }
        }.distinct()
    }

    fun givenSetElementsForType(type: TypeRef): List<CallableRef> = givenSetElementsByType.getOrPut(type) {
        givenSetElements
            .filter { it.type.isAssignableTo(type) }
            .map { it.substitute(getSubstitutionMap(listOf(type to it.type))) }
    }

    fun interceptorsForType(type: TypeRef): List<InterceptorNode> = interceptorsByType.getOrPut(type) {
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
                    it.getGivenRequests(false)
                )
            }
    }

    private fun collectMacroContributions(initialContributions: List<TypeRef>) {
        if (macros.isEmpty() || initialContributions.isEmpty()) return

        val allContributions = mutableListOf<CallableRef>()

        val processedContributions = mutableSetOf<TypeRef>()
        var contributionsToProcess = initialContributions

        while (contributionsToProcess.isNotEmpty()) {
            val newContributions = mutableListOf<CallableRef>()

            for (contribution in contributionsToProcess) {
                if (contribution in processedContributions) continue
                processedContributions += contribution
                for (macro in macros) {
                    val macroType = macro.callable.typeParameters.first()
                        .defaultType.toTypeRef()
                    if (!contribution.isSubTypeOf(macroType)) continue
                    val substitutionMap = getSubstitutionMap(
                        listOf(contribution to macroType),
                        macro.callable.typeParameters.map { it.toClassifierRef() }
                    )
                    val result = macro.substitute(substitutionMap)
                        .copy(
                            isMacro = false,
                            isFromMacro = true,
                            typeArguments = substitutionMap
                        )
                    newContributions += result
                }
            }

            allContributions += newContributions
            contributionsToProcess = newContributions
                .filter { it.contributionKind == ContributionKind.VALUE }
                .map { it.type }
        }

        allContributions.forEach { contribution ->
            contribution.collectContributions(
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
    contributions = declarationStore.globalContributions
        .filter { it.callable.isExternalDeclaration() }
        .filter { it.callable.visibility == DescriptorVisibilities.PUBLIC }
)

fun InternalResolutionScope(
    parent: ResolutionScope,
    declarationStore: DeclarationStore,
): ResolutionScope = ResolutionScope(
    name = "INTERNAL",
    declarationStore = declarationStore,
    callContext = CallContext.DEFAULT,
    parent = parent,
    contributions = declarationStore.globalContributions
        .filterNot { it.callable.isExternalDeclaration() }
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
    contributions = descriptor.unsubstitutedMemberScope
        .collectContributions(descriptor.defaultType.toTypeRef()) +
            CallableRef(descriptor.thisAsReceiverParameter, contributionKind = ContributionKind.VALUE)
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
    contributions = descriptor.collectContributions()
)

fun LocalDeclarationResolutionScope(
    declarationStore: DeclarationStore,
    parent: ResolutionScope,
    declaration: DeclarationDescriptor
): ResolutionScope {
    val declarations: List<CallableRef> = when (declaration) {
        is ClassDescriptor -> declaration.getContributionConstructors()
        is CallableDescriptor -> declaration
            .contributionKind()
            ?.let { listOf(CallableRef(declaration, contributionKind = it)) }
        else -> null
    } ?: return parent
    return ResolutionScope(
        name = "LOCAL",
        declarationStore = declarationStore,
        callContext = parent.callContext,
        parent = parent,
        contributions = declarations
    )
}
