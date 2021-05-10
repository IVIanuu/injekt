/*
 * Copyright 2021 Manuel Wrage
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
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.callUtil.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class ResolutionScope(
    val name: String,
    val parent: ResolutionScope?,
    val context: InjektContext,
    val callContext: CallContext,
    val ownerDescriptor: DeclarationDescriptor?,
    val trace: BindingTrace,
    initialGivens: List<CallableRef>,
    val imports: List<GivenImport>,
    val typeParameters: List<ClassifierRef>
) {
    val chain: MutableList<Pair<GivenRequest, GivenNode>> = parent?.chain ?: mutableListOf()
    val resultsByType = mutableMapOf<TypeRef, ResolutionResult>()
    val resultsByCandidate = mutableMapOf<GivenNode, ResolutionResult>()

    private data class GivenKey(
        val type: TypeRef,
        val callable: CallableDescriptor,
        val source: CallableRef?
    )

    private val CallableRef.givenKey: GivenKey
        get() = GivenKey(type, callable, source)

    /**
     * There should be only one given for a type + callable combination
     * If there are duplicates we choose the best version
     */
    private fun addGivenIfAbsentOrBetter(callable: CallableRef) {
        if (!callable.isApplicable()) return
        val key = callable.givenKey
        val existing = givens[key]
        if (compareCallable(callable, existing) < 0)
            givens[key] = callable
    }

    private val givens = mutableMapOf<GivenKey, CallableRef>()

    private val constrainedGivens = mutableListOf<ConstrainedGivenNode>()
    private val constrainedGivenCandidates = mutableListOf<ConstrainedGivenCandidate>()

    private data class ConstrainedGivenNode(
        val callable: CallableRef,
        val constraintType: TypeRef = callable.typeParameters.single {
            it.isGivenConstraint
        }.defaultType.substitute(callable.typeArguments),
        val processedCandidateTypes: MutableSet<TypeRef> = mutableSetOf(),
        val resultingFrameworkKeys: MutableSet<Int> = mutableSetOf()
    ) {
        fun copy() = ConstrainedGivenNode(
            callable,
            constraintType,
            processedCandidateTypes.toMutableSet(),
            resultingFrameworkKeys.toMutableSet()
        )
    }
    private data class ConstrainedGivenCandidate(
        val type: TypeRef,
        val rawType: TypeRef,
        val typeParameters: List<ClassifierRef>,
        val source: CallableRef?
    )

    val allParents: List<ResolutionScope> = parent?.allScopes ?: emptyList()
    val allScopes: List<ResolutionScope> = allParents + this

    val allStaticTypeParameters = allScopes.flatMap { it.typeParameters }

    private val givensByType = mutableMapOf<RequestKey, List<GivenNode>?>()
    private val setElementsByType = mutableMapOf<RequestKey, List<TypeRef>?>()

    data class RequestKey(
        val type: TypeRef,
        val staticTypeParameters: List<ClassifierRef>
    )

    init {
        println("initialize scope $name")
        initialGivens
            .forEach { given ->
                given.collectGivens(
                    context = context,
                    scope = this,
                    trace = trace,
                    addGiven = { callable ->
                        addGivenIfAbsentOrBetter(callable.copy(source = given))
                        val typeWithFrameworkKey = callable.type
                            .copy(frameworkKey = generateFrameworkKey())
                        addGivenIfAbsentOrBetter(callable.copy(type = typeWithFrameworkKey, source = given))
                        constrainedGivenCandidates += ConstrainedGivenCandidate(
                            type = typeWithFrameworkKey,
                            rawType = callable.type,
                            typeParameters = callable.typeParameters,
                            source = given
                        )
                    },
                    addConstrainedGiven = { constrainedGivens += ConstrainedGivenNode(it) }
                )
            }

        val hasConstrainedGivens = constrainedGivens.isNotEmpty()
        val hasConstrainedGivensCandidates = constrainedGivenCandidates.isNotEmpty()
        if (parent != null) {
            constrainedGivens.addAll(
                0,
                parent.constrainedGivens
                    .map { if (hasConstrainedGivensCandidates) it.copy() else it }
            )
            constrainedGivenCandidates.addAll(0, parent.constrainedGivenCandidates)
        }

        if ((hasConstrainedGivens && constrainedGivenCandidates.isNotEmpty()) ||
            (hasConstrainedGivensCandidates && constrainedGivens.isNotEmpty())) {
            constrainedGivenCandidates
                .toList()
                .forEach { collectConstrainedGivens(it) }
        }
    }

    fun recordLookup(location: KotlinLookupLocation) {
        parent?.recordLookup(location)
        fun recordLookup(declaration: DeclarationDescriptor) {
            if (declaration is ConstructorDescriptor) {
                recordLookup(declaration.constructedClass)
                return
            }
            when (val containingDeclaration = declaration.containingDeclaration) {
                is ClassDescriptor -> containingDeclaration.unsubstitutedMemberScope
                is PackageFragmentDescriptor -> containingDeclaration.getMemberScope()
                else -> null
            }?.recordLookup(declaration.name, location)
        }
        givens.forEach { recordLookup(it.value.callable) }
        constrainedGivens.forEach { recordLookup(it.callable.callable) }
        imports
            .filter { it.importPath != null }
            .filter { it.importPath!!.endsWith(".*") }
            .map { FqName(it.importPath!!.removeSuffix(".*")) }
            .forEach { fqName ->
                context.memberScopeForFqName(fqName)
                    ?.recordLookup("givens".asNameId(), location)
            }
    }

    fun givensForRequest(request: GivenRequest, requestingScope: ResolutionScope): List<GivenNode>? {
        // we return merged collections
        if (request.type.frameworkKey == null &&
            request.type.classifier == context.setClassifier) return null
        return givensForType(RequestKey(request.type, requestingScope.allStaticTypeParameters))
    }

    private fun givensForType(key: RequestKey): List<GivenNode>? {
        if (givens.isEmpty()) return parent?.givensForType(key)
        return givensByType.getOrPut(key) {
            val thisGivens = givens
                .asSequence()
                .mapNotNull { (_, candidate) ->
                    if (candidate.type.frameworkKey != key.type.frameworkKey)
                        return@mapNotNull null
                    val context = candidate.type.buildContext(context, key.staticTypeParameters, key.type)
                    if (!context.isOk) return@mapNotNull null
                    val substitutionMap = context.fixedTypeVariables
                    val finalCandidate = candidate.substitute(substitutionMap)
                    CallableGivenNode(
                        key.type,
                        finalCandidate.getGivenRequests(this.context, trace),
                        this,
                        finalCandidate
                    )
                }
                .toList()
                .takeIf { it.isNotEmpty() }
            val parentGivens = parent?.givensForType(key)
            if (parentGivens != null && thisGivens != null) parentGivens + thisGivens
            else thisGivens ?: parentGivens
        }
    }

    fun frameworkGivenForRequest(request: GivenRequest): GivenNode? {
        if (request.type.frameworkKey != null) return null
        if (request.type.isFunctionTypeWithOnlyGivenParameters) {
            return ProviderGivenNode(
                type = request.type,
                ownerScope = this,
                dependencyCallContext = if (request.isInline) callContext
                else request.type.callContext
            )
        } else if (request.type.classifier == context.setClassifier) {
            val singleElementType = request.type.arguments[0]
            val collectionElementType = context.collectionClassifier.defaultType
                .withArguments(listOf(singleElementType))

            var elements = setElementsForType(singleElementType, collectionElementType,
                RequestKey(request.type, allStaticTypeParameters)
            )
            if (elements == null &&
                singleElementType.isFunctionTypeWithOnlyGivenParameters) {
                val providerReturnType = singleElementType.arguments.last()
                elements = setElementsForType(providerReturnType, context.collectionClassifier
                    .defaultType.withArguments(listOf(providerReturnType)),
                    RequestKey(providerReturnType, allStaticTypeParameters)
                )
                    ?.map { elementType ->
                        singleElementType.copy(
                            arguments = singleElementType.arguments
                                .dropLast(1) + elementType
                        )
                    }
            }

            if (elements != null) {
                val elementRequests = elements
                    .mapIndexed { index, element ->
                        GivenRequest(
                            type = element,
                            defaultStrategy = if (request.type.ignoreElementsWithErrors)
                                GivenRequest.DefaultStrategy.DEFAULT_ON_ALL_ERRORS
                            else GivenRequest.DefaultStrategy.NONE,
                            callableFqName = FqName("com.ivianuu.injekt.givenSetOf<${request.type.arguments[0].render()}>"),
                            parameterName = "element$index".asNameId(),
                            isInline = false,
                            isLazy = false
                        )
                    }
                return SetGivenNode(
                    type = request.type,
                    ownerScope = this,
                    dependencies = elementRequests,
                    singleElementType = singleElementType,
                    collectionElementType = collectionElementType
                )
            }
        }

        return null
    }

    private fun setElementsForType(
        singleElementType: TypeRef,
        collectionElementType: TypeRef,
        key: RequestKey
    ): List<TypeRef>? {
        if (givens.isEmpty())
            return parent?.setElementsForType(singleElementType, collectionElementType, key)
        return setElementsByType.getOrPut(key) {
            val thisElements: List<TypeRef>? = givens
                .toList()
                .asSequence()
                .mapNotNull { (_, candidate) ->
                    if (candidate.type.frameworkKey != key.type.frameworkKey)
                        return@mapNotNull null
                    var context = candidate.type.buildContext(context, key.staticTypeParameters, singleElementType)
                    if (!context.isOk) {
                        context = candidate.type.buildContext(this.context, key.staticTypeParameters, collectionElementType)
                    }
                    if (!context.isOk) return@mapNotNull null
                    val substitutionMap = context.fixedTypeVariables
                    candidate.substitute(substitutionMap)
                }
                .map { callable ->
                    val typeWithFrameworkKey = callable.type.copy(
                        frameworkKey = generateFrameworkKey()
                    )
                    addGivenIfAbsentOrBetter(callable.copy(type = typeWithFrameworkKey))
                    typeWithFrameworkKey
                }
                .toList()
                .takeIf { it.isNotEmpty() }
            val parentElements = parent?.setElementsForType(singleElementType, collectionElementType, key)
            if (parentElements != null && thisElements != null) parentElements + thisElements
            else thisElements ?: parentElements
        }
    }

    private fun collectConstrainedGivens(candidate: ConstrainedGivenCandidate) {
        for (constrainedGiven in constrainedGivens.toList())
            collectConstrainedGivens(constrainedGiven, candidate)
    }

    private fun collectConstrainedGivens(
        constrainedGiven: ConstrainedGivenNode,
        candidate: ConstrainedGivenCandidate
    ) {
        if (candidate.type.frameworkKey in constrainedGiven.resultingFrameworkKeys) return
        if (candidate.type in constrainedGiven.processedCandidateTypes) return
        constrainedGiven.processedCandidateTypes += candidate.type
        val (context, substitutionMap) = buildContextForConstrainedGiven(
            context,
            constrainedGiven.constraintType,
            candidate.type,
            allStaticTypeParameters
        )
        if (!context.isOk) return

        val newGivenType = constrainedGiven.callable.type
            .substitute(substitutionMap)
            .copy(frameworkKey = null)
        val newGiven = constrainedGiven.callable
            .copy(
                type = newGivenType,
                originalType = newGivenType,
                parameterTypes = constrainedGiven.callable.parameterTypes
                    .mapValues { it.value.substitute(substitutionMap) },
                typeArguments = constrainedGiven.callable
                    .typeArguments
                    .mapValues { it.value.substitute(substitutionMap) },
                source = candidate.source
            )

        newGiven.collectGivens(
            context = this.context,
            scope = this,
            trace = trace,
            addGiven = { newInnerGiven ->
                val finalNewInnerGiven = newInnerGiven
                    .copy(
                        source = candidate.source,
                        originalType = newInnerGiven.type
                    )
                addGivenIfAbsentOrBetter(finalNewInnerGiven)
                val newInnerGivenWithFrameworkKey = finalNewInnerGiven.copy(
                    type = finalNewInnerGiven.type.copy(
                        frameworkKey = generateFrameworkKey()
                            .also { constrainedGiven.resultingFrameworkKeys += it }
                    )
                )
                addGivenIfAbsentOrBetter(newInnerGivenWithFrameworkKey)
                val newCandidate = ConstrainedGivenCandidate(
                    type = newInnerGivenWithFrameworkKey.type,
                    rawType = finalNewInnerGiven.type,
                    typeParameters = finalNewInnerGiven.typeParameters,
                    source = candidate.source
                )
                constrainedGivenCandidates += newCandidate
                collectConstrainedGivens(newCandidate)
            },
            addConstrainedGiven = { newInnerConstrainedGiven ->
                val finalNewInnerConstrainedGiven = newInnerConstrainedGiven
                    .copy(
                        source = candidate.source,
                        originalType = newInnerConstrainedGiven.type
                    )
                val newConstrainedGiven = ConstrainedGivenNode(finalNewInnerConstrainedGiven)
                constrainedGivens += newConstrainedGiven
                constrainedGivenCandidates
                    .toList()
                    .forEach {
                        collectConstrainedGivens(newConstrainedGiven, it)
                    }
            }
        )
    }

    /**
     * A callable is not applicable if it is a given constructor parameter property
     * of a given class but not in the scope.
     * without removing the property this would result in a divergent request
     */
    private fun CallableRef.isApplicable(): Boolean {
        if (callable !is PropertyDescriptor ||
                callable.dispatchReceiverParameter == null) return true
        val containing = callable.containingDeclaration as ClassDescriptor
        if (containing.kind == ClassKind.OBJECT) return true
        val containingClassifier = containing.toClassifierRef(context, trace)
        if (callable.name !in containingClassifier.primaryConstructorPropertyParameters) return true
        return allScopes.any { it.ownerDescriptor == containing } ||
                !containingClassifier.descriptor!!.isGiven(context, trace)
    }

    override fun toString(): String = "ResolutionScope($name)"
}
