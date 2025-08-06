/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package injekt.compiler.resolution

import injekt.compiler.*
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.util.*

sealed interface InjectionResult {
  val scope: InjectablesScope
  val callee: InjektCallable

  data class Success(
    override val scope: InjectablesScope,
    override val callee: InjektCallable,
    val results: Map<InjectableRequest, ResolutionResult.Success>
  ) : InjectionResult

  data class Error(
    override val scope: InjectablesScope,
    override val callee: InjektCallable,
    val failureRequest: InjectableRequest,
    val failure: ResolutionResult.Failure
  ) : InjectionResult
}

sealed interface ResolutionResult {
  sealed interface Success : ResolutionResult {
    data object DefaultValue : Success

    data class Value(
      val candidate: Injectable,
      val scope: InjectablesScope,
      val dependencyResults: Map<InjectableRequest, Success>
    ) : Success
  }

  sealed interface Failure : ResolutionResult {
    val failureOrdering: Int

    sealed interface WithCandidate : Failure {
      val candidate: Injectable

      data class DivergentInjectable(override val candidate: Injectable) : WithCandidate {
        override val failureOrdering: Int
          get() = 2
      }

      data class CallContextMismatch(
        val actualCallContext: CallContext,
        override val candidate: Injectable,
      ) : WithCandidate {
        override val failureOrdering: Int
          get() = 1
      }

      data class ReifiedTypeArgumentMismatch(
        val parameter: InjektClassifier,
        val argument: InjektClassifier,
        override val candidate: Injectable
      ) : WithCandidate {
        override val failureOrdering: Int
          get() = 1
      }

      data class DependencyFailure(
        override val candidate: Injectable,
        val dependencyRequest: InjectableRequest,
        val dependencyFailure: Failure,
      ) : WithCandidate {
        override val failureOrdering: Int
          get() = 1
      }
    }

    data class CandidateAmbiguity(
      val request: InjectableRequest,
      val candidateResults: List<Success.Value>
    ) : Failure {
      override val failureOrdering: Int
        get() = 0
    }

    data class NoCandidates(val request: InjectableRequest) : Failure {
      override val failureOrdering: Int
        get() = 3
    }
  }
}

context(scope: InjectablesScope)
fun resolveRequests(
  callee: InjektCallable,
  requests: List<InjectableRequest>
): InjectionResult {
  val successes = mutableMapOf<InjectableRequest, ResolutionResult.Success>()
  var failureRequest: InjectableRequest? = null
  var failure: ResolutionResult.Failure? = null
  for (request in requests)
    when (val result = resolveRequest(request)) {
      is ResolutionResult.Success -> successes[request] = result
      is ResolutionResult.Failure ->
        if (!request.isRequired &&
          result.unwrapDependencyFailure(request).second is ResolutionResult.Failure.NoCandidates) {
          successes[request] = ResolutionResult.Success.DefaultValue
        } else if (compareResult(result, failure) < 0) {
          failureRequest = request
          failure = result
        }
    }
  return if (failure == null) InjectionResult.Success(scope, callee, successes)
  else InjectionResult.Error(scope, callee, failureRequest!!, failure)
}

context(scope: InjectablesScope)
private fun resolveRequest(request: InjectableRequest): ResolutionResult {
  scope.resultsByType[request.type]?.let { return it }

  val result = tryToResolveRequestWithUserInjectables(request)
    ?: tryToResolveRequestWithBuiltInInjectable(request)
    ?: ResolutionResult.Failure.NoCandidates(request)

  scope.resultsByType[request.type] = result
  return result
}

context(scope: InjectablesScope)
private fun tryToResolveRequestWithUserInjectables(
  request: InjectableRequest
): ResolutionResult? = scope.injectablesForRequest(request, scope)
  .takeIf { it.isNotEmpty() }
  ?.let { resolveCandidates(request, it) }

context(scope: InjectablesScope)
private fun tryToResolveRequestWithBuiltInInjectable(
  request: InjectableRequest
): ResolutionResult? = scope.builtInInjectableForRequest(request)?.let { resolveCandidate(it) }

