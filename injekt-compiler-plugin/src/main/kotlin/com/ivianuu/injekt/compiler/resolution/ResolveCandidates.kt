package com.ivianuu.injekt.compiler.resolution

import org.jetbrains.kotlin.descriptors.CallableDescriptor

sealed class GivenGraph {
    data class Success(val givens: Map<GivenRequest, GivenNode>) : GivenGraph()
    data class Error(val failures: List<ResolutionResult.Failure>) : GivenGraph()
}

sealed class CandidateResolutionResult {
    abstract val request: GivenRequest
    abstract val candidate: GivenNode

    data class Success(
        override val request: GivenRequest,
        override val candidate: GivenNode,
        val dependencyResults: List<ResolutionResult.Success>,
    ) : CandidateResolutionResult()

    data class Failure(
        override val request: GivenRequest,
        override val candidate: GivenNode,
        val dependencyFailureResults: List<ResolutionResult.Failure>,
    ) : CandidateResolutionResult()
}

sealed class ResolutionResult {
    abstract val request: GivenRequest

    data class Success(
        override val request: GivenRequest,
        val candidateResult: CandidateResolutionResult.Success,
    ) : ResolutionResult()

    sealed class Failure : ResolutionResult() {
        abstract val failureOrdering: Int

        data class CandidateAmbiguity(
            override val request: GivenRequest,
            val candidateResults: List<CandidateResolutionResult.Success>,
        ) : Failure() {
            override val failureOrdering: Int
                get() = 0
        }

        data class CircularDependency(
            override val request: GivenRequest,
            val chain: List<GivenRequest>,
        ) : Failure() {
            override val failureOrdering: Int
                get() = 1
        }

        data class CandidateFailures(
            override val request: GivenRequest,
            val candidateResults: List<CandidateResolutionResult.Failure>,
        ) : Failure() {
            override val failureOrdering: Int
                get() = 2
        }

        data class NoCandidates(override val request: GivenRequest) : Failure() {
            override val failureOrdering: Int
                get() = 3
        }
    }
}

fun ResolutionScope.resolveGraph(requests: List<GivenRequest>): GivenGraph {
    val (successResults, failureResults) = requests
        .flatMap { resolveRequest(it, null) }
        .let {
            it
                .filterIsInstance<ResolutionResult.Success>() to
                    it.filterIsInstance<ResolutionResult.Failure>()
        }
    return if (failureResults.isEmpty()) {
        successResults.toSuccessGraph()
    } else GivenGraph.Error(failureResults)
}

private fun ResolutionScope.resolveRequest(
    request: GivenRequest,
    parentContext: ResolutionContext?,
): List<ResolutionResult> {
    var currentScope: ResolutionScope? = this
    val failureResults = mutableListOf<ResolutionResult.Failure>()
    while (currentScope != null) {
        val context = ResolutionContext(currentScope, parentContext)
        when (val result = context.resolveInScope(request,
            currentScope.givensForTypeInThisScope(request.type))) {
            is ResolutionResult.Success -> return listOf(result)
            is ResolutionResult.Failure -> failureResults += result
        }
        currentScope = currentScope.parent
    }

    val frameworkGivensResult = ResolutionContext(this, parentContext)
        .resolveInScope(request, getFrameworkCandidates(request))
    if (frameworkGivensResult is ResolutionResult.Success) return listOf(frameworkGivensResult)

    return if (failureResults.isNotEmpty()) {
        failureResults.sortedBy { it.failureOrdering }.distinct()
        //listOf(failureResults.minBy { it.failureOrdering }!!)
    } else listOf(ResolutionResult.Failure.NoCandidates(request))
}

private fun List<ResolutionResult.Success>.toSuccessGraph(): GivenGraph.Success {
    val givensByRequest = mutableMapOf<GivenRequest, GivenNode>()
    fun ResolutionResult.Success.visit() {
        givensByRequest[request] = candidateResult.candidate
        candidateResult.dependencyResults
            .forEach { it.visit() }
    }
    forEach { it.visit() }
    return GivenGraph.Success(givensByRequest)
}

