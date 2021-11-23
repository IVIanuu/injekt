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

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.DISPATCH_RECEIVER_INDEX
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.uniqueKey
import com.ivianuu.shaded_injekt.Inject
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.LinkedList
import kotlin.math.sign
import kotlin.reflect.KClass

sealed class InjectionGraph {
  abstract val scope: InjectablesScope
  abstract val callee: CallableRef

  data class Success(
    override val scope: InjectablesScope,
    override val callee: CallableRef,
    val results: Map<InjectableRequest, ResolutionResult.Success>,
    val usages: Map<UsageKey, MutableSet<InjectableRequest>>
  ) : InjectionGraph()

  data class Error(
    override val scope: InjectablesScope,
    override val callee: CallableRef,
    val failureRequest: InjectableRequest,
    val failure: ResolutionResult.Failure,
    val importSuggestions: List<CallableRef>
  ) : InjectionGraph()
}

sealed class ResolutionResult {
  sealed class Success : ResolutionResult() {
    object DefaultValue : Success()

    sealed class WithCandidate : ResolutionResult.Success() {
      abstract val candidate: Injectable
      abstract val scope: InjectablesScope

      data class CircularDependency(
        override val candidate: Injectable,
        override val scope: InjectablesScope
      ) : Success.WithCandidate()

      data class Value(
        override val candidate: Injectable,
        override val scope: InjectablesScope,
        val dependencyResults: Map<InjectableRequest, Success>
      ) : Success.WithCandidate() {
        val highestScope: InjectablesScope = run {
          if (candidate.scopeComponentType != null)
            return@run scope.allScopes.lastOrNull { candidateScope ->
              candidateScope.isDeclarationContainer &&
                  candidateScope.canSeeInjectablesOf(candidate.ownerScope) &&
                  candidateScope.componentType == candidate.scopeComponentType
            } ?: scope

          val anchorScopes = mutableSetOf<InjectablesScope>()

          fun collectScopesRecursive(result: Value) {
            if (result.candidate is CallableInjectable) {
              anchorScopes += result.candidate.ownerScope
              result.candidate.scopeComponentOrNull(result.scope)?.let { anchorScopes += it }
            }
            result.dependencyResults.values
              .filterIsInstance<Value>()
              .forEach { collectScopesRecursive(it) }
          }

          collectScopesRecursive(this)

          scope.allScopes
            .sortedBy { it.nesting }
            .firstOrNull { candidateScope ->
              candidateScope.isDeclarationContainer &&
                  anchorScopes.all {
                    (candidateScope.canSeeInjectablesOf(it) ||
                        candidateScope.canSeeInjectablesOf(scope))
                  } &&
                  candidateScope.callContext.canCall(candidate.callContext)
            } ?: scope
        }

        val usageKey = UsageKey(candidate.usageKey, candidate::class, highestScope)
      }
    }
  }

  sealed class Failure : ResolutionResult() {
    abstract val failureOrdering: Int

    sealed class WithCandidate : Failure() {
      abstract val candidate: Injectable

      data class CallContextMismatch(
        val actualCallContext: CallContext,
        override val candidate: Injectable,
      ) : WithCandidate() {
        override val failureOrdering: Int
          get() = 1
      }

      data class ClashingSuperTypes(
        val superTypeA: TypeRef,
        val superTypeB: TypeRef,
        override val candidate: AbstractInjectable
      ) : WithCandidate() {
        override val failureOrdering: Int
          get() = 1
      }

      data class DivergentInjectable(override val candidate: Injectable) : WithCandidate() {
        override val failureOrdering: Int
          get() = 1
      }

      data class ReifiedTypeArgumentMismatch(
        val parameter: ClassifierRef,
        val argument: ClassifierRef,
        override val candidate: Injectable
      ) : WithCandidate() {
        override val failureOrdering: Int
          get() = 1
      }

      data class ScopeNotFound(
        override val candidate: Injectable,
        val scopeComponent: TypeRef
      ) : WithCandidate() {
        override val failureOrdering: Int
          get() = 1
      }

      data class DependencyFailure(
        override val candidate: Injectable,
        val dependencyRequest: InjectableRequest,
        val dependencyFailure: Failure,
      ) : WithCandidate() {
        override val failureOrdering: Int
          get() = 1
      }
    }

    data class CandidateAmbiguity(
      val request: InjectableRequest,
      val candidateResults: List<Success.WithCandidate.Value>
    ) : Failure() {
      override val failureOrdering: Int
        get() = 0
    }

