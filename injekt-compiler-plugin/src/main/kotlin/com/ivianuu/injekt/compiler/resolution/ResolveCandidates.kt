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

import com.ivianuu.injekt.compiler.forEachWith
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.unsafeLazy

sealed class GivenGraph {
    data class Success(
        val scope: ResolutionScope,
        val results: List<ResolutionResult.Success>
    ) : GivenGraph()

    data class Error(val failures: List<ResolutionResult.Failure>) : GivenGraph()
}

sealed class ResolutionResult {
    abstract val request: GivenRequest

    data class Success(
        override val request: GivenRequest,
        val candidate: GivenNode,
        val scope: ResolutionScope,
        val dependencyResults: List<Success>
    ) : ResolutionResult() {
        val outerMostScope: ResolutionScope by unsafeLazy {
            when {
                dependencyResults.isEmpty() -> scope.allScopes.first {
                    it.allParents.size >= candidate.ownerScope.allParents.size &&
                            it.callContext.canCall(candidate.callContext)
                }
                candidate.dependencyScope != null -> {
                    val allOuterMostScopes = mutableListOf<ResolutionScope>()
                    fun Success.visit() {
                        allOuterMostScopes += outerMostScope
                        dependencyResults.forEach { it.visit() }
                    }
                    dependencyResults.single().visit()
                    allOuterMostScopes
                        .sortedBy { it.allParents.size }
                        .filter { it.allParents.size < candidate.dependencyScope!!.allParents.size }
                        .lastOrNull {
                            it.callContext.canCall(candidate.callContext)
                        } ?: scope.allScopes.first()
                }
                else -> {
                    val dependencyScope = dependencyResults.maxByOrNull {
                        it.outerMostScope.allParents.size
                    }!!.outerMostScope
                    when {
                        dependencyScope.allParents.size <
                                candidate.ownerScope.allParents.size -> scope.allScopes.first {
                            it.allParents.size >= candidate.ownerScope.allParents.size &&
                                    it.callContext.canCall(scope.callContext)
                        }
                        dependencyScope.callContext.canCall(scope.callContext) -> dependencyScope
                        else -> scope.allScopes.first {
                            it.allParents.size >= candidate.ownerScope.allParents.size &&
                                    it.callContext.canCall(scope.callContext)
                        }
                    }
                }
            }
        }
    }

    sealed class Failure : ResolutionResult() {
        abstract val failureOrdering: Int

        data class CandidateAmbiguity(
            override val request: GivenRequest,
            val candidateResults: List<Success>,
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
            val candidate: GivenNode
        ) : Failure() {
            override val failureOrdering: Int
                get() = 1
        }

        data class DependencyFailure(
            override val request: GivenRequest,
            val candidate: GivenNode,
            val candidateFailure: Failure,
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

fun ResolutionScope.resolveRequests(requests: List<GivenRequest>): GivenGraph {
    val (successResults, failureResults) = requests
        .map { resolveRequest(it) }
        .filter { it is ResolutionResult.Success || it.request.required }
        .let {
            it
                .filterIsInstance<ResolutionResult.Success>() to
                    it.filterIsInstance<ResolutionResult.Failure>()
        }
    return if (failureResults.isEmpty()) GivenGraph.Success(this, successResults)
    else GivenGraph.Error(
        failureResults
            .sortedWith { a, b -> compareResult(a, b) }
    )
}

private fun ResolutionScope.resolveRequest(request: GivenRequest): ResolutionResult {
    resultsByRequest[request]?.let { return it }
    val result = resolveCandidates(request, givensForType(request.type))
    resultsByRequest[request] = result
    return result
}

private fun ResolutionScope.computeForCandidate(
    request: GivenRequest,
    candidate: GivenNode,
    compute: () -> ResolutionResult,
): ResolutionResult {
    if (chain.isNotEmpty()) {
        var lazyDependencies = false
        for (i in chain.lastIndex downTo 0) {
            val prev = chain[i]
            lazyDependencies = lazyDependencies || prev.lazyDependencies
            if (prev.callableFqName == candidate.callableFqName &&
                prev.type.coveringSet == candidate.type.coveringSet &&
                (prev.type.typeSize < candidate.type.typeSize ||
                        (prev.type == candidate.type && !lazyDependencies))
            ) {
                return ResolutionResult.Failure.DivergentGiven(request, candidate)
            }
        }
    }

    if (candidate in chain)
        return ResolutionResult.Success(request, candidate, this, emptyList())

    chain += candidate
    val result = compute()
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
        return resolveCandidate(request, candidate)
    }

    val successes = mutableListOf<ResolutionResult.Success>()
    var failure: ResolutionResult.Failure? = null
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
            is ResolutionResult.Success -> {
                val firstSuccessResult = successes.firstOrNull()
                when (compareResult(candidateResult, firstSuccessResult)) {
                    -1 -> {
                        successes.clear()
                        successes += candidateResult
                    }
                    0 -> successes += candidateResult
                }
            }
            is ResolutionResult.Failure -> {
                if (compareResult(candidateResult, failure) < 0)
                    failure = candidateResult
            }
        }
    }