context(scope: InjectablesScope)
private fun computeForCandidate(
  candidate: Injectable,
  compute: () -> ResolutionResult,
): ResolutionResult {
  scope.resultsByCandidate[candidate]?.let { return it }

  if (candidate.dependencies.isEmpty())
    return compute().also { scope.resultsByCandidate[candidate] = it }

  if (scope.resolutionChain.isNotEmpty())
    for (i in scope.resolutionChain.lastIndex downTo 0) {
      val previousCandidate = scope.resolutionChain[i]

      val isSameCallable = if (candidate is CallableInjectable &&
        candidate.callable.symbol.fqName
          .asString().startsWith(StandardNames.FqNames.functionSupertype.asString()) &&
        previousCandidate is CallableInjectable &&
        previousCandidate.callable.symbol.fqName
          .asString().startsWith(StandardNames.FqNames.functionSupertype.asString()))
        candidate.dependencies.first().type == previousCandidate.dependencies.first().type
      else previousCandidate.chainFqName == candidate.chainFqName

      if (isSameCallable && previousCandidate.type == candidate.type) {
        val result = ResolutionResult.Failure.WithCandidate.DivergentInjectable(candidate)
        scope.resultsByCandidate[candidate] = result
        return result
      }
    }

  scope.resolutionChain += candidate
  val result = compute()
  scope.resultsByCandidate[candidate] = result
  scope.resolutionChain -= candidate
  return result
}

context(scope: InjectablesScope)
private fun resolveCandidates(
  request: InjectableRequest,
  candidates: List<Injectable>
): ResolutionResult {
  if (candidates.size == 1) return resolveCandidate(candidates.single())

  val successes = mutableListOf<ResolutionResult.Success>()
  var failure: ResolutionResult.Failure? = null
  val remaining = candidates
    .let {
      try {
        it.sortedWith { a, b -> compareCandidate(a, b) }
      } catch (e: Throwable) {
        it
      }
    }
    .toCollection(LinkedList())
  while (remaining.isNotEmpty()) {
    val candidate = remaining.removeFirst()
    if (compareCandidate(
        successes.firstOrNull()
          ?.safeAs<ResolutionResult.Success.Value>()?.candidate, candidate
      ) < 0
    ) break // we cannot get a better result

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
      is ResolutionResult.Failure ->
        if (compareResult(candidateResult, failure) < 0)
          failure = candidateResult
    }
  }

  return when {
    successes.size == 1 -> successes.single()
    successes.isNotEmpty() -> ResolutionResult.Failure.CandidateAmbiguity(request, successes.cast())
    else -> failure!!
  }
}

context(scope: InjectablesScope)
private fun resolveCandidate(
  candidate: Injectable
): ResolutionResult = computeForCandidate(candidate) {
  if (!scope.callContext.canCall(candidate.callContext))
    return@computeForCandidate ResolutionResult.Failure.WithCandidate.CallContextMismatch(
      scope.callContext,
      candidate
    )

  if (candidate is CallableInjectable)
    for ((typeParameter, typeArgument) in candidate.callable.typeArguments) {
      val argumentSymbol = typeArgument.classifier.symbol as? FirTypeParameterSymbol
        ?: continue
      val parameterSymbol = typeParameter.symbol as FirTypeParameterSymbol
      if (parameterSymbol.isReified && !argumentSymbol.isReified)
        return@computeForCandidate ResolutionResult.Failure.WithCandidate.ReifiedTypeArgumentMismatch(
          typeParameter,
          typeArgument.classifier,
          candidate
        )
    }

  if (candidate.dependencies.isEmpty())
    return@computeForCandidate ResolutionResult.Success.Value(candidate, scope, emptyMap())

  val successDependencyResults = mutableMapOf<InjectableRequest, ResolutionResult.Success>()
  for (dependency in candidate.dependencies) {
    val dependencyResult = with((candidate.dependencyScope ?: scope)) { resolveRequest(dependency) }
    when (dependencyResult) {
      is ResolutionResult.Success -> successDependencyResults[dependency] = dependencyResult
      is ResolutionResult.Failure -> when {
        !dependency.isRequired &&
            dependencyResult.unwrapDependencyFailure(dependency).second is ResolutionResult.Failure.NoCandidates ->
          successDependencyResults[dependency] = ResolutionResult.Success.DefaultValue

        else -> return@computeForCandidate ResolutionResult.Failure.WithCandidate.DependencyFailure(
          candidate,
          dependency,
          dependencyResult
        )
      }
    }
  }

  return@computeForCandidate ResolutionResult.Success.Value(
    candidate,
    scope,
    successDependencyResults
  )
}

