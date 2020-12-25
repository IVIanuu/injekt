package com.ivianuu.injekt.compiler.resolution

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

sealed class GivenGraph {
    abstract val requests: List<GivenRequest>

    data class Success(
        override val requests: List<GivenRequest>,
        val scope: ResolutionScope,
        val givens: Map<GivenRequest, GivenNode>,
    ) : GivenGraph()

    data class Error(
        val scope: ResolutionScope,
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

        data class CallContextMismatch(
            override val request: GivenRequest,
            val actualCallContext: CallContext,
            val candidate: GivenNode,
        ) : Failure() {
            override val failureOrdering: Int
                get() = 1
        }

        data class DivergentGiven(override val request: GivenRequest) : Failure() {
            override val failureOrdering: Int
                get() = 1
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
                get() = 1
        }

        data class NoCandidates(override val request: GivenRequest) : Failure() {
            override val failureOrdering: Int
                get() = 2
        }
    }
}

fun ResolutionScope.resolveGiven(requests: List<GivenRequest>): GivenGraph {
    val (successResults, failureResults) = requests
        .map { resolveRequest(it) }
        .let {
            it
                .filterIsInstance<ResolutionResult.Success>() to
                    it.filterIsInstance<ResolutionResult.Failure>()
        }
    return if (failureResults.isEmpty()) {
        successResults.toSuccessGraph(this, requests)
    } else failureResults.toErrorGraph(this, requests)
}

private fun ResolutionScope.resolveRequest(request: GivenRequest): ResolutionResult {
    resultsByRequest[request]?.let { return it }
    val result = resolveCandidates(
        request,
        givensForType(request.type)
    )
    resultsByRequest[request] = result
    return result
}

private fun List<ResolutionResult.Success>.toSuccessGraph(
    scope: ResolutionScope,
    requests: List<GivenRequest>,
): GivenGraph.Success {
    val givensByType = mutableMapOf<GivenRequest, GivenNode>()
    fun ResolutionResult.Success.visit() {
        givensByType[request] = candidateResult.candidate
        candidateResult.dependencyResults
            .forEach { it.visit() }
    }
    forEach { it.visit() }
    return GivenGraph.Success(requests, scope, givensByType)
}

private fun List<ResolutionResult.Failure>.toErrorGraph(
    scope: ResolutionScope,
    requests: List<GivenRequest>,
): GivenGraph.Error {
    val failuresByRequest = mutableMapOf<GivenRequest, MutableList<ResolutionResult.Failure>>()
    fun ResolutionResult.Failure.visit() {
        failuresByRequest.getOrPut(request) { mutableListOf() } += this
    }
    forEach { it.visit() }
    return GivenGraph.Error(scope, requests, failuresByRequest)
}

private fun ResolutionScope.computeForCandidate(
    request: GivenRequest,
    candidate: GivenNode,
    compute: () -> CandidateResolutionResult,
): CandidateResolutionResult {
    resultsByCandidate[candidate]?.let { return it }
    chain.reversed().forEach { prev ->
        if (prev.callableFqName == candidate.callableFqName &&
            prev.type.coveringSet == candidate.type.coveringSet &&
            candidate.type.typeSize > prev.type.typeSize
        ) {
            return CandidateResolutionResult.Failure(
                request,
                candidate,
                ResolutionResult.Failure.DivergentGiven(request)
            )
        }
    }

    if (candidate in chain) {
        val chainList = chain.toList()
        val cycleChain = chainList.subList(chainList.indexOf(candidate), chainList.size)
        return CandidateResolutionResult.Failure(
            request,
            candidate,
            ResolutionResult.Failure.CircularDependency(request, emptyList()) // todo
        )
    }
    chain += candidate
    val result = compute()
    resultsByCandidate[candidate] = result
    chain -= candidate
    return result
}

