package com.ivianuu.injekt.compiler.resolution

sealed class GivenGraph {
    data class Success(
        val scope: ResolutionScope,
        val givens: Map<GivenRequest, GivenNode>,
    ) : GivenGraph()

    data class Error(
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

        data class DivergentGiven(
            override val request: GivenRequest,
            val chain: List<GivenRequest>
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
        successResults.toSuccessGraph(this)
    } else failureResults.toErrorGraph()
}

private fun ResolutionScope.resolveRequest(request: GivenRequest): ResolutionResult {
    resultsByRequest[request]?.let { return it }
    val result = resolveCandidates(
        request,
        givensForType(request.type)
    ).fallbackToDefaultIfNeeded(this)
    resultsByRequest[request] = result
    return result
}

private fun List<ResolutionResult.Success>.toSuccessGraph(scope: ResolutionScope): GivenGraph.Success {
    val givensByType = mutableMapOf<GivenRequest, GivenNode>()
    fun ResolutionResult.Success.visit() {
        givensByType[request] = candidateResult.candidate
        candidateResult.dependencyResults
            .forEach { it.visit() }
    }
    forEach { it.visit() }
    return GivenGraph.Success(scope, givensByType)
}

private fun List<ResolutionResult.Failure>.toErrorGraph(): GivenGraph.Error {
    val failuresByRequest = mutableMapOf<GivenRequest, MutableList<ResolutionResult.Failure>>()
    fun ResolutionResult.Failure.visit() {
        failuresByRequest.getOrPut(request) { mutableListOf() } += this
    }
    forEach { it.visit() }
    return GivenGraph.Error(failuresByRequest)
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
            (prev.type.typeSize < candidate.type.typeSize ||
                    prev.type == candidate.type)
        ) {
            return CandidateResolutionResult.Failure(
                request,
                candidate,
                ResolutionResult.Failure.DivergentGiven(request, emptyList()) // todo
            )
        }
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
    if (candidates.isEmpty()) return ResolutionResult.Failure.NoCandidates(request)

    if (candidates.size == 1) {
        val candidate = candidates.single()
        return when (val candidateResult = resolveCandidate(request, candidate)) {
            is CandidateResolutionResult.Success ->
                ResolutionResult.Success(request, candidateResult)
            is CandidateResolutionResult.Failure ->
                ResolutionResult.Failure.CandidateFailures(request, candidateResult)
        }
    }

    val successes = mutableListOf<CandidateResolutionResult.Success>()
    var failure: CandidateResolutionResult.Failure? = null
    val remaining = candidates
        .sortedWith { a, b -> compareCandidate(a, b) }
        .toMutableList()
    while (remaining.isNotEmpty()) {
        val candidate = remaining.removeAt(0)
        if (compareCandidate(successes.firstOrNull()?.candidate, candidate) < 0) {
            // we cannot get a better result
            break
        }

        when (val candidateResult = resolveCandidate(request, candidate)) {
            is CandidateResolutionResult.Success -> {
                val firstSuccessResult = successes.firstOrNull()
                when (compareResult(candidateResult, firstSuccessResult)) {
                    -1 -> {
                        successes.clear()
                        successes += candidateResult
                    }
                    0 -> successes += candidateResult
                }
            }
            is CandidateResolutionResult.Failure -> {
                if (compareResult(candidateResult, failure) < 0)
                    failure = candidateResult
            }
        }
    }

    return if (successes.isNotEmpty()) {
        successes.singleOrNull()?.let {
            ResolutionResult.Success(request, it)
        } ?: ResolutionResult.Failure.CandidateAmbiguity(request, successes)
    } else {
        ResolutionResult.Failure.CandidateFailures(request, failure!!)
    }
}

private fun ResolutionResult.fallbackToDefaultIfNeeded(
    scope: ResolutionScope
): ResolutionResult = when (this) {
    is ResolutionResult.Success -> this
    is ResolutionResult.Failure -> if (request.required) this
    else ResolutionResult.Success(request, CandidateResolutionResult.Success(
        request, DefaultGivenNode(request.type, scope), emptyList()
    ))
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

private fun ResolutionScope.compareResult(
    a: CandidateResolutionResult?,
    b: CandidateResolutionResult?,
): Int {
    if (a != null && b == null) return -1
    if (b != null && a == null) return 1
    if (a == null && b == null) return 0
    a!!
    b!!

    if (a is CandidateResolutionResult.Success &&
            b !is CandidateResolutionResult.Success) return -1
    if (b is CandidateResolutionResult.Success &&
        a !is CandidateResolutionResult.Success) return 1

    if (a is CandidateResolutionResult.Success &&
            b is CandidateResolutionResult.Success) {
        var diff = compareCandidate(a.candidate, b.candidate)
        if (diff < 0) return -1
        else if (diff > 0) return 1

        diff = 0

        if (a.dependencyResults.size < b.dependencyResults.size) return -1
        if (b.dependencyResults.size < a.dependencyResults.size) return 1

        for (aDependency in a.dependencyResults) {
            for (bDependency in b.dependencyResults) {
                diff += compareResult(aDependency.candidateResult, bDependency.candidateResult)
            }
        }
        return when {
            diff < 0 -> -1
            diff > 0 -> 1
            else -> 0
        }
    } else if (a is CandidateResolutionResult.Failure &&
            b is CandidateResolutionResult.Failure) {
        return a.failure.failureOrdering.compareTo(b.failure.failureOrdering)
    } else {
        throw AssertionError()
    }
}

private fun ResolutionScope.compareCandidate(a: GivenNode?, b: GivenNode?): Int {
    if (a != null && b == null) return -1
    if (b != null && a == null) return 1
    if (a == null && b == null) return 0
    a!!
    b!!

    if (a is ObjectGivenNode && b !is ObjectGivenNode) return -1
    if (b is ObjectGivenNode && a !is ObjectGivenNode) return 1

    if (!a.isFrameworkGiven && !b.isFrameworkGiven) {
        val depthA = a.depth(this)
        val depthB = b.depth(this)
        if (depthA < depthB) return -1
        if (depthB < depthA) return 1
    }

    val diff = compareType(a.originalType, b.originalType)
    if (diff < 0) return -1
    if (diff > 0) return 1

    if (!a.isFrameworkGiven && b.isFrameworkGiven) return -1
    if (!b.isFrameworkGiven && a.isFrameworkGiven) return 1

    return 0
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
