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

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.generateFrameworkKey
import com.ivianuu.injekt.compiler.isExternalDeclaration
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.LazyImportScope
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class ResolutionScope(
    val name: String,
    val parent: ResolutionScope?,
    val context: InjektContext,
    val callContext: CallContext,
    val ownerDescriptor: DeclarationDescriptor?,
    val trace: BindingTrace,
    initialGivens: List<CallableRef>
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
        get() = GivenKey(type, callable, constrainedGivenSource)

    private fun addGivenIfAbsentOrBetter(callable: CallableRef) {
        val key = callable.givenKey
        val existing = givens[key]
        if (compareCallable(callable, existing) < 0)
            givens[key] = callable
    }

    private val givens = mutableMapOf<GivenKey, CallableRef>()
    private val abstractGivens = mutableListOf<CallableRef>()

    private val constrainedGivens = mutableListOf<ConstrainedGivenNode>()
    private val constrainedGivenCandidates = mutableListOf<ConstrainedGivenCandidate>()

    private data class ConstrainedGivenNode(
        val callable: CallableRef,
        val constraintType: TypeRef = callable.typeParameters.single {
            it.isGivenConstraint
        }.defaultType,
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
        val notGivens: List<TypeRef>,
        val source: CallableRef
    )

    val allParents: List<ResolutionScope> = parent?.allScopes ?: emptyList()
    val allScopes: List<ResolutionScope> = allParents + this

    private val givensByType = mutableMapOf<TypeRef, List<GivenNode>?>()
    private val setElementsByType = mutableMapOf<TypeRef, List<TypeRef>?>()

    init {
        if (parent != null) abstractGivens += parent.abstractGivens
        initialGivens
            .forEach { given ->
                given.collectGivens(
                    context = context,
                    scope = this,
                    trace = trace,
                    addGiven = { callable ->
                        addGivenIfAbsentOrBetter(callable)
                        val typeWithFrameworkKey = callable.type
                            .copy(frameworkKey = generateFrameworkKey())
                        addGivenIfAbsentOrBetter(callable.copy(type = typeWithFrameworkKey))
                        constrainedGivenCandidates += ConstrainedGivenCandidate(
                            type = typeWithFrameworkKey,
                            rawType = callable.type,
                            notGivens = callable.notGivens,
                            source = callable
                        )
                    },
                    addAbstractGiven = { callable ->
                        abstractGivens += callable
                        val typeWithFrameworkKey = callable.type
                            .copy(frameworkKey = generateFrameworkKey())
                        abstractGivens += callable.copy(type = typeWithFrameworkKey)
                        constrainedGivenCandidates += ConstrainedGivenCandidate(
                            type = typeWithFrameworkKey,
                            rawType = callable.type,
                            notGivens = callable.notGivens,
                            source = callable
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
    }

    fun givensForRequest(request: GivenRequest): List<GivenNode>? = givensForType(request.type)
        ?.filter { given ->
            given !is CallableGivenNode ||
                    (given.callable.callable.visibility != DescriptorVisibilities.INTERNAL ||
                    !given.callable.callable.isExternalDeclaration() ||
                    DescriptorVisibilities.INTERNAL.isVisible(
                        null,
                        given.callable.callable,
                        request.requestDescriptor
                    )) && (!request.forNotGiven || request.requestDescriptor != given.callable.callable)
        }?.takeIf { it.isNotEmpty() }

    fun givensForType(type: TypeRef): List<GivenNode>? {
        if (givens.isEmpty()) return parent?.givensForType(type)
        // we return merged collections
        if (type.frameworkKey == null &&
            type.classifier == context.setClassifier) return null
        return givensByType.getOrPut(type) {
            val thisGivens = givens
                .asSequence()
                .filter {
                    it.value.type.frameworkKey == type.frameworkKey
                            && it.value.type.isAssignableTo(context, type) &&
                            it.value.isApplicable()
                }
                .map { it.value.toGivenNode(type, this) }
                .toList()
                .takeIf { it.isNotEmpty() }
            val parentGivens = parent?.givensForType(type)
            if (parentGivens != null && thisGivens != null) parentGivens + thisGivens
            else thisGivens ?: parentGivens
        }
    }

    fun frameworkGivensForRequest(request: GivenRequest): List<GivenNode>? {
        if (request.type.frameworkKey == null &&
            request.type.qualifiers.isEmpty() &&
            request.type.isFunctionTypeWithOnlyGivenParameters) {
            return listOf(
                ProviderGivenNode(
                    type = request.type,
                    ownerScope = this,
                    dependencyCallContext = if (request.isInline) callContext
                    else request.type.callContext
                )
            )
        } else if (request.type.frameworkKey == null &&
            request.type.qualifiers.isEmpty() &&
            request.type.classifier == context.setClassifier) {
            val singleElementType = request.type.arguments[0]
            val collectionElementType = context.collectionClassifier.defaultType
                .typeWith(listOf(singleElementType))

            val frameworkElements =
                frameworkSetElementsForType(singleElementType, collectionElementType)
            var elements = if (frameworkElements != null) {
                setElementsForType(singleElementType, collectionElementType)
                    ?.let { it + frameworkElements } ?: frameworkElements
            } else {
                setElementsForType(singleElementType, collectionElementType)
            }
            if (elements == null &&
                singleElementType.qualifiers.isEmpty() &&
                singleElementType.isFunctionTypeWithOnlyGivenParameters) {
                val providerReturnType = singleElementType.arguments.last()
                elements = setElementsForType(providerReturnType, context.collectionClassifier
                    .defaultType.typeWith(listOf(providerReturnType)))
                    ?.map { elementType ->
                        singleElementType.copy(
                            arguments = singleElementType.arguments
                                .dropLast(1) + elementType
                        )
                    }
            }

            val elementRequests = elements
                ?.map { element ->
                    GivenRequest(
                        type = element,
                        isRequired = true,
                        callableFqName = FqName("com.ivianuu.injekt.givenSetOf<${request.type.arguments[0].render()}>"),
                        parameterName = "element".asNameId(),
                        isInline = false,
                        isLazy = false,
                        forNotGiven = false,
                        requestDescriptor = ownerDescriptor.cast()
                    )
                }
                ?.filter { candidateRequest ->
                    val candidate = (givensForRequest(candidateRequest)
                        ?: frameworkGivensForRequest(candidateRequest))!!.single()
                    candidate.notGivens.isEmpty() || resolveRequest(
                        candidateRequest.copy(forNotGiven = true)
                    ) is ResolutionResult.Failure.NoCandidates
                }
                ?.takeIf { it.isNotEmpty() }

            if (elementRequests != null) {
                return listOf(
                    SetGivenNode(
                        type = request.type,
                        ownerScope = this,
                        dependencies = elementRequests,
                        singleElementType = singleElementType,
                        collectionElementType = collectionElementType
                    )
                )
            }
        } else if (abstractGivens.isNotEmpty()) {
            abstractGivens
                .asSequence()
                .filter {
                    it.type.frameworkKey == request.type.frameworkKey
                            && it.type.isAssignableTo(context, request.type) &&
                            it.isApplicable()
                }
                .map { AbstractGivenNode(request.type, it.type, this) }
                .toList()
                .takeIf { it.isNotEmpty() }
                ?.let { return it }
        }

        return null
    }

    private fun setElementsForType(
        singleElementType: TypeRef,
        collectionElementType: TypeRef
    ): List<TypeRef>? {
        if (givens.isEmpty())
            return parent?.setElementsForType(singleElementType, collectionElementType)
        return setElementsByType.getOrPut(singleElementType) {
            val thisElements = givens
                .toList()
                .asSequence()
                .filter { (_, candidate) ->
                    ((candidate.type.frameworkKey == singleElementType.frameworkKey &&
                            candidate.type.isAssignableTo(context, singleElementType)) ||
                            (candidate.type.frameworkKey == collectionElementType.frameworkKey) &&
                            candidate.type.isAssignableTo(context, collectionElementType)) &&
                            candidate.isApplicable()
                }
                .map { (_, candidate) ->
                    candidate.substitute(
                        when {
                            candidate.type.isAssignableTo(context, singleElementType) ->
                                getSubstitutionMap(context, listOf(singleElementType to candidate.type))
                            candidate.type.isAssignableTo(context, collectionElementType) ->
                                getSubstitutionMap(context,
                                    listOf(collectionElementType to candidate.type.subtypeView(context.collectionClassifier)!!))
                            else -> throw AssertionError()
                        }
                    )
                }
                .map { candidate ->
                    val typeWithFrameworkKey = candidate.type.copy(
                        frameworkKey = generateFrameworkKey()
                    )
                    addGivenIfAbsentOrBetter(candidate.copy(type = typeWithFrameworkKey))
                    typeWithFrameworkKey
                }
                .toList()
                .takeIf { it.isNotEmpty() }
            val parentElements = parent?.setElementsForType(singleElementType, collectionElementType)
            if (parentElements != null && thisElements != null) parentElements + thisElements
            else thisElements ?: parentElements
        }
    }

    private fun frameworkSetElementsForType(
        singleElementType: TypeRef,
        collectionElementType: TypeRef
    ): List<TypeRef>? {
        if (abstractGivens.isEmpty()) return null
        return abstractGivens
            .toList()
            .asSequence()
            .filter {
                it.type.isAssignableTo(context, singleElementType) ||
                        it.type.isAssignableTo(context, collectionElementType)
            }
            .map { callable ->
                val typeWithFrameworkKey = callable.type.copy(
                    frameworkKey = generateFrameworkKey()
                )
                abstractGivens += callable.copy(type = typeWithFrameworkKey)
                typeWithFrameworkKey
            }
            .toList()
            .takeIf { it.isNotEmpty() }
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
        if (candidate.type in constrainedGiven.processedCandidateTypes)
            return
        constrainedGiven.processedCandidateTypes += candidate.type
        if (!candidate.rawType.isSubTypeOf(context, constrainedGiven.constraintType))
            return

        val inputsSubstitutionMap = getSubstitutionMap(
            context,
            listOf(candidate.type to constrainedGiven.constraintType)
        )
        // if we could not get all type arguments it must be an incompatible type
        if (constrainedGiven.callable.typeParameters.any { it !in inputsSubstitutionMap })
            return
        val outputsSubstitutionMap = getSubstitutionMap(
            context,
            listOf(candidate.rawType to constrainedGiven.constraintType)
        )
        if (constrainedGiven.callable.typeParameters.any { it !in outputsSubstitutionMap })
            return
        val newGivenType = constrainedGiven.callable.type.substitute(outputsSubstitutionMap)
        val newGiven = constrainedGiven.callable
            .substituteInputs(inputsSubstitutionMap)
            .let { tmp ->
                tmp.copy(
                    constrainedGivenSource = candidate.source,
                    typeArguments = constrainedGiven.callable
                        .typeArguments
                        .mapValues { it.value.substitute(inputsSubstitutionMap) },
                    notGivens = tmp.notGivens + candidate.notGivens
                        .map { it.substitute(outputsSubstitutionMap) },
                    type = newGivenType,
                    originalType = newGivenType
                )
            }

        newGiven.collectGivens(
            context = context,
            scope = this,
            trace = trace,
            addGiven = { newInnerGiven ->
                val finalNewInnerGiven = newInnerGiven
                    .copy(
                        constrainedGivenSource = candidate.source,
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
                    notGivens = finalNewInnerGiven.notGivens,
                    source = candidate.source
                )
                constrainedGivenCandidates += newCandidate
                collectConstrainedGivens(newCandidate)
            },
            addAbstractGiven = { newInnerAbstractGiven ->
                val finalNewInnerAbstractGiven = newInnerAbstractGiven
                    .copy(
                        constrainedGivenSource = candidate.source,
                        originalType = newInnerAbstractGiven.type
                    )
                abstractGivens += finalNewInnerAbstractGiven
                val newInnerGivenWithFrameworkKey = finalNewInnerAbstractGiven.copy(
                    type = finalNewInnerAbstractGiven.type.copy(
                        frameworkKey = generateFrameworkKey()
                            .also { constrainedGiven.resultingFrameworkKeys += it }
                    )
                )
                abstractGivens += newInnerGivenWithFrameworkKey
                val newCandidate = ConstrainedGivenCandidate(
                    type = newInnerGivenWithFrameworkKey.type,
                    rawType = finalNewInnerAbstractGiven.type,
                    notGivens = finalNewInnerAbstractGiven.notGivens,
                    source = candidate.source
                )
                constrainedGivenCandidates += newCandidate
                collectConstrainedGivens(newCandidate)
            },
            addConstrainedGiven = { newInnerConstrainedGiven ->
                val finalNewInnerConstrainedGiven = newInnerConstrainedGiven
                    .copy(
                        constrainedGivenSource = candidate.source,
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

fun HierarchicalResolutionScope(
    context: InjektContext,
    scope: HierarchicalScope,
    trace: BindingTrace
): ResolutionScope {
    trace[InjektWritableSlices.RESOLUTION_SCOPE_FOR_SCOPE, scope]?.let { return it }

    val allScopes = scope.parentsWithSelf.toList()

    val importScopes = allScopes
        .asSequence()
        .filterIsInstance<ImportingScope>()
        .filter { importScope ->
            if (importScope is LazyImportScope) {
                val scopeString = importScope.toString()
                "LazyImportScope: Explicit imports in LazyFileScope for file" in scopeString
                        || ("LazyImportScope: All under imports in LazyFileScope for file" in scopeString &&
                        !scopeString.endsWith("(invisible classes only)"))
            } else true
        }
        .toList()

    val importsResolutionScope = trace.get(InjektWritableSlices.IMPORT_RESOLUTION_SCOPE, importScopes)
        ?: ResolutionScope(
            name = "IMPORTS",
            context = context,
            callContext = CallContext.DEFAULT,
            parent = null,
            ownerDescriptor = null,
            trace = trace,
            initialGivens = importScopes
                .flatMap { it.collectGivens(context, trace) }
        ).also { trace.record(InjektWritableSlices.IMPORT_RESOLUTION_SCOPE, importScopes, it) }

    return allScopes
        .filter { it !in importScopes }
        .reversed()
        .asSequence()
        .filter {
            it is LexicalScope && (
                    (it.ownerDescriptor is ClassDescriptor &&
                            it.kind == LexicalScopeKind.CLASS_MEMBER_SCOPE) ||
                            (it.ownerDescriptor is FunctionDescriptor &&
                                    it.kind == LexicalScopeKind.FUNCTION_INNER_SCOPE) ||
                            it.kind == LexicalScopeKind.CODE_BLOCK ||
                            it.kind == LexicalScopeKind.CLASS_INITIALIZER
                    )
        }
        .fold(importsResolutionScope) { parent, next ->
            when {
                next is LexicalScope && next.ownerDescriptor is ClassDescriptor -> {
                    val clazz = next.ownerDescriptor as ClassDescriptor
                    val companionScope = clazz.companionObjectDescriptor
                        ?.let { companionDescriptor ->
                            trace.get(InjektWritableSlices.CLASS_RESOLUTION_SCOPE, companionDescriptor)
                                ?: ResolutionScope(
                                    name = "CLASS COMPANION ${clazz.fqNameSafe}",
                                    context = context,
                                    callContext = next.callContext(trace.bindingContext),
                                    parent = parent,
                                    ownerDescriptor = companionDescriptor,
                                    trace = trace,
                                    initialGivens = listOf(
                                        companionDescriptor
                                            .thisAsReceiverParameter
                                            .toCallableRef(context, trace)
                                            .makeGiven()
                                    )
                                ).also { trace.record(InjektWritableSlices.CLASS_RESOLUTION_SCOPE, companionDescriptor, it) }
                        }
                    trace.get(InjektWritableSlices.CLASS_RESOLUTION_SCOPE, clazz)
                        ?: ResolutionScope(
                            name = "CLASS ${clazz.fqNameSafe}",
                            context = context,
                            callContext = next.callContext(trace.bindingContext),
                            parent = companionScope ?: parent,
                            ownerDescriptor = clazz,
                            trace = trace,
                            initialGivens = listOf(
                                clazz.thisAsReceiverParameter.toCallableRef(context, trace)
                                    .makeGiven()
                            )
                        ).also { trace.record(InjektWritableSlices.CLASS_RESOLUTION_SCOPE, clazz, it) }
                }
                next is LexicalScope && next.ownerDescriptor is FunctionDescriptor &&
                next.kind == LexicalScopeKind.FUNCTION_INNER_SCOPE -> {
                    val function = next.ownerDescriptor as FunctionDescriptor
                    trace.get(InjektWritableSlices.FUNCTION_RESOLUTION_SCOPE, function)
                        ?: ResolutionScope(
                            name = "FUNCTION ${function.fqNameSafe}",
                            context = context,
                            callContext = function.callContext(trace.bindingContext),
                            parent = parent,
                            ownerDescriptor = function,
                            trace = trace,
                            initialGivens = function.allParameters
                                .asSequence()
                                .filter { it.isGiven(context, trace) || it === function.extensionReceiverParameter }
                                .map { it.toCallableRef(context, trace).makeGiven() }
                                .toList()
                        ).also { trace.record(InjektWritableSlices.FUNCTION_RESOLUTION_SCOPE, function, it) }
                }
                else -> {
                    trace.get(InjektWritableSlices.RESOLUTION_SCOPE_FOR_SCOPE, next)
                        ?: ResolutionScope(
                            name = "Hierarchical $next",
                            context = context,
                            callContext = next.callContext(trace.bindingContext),
                            parent = parent,
                            ownerDescriptor = next.parentsWithSelf
                                .firstIsInstance<LexicalScope>()
                                .ownerDescriptor,
                            trace = trace,
                            initialGivens = next.collectGivens(context, trace)
                        ).also { trace.record(InjektWritableSlices.RESOLUTION_SCOPE_FOR_SCOPE, next, it) }
                }
            }
        }
        .also { trace.record(InjektWritableSlices.RESOLUTION_SCOPE_FOR_SCOPE, scope, it) }
}
