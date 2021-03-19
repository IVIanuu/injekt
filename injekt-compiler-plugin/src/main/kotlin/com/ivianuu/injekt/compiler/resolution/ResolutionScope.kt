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
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
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
    val chain: MutableList<GivenNode> = parent?.chain ?: mutableListOf()
    val resultsByType = mutableMapOf<TypeRef, ResolutionResult>()
    val resultsByCandidate = mutableMapOf<GivenNode, ResolutionResult>()

    private val givens = mutableListOf<CallableRef>()

    private val constrainedGivens = mutableListOf<ConstrainedGivenNode>()
    private val constrainedGivenCandidates = mutableListOf<ConstrainedGivenCandidate>()

    private data class ConstrainedGivenNode(val callable: CallableRef) {
        val processedCandidateTypes = mutableSetOf<TypeRef>()
        val resultingFrameworkKeys = mutableSetOf<String>()
        fun copy() = ConstrainedGivenNode(callable).also {
            it.processedCandidateTypes += processedCandidateTypes
        }
    }
    private data class ConstrainedGivenCandidate(
        val type: TypeRef,
        val rawType: TypeRef
    )

    val allParents: List<ResolutionScope> = parent?.allScopes ?: emptyList()
    val allScopes: List<ResolutionScope> = allParents + this

    private val givensByType = mutableMapOf<TypeRef, List<GivenNode>>()
    private val setElementsByType = mutableMapOf<TypeRef, List<TypeRef>>()

    init {
        if (parent != null) {
            constrainedGivens += parent.constrainedGivens
                .map { it.copy() }
            constrainedGivenCandidates += parent.constrainedGivenCandidates
        }

        var hasGivens = false

        initialGivens
            .forEach { given ->
                given.collectGivens(
                    context = context,
                    scope = this,
                    substitutionMap = emptyMap(),
                    trace = trace,
                    addGiven = { callable ->
                        hasGivens = true
                        givens += callable
                        val typeWithFrameworkKey = callable.type
                            .copy(frameworkKey = generateFrameworkKey())
                        givens += callable.copy(type = typeWithFrameworkKey)
                        constrainedGivenCandidates += ConstrainedGivenCandidate(
                            type = typeWithFrameworkKey,
                            rawType = callable.type
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
        givens.forEach { recordLookup(it.callable) }
        constrainedGivens.forEach { recordLookup(it.callable.callable) }
    }

    fun givensForType(type: TypeRef): List<GivenNode> = givensByType.getOrPut(type) {
        buildList<GivenNode> {
            parent?.givensForType(type)
                ?.filterNot { it.isFrameworkGiven }
                ?.let { this += it }
            this += givens
                .filter {
                    it.type.frameworkKey == type.frameworkKey
                            && it.type.isAssignableTo(context, type)
                }
                .filter { it.isApplicable() }
                .map { it.toGivenNode(type, this@ResolutionScope) }

            if (type.qualifiers.isEmpty() &&
                type.frameworkKey == null) {
                if (type.isFunctionType &&
                    type.arguments.dropLast(1).all { it.isGiven }) {
                    this += ProviderGivenNode(
                        type = type,
                        ownerScope = this@ResolutionScope,
                        context = context
                    )
                } else if (type.classifier == context.setType.classifier) {
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

                    if (elementTypes.isNotEmpty()) {
                        val elements = elementTypes
                            .mapIndexed { index, element ->
                                GivenRequest(
                                    type = element,
                                    isRequired = true,
                                    callableFqName = FqName("com.ivianuu.injekt.givenSetOf"),
                                    parameterName = "element$index".asNameId(),
                                    isInline = false
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
        val parentSetElements = parent?.setElementsForType(type) ?: emptyList()
        if (givens.isEmpty()) return parentSetElements
        return setElementsByType.getOrPut(type) {
            parentSetElements + givens
                .filter { it.type.frameworkKey == type.frameworkKey }
                .filter { it.type.isAssignableTo(context, type) }
                .map { it.substitute(getSubstitutionMap(context, listOf(type to it.type))) }
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
        if (candidate.type.frameworkKey in constrainedGiven.resultingFrameworkKeys) return
        constrainedGiven.processedCandidateTypes += candidate.type

        val constraintType = constrainedGiven.callable.typeParameters.single {
            it.isGivenConstraint
        }.defaultType
        if (!candidate.rawType
                .isSubTypeOf(context, constraintType)) return

        val inputsSubstitutionMap = getSubstitutionMap(
            context,
            listOf(candidate.type to constraintType)
        )
        val outputsSubstitutionMap = getSubstitutionMap(
            context,
            listOf(candidate.rawType to constraintType)
        )
        check(inputsSubstitutionMap.size == constrainedGiven.callable.typeParameters.size) {
            "Corrupt substitution map $inputsSubstitutionMap for $constrainedGiven with candidate $candidate"
        }
        check(outputsSubstitutionMap.size == constrainedGiven.callable.typeParameters.size) {
            "Corrupt substitution map $outputsSubstitutionMap for $constrainedGiven with candidate $candidate"
        }
        val newGiven = constrainedGiven.callable.substituteInputs(inputsSubstitutionMap)
            .copy(
                fromGivenConstraint = true,
                typeArguments = constrainedGiven.callable
                    .typeArguments
                    .mapValues { it.value.substitute(inputsSubstitutionMap) },
                type = constrainedGiven.callable.type.substitute(outputsSubstitutionMap)
            )

        newGiven.collectGivens(
            context = context,
            scope = this,
            substitutionMap = outputsSubstitutionMap,
            trace = trace,
            addGiven = { newInnerGiven ->
                val finalNewInnerGiven = newInnerGiven
                    .copy(fromGivenConstraint = true)
                givens += finalNewInnerGiven
                val newInnerGivenWithFrameworkKey = finalNewInnerGiven.copy(
                    type = finalNewInnerGiven.type.copy(
                        frameworkKey = generateFrameworkKey()
                            .also { constrainedGiven.resultingFrameworkKeys += it }
                    )
                )
                givens += newInnerGivenWithFrameworkKey
                val newCandidate = ConstrainedGivenCandidate(
                    type = newInnerGivenWithFrameworkKey.type,
                    rawType = finalNewInnerGiven.type
                )
                constrainedGivenCandidates += newCandidate
                collectConstrainedGivens(newCandidate)
            },
            addConstrainedGiven = { newInnerConstrainedGiven ->
                val finalNewInnerConstrainedGiven = newInnerConstrainedGiven
                    .copy(fromGivenConstraint = true)
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
     * of a class which itself is given but not in the scope.
     * Without removing the property this would result in a divergent request
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
                .flatMap { it.collectGivens(context, trace, null, emptyMap()) }
        ).also { trace.record(InjektWritableSlices.IMPORT_RESOLUTION_SCOPE, importScopes, it) }

    return allScopes
        .filter { it !in importScopes }
        .reversed()
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
                            callContext = function.callContext,
                            parent = parent,
                            ownerDescriptor = function,
                            trace = trace,
                            initialGivens = function.allParameters
                                .filter { it.isGiven(context, trace) || it === function.extensionReceiverParameter }
                                .map { it.toCallableRef(context, trace).makeGiven() }
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
                            initialGivens = next.collectGivens(context, trace, null, emptyMap())
                        ).also { trace.record(InjektWritableSlices.RESOLUTION_SCOPE_FOR_SCOPE, next, it) }
                }
            }
        }
        .also { trace.record(InjektWritableSlices.RESOLUTION_SCOPE_FOR_SCOPE, scope, it) }
}