    data class NoCandidates(
      val scope: InjectablesScope,
      val request: InjectableRequest
    ) : Failure() {
      override val failureOrdering: Int
        get() = 2
    }
  }
}

private fun InjectablesScope.canSeeInjectablesOf(other: InjectablesScope): Boolean =
  other in allScopes

private fun Injectable.scopeComponentOrNull(scope: InjectablesScope): InjectablesScope? =
  scopeComponentType?.let {
    scope.allScopes.last { it.componentType == scopeComponentType }
  }

data class UsageKey(
  val key: Any,
  val type: KClass<out Injectable>,
  val highestScope: InjectablesScope
)

fun InjectablesScope.resolveRequests(
  callee: CallableRef,
  requests: List<InjectableRequest>,
  lookupLocation: LookupLocation,
  onEachResult: (InjectableRequest, ResolutionResult) -> Unit
): InjectionGraph {
  recordLookup(lookupLocation)
  val successes = mutableMapOf<InjectableRequest, ResolutionResult.Success>()
  var failureRequest: InjectableRequest? = null
  var failure: ResolutionResult.Failure? = null
  for (request in requests) {
    when (val result = resolveRequest(request, lookupLocation, false)) {
      is ResolutionResult.Success -> successes[request] = result
      is ResolutionResult.Failure ->
        if (request.isRequired || result.unwrapDependencyFailure() is ResolutionResult.Failure.CandidateAmbiguity) {
          if (compareResult(result, failure) < 0) {
            failureRequest = request
            failure = result
          }
        } else {
          successes[request] = ResolutionResult.Success.DefaultValue
        }
    }
  }
  val usages = mutableMapOf<UsageKey, MutableSet<InjectableRequest>>()
  return if (failure == null) InjectionGraph.Success(
    this,
    callee,
    successes,
    usages
  ).also { it.postProcess(onEachResult, usages) }
  else {
    val unwrappedFailure = failure.unwrapDependencyFailure()
    val importSuggestions = if (unwrappedFailure is ResolutionResult.Failure.NoCandidates &&
       !unwrappedFailure.request.type.hasErrors)
         unwrappedFailure.scope.computeImportSuggestions(unwrappedFailure.request, lookupLocation)
    else emptyList()
    InjectionGraph.Error(
      this,
      callee,
      failureRequest!!,
      failure,
      importSuggestions
    )
  }
}

private fun InjectablesScope.resolveRequest(
  request: InjectableRequest,
  lookupLocation: LookupLocation,
  fromTypeScope: Boolean
): ResolutionResult {
  if (request.type.hasErrors)
    return ResolutionResult.Failure.NoCandidates(this, request)

  resultsByType[request.type]?.let { return it }

  val result: ResolutionResult = tryToResolveRequestWithUserInjectables(request, lookupLocation)
    .let { userResult ->
      if (userResult is ResolutionResult.Success ||
          userResult is ResolutionResult.Failure.CandidateAmbiguity)
            userResult
      else if (!fromTypeScope) {
        tryToResolveRequestInTypeScope(request, lookupLocation)
          ?.takeUnless { it is ResolutionResult.Failure.NoCandidates }
          .let { typeScopeResult ->
            when (typeScopeResult) {
              is ResolutionResult.Failure.CandidateAmbiguity -> typeScopeResult
              is ResolutionResult.Failure.WithCandidate.DivergentInjectable -> userResult
              else -> if (compareResult(userResult, typeScopeResult) < 0) userResult else typeScopeResult
            }
          }
          ?: tryToResolveRequestWithFrameworkInjectable(request, lookupLocation)
          ?: userResult
      } else if (request.type.unwrapTags().isComponent())
        tryToResolveRequestWithFrameworkInjectable(request, lookupLocation)
          ?: userResult
      else
        userResult
    } ?: ResolutionResult.Failure.NoCandidates(this, request)

  resultsByType[request.type] = result
  return result
}

private fun InjectablesScope.tryToResolveRequestWithUserInjectables(
  request: InjectableRequest,
  lookupLocation: LookupLocation
): ResolutionResult? = injectablesForRequest(request, this)
  .takeIf { it.isNotEmpty() }
  ?.let { resolveCandidates(request, it, lookupLocation) }

