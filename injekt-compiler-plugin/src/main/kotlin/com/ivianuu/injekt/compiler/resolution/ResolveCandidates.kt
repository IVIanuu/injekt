package com.ivianuu.injekt.compiler.resolution

import org.jetbrains.kotlin.descriptors.CallableDescriptor

sealed class GivenGraph {
    abstract val requests: List<GivenRequest>

    data class Success(
        override val requests: List<GivenRequest>,
        val givens: Map<GivenRequest, GivenNode>,
    ) : GivenGraph()

    data class Error(
        override val requests: List<GivenRequest>,
        val failures: Map<GivenRequest, List<ResolutionResult.Failure>>,
    ) : GivenGraph()
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
        val failure: ResolutionResult.Failure,
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
            val candidateFailure: CandidateResolutionResult.Failure,
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

fun ResolutionScope.resolveGiven(requests: List<GivenRequest>): GivenGraph {
    val context = ResolutionContext(this)
    val (successResults, failureResults) = requests
        .map { resolveRequest(context, it) }
        .let {
            it
                .filterIsInstance<ResolutionResult.Success>() to
                    it.filterIsInstance<ResolutionResult.Failure>()
        }
    return if (failureResults.isEmpty()) {
        successResults.toSuccessGraph(requests)
    } else failureResults.toErrorGraph(requests)
}

private fun ResolutionScope.resolveRequest(
    context: ResolutionContext,
    request: GivenRequest,
): ResolutionResult {
    var currentScope: ResolutionScope? = this
    val failureResults = mutableListOf<ResolutionResult.Failure>()
    while (currentScope != null) {
        when (val result =
            context.resolveInScope(request, currentScope.givensForTypeInThisScope(request.type))) {
            is ResolutionResult.Success -> return result
            is ResolutionResult.Failure -> failureResults += result
        }
        currentScope = currentScope.parent
    }

    val frameworkGivensResult = context.resolveInScope(request, getFrameworkCandidates(request))
    if (frameworkGivensResult is ResolutionResult.Success) return frameworkGivensResult

    return if (failureResults.isNotEmpty()) {
        failureResults.minBy { it.failureOrdering }!!
    } else ResolutionResult.Failure.NoCandidates(request)
}

private fun List<ResolutionResult.Success>.toSuccessGraph(
    requests: List<GivenRequest>,
): GivenGraph.Success {
    val givensByRequest = mutableMapOf<GivenRequest, GivenNode>()
    fun ResolutionResult.Success.visit() {
        givensByRequest[request] = candidateResult.candidate
        candidateResult.dependencyResults
            .forEach { it.visit() }
    }
    forEach { it.visit() }
    return GivenGraph.Success(requests, givensByRequest)
}

private fun List<ResolutionResult.Failure>.toErrorGraph(
    requests: List<GivenRequest>,
): GivenGraph.Error {
    val failuresByRequest = mutableMapOf<GivenRequest, MutableList<ResolutionResult.Failure>>()
    fun ResolutionResult.Failure.visit() {
        failuresByRequest.getOrPut(request) { mutableListOf() } += this
    }
    forEach { it.visit() }
    return GivenGraph.Error(requests, failuresByRequest)
}

class ResolutionContext(val originScope: ResolutionScope) {
    private val chain: MutableSet<GivenRequest> = mutableSetOf()
    private val successResultsForRequest = mutableMapOf<GivenRequest, ResolutionResult.Success>()

    fun computeForRequest(
        request: GivenRequest,
        compute: () -> ResolutionResult,
    ): ResolutionResult {
        successResultsForRequest[request]?.let { return it }

        if (request in chain) {
            val chainList = chain.toList()
            val cycleChain = chainList.subList(chainList.indexOf(request), chainList.size)
            return ResolutionResult.Failure.CircularDependency(request, cycleChain)
        }

        chain += request
        val result = compute()
        if (result is ResolutionResult.Success) successResultsForRequest[request] = result
        chain -= request
        return result
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
                ResolutionResult.Failure.CandidateFailures(request, candidateResult)
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
        successResults.size > 1 -> {
            // todo try to pick most specific
            ResolutionResult.Failure.CandidateAmbiguity(request,
                successResults)
        }
        else -> ResolutionResult.Failure.CandidateFailures(request, failureResults.first())
    }
}

private fun ResolutionContext.resolveCandidate(
    request: GivenRequest,
    candidate: GivenNode,
): CandidateResolutionResult {
    val successDependencyResults = mutableListOf<ResolutionResult.Success>()
    for (dependency in candidate.dependencies) {
        when (val result = originScope.resolveRequest(this, dependency)) {
            is ResolutionResult.Success -> successDependencyResults += result
            is ResolutionResult.Failure -> return CandidateResolutionResult.Failure(request,
                candidate,
                result)
        }
    }
    return CandidateResolutionResult.Success(request, candidate, successDependencyResults)
}

private fun ResolutionScope.getFrameworkCandidates(request: GivenRequest): List<GivenNode> {
    if (request.type.classifier.fqName.asString() == "kotlin.Function0") {
        return listOf(
            ProviderGivenNode(
                request.type,
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
