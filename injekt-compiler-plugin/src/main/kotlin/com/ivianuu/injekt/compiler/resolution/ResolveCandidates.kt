/*
 * Copyright 2021 Manuel Wrage
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

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.com.intellij.openapi.progress.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.utils.addToStdlib.*

sealed class GivenGraph {
  data class Success(
    val scope: ResolutionScope,
    val results: Map<GivenRequest, ResolutionResult.Success>,
    val usages: Map<UsageKey, List<GivenRequest>>
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
              dependencyResults.values.forEach { it.safeAs<Value>()?.visit() }
              allOuterMostScopes
                .asSequence()
                .sortedBy { it.allParents.size }
                .filter { outerMostScope ->
                  outerMostScope.allParents.size <
                      candidate.dependencyScope!!.allParents.size
                }
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

    data class CandidateAmbiguity(val candidateResults: List<Success.WithCandidate.Value>) :
      Failure() {
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

fun ResolutionScope.resolveRequests(
  requests: List<GivenRequest>,
  lookupLocation: KotlinLookupLocation,
  onEachResult: (ResolutionResult.Success.WithCandidate.Value) -> Unit
): GivenGraph = measureTimeMillisWithResult {
  recordLookup(lookupLocation)
  val successes = mutableMapOf<GivenRequest, ResolutionResult.Success>()
  var failureRequest: GivenRequest? = null
  var failure: ResolutionResult.Failure? = null
  for (request in requests) {
    when (val result = resolveRequest(request, lookupLocation)) {
      is ResolutionResult.Success -> successes[request] = result
      is ResolutionResult.Failure -> if ((request.defaultStrategy == GivenRequest.DefaultStrategy.NONE ||
            (request.defaultStrategy == GivenRequest.DefaultStrategy.DEFAULT_IF_NOT_GIVEN &&
                result !is ResolutionResult.Failure.NoCandidates)) &&
        compareResult(result, failure) < 0
      ) {
        failureRequest = request
        failure = result
      }
    }
  }
  val usages = mutableMapOf<UsageKey, MutableList<GivenRequest>>()
  return@measureTimeMillisWithResult if (failure == null) GivenGraph.Success(
    this,
    successes,
    usages
  )
    .also { it.postProcess(onEachResult, usages) }
  else GivenGraph.Error(this, failureRequest!!, failure)
}.also {
  println("resolving requests $requests in $name took ${it.first} ms")
}.second

private fun ResolutionScope.resolveRequest(
  request: GivenRequest,
  lookupLocation: KotlinLookupLocation
): ResolutionResult {
  checkCancelled()
  resultsByType[request.type]?.let { return it }
  val userCandidates = givensForRequest(request, this)
    ?: TypeResolutionScope(context, trace, request.type, lookupLocation)
      .also { it.recordLookup(lookupLocation) }
      .givensForRequest(request, this)
  val result = if (userCandidates != null) {
    resolveCandidates(request, userCandidates, lookupLocation)
  } else {
    val frameworkCandidate = frameworkGivenForRequest(request)
    when {
      frameworkCandidate != null -> resolveCandidate(request, frameworkCandidate, lookupLocation)
      request.defaultStrategy == GivenRequest.DefaultStrategy.NONE -> ResolutionResult.Failure.NoCandidates
      else -> ResolutionResult.Success.DefaultValue
    }
  }
  resultsByType[request.type] = result
  return result
}

private fun ResolutionScope.computeForCandidate(
  request: GivenRequest,
  candidate: GivenNode,
  compute: () -> ResolutionResult,
): ResolutionResult {
  checkCancelled()
  resultsByCandidate[candidate]?.let { return it }
  if (candidate.dependencies.isEmpty())
    return compute().also { resultsByCandidate[candidate] = it }

  if (chain.isNotEmpty()) {
    var isLazy = false
    for (i in chain.lastIndex downTo 0) {
      val prev = chain[i]
      isLazy = isLazy || prev.first.isLazy
      if (prev.second.callableFqName == candidate.callableFqName &&
        prev.second.type.coveringSet == candidate.type.coveringSet &&
        (prev.second.type.typeSize < candidate.type.typeSize ||
            (prev.second.type == candidate.type && !isLazy))
      ) {
        val result = ResolutionResult.Failure.DivergentGiven(candidate)
        resultsByCandidate[candidate] = result
        return result
      }
    }
  }

  if (chain.any { it.second == candidate })
    return ResolutionResult.Success.WithCandidate.CircularDependency(candidate, this)

  val pair = request to candidate
  chain += pair
  val result = compute()
  resultsByCandidate[candidate] = result
  chain -= pair
  return result
}

private fun ResolutionScope.resolveCandidates(
  request: GivenRequest,
  candidates: List<GivenNode>,
  lookupLocation: KotlinLookupLocation
): ResolutionResult {
  if (candidates.size == 1) {
    val candidate = candidates.single()
    return resolveCandidate(request, candidate, lookupLocation)
  }

  val successes = mutableListOf<ResolutionResult.Success>()
  var failure: ResolutionResult.Failure? = null
  val remaining = candidates
    .asSequence()
    .sortedWith { a, b -> compareCandidate(a, b) }
    .toMutableList()
  while (remaining.isNotEmpty()) {
    val candidate = remaining.removeAt(0)
    if (compareCandidate(
        successes.firstOrNull()
          ?.safeAs<ResolutionResult.Success.WithCandidate>()?.candidate, candidate
      ) < 0
    ) {
      // we cannot get a better result
      break
    }

    when (val candidateResult = resolveCandidate(request, candidate, lookupLocation)) {
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
  request: GivenRequest,
  candidate: GivenNode,
  lookupLocation: KotlinLookupLocation
): ResolutionResult = computeForCandidate(request, candidate) {
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
      if (parameterDescriptor.classifierInfo(context, trace).isForTypeKey &&
        !argumentDescriptor.classifierInfo(context, trace).isForTypeKey
      ) {
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
    return@computeForCandidate ResolutionResult.Success.WithCandidate.Value(
      candidate,
      this,
      emptyMap()
    )

  val successDependencyResults = mutableMapOf<GivenRequest, ResolutionResult.Success>()
  for (dependency in candidate.dependencies) {
    val dependencyScope = candidate.dependencyScope ?: this
    when (val dependencyResult = dependencyScope.resolveRequest(dependency, lookupLocation)) {
      is ResolutionResult.Success -> successDependencyResults[dependency] = dependencyResult
      is ResolutionResult.Failure -> {
        when {
          candidate is ProviderGivenNode && dependencyResult is ResolutionResult.Failure.NoCandidates ->
            return@computeForCandidate ResolutionResult.Failure.NoCandidates
          dependency.defaultStrategy == GivenRequest.DefaultStrategy.NONE ||
              (dependency.defaultStrategy == GivenRequest.DefaultStrategy.DEFAULT_IF_NOT_GIVEN &&
                  dependencyResult !is ResolutionResult.Failure.NoCandidates) ->
            return@computeForCandidate ResolutionResult.Failure.DependencyFailure(
              dependency, dependencyResult
            )
          else -> successDependencyResults[dependency] = ResolutionResult.Success.DefaultValue
        }
      }
    }
  }
  return@computeForCandidate ResolutionResult.Success.WithCandidate.Value(
    candidate,
    this,
    successDependencyResults
  )
}

private fun ResolutionScope.compareResult(a: ResolutionResult?, b: ResolutionResult?): Int {
  if (a === b) return 0
  if (a != null && b == null) return -1
  if (b != null && a == null) return 1
  if (a == null && b == null) return 0
  a!!
  b!!

  if (a is ResolutionResult.Success &&
    b !is ResolutionResult.Success
  ) return -1
  if (b is ResolutionResult.Success &&
    a !is ResolutionResult.Success
  ) return 1

  if (a is ResolutionResult.Success &&
    b is ResolutionResult.Success
  ) {
    if (a !is ResolutionResult.Success.DefaultValue &&
      b is ResolutionResult.Success.DefaultValue
    ) return -1
    if (b !is ResolutionResult.Success.DefaultValue &&
      a is ResolutionResult.Success.DefaultValue
    ) return 1

    if (a is ResolutionResult.Success.WithCandidate &&
      b is ResolutionResult.Success.WithCandidate
    )
      return compareCandidate(a.candidate, b.candidate)

    return 0
  } else {
    a as ResolutionResult.Failure
    b as ResolutionResult.Failure

    return a.failureOrdering.compareTo(b.failureOrdering)
  }
}

private inline fun <T> ResolutionScope.compareCandidate(
  a: T?,
  b: T?,
  requestedType: TypeRef?,
  type: (T) -> TypeRef,
  scopeNesting: (T) -> Int,
  owner: (T) -> ClassifierRef?,
  subClassNesting: (T) -> Int
): Int {
  if (a === b) return 0
  if (a != null && b == null) return -1
  if (b != null && a == null) return 1
  if (a == null && b == null) return 0
  a!!
  b!!

  val aScopeNesting = scopeNesting(a)
  val bScopeNesting = scopeNesting(b)
  if (aScopeNesting > bScopeNesting) return -1
  if (bScopeNesting > aScopeNesting) return 1

  val ownerA = owner(a)
  val ownerB = owner(b)
  if (ownerA != null && ownerA == ownerB) {
    val aSubClassNesting = subClassNesting(a)
    val bSubClassNesting = subClassNesting(b)

    if (aSubClassNesting < bSubClassNesting) return -1
    if (bSubClassNesting < aSubClassNesting) return 1
  }

  val diff = compareType(type(a), type(b), requestedType)
  if (diff < 0) return -1
  if (diff > 0) return 1

  return 0
}

private fun ResolutionScope.compareCandidate(a: GivenNode?, b: GivenNode?): Int = compareCandidate(
  a = a,
  b = b,
  requestedType = a?.type ?: b?.type,
  type = { it.originalType },
  scopeNesting = { it.ownerScope.allParents.size },
  owner = { (it as? CallableGivenNode)?.callable?.owner },
  subClassNesting = { (it as? CallableGivenNode)?.callable?.overriddenDepth ?: 0 }
)

fun ResolutionScope.compareType(a: TypeRef, b: TypeRef, requestedType: TypeRef?): Int {
  if (a == b) return 0
  if (!a.isStarProjection && b.isStarProjection) return -1
  if (a.isStarProjection && !b.isStarProjection) return 1

  if (!a.isMarkedNullable && b.isMarkedNullable) return -1
  if (!b.isMarkedNullable && a.isMarkedNullable) return 1

  if (!a.classifier.isTypeParameter && b.classifier.isTypeParameter) return -1
  if (a.classifier.isTypeParameter && !b.classifier.isTypeParameter) return 1

  fun compareSameClassifier(a: TypeRef, b: TypeRef): Int {
    var diff = 0
    a.arguments.forEachWith(b.arguments) { aTypeArgument, bTypeArgument ->
      diff += compareType(aTypeArgument, bTypeArgument, null)
    }
    if (diff < 0) return -1
    if (diff > 0) return 1
    return 0
  }

  if (a.classifier != b.classifier) {
    val aSubTypeOfB = a.isSubTypeOf(context, b)
    val bSubTypeOfA = b.isSubTypeOf(context, a)
    if (aSubTypeOfB && !bSubTypeOfA) return -1
    if (bSubTypeOfA && !aSubTypeOfB) return 1
    if (requestedType != null) {
      val aSubTypeView = a.subtypeView(requestedType.classifier)!!
      val bSubTypeView = b.subtypeView(requestedType.classifier)!!
      val diff = compareSameClassifier(aSubTypeView, bSubTypeView)
      if (diff < 0) return -1
      if (diff > 0) return 1
    }
  } else {
    val diff = compareSameClassifier(a, b)
    if (diff < 0) return -1
    if (diff > 0) return 1
  }

  return 0
}

fun ResolutionScope.compareCallable(a: CallableRef?, b: CallableRef?): Int {
  var diff = compareCandidate(
    a = a,
    b = b,
    requestedType = null,
    type = { it.originalType },
    scopeNesting = { -1 },
    owner = { it.owner },
    subClassNesting = { it.overriddenDepth }
  )
  if (diff < 0) return -1
  if (diff > 0) return 1

  if (a == null || b == null) return 0

  val aDependencies = a.parameterTypes.values
  val bDependencies = b.parameterTypes.values
  if (aDependencies.size < bDependencies.size) return -1
  if (bDependencies.size < aDependencies.size) return 1

  diff = 0
  for (aDependency in aDependencies) {
    for (bDependency in bDependencies) {
      diff += compareType(aDependency, bDependency, null)
    }
  }
  if (diff < 0) return -1
  if (diff > 0) return 1

  return 0
}

private fun GivenGraph.Success.postProcess(
  onEachResult: (ResolutionResult.Success.WithCandidate.Value) -> Unit,
  usages: MutableMap<UsageKey, MutableList<GivenRequest>>
) {
  fun ResolutionResult.Success.WithCandidate.Value.postProcess(request: GivenRequest) {
    usages.getOrPut(usageKey) { mutableListOf() } += request
    onEachResult(this)
    dependencyResults
      .forEach { (request, result) ->
        if (result is ResolutionResult.Success.WithCandidate.Value) {
          result.postProcess(request)
        }
      }
  }

  results
    .forEach { (request, result) ->
      if (result is ResolutionResult.Success.WithCandidate.Value) {
        result.postProcess(request)
      }
    }
}