context(scope: InjectablesScope)
private fun compareResult(a: ResolutionResult?, b: ResolutionResult?): Int {
  if (a === b) return 0

  if (a != null && b == null) return -1
  if (b != null && a == null) return 1
  if (a == null && b == null) return 0
  a!!
  b!!

  if (a is ResolutionResult.Success && b !is ResolutionResult.Success) return -1
  if (b is ResolutionResult.Success && a !is ResolutionResult.Success) return 1

  if (a is ResolutionResult.Success && b is ResolutionResult.Success) {
    if (a !is ResolutionResult.Success.DefaultValue && b is ResolutionResult.Success.DefaultValue)
      return -1
    if (b !is ResolutionResult.Success.DefaultValue && a is ResolutionResult.Success.DefaultValue)
      return 1

    if (a is ResolutionResult.Success.Value && b is ResolutionResult.Success.Value)
      return with(scope.ctx) { compareCandidate(a.candidate, b.candidate) }

    return 0
  } else {
    a as ResolutionResult.Failure
    b as ResolutionResult.Failure

    return a.failureOrdering.compareTo(b.failureOrdering)
  }
}

context(_: TypeCheckerContext)
private fun compareCandidate(a: Injectable?, b: Injectable?): Int {
  if (a === b) return 0

  if (a != null && b == null) return -1
  if (b != null && a == null) return 1

  a!!
  b!!

  val aScopeNesting = a.ownerScope.nesting
  val bScopeNesting = b.ownerScope.nesting
  if (aScopeNesting > bScopeNesting) return -1
  if (bScopeNesting > aScopeNesting) return 1

  if (a is CallableInjectable && b is CallableInjectable) {
    val ownerA = a.callable.parameterTypes[DISPATCH_RECEIVER_NAME]
    val ownerB = b.callable.parameterTypes[DISPATCH_RECEIVER_NAME]
    if (ownerA != null && ownerB != null) {
      if (ownerA.isSubTypeOf(ownerB)) return -1
      if (ownerB.isSubTypeOf(ownerA)) return 1
    }
  }

  return compareType(
    a.safeAs<CallableInjectable>()?.callable?.originalType,
    b.safeAs<CallableInjectable>()?.callable?.originalType
  )
}

context(_: TypeCheckerContext)
private fun compareType(
  a: InjektType?,
  b: InjektType?,
  comparedTypes: MutableSet<Pair<InjektType, InjektType>> = mutableSetOf()
): Int {
  if (a == b) return 0

  if (a != null && b == null) return -1
  if (b != null && a == null) return 1
  a!!
  b!!

  if (!a.isStarProjection && b.isStarProjection) return -1
  if (a.isStarProjection && !b.isStarProjection) return 1

  if (!a.isMarkedNullable && b.isMarkedNullable) return -1
  if (!b.isMarkedNullable && a.isMarkedNullable) return 1

  if (!a.classifier.isTypeParameter && b.classifier.isTypeParameter) return -1
  if (a.classifier.isTypeParameter && !b.classifier.isTypeParameter) return 1

  val pair = a to b
  if (!comparedTypes.add(pair)) return 0

  fun compareSameClassifier(a: InjektType?, b: InjektType?): Int {
    if (a == b) return 0

    if (a != null && b == null) return -1
    if (b != null && a == null) return 1
    a!!
    b!!

    var diff = 0
    a.arguments.zip(b.arguments).forEach { (aTypeArgument, bTypeArgument) ->
      diff += compareType(aTypeArgument, bTypeArgument, comparedTypes)
    }
    if (diff < 0) return -1
    if (diff > 0) return 1
    return 0
  }

  if (a.classifier != b.classifier) {
    val aSubTypeOfB = a.isSubTypeOf(b)
    val bSubTypeOfA = b.isSubTypeOf(a)
    if (aSubTypeOfB && !bSubTypeOfA) return -1
    if (bSubTypeOfA && !aSubTypeOfB) return 1
    val aCommonSuperType = a.superTypes.commonSuperType()
    val bCommonSuperType = b.superTypes.commonSuperType()
    val diff = compareType(aCommonSuperType, bCommonSuperType, comparedTypes)
    if (diff < 0) return -1
    if (diff > 0) return 1
  } else {
    val diff = compareSameClassifier(a, b)
    if (diff < 0) return -1
    if (diff > 0) return 1
  }

  return 0
}

fun ResolutionResult.Failure.unwrapDependencyFailure(
  request: InjectableRequest
): Pair<InjectableRequest, ResolutionResult.Failure> =
  if (this is ResolutionResult.Failure.WithCandidate.DependencyFailure)
    dependencyFailure.unwrapDependencyFailure(dependencyRequest)
  else request to this