    return if (successes.isNotEmpty()) {
        successes.singleOrNull()
            ?: ResolutionResult.Failure.CandidateAmbiguity(request, successes)
    } else failure!!
}

private fun ResolutionScope.resolveCandidate(
    request: GivenRequest,
    candidate: GivenNode,
): ResolutionResult = computeForCandidate(request, candidate) {
    if (!callContext.canCall(candidate.callContext)) {
        return@computeForCandidate ResolutionResult.Failure.CallContextMismatch(request, callContext, candidate)
    }

    val successDependencyResults = mutableListOf<ResolutionResult.Success>()
    val dependencyScope = candidate.dependencyScope ?: this
    for (dependency in candidate.dependencies) {
        when (val dependencyResult = dependencyScope.resolveRequest(dependency)) {
            is ResolutionResult.Success -> successDependencyResults += dependencyResult
            is ResolutionResult.Failure -> {
                if (dependency.required) {
                    return@computeForCandidate ResolutionResult.Failure.DependencyFailure(
                        request,
                        candidate,
                        dependencyResult
                    )
                }
            }
        }
    }
    return@computeForCandidate ResolutionResult.Success(request, candidate, this, successDependencyResults)
}

private fun ResolutionScope.compareResult(
    a: ResolutionResult?,
    b: ResolutionResult?,
): Int {
    if (a != null && b == null) return -1
    if (b != null && a == null) return 1
    if (a == null && b == null) return 0
    a!!
    b!!

    if (a is ResolutionResult.Success &&
            b !is ResolutionResult.Success) return -1
    if (b is ResolutionResult.Success &&
        a !is ResolutionResult.Success) return 1

    if (a is ResolutionResult.Success &&
            b is ResolutionResult.Success) {
        var diff = compareCandidate(a.candidate, b.candidate)
        if (diff < 0) return -1
        else if (diff > 0) return 1

        diff = 0

        for (aDependency in a.dependencyResults) {
            for (bDependency in b.dependencyResults) {
                diff += compareResult(aDependency, bDependency)
            }
        }
        return when {
            diff < 0 -> -1
            diff > 0 -> 1
            else -> 0
        }
    } else if (a is ResolutionResult.Failure &&
            b is ResolutionResult.Failure) {
        return a.failureOrdering.compareTo(b.failureOrdering)
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
        if (a.depth > b.depth) return -1
        if (b.depth > a.depth) return 1
    }

    if (a is CallableGivenNode && b is CallableGivenNode) {
        if (!a.callable.callable.isExternalDeclaration() &&
                b.callable.callable.isExternalDeclaration()) return -1
        if (!b.callable.callable.isExternalDeclaration() &&
            a.callable.callable.isExternalDeclaration()) return 1
    }

    val diff = compareType(a.originalType, b.originalType)
    if (diff < 0) return -1
    if (diff > 0) return 1

    if (!a.isFrameworkGiven && b.isFrameworkGiven) return -1
    if (!b.isFrameworkGiven && a.isFrameworkGiven) return 1

    if (a.dependencies.size < b.dependencies.size) return -1
    if (b.dependencies.size < a.dependencies.size) return 1

    val isAFromGivenConstraint = a is CallableGivenNode && a.callable.fromGivenConstraint
    val isBFromGivenConstraint = b is CallableGivenNode && b.callable.fromGivenConstraint
    if (!isAFromGivenConstraint && isBFromGivenConstraint) return -1
    if (!isBFromGivenConstraint && isAFromGivenConstraint) return 1

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
    a.arguments.forEachWith(b.arguments) { aTypeArgument, bTypeArgument ->
        diff += compareType(aTypeArgument, bTypeArgument)
    }

    return when {
        diff < 0 -> -1
        diff > 0 -> 1
        else -> 0
    }
}
