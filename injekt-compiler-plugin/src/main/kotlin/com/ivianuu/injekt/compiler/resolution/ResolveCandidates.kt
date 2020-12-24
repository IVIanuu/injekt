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
    val context = ResolutionContext(this)
    val (successResults, failureResults) = requests
        .map { context.resolveRequest(it) }
        .let {
            it
                .filterIsInstance<ResolutionResult.Success>() to
                    it.filterIsInstance<ResolutionResult.Failure>()
        }
    return if (failureResults.isEmpty()) {
        successResults.toSuccessGraph(this, requests)
    } else failureResults.toErrorGraph(this, requests)
}

private fun ResolutionContext.resolveRequest(request: GivenRequest): ResolutionResult =
    resolveInScope(
        request,
        (scope.givensForType(request.type) + getFrameworkCandidates(request)).distinct()
    )

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

class ResolutionContext(
    val scope: ResolutionScope,
    val chain: MutableSet<GivenNode> = mutableSetOf()
) {

    fun subContext(scope: ResolutionScope) = ResolutionContext(scope, chain)

    fun computeForCandidate(
        request: GivenRequest,
        candidate: GivenNode,
        compute: () -> CandidateResolutionResult,
    ): CandidateResolutionResult {
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
        chain -= candidate
        return result
    }
}

private fun ResolutionContext.resolveInScope(
    request: GivenRequest,
    candidates: List<GivenNode>,
): ResolutionResult {
    if (candidates.isEmpty()) {
        return if (request.required) ResolutionResult.Failure.NoCandidates(request)
        else ResolutionResult.Success(request, CandidateResolutionResult.Success(
            request, DefaultGivenNode(request.type, scope), emptyList()
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
            .disambiguate(scope)
            .let { finalResults ->
                finalResults.singleOrNull()?.let {
                    ResolutionResult.Success(request, it)
                } ?: ResolutionResult.Failure.CandidateAmbiguity(request, finalResults)
            }
    } else {
        ResolutionResult.Failure.CandidateFailures(request, failureResults.first())
    }
}

private fun ResolutionContext.resolveCandidate(
    request: GivenRequest,
    candidate: GivenNode,
): CandidateResolutionResult = computeForCandidate(request, candidate) {
    if (!scope.callContext.canCall(candidate.callContext)) {
        return@computeForCandidate CandidateResolutionResult.Failure(
            request,
            candidate,
            ResolutionResult.Failure.CallContextMismatch(request, scope.callContext, candidate)
        )
    }

    val successDependencyResults = mutableListOf<ResolutionResult.Success>()
    val dependencyContext = candidate.dependencyScope
        ?.let { subContext(it) } ?: this
    for (dependency in candidate.dependencies) {
        when (val result = dependencyContext.resolveRequest(dependency)) {
            is ResolutionResult.Success -> successDependencyResults += result
            is ResolutionResult.Failure -> return@computeForCandidate CandidateResolutionResult.Failure(
                dependency,
                candidate,
                result)
        }
    }
    return@computeForCandidate CandidateResolutionResult.Success(
        request,
        candidate,
        successDependencyResults
    )
}

private fun ResolutionContext.getFrameworkCandidates(request: GivenRequest): List<GivenNode> {
    if (request.forDispatchReceiver &&
        request.type.classifier.descriptor?.safeAs<ClassDescriptor>()
            ?.kind == ClassKind.OBJECT
    ) return listOf(ObjectGivenNode(request.type, scope))

    if (request.type.classifier.isGivenFunAlias) {
        return listOf(
            FunGivenNode(
                request.type,
                scope,
                CallableRef(
                    scope.declarationStore.functionDescriptorForFqName(request.type.classifier.fqName)
                        .single()
                )
            )
        )
    }

    if (request.type.path == null &&
        (request.type.classifier.fqName.asString().startsWith("kotlin.Function")
                || request.type.classifier.fqName.asString()
            .startsWith("kotlin.coroutines.SuspendFunction")) &&
                request.type.typeArguments.dropLast(1).all {
                    it.givenKind != null
                }
    ) {
        return listOf(
            ProviderGivenNode(
                request.type,
                scope,
                scope.declarationStore,
                request.required
            )
        )
    }

    val setType = scope.declarationStore.module.builtIns.set.defaultType.toTypeRef()
    if (request.type.isSubTypeOf(setType)) {
        val setElementType = request.type.subtypeView(setType.classifier)!!.typeArguments.single()
        val elements = scope.givenSetElementsForType(setElementType)
            .map { it.substitute(getSubstitutionMap(listOf(setElementType to it.originalType))) }
        return listOf(
            SetGivenNode(
                request.type,
                scope,
                elements,
                elements.flatMap { element -> element.getGivenRequests(false) }
            )
        )
    }

    return emptyList()
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

    check(a.classifier == b.classifier) {
        "Wtf ${a.render()} ${b.render()}"
    }

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