class ResolutionContext(
    val originScope: ResolutionScope,
    parentContext: ResolutionContext? = null,
) {
    private val chain: MutableSet<GivenRequest> =
        parentContext?.chain?.toMutableSet() ?: mutableSetOf()
    private val resultsForRequest = mutableMapOf<GivenRequest, ResolutionResult>()

    fun computeForRequest(
        request: GivenRequest,
        compute: () -> ResolutionResult,
    ): ResolutionResult {
        if (request in chain) {
            val chainList = chain.toList()
            val cycleChain = chainList.subList(chainList.indexOf(request), chainList.size)
            return ResolutionResult.Failure.CircularDependency(request, cycleChain)
        }

        return resultsForRequest.getOrPut(request) {
            chain += request
            val result = compute()
            chain -= request
            result
        }
    }
}

private fun ResolutionContext.resolveInScope(
    request: GivenRequest,
    candidates: List<GivenNode>,
): ResolutionResult = computeForRequest(request) {
    if (candidates.isEmpty()) return@computeForRequest ResolutionResult.Failure.NoCandidates(request)
    if (candidates.size == 1) {
        val candidate = candidates.single()
        return@computeForRequest when (val candidateResult = resolveCandidate(request, candidate)) {
            is CandidateResolutionResult.Success ->
                ResolutionResult.Success(request, candidateResult)
            is CandidateResolutionResult.Failure ->
                ResolutionResult.Failure.CandidateFailures(request, listOf(candidateResult))
        }
    }

    val (successResults, failureResults) = candidates
        .map { resolveCandidate(request, it) }
        .let {
            it.filterIsInstance<CandidateResolutionResult.Success>() to
                    it.filterIsInstance<CandidateResolutionResult.Failure>()
        }

    return@computeForRequest when {
        successResults.size == 1 -> ResolutionResult.Success(request, successResults.single())
        successResults.isNotEmpty() -> ResolutionResult.Failure.CandidateAmbiguity(request,
            successResults)
        else -> ResolutionResult.Failure.CandidateFailures(request, failureResults)
    }
}

private fun ResolutionContext.resolveCandidate(
    request: GivenRequest,
    candidate: GivenNode,
): CandidateResolutionResult {
    val (successDependencyResults, failureDependencyResults) = candidate.dependencies
        .flatMap { originScope.resolveRequest(it, this) }
        .let {
            it
                .filterIsInstance<ResolutionResult.Success>() to it
                .filterIsInstance<ResolutionResult.Failure>()
        }
    return if (failureDependencyResults.isEmpty()) {
        CandidateResolutionResult.Success(request, candidate, successDependencyResults)
    } else {
        CandidateResolutionResult.Failure(request, candidate, failureDependencyResults)
    }
}

private fun ResolutionScope.getFrameworkCandidates(request: GivenRequest): List<GivenNode> {
    if (request.type.classifier.fqName.asString() == "kotlin.Function0") {
        return listOf(
            ProviderGivenNode(
                request.type,
                request.origin,
                request.required
            )
        )
    }

    val mapType = declarationStore.module.builtIns.map.defaultType.toTypeRef()
    val setType = declarationStore.module.builtIns.set.defaultType.toTypeRef()
    if (request.type.isSubTypeOf(mapType) || request.type.isSubTypeOf(setType)) {
        val elements = mutableListOf<CallableDescriptor>()
        var currentForElements: ResolutionScope? = this
        while (currentForElements != null) {
            val currentElements = currentForElements
                .givenCollectionElementsForTypeInThisScope(request.type)
            elements += currentElements
            currentForElements = currentForElements.parent
        }
        if (elements.isNotEmpty()) {
            return listOf(
                CollectionGivenNode(
                    request.type,
                    request.origin,
                    elements,
                    elements
                        .flatMap { element ->
                            element.getGivenRequests(request.type, declarationStore)
                        }
                )
            )
        }
    }

    return emptyList()
}
