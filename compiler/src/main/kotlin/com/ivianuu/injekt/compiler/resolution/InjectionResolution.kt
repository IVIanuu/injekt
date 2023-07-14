/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.resolution

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.LinkedList

sealed interface InjectionResult {
  val scope: InjectablesScope
  val callee: CallableRef

  data class Success(
    override val scope: InjectablesScope,
    override val callee: CallableRef,
    val results: Map<InjectableRequest, ResolutionResult.Success>
  ) : InjectionResult

  data class Error(
    override val scope: InjectablesScope,
    override val callee: CallableRef,
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

      data class ReifiedTypeArgumentMismatch(
        val parameter: ClassifierRef,
        val argument: ClassifierRef,
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

fun InjectablesScope.resolveRequests(
  callee: CallableRef,
  requests: List<InjectableRequest>
): InjectionResult {
  val successes = mutableMapOf<InjectableRequest, ResolutionResult.Success>()
  var failureRequest: InjectableRequest? = null
  var failure: ResolutionResult.Failure? = null
  for (request in requests) {
    when (val result = resolveRequest(request)) {
      is ResolutionResult.Success -> successes[request] = result
      is ResolutionResult.Failure ->
        if (request.isRequired ||
          result.unwrapDependencyFailure(request).second is ResolutionResult.Failure.CandidateAmbiguity) {
          if (compareResult(result, failure) < 0) {
            failureRequest = request
            failure = result
          }
        } else {
          successes[request] = ResolutionResult.Success.DefaultValue
        }
    }
  }
  return if (failure == null) InjectionResult.Success(this, callee, successes)
  else InjectionResult.Error(this, callee, failureRequest!!, failure)
}

private fun InjectablesScope.resolveRequest(request: InjectableRequest): ResolutionResult {
  if (request.type.hasErrors)
    return ResolutionResult.Failure.NoCandidates(request)

  resultsByType[request.type]?.let { return it }

  val result = tryToResolveRequestWithUserInjectables(request)
    ?: tryToResolveRequestWithFrameworkInjectable(request)
    ?: ResolutionResult.Failure.NoCandidates(request)

  resultsByType[request.type] = result
  return result
}

private fun InjectablesScope.tryToResolveRequestWithUserInjectables(
  request: InjectableRequest
): ResolutionResult? = injectablesForRequest(request, this)
  .takeIf { it.isNotEmpty() }
  ?.let { resolveCandidates(request, it) }

private fun InjectablesScope.tryToResolveRequestWithFrameworkInjectable(
  request: InjectableRequest
): ResolutionResult? =
  frameworkInjectableForRequest(request)?.let { resolveCandidate(request, it) }

private fun InjectablesScope.computeForCandidate(
  request: InjectableRequest,
  candidate: Injectable,
  compute: () -> ResolutionResult,
): ResolutionResult {
  resultsByCandidate[candidate]?.let { return it }

  if (candidate.dependencies.isEmpty())
    return compute().also { resultsByCandidate[candidate] = it }

  if (chain.isNotEmpty()) {
    for (i in chain.lastIndex downTo 0) {
      val (_, previousCandidate) = chain[i]

      val isSameCallable = if (candidate is CallableInjectable &&
        candidate.callable.callable.containingDeclaration.fqNameSafe
          .asString().startsWith("kotlin.Function") &&
          previousCandidate is CallableInjectable &&
          previousCandidate.callable.callable.containingDeclaration.fqNameSafe
            .asString().startsWith("kotlin.Function"))
        candidate.dependencies.first().type == previousCandidate.dependencies.first().type
        else previousCandidate.callableFqName == candidate.callableFqName
      if (isSameCallable &&
        previousCandidate.type.coveringSet == candidate.type.coveringSet &&
        (previousCandidate.type.typeSize < candidate.type.typeSize ||
            previousCandidate.type == candidate.type)) {
        val result = ResolutionResult.Failure.WithCandidate.DivergentInjectable(candidate)
        resultsByCandidate[candidate] = result
        return result
      }
    }
  }

  val pair = request to candidate
  chain += pair
  val result = compute()
  resultsByCandidate[candidate] = result
  chain -= pair
  return result
}

private fun InjectablesScope.resolveCandidates(
  request: InjectableRequest,
  candidates: List<Injectable>
): ResolutionResult {
  if (candidates.size == 1) {
    val candidate = candidates.single()
    return resolveCandidate(request, candidate)
  }

  val successes = mutableListOf<ResolutionResult.Success>()
  var failure: ResolutionResult.Failure? = null
  val remaining = candidates
    .sortedWith { a, b -> compareCandidate(a, b) }
    .toCollection(LinkedList())
  while (remaining.isNotEmpty()) {
    val candidate = remaining.removeFirst()
    if (compareCandidate(
        successes.firstOrNull()
          ?.safeAs<ResolutionResult.Success.Value>()?.candidate, candidate
      ) < 0
    ) {
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

  return when {
    successes.size == 1 -> successes.single()
    successes.isNotEmpty() -> ResolutionResult.Failure.CandidateAmbiguity(request, successes.cast())
    else -> failure!!
  }
}

private fun InjectablesScope.resolveCandidate(
  request: InjectableRequest,
  candidate: Injectable
): ResolutionResult = computeForCandidate(request, candidate) {
  if (candidate is CallableInjectable) {
    for ((typeParameter, typeArgument) in candidate.callable.typeArguments) {
      val argumentDescriptor = typeArgument.classifier.descriptor as? TypeParameterDescriptor
        ?: continue
      val parameterDescriptor = typeParameter.descriptor as TypeParameterDescriptor
      if (parameterDescriptor.isReified && !argumentDescriptor.isReified) {
        return@computeForCandidate ResolutionResult.Failure.WithCandidate.ReifiedTypeArgumentMismatch(
          typeParameter,
          typeArgument.classifier,
          candidate
        )
      }
    }
  }

  if (candidate.dependencies.isEmpty())
    return@computeForCandidate ResolutionResult.Success.Value(
      candidate,
      this,
      emptyMap()
    )

  val successDependencyResults = mutableMapOf<InjectableRequest, ResolutionResult.Success>()
  for (dependency in candidate.dependencies) {
    when (val dependencyResult = (candidate.dependencyScope ?: this).resolveRequest(dependency)) {
      is ResolutionResult.Success -> successDependencyResults[dependency] = dependencyResult
      is ResolutionResult.Failure -> {
        when {
          dependency.isRequired && candidate is LambdaInjectable &&
              dependencyResult is ResolutionResult.Failure.NoCandidates ->
            return@computeForCandidate ResolutionResult.Failure.NoCandidates(dependency)
          dependency.isRequired ||
              dependencyResult.unwrapDependencyFailure(dependency).second is ResolutionResult.Failure.CandidateAmbiguity ->
            return@computeForCandidate ResolutionResult.Failure.WithCandidate.DependencyFailure(
              candidate,
              dependency,
              dependencyResult
            )
          else -> successDependencyResults[dependency] = ResolutionResult.Success.DefaultValue
        }
      }
    }
  }
  return@computeForCandidate ResolutionResult.Success.Value(
    candidate,
    this,
    successDependencyResults
  )
}

private fun InjectablesScope.compareResult(a: ResolutionResult?, b: ResolutionResult?): Int {
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
      return compareCandidate(a.candidate, b.candidate)

    return 0
  } else {
    a as ResolutionResult.Failure
    b as ResolutionResult.Failure

    return a.failureOrdering.compareTo(b.failureOrdering)
  }
}

private fun InjectablesScope.compareCandidate(a: Injectable?, b: Injectable?): Int {
  if (a === b) return 0

  if (a != null && b == null) return -1
  if (b != null && a == null) return 1

  a!!
  b!!

  val aScopeNesting = a.ownerScope.nesting
  val bScopeNesting = b.ownerScope.nesting
  if (aScopeNesting > bScopeNesting) return -1
  if (bScopeNesting > aScopeNesting) return 1

  return compareCallable(
    a.safeAs<CallableInjectable>()?.callable,
    b.safeAs<CallableInjectable>()?.callable
  )
}

private fun InjectablesScope.compareCallable(a: CallableRef?, b: CallableRef?): Int {
  if (a == b) return 0

  if (a != null && b == null) return -1
  if (b != null && a == null) return 1
  a!!
  b!!

  val ownerA = a.callable.containingDeclaration
  val ownerB = b.callable.containingDeclaration
  if (ownerA == ownerB) {
    val aSubClassNesting = a.callable
      .overriddenTreeUniqueAsSequence(false).count().dec()
    val bSubClassNesting = b.callable
      .overriddenTreeUniqueAsSequence(false).count().dec()

    if (aSubClassNesting < bSubClassNesting) return -1
    if (bSubClassNesting < aSubClassNesting) return 1
  }

  val diff = compareType(a.originalType, b.originalType)
  if (diff < 0) return -1
  if (diff > 0) return 1

  return 0
}

private fun InjectablesScope.compareType(
  a: TypeRef?,
  b: TypeRef?,
  comparedTypes: MutableSet<Pair<TypeRef, TypeRef>> = mutableSetOf()
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

  fun compareSameClassifier(a: TypeRef?, b: TypeRef?): Int {
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
    val aSubTypeOfB = a.isSubTypeOf(b, ctx)
    val bSubTypeOfA = b.isSubTypeOf(a, ctx)
    if (aSubTypeOfB && !bSubTypeOfA) return -1
    if (bSubTypeOfA && !aSubTypeOfB) return 1
    val aCommonSuperType = commonSuperType(a.superTypes, ctx = ctx)
    val bCommonSuperType = commonSuperType(b.superTypes, ctx = ctx)
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
