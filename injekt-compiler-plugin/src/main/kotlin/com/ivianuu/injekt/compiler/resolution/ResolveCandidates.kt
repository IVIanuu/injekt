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

sealed class GivenGraph {
    data class Success(
        val scope: ResolutionScope,
        val results: Map<GivenRequest, CandidateResolutionResult.Success>
    ) : GivenGraph()

    data class Error(
        val failures: Map<GivenRequest, List<ResolutionResult.Failure>>,
    ) : GivenGraph()
}

sealed class CandidateResolutionResult {
    abstract val candidate: GivenNode

    data class Success(
        override val candidate: GivenNode,
        val scope: ResolutionScope,
        val dependencyResults: Map<GivenRequest, Success>
    ) : CandidateResolutionResult()

    data class Failure(
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
        .filter { it is ResolutionResult.Success || it.request.required }
        .let {
            it
                .filterIsInstance<ResolutionResult.Success>() to
                    it.filterIsInstance<ResolutionResult.Failure>()
        }
    return if (failureResults.isEmpty()) successResults.toSuccessGraph(requests, this)
    else failureResults.toErrorGraph()
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
    requests: List<GivenRequest>,
    scope: ResolutionScope
): GivenGraph.Success = GivenGraph.Success(scope, requests.zip(map { it.candidateResult }).toMap())

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
    val subChain = mutableSetOf(candidate)
    chain.reversed().forEach { prev ->
        subChain += prev
        if (prev.callableFqName == candidate.callableFqName &&
            prev.type.coveringSet == candidate.type.coveringSet &&
            (prev.type.typeSize < candidate.type.typeSize ||
                    (prev.type == candidate.type &&
                            subChain.none { it.lazyDependencies }))
        ) {
            return CandidateResolutionResult.Failure(
                candidate,
                ResolutionResult.Failure.DivergentGiven(request, emptyList()) // todo
            )
        }
    }

    if (candidate in chain)
        return CandidateResolutionResult.Success(candidate, this, emptyMap())

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

private fun ResolutionScope.resolveCandidate(
    request: GivenRequest,
    candidate: GivenNode,
): CandidateResolutionResult = computeForCandidate(request, candidate) {
    if (!callContext.canCall(candidate.callContext)) {
        return@computeForCandidate CandidateResolutionResult.Failure(
            candidate,
            ResolutionResult.Failure.CallContextMismatch(request, callContext, candidate)
        )
    }

    val successDependencyResults = mutableMapOf<GivenRequest, CandidateResolutionResult.Success>()
    val dependencyScope = candidate.dependencyScope ?: this
    for (dependency in candidate.dependencies) {
        when (val result = dependencyScope.resolveRequest(dependency)) {
            is ResolutionResult.Success -> successDependencyResults[dependency] = result.candidateResult
            is ResolutionResult.Failure -> {
                if (dependency.required) {
                    return@computeForCandidate CandidateResolutionResult.Failure(
                        candidate,
                        result
                    )
                }
            }
        }
    }
    return@computeForCandidate CandidateResolutionResult.Success(candidate, this, successDependencyResults)
}

private fun ResolutionScope.depth(scope: ResolutionScope): Int {
    var currentScope: ResolutionScope? = scope
    var depth = 0
    while (currentScope != null && currentScope != this) {
        depth++
        currentScope = currentScope.parent
    }
    if (currentScope == null) {
        currentScope = this
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

        for (aDependency in a.dependencyResults) {
            for (bDependency in b.dependencyResults) {
                diff += compareResult(aDependency.value, bDependency.value)
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

    if (!a.isFrameworkGiven && !b.isFrameworkGiven) {
        val depthA = a.ownerScope.depth(this)
        val depthB = b.ownerScope.depth(this)
        if (depthA < depthB) return -1
        if (depthB < depthA) return 1
    }

    val diff = compareType(a.originalType, b.originalType)
    if (diff < 0) return -1
    if (diff > 0) return 1

    if (!a.isFrameworkGiven && b.isFrameworkGiven) return -1
    if (!b.isFrameworkGiven && a.isFrameworkGiven) return 1

    if (a.dependencies.size < b.dependencies.size) return -1
    if (b.dependencies.size < a.dependencies.size) return 1

    val isAMacro = a is CallableGivenNode && a.callable.isFromMacro
    val isBMacro = b is CallableGivenNode && b.callable.isFromMacro
    if (!isAMacro && isBMacro) return -1
    if (!isBMacro && isAMacro) return 1

    if (callContext == a.callContext &&
            callContext != b.callContext) return -1

    if (callContext == b.callContext &&
        callContext != a.callContext) return 1

    return 0
}

private fun compareType(a: TypeRef, b: TypeRef): Int {
    if (!a.isStarProjection && b.isStarProjection) return -1
    if (a.isStarProjection && !b.isStarProjection) return 1

    if (!a.classifier.isTypeParameter && b.classifier.isTypeParameter) return -1
    if (a.classifier.isTypeParameter && !b.classifier.isTypeParameter) return 1

    if (!a.classifier.isTypeAlias && b.classifier.isTypeAlias) return -1
    if (a.classifier.isTypeAlias && !b.classifier.isTypeAlias) return 1

    if (a.arguments.size < b.arguments.size) return -1
    if (b.arguments.size < a.arguments.size) return 1

    if (a.classifier != b.classifier) return 0

    var diff = 0
    for (i in a.arguments.indices) {
        val aTypeArgument = a.arguments[i]
        val bTypeArgument = b.arguments[i]
        diff += compareType(aTypeArgument, bTypeArgument)
    }

    return when {
        diff < 0 -> -1
        diff > 0 -> 1
        else -> 0
    }
}
