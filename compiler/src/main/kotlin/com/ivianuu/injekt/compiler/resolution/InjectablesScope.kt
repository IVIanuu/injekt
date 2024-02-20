/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*

class InjectablesScope(
  val name: String,
  val parent: InjectablesScope?,
  val owner: FirElement? = null,
  val initialInjectables: List<InjektCallable> = emptyList(),
  val injectablesPredicate: (InjektCallable) -> Boolean = { true },
  val typeParameters: List<FirTypeParameterSymbol> = emptyList(),
  val nesting: Int = parent?.nesting?.inc() ?: 0,
  val session: FirSession
) {
  val resolutionChain: MutableList<Injectable> = parent?.resolutionChain ?: mutableListOf()
  val resultsByType = mutableMapOf<ConeKotlinType, ResolutionResult>()
  val resultsByCandidate = mutableMapOf<Injectable, ResolutionResult>()

  private val injectables = mutableListOf<InjektCallable>()
  private val allInjectables get() = allScopes.flatMap { injectables }

  private val spreadingInjectables: MutableList<SpreadingInjectable> =
    parent?.spreadingInjectables?.mapTo(mutableListOf()) { it.copy() } ?: mutableListOf()

  private val spreadingInjectableChain: MutableList<SpreadingInjectable> =
    parent?.spreadingInjectables ?: mutableListOf()

  inner class SpreadingInjectable(
    val callable: InjektCallable,
    val constraintType: ConeKotlinType = callable.typeArguments.keys.single {
      it.hasAnnotation(InjektFqNames.Spread, session)
    }.toConeType()/*.substitute(callable.typeArguments) TODO*/,
    val processedCandidateTypes: MutableSet<ConeKotlinType> = mutableSetOf()
  ) {
    fun copy() = SpreadingInjectable(
      callable,
      constraintType,
      processedCandidateTypes.toMutableSet()
    )
  }

  val allScopes: List<InjectablesScope> = parent?.allScopes?.let { it + this } ?: listOf(this)

  val allStaticTypeParameters = allScopes.flatMap { it.typeParameters }

  data class CallableRequestKey(val type: ConeKotlinType, val staticTypeParameters: List<FirTypeParameterSymbol>)
  private val injectablesByRequest = mutableMapOf<CallableRequestKey, List<CallableInjectable>>()

  init {
    for (injectable in initialInjectables)
      injectable.collectInjectables(
        scope = this,
        addInjectable = { injectables += it },
        addSpreadingInjectable = { spreadingInjectables += SpreadingInjectable(it) },
        session = session
      )

    spreadingInjectables.toList().forEach { spreadInjectables(it) }
  }

  fun injectablesForRequest(
    request: InjectableRequest,
    requestingScope: InjectablesScope
  ): List<Injectable> {
    // we return merged collections
    if (request.type.frameworkKey == null &&
      request.type.isSubtypeOf(session.listType, session)) return emptyList()

    return injectablesForType(
      CallableRequestKey(request.type, requestingScope.allStaticTypeParameters)
    ).filter { injectable -> allScopes.all { it.injectablesPredicate(injectable.callable) } }
  }

  private fun injectablesForType(key: CallableRequestKey): List<CallableInjectable> {
    if (injectables.isEmpty())
      return parent?.injectablesForType(key) ?: emptyList()
    return injectablesByRequest.getOrPut(key) {
      buildList {
        parent?.injectablesForType(key)?.let { addAll(it) }

        for (candidate in injectables) {
          if (key.type.frameworkKey != null &&
            candidate.type.frameworkKey != key.type.frameworkKey) continue
          val substitutor = candidate.type.testCandidateAndCreateSubstitutor(key.type, key.staticTypeParameters, session = session)
            ?: continue
          this += CallableInjectable(
            this@InjectablesScope,
            candidate.substitute(substitutor),
            key.type
          )
        }
      }
    }
  }

  fun frameworkInjectableForRequest(request: InjectableRequest): Injectable? = when {
    /*request.type.isFunctionType && !request.type.isProvide -> LambdaInjectable(this, request)
    request.type.classifier == ctx.listClassifier -> {
      val singleElementType = request.type.arguments[0]
      val collectionElementType = ctx.collectionClassifier.defaultType
        .withArguments(listOf(singleElementType))

      val key = CallableRequestKey(request.type, allStaticTypeParameters)

      val elements = listElementsTypesForType(singleElementType, collectionElementType, key)

      if (elements.isEmpty()) null
      else ListInjectable(
        type = request.type,
        ownerScope = this,
        elements = elements,
        singleElementType = singleElementType,
        collectionElementType = collectionElementType
      )
    }
    request.type.classifier.fqName == InjektFqNames.TypeKey.asSingleFqName() ->
      TypeKeyInjectable(request.type, this)*/
    else -> null
  }

  private fun listElementsTypesForType(
    singleElementType: ConeKotlinType,
    collectionElementType: ConeKotlinType,
    key: CallableRequestKey
  ): List<ConeKotlinType> {
    if (injectables.isEmpty())
      return parent?.listElementsTypesForType(singleElementType, collectionElementType, key) ?: emptyList()

    return buildList {
      parent?.listElementsTypesForType(singleElementType, collectionElementType, key)
        ?.let { addAll(it) }

      for (candidate in injectables.toList()) {
        val substitutor =
          candidate.type.testCandidateAndCreateSubstitutor(singleElementType, key.staticTypeParameters, session = session)
            ?: candidate.type.testCandidateAndCreateSubstitutor(collectionElementType, key.staticTypeParameters, session = session)
            ?: continue
        val substitutedCandidate = candidate.substitute(substitutor)
        add(substitutedCandidate.type)
      }
    }
  }

  private fun spreadInjectables(spreadingInjectable: SpreadingInjectable) {
    for (candidate in allInjectables.toList())
      spreadInjectables(spreadingInjectable, candidate.type)
  }

  private fun spreadInjectables(spreadingInjectable: SpreadingInjectable, candidateType: ConeKotlinType) {
    /*if (!spreadingInjectable.processedCandidateTypes.add(candidateType) ||
      spreadingInjectable in spreadingInjectableChain) return

    val substitutor = testSpreadCandidateAndCreateSubstitutor(
      spreadingInjectable.constraintType,
      candidateType,
      allStaticTypeParameters,
      session
    ) ?: return

    val substitutedInjectable = spreadingInjectable.callable
      .copy(
        type = spreadingInjectable.callable.type
          .substitute(substitutor.fixedTypeVariables),
        parameterTypes = spreadingInjectable.callable.parameterTypes
          .mapValues { it.value.substitute(substitutor.fixedTypeVariables) },
        typeArguments = spreadingInjectable.callable
          .typeArguments
          .mapValues { it.value.substitute(substitutor.fixedTypeVariables) }
      )

    spreadingInjectableChain += spreadingInjectable

    substitutedInjectable.collectInjectables(
      scope = this,
      addInjectable = { next ->
        injectables += next
        for (innerSpreadingInjectable in spreadingInjectables.toList())
          spreadInjectables(innerSpreadingInjectable, next.type)
      },
      addSpreadingInjectable = { next ->
        val newSpreadingInjectable = SpreadingInjectable(next)
        spreadingInjectables += newSpreadingInjectable
        spreadInjectables(newSpreadingInjectable)
      },
      ctx = ctx
    )

    spreadingInjectableChain -= spreadingInjectable*/
  }

  override fun toString(): String = "InjectablesScope($name)"
}
