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
import com.ivianuu.injekt.compiler.isForTypeKey
import com.ivianuu.injekt.compiler.unsafeLazy
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

sealed class GivenGraph {
    data class Success(
        val scope: ResolutionScope,
        val results: Map<GivenRequest, ResolutionResult.Success>
    ) : GivenGraph()

    data class Error(
        val scope: ResolutionScope,
        val failureRequest: GivenRequest,
        val failure: ResolutionResult.Failure
    ) : GivenGraph()
}

sealed class ResolutionResult {
    sealed class Success : ResolutionResult() {
        object DefaultValue : Success()
        sealed class WithCandidate : ResolutionResult.Success() {
            abstract val candidate: GivenNode
            abstract val scope: ResolutionScope
            data class CircularDependency(
                override val candidate: GivenNode,
                override val scope: ResolutionScope
            ) : Success.WithCandidate()
            data class Value(
                override val candidate: GivenNode,
                override val scope: ResolutionScope,
                val dependencyResults: Map<GivenRequest, Success>
            ) : Success.WithCandidate() {
                val usageKey by unsafeLazy { UsageKey(candidate.type, outerMostScope) }
                val outerMostScope: ResolutionScope by unsafeLazy {
                    when {
                        dependencyResults.isEmpty() -> scope.allScopes.first {
                            it.allParents.size >= candidate.ownerScope.allParents.size &&
                                    it.callContext.canCall(candidate.callContext)
                        }
                        candidate.dependencyScope != null -> {
                            val allOuterMostScopes = mutableListOf<ResolutionScope>()
                            fun Value.visit() {
                                allOuterMostScopes += outerMostScope
                                dependencyResults.forEach {
                                    (it.value as? Value)?.visit()
                                }
                            }
                            dependencyResults.values.single().safeAs<Value>()?.visit()
                            allOuterMostScopes
                                .asSequence()
                                .sortedBy { it.allParents.size }
                                .filter { it.allParents.size < candidate.dependencyScope!!.allParents.size }
                                .lastOrNull {
                                    it.callContext.canCall(candidate.callContext)
                                } ?: scope.allScopes.first()
                        }
                        else -> {
                            val dependencyScope = dependencyResults
                                .filterValues { it is Value }
                                .mapValues { it.value as Value }
                                .maxByOrNull {
                                    it.value.outerMostScope.allParents.size
                                }?.value?.outerMostScope
                            if (dependencyScope != null) {
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
                            } else {
                                scope.allScopes.first {
                                    it.allParents.size >= candidate.ownerScope.allParents.size &&
                                            it.callContext.canCall(scope.callContext)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    sealed class Failure : ResolutionResult() {
        abstract val failureOrdering: Int

        data class CandidateAmbiguity(val candidateResults: List<Success.WithCandidate.Value>) : Failure() {
            override val failureOrdering: Int
                get() = 0
        }

        data class CallContextMismatch(
            val actualCallContext: CallContext,
            val candidate: GivenNode,
        ) : Failure() {
            override val failureOrdering: Int
                get() = 1
        }

        data class TypeArgumentKindMismatch(
            val kind: TypeArgumentKind,
            val parameter: ClassifierRef,
            val argument: ClassifierRef,
            val candidate: GivenNode
        ) : Failure() {
            override val failureOrdering: Int
                get() = 1
            enum class TypeArgumentKind {
                REIFIED, FOR_TYPE_KEY
            }
        }

        data class DivergentGiven(val candidate: GivenNode) : Failure() {
            override val failureOrdering: Int
                get() = 1
        }

        data class DependencyFailure(
            val dependencyRequest: GivenRequest,
            val dependencyFailure: Failure,
        ) : Failure() {
            override val failureOrdering: Int
                get() = 1
        }

        object NoCandidates : Failure() {
            override val failureOrdering: Int
                get() = 2
        }
    }
}

data class UsageKey(val type: TypeRef, val outerMostScope: ResolutionScope)

fun ResolutionScope.resolveRequests(requests: List<GivenRequest>): GivenGraph {
    val successes = mutableMapOf<GivenRequest, ResolutionResult.Success>()
    var failureRequest: GivenRequest? = null
    var failure: ResolutionResult.Failure? = null
    for (request in requests) {
        when (val result = resolveRequest(request)) {
            is ResolutionResult.Success -> successes[request] = result
            is ResolutionResult.Failure ->
                if ((request.isRequired || result !is ResolutionResult.Failure.NoCandidates) &&
                    compareResult(result, failure) < 0) {
                    failureRequest = request
                    failure = result
                }
        }
    }
    return if (failure == null) GivenGraph.Success(this, successes)
        .also { it.validate() }
    else GivenGraph.Error(this, failureRequest!!, failure)
}

private fun ResolutionScope.resolveRequest(request: GivenRequest): ResolutionResult {
    resultsByType[request.type]?.let { return it }
    val userGivens = givensForType(request.type)
    val result = if (userGivens.isNotEmpty()) {
        resolveCandidates(userGivens)
    } else {
        val frameworkCandidate = frameworkGivenForType(request.type)
        if (frameworkCandidate != null) {
            resolveCandidate(frameworkCandidate)
        } else {
            ResolutionResult.Failure.NoCandidates
        }
    }
    resultsByType[request.type] = result
    return result
}

private fun ResolutionScope.computeForCandidate(
    candidate: GivenNode,
    compute: () -> ResolutionResult,
): ResolutionResult {
    resultsByCandidate[candidate]?.let { return it }
    if (candidate.dependencies.isEmpty())
        return compute().also { resultsByCandidate[candidate] = it }

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
                val result = ResolutionResult.Failure.DivergentGiven(candidate)
                resultsByCandidate[candidate] = result
                return result
            }
        }
    }

    if (candidate in chain)
        return ResolutionResult.Success.WithCandidate.CircularDependency(candidate, this)

    chain += candidate
    val result = compute()
    resultsByCandidate[candidate] = result
    chain -= candidate
    return result
}

private fun ResolutionScope.resolveCandidates(
    candidates: List<GivenNode>,
): ResolutionResult {
    if (candidates.isEmpty()) return ResolutionResult.Failure.NoCandidates

    if (candidates.size == 1) {
        val candidate = candidates.single()
        return resolveCandidate(candidate)
    }

    val successes = mutableListOf<ResolutionResult.Success>()
    var failure: ResolutionResult.Failure? = null
    val remaining = candidates
        .asSequence()
        .sortedWith { a, b -> compareCandidate(a, b) }
        .toMutableList()
    while (remaining.isNotEmpty()) {
        val candidate = remaining.removeAt(0)
        if (compareCandidate(successes.firstOrNull()
                ?.safeAs<ResolutionResult.Success.WithCandidate>()?.candidate, candidate) < 0) {
            // we cannot get a better result
            break
        }

        when (val candidateResult = resolveCandidate(candidate)) {
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
            ?: ResolutionResult.Failure.CandidateAmbiguity(successes.cast())
    } else failure!!
}

private fun ResolutionScope.resolveCandidate(
    candidate: GivenNode
): ResolutionResult = computeForCandidate(candidate) {
    if (!callContext.canCall(candidate.callContext)) {
        return@computeForCandidate ResolutionResult.Failure.CallContextMismatch(callContext, candidate)
    }

    if (candidate is CallableGivenNode) {
        for ((typeParameter, typeArgument) in candidate.callable.typeArguments) {
            val argumentDescriptor = typeArgument.classifier.descriptor as? TypeParameterDescriptor
                ?: continue
            val parameterDescriptor = typeParameter.descriptor as TypeParameterDescriptor
            if (parameterDescriptor.isReified && !argumentDescriptor.isReified) {
                return@computeForCandidate ResolutionResult.Failure.TypeArgumentKindMismatch(
                    ResolutionResult.Failure.TypeArgumentKindMismatch.TypeArgumentKind.REIFIED,
                    typeParameter,
                    typeArgument.classifier,
                    candidate
                )
            }
            if (parameterDescriptor.isForTypeKey(context, trace) &&
                    !argumentDescriptor.isForTypeKey(context, trace)) {
                return@computeForCandidate ResolutionResult.Failure.TypeArgumentKindMismatch(
                    ResolutionResult.Failure.TypeArgumentKindMismatch.TypeArgumentKind.FOR_TYPE_KEY,
                    typeParameter,
                    typeArgument.classifier,
                    candidate
                )
            }
        }
    }

    if (candidate.dependencies.isEmpty())
        return@computeForCandidate ResolutionResult.Success.WithCandidate.Value(candidate, this, emptyMap())

    val successDependencyResults = mutableMapOf<GivenRequest, ResolutionResult.Success>()
    val dependencyScope = candidate.dependencyScope ?: this
    for (dependency in candidate.dependencies) {
        when (val dependencyResult = dependencyScope.resolveRequest(dependency)) {
            is ResolutionResult.Success -> successDependencyResults[dependency] = dependencyResult
            is ResolutionResult.Failure -> {
                when {
                    candidate is ProviderGivenNode && dependencyResult is ResolutionResult.Failure.NoCandidates ->
                        return@computeForCandidate ResolutionResult.Failure.NoCandidates
                    dependency.isRequired || dependencyResult !is ResolutionResult.Failure.NoCandidates -> {
                        return@computeForCandidate ResolutionResult.Failure.DependencyFailure(
                            dependency,
                            dependencyResult
                        )
                    }
                    else -> {
                        successDependencyResults[dependency] = ResolutionResult.Success.DefaultValue
                    }
                }
            }
        }
    }
    return@computeForCandidate ResolutionResult.Success.WithCandidate.Value(candidate, this, successDependencyResults)
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
        if (a !is ResolutionResult.Success.DefaultValue &&
                b is ResolutionResult.Success.DefaultValue) return -1
        if (b !is ResolutionResult.Success.DefaultValue &&
            a is ResolutionResult.Success.DefaultValue) return 1

        if (a is ResolutionResult.Success.WithCandidate &&
            b is ResolutionResult.Success.WithCandidate) {
            var diff = compareCandidate(a.candidate, b.candidate)
            if (diff < 0) return -1
            else if (diff > 0) return 1

            diff = 0

            if (a is ResolutionResult.Success.WithCandidate.Value &&
                    b is ResolutionResult.Success.WithCandidate.Value) {
                for (aDependency in a.dependencyResults) {
                    for (bDependency in b.dependencyResults) {
                        diff += compareResult(aDependency.value, bDependency.value)
                    }
                }
            }
            return when {
                diff < 0 -> -1
                diff > 0 -> 1
                else -> 0
            }
        } else throw AssertionError()
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

    if (a.originalType == a.type && b.originalType != a.type) return -1
    if (b.originalType == b.type && a.originalType != b.type) return 1

    val diff = compareType(a.originalType, b.originalType)
    if (diff < 0) return -1
    if (diff > 0) return 1

    if (!a.isFrameworkGiven && !b.isFrameworkGiven) {
        if (a.ownerScope.allParents.size > b.ownerScope.allParents.size) return -1
        if (b.ownerScope.allParents.size > a.ownerScope.allParents.size) return 1
    }

    if (a is CallableGivenNode && b is CallableGivenNode) {
        if (!a.callable.callable.isExternalDeclaration() &&
            b.callable.callable.isExternalDeclaration()) return -1
        if (!b.callable.callable.isExternalDeclaration() &&
            a.callable.callable.isExternalDeclaration()) return 1
    }

    if (!a.isFrameworkGiven && b.isFrameworkGiven) return -1
    if (!b.isFrameworkGiven && a.isFrameworkGiven) return 1

    if (a.dependencies.size < b.dependencies.size) return -1
    if (b.dependencies.size < a.dependencies.size) return 1

    val isAFromGivenConstraint = a is CallableGivenNode && a.callable.fromGivenConstraint
    val isBFromGivenConstraint = b is CallableGivenNode && b.callable.fromGivenConstraint
    if (isAFromGivenConstraint && !isBFromGivenConstraint) return -1
    if (isBFromGivenConstraint && !isAFromGivenConstraint) return 1

    if (callContext == a.callContext &&
            callContext != b.callContext) return -1
    if (callContext == b.callContext &&
        callContext != a.callContext) return 1

    if (a is CallableGivenNode && b is CallableGivenNode) {
        if (a.callable.callable.containingDeclaration ==
            b.callable.callable.containingDeclaration) {
            val aOverriddenTreeSize = a.callable.callable.overriddenTreeUniqueAsSequence(true).count()
            val bOverriddenTreeSize = b.callable.callable.overriddenTreeUniqueAsSequence(true).count()
            if (aOverriddenTreeSize < bOverriddenTreeSize) return -1
            if (bOverriddenTreeSize < aOverriddenTreeSize) return 1
        }
    }

    return 0
}

private fun compareType(a: TypeRef, b: TypeRef): Int {
    if (!a.isStarProjection && b.isStarProjection) return -1
    if (a.isStarProjection && !b.isStarProjection) return 1

    if (!a.classifier.isTypeParameter && b.classifier.isTypeParameter) return -1
    if (a.classifier.isTypeParameter && !b.classifier.isTypeParameter) return 1

    if (a.arguments.size < b.arguments.size) return -1
    if (b.arguments.size < a.arguments.size) return 1

    if (a.qualifier == null && b.qualifier != null) return -1
    if (b.qualifier == null && a.qualifier != null) return 1

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

private fun GivenGraph.Success.validate() {
    validateAllTypeParametersSubstituted()
}

private fun GivenGraph.Success.validateAllTypeParametersSubstituted() {
    val typeParametersInScope = scope.allScopes
        .flatMap { scope ->
            (scope.ownerDescriptor.safeAs<ClassDescriptor>()
                ?.declaredTypeParameters ?:
                scope.ownerDescriptor.safeAs<FunctionDescriptor>()
                    ?.typeParameters ?:
                    scope.ownerDescriptor.safeAs<PropertyDescriptor>()
                        ?.typeParameters)
                ?.map { it.toClassifierRef(scope.context, scope.trace) }
                ?: emptyList()
        }

    fun ResolutionResult.Success.WithCandidate.Value.validate() {
        fun TypeRef.validate() {
            if (classifier.isTypeParameter &&
                    classifier !in typeParametersInScope) {
                error("Invalid graph: unsubstituted type parameter $classifier")
            }

            arguments.forEach { it.validate() }
            qualifier?.validate()
        }

        candidate.type.validate()
        if (candidate is CallableGivenNode) {
            candidate.callable.typeArguments.forEach { it.value.validate() }
        }

        dependencyResults
            .values
            .filterIsInstance<ResolutionResult.Success.WithCandidate.Value>()
            .forEach { it.validate() }
    }

    results
        .values
        .filterIsInstance<ResolutionResult.Success.WithCandidate.Value>()
        .forEach { it.validate() }
}