private fun ResolutionScope.resolveCandidates(
    request: GivenRequest,
    candidates: List<GivenNode>,
): ResolutionResult {
    if (candidates.isEmpty()) {
        return if (request.required) ResolutionResult.Failure.NoCandidates(request)
        else ResolutionResult.Success(request, CandidateResolutionResult.Success(
            request, DefaultGivenNode(request.type, this), emptyList()
        ))
    }
    if (candidates.size == 1) {
        val candidate = candidates.single()
        return when (val candidateResult = resolveCandidate(request, candidate)) {
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

    return if (successResults.isNotEmpty()) {
        successResults
            .disambiguate(this)
            .let { finalResults ->
                finalResults.singleOrNull()?.let {
                    ResolutionResult.Success(request, it)
                } ?: ResolutionResult.Failure.CandidateAmbiguity(request, finalResults)
            }
    } else {
        ResolutionResult.Failure.CandidateFailures(request, failureResults.first())
    }
}

private fun ResolutionScope.resolveCandidate(
    request: GivenRequest,
    candidate: GivenNode,
): CandidateResolutionResult = computeForCandidate(request, candidate) {
    if (!callContext.canCall(candidate.callContext)) {
        return@computeForCandidate CandidateResolutionResult.Failure(
            request,
            candidate,
            ResolutionResult.Failure.CallContextMismatch(request, callContext, candidate)
        )
    }

    val successDependencyResults = mutableListOf<ResolutionResult.Success>()
    val dependencyScope = candidate.dependencyScope ?: this
    for (dependency in candidate.dependencies) {
        when (val result = dependencyScope.resolveRequest(dependency)) {
            is ResolutionResult.Success -> successDependencyResults += result
            is ResolutionResult.Failure -> return@computeForCandidate CandidateResolutionResult.Failure(
                dependency,
                candidate,
                result
            )
        }
    }
    for (interceptor in candidate.interceptors) {
        for (dependency in interceptor.dependencies) {
            when (val result = resolveRequest(dependency)) {
                is ResolutionResult.Success -> successDependencyResults += result
                is ResolutionResult.Failure -> return@computeForCandidate CandidateResolutionResult.Failure(
                    dependency,
                    candidate,
                    result
                )
            }
        }
    }
    return@computeForCandidate CandidateResolutionResult.Success(
        request,
        candidate,
        successDependencyResults
    )
}

private fun GivenNode.depth(scope: ResolutionScope): Int {
    var currentScope: ResolutionScope? = scope
    var depth = 0
    while (currentScope != null && currentScope != ownerScope) {
        depth++
        currentScope = currentScope.parent
    }
    if (currentScope == null) {
        currentScope = ownerScope
        depth = 0
        while (currentScope != null && currentScope != scope) {
            depth--
            currentScope = currentScope.parent
        }
    }
    return depth
}

private fun ResolutionScope.compareCandidate(
    a: CandidateResolutionResult.Success?,
    b: CandidateResolutionResult.Success?,
): Int {
    if (a != null && b == null) return -1
    if (b != null && a == null) return 1
    if (a == null && b == null) return 0
    a!!
    b!!

    if (!a.candidate.isFrameworkGiven &&
        b.candidate.isFrameworkGiven) return -1
    if (!b.candidate.isFrameworkGiven &&
        a.candidate.isFrameworkGiven) return 1

    val depthA = a.candidate.depth(this)
    val depthB = b.candidate.depth(this)
    if (depthA < depthB) return -1
    if (depthB < depthA) return 1

    if (a.dependencyResults.size < b.dependencyResults.size) return -1
    if (b.dependencyResults.size < a.dependencyResults.size) return 1

    var diff = compareType(a.candidate.originalType, b.candidate.originalType)
    if (diff < 0) return -1
    if (diff > 0) return 1

    for (aDependency in a.dependencyResults) {
        for (bDependency in b.dependencyResults) {
            diff += compareCandidate(aDependency.candidateResult, bDependency.candidateResult)
        }
    }
    return when {
        diff < 0 -> -1
        diff > 0 -> 1
        else -> 0
    }
}

private fun compareType(a: TypeRef, b: TypeRef): Int {
    if (!a.classifier.isTypeParameter && b.classifier.isTypeParameter) return -1
    if (a.classifier.isTypeParameter && !b.classifier.isTypeParameter) return 1

    if (!a.classifier.isTypeAlias && b.classifier.isTypeAlias) return -1
    if (a.classifier.isTypeAlias && !b.classifier.isTypeAlias) return 1

    if (a.typeArguments.size < b.typeArguments.size) return -1
    if (b.typeArguments.size < a.typeArguments.size) return 1

    if (a.classifier != b.classifier) return 0

    var diff = 0
    for (i in a.typeArguments.indices) {
        val aTypeArgument = a.typeArguments[i]
        val bTypeArgument = b.typeArguments[i]
        diff += compareType(aTypeArgument, bTypeArgument)
    }

    return when {
        diff < 0 -> -1
        diff > 0 -> 1
        else -> 0
    }
}

private fun List<CandidateResolutionResult.Success>.disambiguate(
    scope: ResolutionScope
): List<CandidateResolutionResult.Success> {
    if (size <= 1) return this
    val results = mutableListOf<CandidateResolutionResult.Success>()
    forEach { result ->
        when (scope.compareCandidate(results.lastOrNull(), result)) {
            1 -> {
                results.clear()
                results += result
            }
            0 -> results += result
        }
    }
    return results
}