private fun InjectablesScope.tryToResolveRequestInTypeScope(
  request: InjectableRequest,
  lookupLocation: LookupLocation
): ResolutionResult? {
  // try the type scope if the requested type is not a framework type
  return if (!request.type.isFunctionType &&
    request.type.classifier != ctx.listClassifier &&
    request.type.classifier.fqName != injektFqNames().typeKey &&
    request.type.classifier.fqName != injektFqNames().sourceKey)
      with(TypeInjectablesScope(request.type, this)) {
        recordLookup(lookupLocation)
        resolveRequest(request, lookupLocation, true)
      }
  else null
}

private fun InjectablesScope.tryToResolveRequestWithFrameworkInjectable(
  request: InjectableRequest,
  lookupLocation: LookupLocation
): ResolutionResult? =
  frameworkInjectableForRequest(request)?.let { resolveCandidate(request, it, lookupLocation) }

private fun InjectablesScope.computeForCandidate(
  request: InjectableRequest,
  candidate: Injectable,
  compute: () -> ResolutionResult,
): ResolutionResult {
  resultsByCandidate[candidate]?.let { return it }
  val isAbstractInjectable = candidate.originalType.isAbstractInjectable()
  if (candidate.dependencies.isEmpty() && !isAbstractInjectable)
    return compute().also { resultsByCandidate[candidate] = it }

  val abstractInjectable = if (!isAbstractInjectable) null
  else chain.lastOrNull {
    it.first.type == request.type &&
        it.second is AbstractInjectable
  }?.second.safeAs<AbstractInjectable>()

  if (chain.isNotEmpty()) {
    var isLazy = false
    for (i in chain.lastIndex downTo 0) {
      val prev = chain[i]
      isLazy = isLazy || prev.first.isLazy

      if ((prev.second.callableFqName == candidate.callableFqName &&
        prev.second.type.coveringSet == candidate.type.coveringSet &&
        (prev.second.type.typeSize < candidate.type.typeSize ||
            (prev.second.type == candidate.type && (!isLazy || prev.first.isInline)))) ||
        (abstractInjectable != null &&
            request.parameterIndex == DISPATCH_RECEIVER_INDEX &&
            prev.second.safeAs<CallableInjectable>()?.callable?.callable?.uniqueKey() in
            abstractInjectable.requestCallables.map { it.callable.uniqueKey() })
      ) {
        val result = ResolutionResult.Failure.WithCandidate.DivergentInjectable(candidate)
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

private fun InjectablesScope.resolveCandidates(
  request: InjectableRequest,
  candidates: List<Injectable>,
  lookupLocation: LookupLocation
): ResolutionResult {
  if (candidates.size == 1) {
    val candidate = candidates.single()
    return resolveCandidate(request, candidate, lookupLocation)
  }

  val successes = mutableListOf<ResolutionResult.Success>()
  var failure: ResolutionResult.Failure? = null
  val remaining = candidates
    .sortedWithValidation { a, b -> compareCandidate(a, b) }
    .distinctBy {
      if (it is CallableInjectable) it.callable.callable.uniqueKey()
      else it
    }
    .toCollection(LinkedList())
  while (remaining.isNotEmpty()) {
    val candidate = remaining.removeFirst()
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

  return when {
    successes.size == 1 -> successes.single()
    successes.isNotEmpty() -> ResolutionResult.Failure.CandidateAmbiguity(request, successes.cast())
    else -> failure!!
  }
}

private fun InjectablesScope.resolveCandidate(
  request: InjectableRequest,
  candidate: Injectable,
  lookupLocation: LookupLocation
): ResolutionResult = computeForCandidate(request, candidate) {
  if (!callContext.canCall(candidate.callContext))
    return@computeForCandidate ResolutionResult.Failure.WithCandidate.CallContextMismatch(callContext, candidate)

  if (candidate.scopeComponentType != null &&
      allScopes.none { it.componentType == candidate.scopeComponentType })
        return@computeForCandidate ResolutionResult.Failure.WithCandidate.ScopeNotFound(
          candidate, candidate.scopeComponentType!!)

  if (candidate is AbstractInjectable) {
    candidate.requestCallables
      .forEach { callable ->
        candidate.requestCallables
          .filter { it != callable }
          .forEach { other ->
            if (callable.clashesWith(other)) {
              return@computeForCandidate ResolutionResult.Failure.WithCandidate.ClashingSuperTypes(
                callable.parameterTypes[DISPATCH_RECEIVER_INDEX]!!,
                other.parameterTypes[DISPATCH_RECEIVER_INDEX]!!,
                candidate
              )
            }
          }
      }
  }

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
    return@computeForCandidate ResolutionResult.Success.WithCandidate.Value(
      candidate,
      this,
      emptyMap()
    )

  val successDependencyResults = mutableMapOf<InjectableRequest, ResolutionResult.Success>()
  for (dependency in candidate.dependencies) {
    val dependencyScope = candidate.dependencyScopes[dependency] ?:
      candidate.scopeComponentType?.let {
        allScopes.last { it.componentType == candidate.scopeComponentType }
      } ?: this
    when (val dependencyResult = dependencyScope.resolveRequest(dependency, lookupLocation, false)) {
      is ResolutionResult.Success -> successDependencyResults[dependency] = dependencyResult
      is ResolutionResult.Failure -> {
        when {
          dependency.isRequired && candidate is ProviderInjectable &&
              dependencyResult is ResolutionResult.Failure.NoCandidates ->
            return@computeForCandidate ResolutionResult.Failure.NoCandidates(dependencyScope, dependency)
          dependency.isRequired ||
              dependencyResult.unwrapDependencyFailure() is ResolutionResult.Failure.CandidateAmbiguity ||
              (dependency.failOnAllCandidateErrors &&
                  dependencyResult is ResolutionResult.Failure.WithCandidate.DependencyFailure) ->
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
  return@computeForCandidate ResolutionResult.Success.WithCandidate.Value(
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

private fun InjectablesScope.compareCandidate(a: Injectable?, b: Injectable?): Int {
  if (a === b) return 0

  if (a != null && b == null) return -1
  if (b != null && a == null) return 1

  a!!
  b!!

  val aIsFromTypeScope = a.ownerScope.typeScopeType != null
  val bIsFromTypeScope = b.ownerScope.typeScopeType != null
  if (!aIsFromTypeScope && bIsFromTypeScope) return -1
  if (!bIsFromTypeScope && aIsFromTypeScope) return 1

  val aScopeNesting = a.ownerScope.nesting
  val bScopeNesting = b.ownerScope.nesting
  if (aScopeNesting > bScopeNesting) return -1
  if (bScopeNesting > aScopeNesting) return 1

  return compareCallable(
    a.safeAs<CallableInjectable>()?.callable,
    b.safeAs<CallableInjectable>()?.callable,
    false
  )
}

private fun InjectablesScope.compareCallable(
  a: CallableRef?,
  b: CallableRef?,
  compareCompilation: Boolean
): Int {
  if (a == b) return 0

  if (a != null && b == null) return -1
  if (b != null && a == null) return 1
  a!!
  b!!

  if (compareCompilation) {
    val isAExternal = a.callable.isExternalDeclaration()
    val isBExternal = b.callable.isExternalDeclaration()
    if (!isAExternal && isBExternal) return -1
    if (!isBExternal && isAExternal) return 1
  }

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
  if (pair in comparedTypes) return 0
  comparedTypes += pair

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
    val aSubTypeOfB = a.isSubTypeOf(b)
    val bSubTypeOfA = b.isSubTypeOf(a)
    if (aSubTypeOfB && !bSubTypeOfA) return -1
    if (bSubTypeOfA && !aSubTypeOfB) return 1
    val aCommonSuperType = commonSuperType(a.superTypes)
    val bCommonSuperType = commonSuperType(b.superTypes)
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

private fun InjectionGraph.Success.postProcess(
  onEachResult: (InjectableRequest, ResolutionResult) -> Unit,
  usages: MutableMap<UsageKey, MutableSet<InjectableRequest>>
) {
  visitRecursive { request, result ->
    if (result is ResolutionResult.Success.WithCandidate.Value)
      usages.getOrPut(result.usageKey) { mutableSetOf() } += request
    onEachResult(request, result)
  }
}

fun ResolutionResult.visitRecursive(
  request: InjectableRequest,
  action: (InjectableRequest, ResolutionResult) -> Unit
) {
  action(request, this)
  if (this is ResolutionResult.Success.WithCandidate.Value) {
    dependencyResults
      .forEach { (request, result) ->
        result.visitRecursive(request, action)
      }
  }
}

private fun CallableRef.clashesWith(other: CallableRef, @Inject ctx: Context): Boolean {
  if (callable.name != other.callable.name) return false
  if (parameterTypes.size != other.parameterTypes.size) return false
  if (callable is PropertyDescriptor != other.callable is PropertyDescriptor)
    return false

  val receiver = parameterTypes[DISPATCH_RECEIVER_INDEX]!!
  val otherReceiver = other.parameterTypes[DISPATCH_RECEIVER_INDEX]!!
  if (receiver.isSubTypeOf(otherReceiver) || otherReceiver.isSubTypeOf(receiver))
    return false

  if (type.isSubTypeOf(other.type) || other.type.isSubTypeOf(type))
    return false

  return true
}

fun InjectionGraph.visitRecursive(action: (InjectableRequest, ResolutionResult) -> Unit) {
  val results = when (this) {
    is InjectionGraph.Success -> results
    is InjectionGraph.Error -> mapOf(failureRequest to failure)
  }

  results
    .forEach { (request, result) ->
      result.visitRecursive(request, action)
    }
}



private fun InjectablesScope.computeImportSuggestions(
  failureRequest: InjectableRequest,
  lookupLocation: LookupLocation
): List<CallableRef> {
  val candidates = collectImportSuggestionInjectables()
    .filter {
      it.type.buildContext(failureRequest.type, allStaticTypeParameters).isOk
    }
    .sortedWithValidation { a, b -> compareCallable(a, b, true) }

  val successes = mutableListOf<ResolutionResult.Success>()
  val remaining = candidates.toCollection(LinkedList())
  while (remaining.isNotEmpty()) {
    if (successes.size >= 10) break

    val candidate = remaining.removeFirst()
    val scope = ImportSuggestionInjectablesScope(this, candidate)

    if (compareCallable(
        successes.firstOrNull()
          ?.safeAs<ResolutionResult.Success.WithCandidate>()
          ?.candidate?.safeAs<CallableInjectable>()?.callable,
        candidate,
        true
      ) < 0
    ) {
      // we cannot get a better result
      break
    }

    val candidateResult = scope.resolveRequest(failureRequest, lookupLocation, false)
    if (candidateResult is ResolutionResult.Success) {
      val firstSuccessResult = successes.firstOrNull()
      when (compareResult(candidateResult, firstSuccessResult)) {
        -1 -> {
          successes.clear()
          successes += candidateResult
        }
        0 -> successes += candidateResult
      }
    }
  }

  return successes
    .mapNotNull {
      it.cast<ResolutionResult.Success.WithCandidate.Value>()
        .candidate.safeAs<CallableInjectable>()
        ?.callable
    }
    .takeIf { it.isNotEmpty() }
    ?: candidates
      .take(10)
}

private fun ResolutionResult.Failure.unwrapDependencyFailure(): ResolutionResult.Failure =
  if (this is ResolutionResult.Failure.WithCandidate.DependencyFailure)
    dependencyFailure.unwrapDependencyFailure()
  else this

private fun <T> List<T>.sortedWithValidation(comparator: Comparator<T>) = try {
  sortedWith(comparator)
} catch (e: IllegalArgumentException) {
  comparator.validate(this)
  throw e
}

private fun <T> Comparator<T>.validate(candidates: List<T>) {
  for (x in candidates) {
    for (y in candidates) {
      val cmpXY = compare(x, y).sign
      val cmpYX = compare(y, x).sign
      for (z in candidates) {
        val cmpXZ = compare(x, z).sign
        val cmpYZ = compare(y, z).sign

        if (cmpXY != -cmpYX) {
          error(
            """signum(cmp(x, y)) == -signum(cmp(y, x)) given:
                 |x = $x
                 |y = $y
                 |cmpXY = $cmpXY
                 |cmpYX = $cmpYX"""
          )
        }

        if (cmpXY != 0 && cmpXY == cmpYZ && cmpXZ != cmpXY) {
          error(
            """transitivity given:
                 |x = $x
                 |y = $y
                 |z = $z
                 |cmpXY = $cmpXY
                 |cmpXZ = $cmpXZ
                 |cmpYZ = $cmpYZ"""
          )
        }

        if (cmpXY == 0 && cmpXZ != cmpYZ) {
          error(
            """cmp(x, y) == 0 implies that signum(cmp(x, z)) == signum(cmp(y, z)) given:
                 |x = $x
                 |y = $y
                 |z = $z
                 |cmpXY = $cmpXY
                 |cmpXZ = $cmpXZ
                 |cmpYZ = $cmpYZ"""
          )
        }
      }
    }
  }
}
