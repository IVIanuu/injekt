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
        val source: CallableRef?
    )

    val allParents: List<ResolutionScope> = parent?.allScopes ?: emptyList()
    val allScopes: List<ResolutionScope> = allParents + this

    val typeContext = TypeContext(context, allScopes.flatMap { it.typeParameters })

    private val givensByType = mutableMapOf<RequestKey, List<GivenNode>?>()
    private val setElementsByType = mutableMapOf<RequestKey, List<TypeRef>?>()

    data class RequestKey(val type: TypeRef, val context: TypeContext)

    init {
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

    fun givensForRequest(request: GivenRequest, typeContext: TypeContext): List<GivenNode>? {
        // we return merged collections
        if (request.type.frameworkKey == null &&
            request.type.classifier == context.setClassifier) return null
        return givensForType(RequestKey(request.type, typeContext))
    }

    private fun givensForType(key: RequestKey): List<GivenNode>? {
        if (givens.isEmpty()) return parent?.givensForType(key)
        return givensByType.getOrPut(key) {
            val thisGivens = givens
                .asSequence()
                .filter {
                    it.value.type.frameworkKey == key.type.frameworkKey &&
                            it.value.type.isAssignableTo(key.context, key.type)
                }
                .map {
                    val finalCallable = it.value.substitute(getSubstitutionMap(key.context, it.value.type, key.type))
                    CallableGivenNode(
                        key.type,
                        finalCallable.getGivenRequests(context, trace),
                        this,
                        finalCallable
                    )
                }
                .toList()
                .takeIf { it.isNotEmpty() }
            val parentGivens = parent?.givensForType(key)
            if (parentGivens != null && thisGivens != null) parentGivens + thisGivens
            else thisGivens ?: parentGivens
        }
    }

    fun frameworkGivenForRequest(request: GivenRequest, typeContext: TypeContext): GivenNode? {
        if (request.type.frameworkKey != null ||
                request.type.qualifiers.isNotEmpty()) return null
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
                .typeWith(listOf(singleElementType))

            var elements = setElementsForType(singleElementType, collectionElementType,
                RequestKey(request.type, typeContext)
            )
            if (elements == null &&
                singleElementType.qualifiers.isEmpty() &&
                singleElementType.isFunctionTypeWithOnlyGivenParameters) {
                val providerReturnType = singleElementType.arguments.last()
                elements = setElementsForType(providerReturnType, context.collectionClassifier
                    .defaultType.typeWith(listOf(providerReturnType)),
                    RequestKey(providerReturnType, typeContext)
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
            val thisElements = givens
                .toList()
                .asSequence()
                .filter { (_, candidate) ->
                    ((candidate.type.frameworkKey == singleElementType.frameworkKey &&
                            candidate.type.isAssignableTo(key.context, singleElementType)) ||
                            (candidate.type.frameworkKey == collectionElementType.frameworkKey) &&
                            candidate.type.isAssignableTo(key.context, collectionElementType))
                }
                .map { (_, candidate) ->
                    candidate.substitute(
                        when {
                            candidate.type.isAssignableTo(key.context, singleElementType) ->
                                getSubstitutionMap(key.context, singleElementType, candidate.type)
                            candidate.type.isAssignableTo(key.context, collectionElementType) ->
                                getSubstitutionMap(key.context,
                                    collectionElementType, candidate.type.subtypeView(context.collectionClassifier)!!)
                            else -> throw AssertionError()
                        }
                    )
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
        if (!candidate.rawType.isSubTypeOf(typeContext, constrainedGiven.constraintType)) return

        val inputsSubstitutionMap = getSubstitutionMap(
            typeContext,
            candidate.type,
            constrainedGiven.constraintType
        )
        // if we could not get all type arguments it must be an incompatible type
        if (constrainedGiven.callable.typeParameters.any {
                constrainedGiven.callable.typeArguments[it] == it.defaultType &&
                        it !in inputsSubstitutionMap
        }) return
        val outputsSubstitutionMap = getSubstitutionMap(
            typeContext,
            candidate.rawType,
            constrainedGiven.constraintType
        )
        if (constrainedGiven.callable.typeParameters.any {
                constrainedGiven.callable.typeArguments[it] == it.defaultType &&
                        it !in outputsSubstitutionMap
        }) return
        val newGivenType = constrainedGiven.callable.type.substitute(outputsSubstitutionMap)
        val newGiven = constrainedGiven.callable.substituteInputs(inputsSubstitutionMap)
            .copy(
                source = candidate.source,
                typeArguments = constrainedGiven.callable
                    .typeArguments
                    .mapValues { it.value.substitute(inputsSubstitutionMap) },
                type = newGivenType,
                originalType = newGivenType
            )

        newGiven.collectGivens(
            context = context,
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

fun HierarchicalResolutionScope(
    context: InjektContext,
    scope: HierarchicalScope,
    trace: BindingTrace
): ResolutionScope {
    val finalScope = scope.takeSnapshot()
    trace[InjektWritableSlices.RESOLUTION_SCOPE_FOR_SCOPE, finalScope]?.let { return it }

    val allScopes = finalScope.parentsWithSelf.toList()

    val file = allScopes
        .filterIsInstance<LexicalScope>()
        .first()
        .ownerDescriptor
        .findPsi()!!
        .cast<KtElement>()
        .containingKtFile

    val fileImports = (file.getGivenImports() + GivenImport(null, "${file.packageFqName}.*"))
        .sortedBy { it.importPath }

    val importsResolutionScope = trace.get(InjektWritableSlices.IMPORT_RESOLUTION_SCOPE, fileImports)
        ?: run {
            fileImports
                .toImportResolutionScope("FILE", null, context, trace)
                .also { trace.record(InjektWritableSlices.IMPORT_RESOLUTION_SCOPE, fileImports, it) }
        }

    return allScopes
        .filter { it !is ImportingScope }
        .reversed()
        .asSequence()
        .filter {
            it is LexicalScope && (
                    (it.ownerDescriptor is ClassDescriptor &&
                            it.kind == LexicalScopeKind.CLASS_MEMBER_SCOPE) ||
                            (it.ownerDescriptor is FunctionDescriptor &&
                                    it.kind == LexicalScopeKind.FUNCTION_INNER_SCOPE) ||
                            (it.ownerDescriptor is PropertyDescriptor &&
                                    it.kind == LexicalScopeKind.PROPERTY_INITIALIZER_OR_DELEGATE) ||
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
                                ?: run {
                                    val finalParent = companionDescriptor
                                        .findPsi()
                                        .safeAs<KtClassOrObject>()
                                        ?.getGivenImports()
                                        ?.takeIf { it.isNotEmpty() }
                                        ?.toImportResolutionScope("CLASS COMPANION ${clazz.fqNameSafe}", parent, context, trace)
                                        ?: parent
                                    ResolutionScope(
                                        name = "CLASS COMPANION ${clazz.fqNameSafe}",
                                        context = context,
                                        callContext = next.callContext(trace.bindingContext),
                                        parent = finalParent,
                                        ownerDescriptor = companionDescriptor,
                                        trace = trace,
                                        initialGivens = listOf(
                                            companionDescriptor
                                                .thisAsReceiverParameter
                                                .toCallableRef(context, trace)
                                                .makeGiven()
                                        ),
                                        imports = emptyList(),
                                        typeParameters = emptyList()
                                    ).also { trace.record(InjektWritableSlices.CLASS_RESOLUTION_SCOPE, companionDescriptor, it) }
                                }
                        }
                    val finalParent = clazz
                        .findPsi()
                        .safeAs<KtClassOrObject>()
                        ?.getGivenImports()
                        ?.takeIf { it.isNotEmpty() }
                        ?.toImportResolutionScope("CLASS ${clazz.fqNameSafe}",
                            companionScope ?: parent, context, trace)
                        ?: companionScope ?: parent
                    trace.get(InjektWritableSlices.CLASS_RESOLUTION_SCOPE, clazz)
                        ?: ResolutionScope(
                            name = "CLASS ${clazz.fqNameSafe}",
                            context = context,
                            callContext = next.callContext(trace.bindingContext),
                            parent = finalParent,
                            ownerDescriptor = clazz,
                            trace = trace,
                            initialGivens = listOf(
                                clazz.thisAsReceiverParameter.toCallableRef(context, trace)
                                    .makeGiven()
                            ),
                            imports = emptyList(),
                            typeParameters = clazz.declaredTypeParameters.map { it.toClassifierRef(context, trace) }
                        ).also { trace.record(InjektWritableSlices.CLASS_RESOLUTION_SCOPE, clazz, it) }
                }
                next is LexicalScope && next.ownerDescriptor is FunctionDescriptor &&
                next.kind == LexicalScopeKind.FUNCTION_INNER_SCOPE -> {
                    val function = next.ownerDescriptor as FunctionDescriptor
                    trace.get(InjektWritableSlices.FUNCTION_RESOLUTION_SCOPE, function)
                        ?: run {
                            val finalParent = function
                                .findPsi()
                                .safeAs<KtFunction>()
                                ?.getGivenImports()
                                ?.takeIf { it.isNotEmpty() }
                                ?.toImportResolutionScope("FUNCTION ${function.fqNameSafe}", parent, context, trace)
                                ?: parent
                            ResolutionScope(
                                name = "FUNCTION ${function.fqNameSafe}",
                                context = context,
                                callContext = function.callContext(trace.bindingContext),
                                parent = finalParent,
                                ownerDescriptor = function,
                                trace = trace,
                                initialGivens = function.allParameters
                                    .asSequence()
                                    .filter { it.isGiven(context, trace) || it === function.extensionReceiverParameter }
                                    .map { it.toCallableRef(context, trace).makeGiven() }
                                    .toList(),
                                imports = emptyList(),
                                typeParameters = function.typeParameters.map { it.toClassifierRef(context, trace) }
                            ).also { trace.record(InjektWritableSlices.FUNCTION_RESOLUTION_SCOPE, function, it) }
                        }
                }
                next is LexicalScope && next.ownerDescriptor is PropertyDescriptor -> {
                    val property = next.ownerDescriptor as PropertyDescriptor
                    trace.get(InjektWritableSlices.PROPERTY_RESOLUTION_SCOPE, property)
                        ?: run {
                            val finalParent = property
                                .findPsi()
                                .safeAs<KtProperty>()
                                ?.getGivenImports()
                                ?.takeIf { it.isNotEmpty() }
                                ?.toImportResolutionScope("PROPERTY ${property.fqNameSafe}", parent, context, trace)
                                ?: parent
                            ResolutionScope(
                                name = "Hierarchical $next",
                                context = context,
                                callContext = next.callContext(trace.bindingContext),
                                parent = finalParent,
                                ownerDescriptor = property,
                                trace = trace,
                                initialGivens = next.collectGivens(context, trace),
                                imports = emptyList(),
                                typeParameters = property.typeParameters.map { it.toClassifierRef(context, trace) }
                            ).also { trace.record(InjektWritableSlices.PROPERTY_RESOLUTION_SCOPE, property, it) }
                        }
                }
                else -> {
                    val ownerDescriptor = next.parentsWithSelf
                        .firstIsInstance<LexicalScope>()
                        .ownerDescriptor
                    val finalParent = ownerDescriptor
                        .safeAs<AnonymousFunctionDescriptor>()
                        ?.findPsi()
                        ?.getParentOfType<KtCallExpression>(false)
                        ?.getResolvedCall(trace.bindingContext)
                        ?.valueArguments
                        ?.values
                        ?.firstOrNull()
                        ?.safeAs<VarargValueArgument>()
                        ?.arguments
                        ?.map { it.toGivenImport() }
                        ?.takeIf { it.isNotEmpty() }
                        ?.toImportResolutionScope("BLOCK", parent, context, trace)
                        ?: parent
                    trace.get(InjektWritableSlices.RESOLUTION_SCOPE_FOR_SCOPE, next)
                        ?: ResolutionScope(
                            name = "Hierarchical $next",
                            context = context,
                            callContext = next.callContext(trace.bindingContext),
                            parent = finalParent,
                            ownerDescriptor = ownerDescriptor,
                            trace = trace,
                            initialGivens = next.collectGivens(context, trace),
                            imports = emptyList(),
                            typeParameters = emptyList()
                        )//.also { trace.record(InjektWritableSlices.RESOLUTION_SCOPE_FOR_SCOPE, next, it) }
                }
            }
        }
        .also { trace.record(InjektWritableSlices.RESOLUTION_SCOPE_FOR_SCOPE, finalScope, it) }
}

private fun List<GivenImport>.toImportResolutionScope(
    namePrefix: String,
    parent: ResolutionScope?,
    context: InjektContext,
    trace: BindingTrace
): ResolutionScope {
    val (externalImportedGivens, internalImportedGivens) = this
        .collectImportGivens(context, trace)
        .partition { it.callable.isExternalDeclaration(context) }
    return ResolutionScope(
        name = "$namePrefix INTERNAL IMPORTS",
        context = context,
        callContext = CallContext.DEFAULT,
        parent = ResolutionScope(
            name = "$namePrefix EXTERNAL IMPORTS",
            context = context,
            callContext = CallContext.DEFAULT,
            parent = parent,
            ownerDescriptor = null,
            trace = trace,
            initialGivens = externalImportedGivens,
            imports = emptyList(),
            typeParameters = emptyList()
        ),
        ownerDescriptor = null,
        trace = trace,
        initialGivens = internalImportedGivens,
        imports = this,
        typeParameters = emptyList()
    )
}
