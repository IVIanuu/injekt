/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.di.old

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.psi.*

class InjectablesScope(
  val name: String,
  val parent: InjectablesScope?,
  val owner: KtElement? = null,
  val initialInjectables: List<InjektCallable> = emptyList(),
  val injectablesPredicate: (InjektCallable) -> Boolean = { true },
  val typeParameters: List<InjektClassifier> = emptyList(),
  val nesting: Int = parent?.nesting?.inc() ?: 0,
  val ctx: Context
) {
  val resolutionChain: MutableList<Injectable> = parent?.resolutionChain ?: mutableListOf()
  val resultsByType = mutableMapOf<InjektType, ResolutionResult>()
  val resultsByCandidate = mutableMapOf<Injectable, ResolutionResult>()

  private val injectables = mutableListOf<InjektCallable>()
  private val allInjectables get() = allScopes.flatMap { injectables }

  private val spreadingInjectables: MutableList<SpreadingInjectable> =
    parent?.spreadingInjectables?.mapTo(mutableListOf()) { it.copy() } ?: mutableListOf()

  private val spreadingInjectableChain: MutableList<SpreadingInjectable> =
    parent?.spreadingInjectables ?: mutableListOf()

  data class SpreadingInjectable(
    val callable: InjektCallable,
    val constraintType: InjektType = callable.typeParameters.single {
      it.isSpread
    }.defaultType.substitute(callable.typeArguments),
    val processedCandidateTypes: MutableSet<InjektType> = mutableSetOf()
  ) {
    fun copy() = SpreadingInjectable(
      callable,
      constraintType,
      processedCandidateTypes.toMutableSet()
    )
  }

  val allScopes: List<InjectablesScope> = parent?.allScopes?.let { it + this } ?: listOf(this)

  val allStaticTypeParameters = allScopes.flatMap { it.typeParameters }

  data class CallableRequestKey(val type: InjektType, val staticTypeParameters: List<InjektClassifier>)
  private val injectablesByRequest = mutableMapOf<CallableRequestKey, List<CallableInjectable>>()

  init {
    for (injectable in initialInjectables)
      injectable.collectInjectables(
        scope = this,
        addInjectable = { injectables += it },
        addSpreadingInjectable = { spreadingInjectables += SpreadingInjectable(it) },
        ctx = ctx
      )

    spreadingInjectables.toList().forEach { spreadInjectables(it) }
  }

  fun injectablesForRequest(
    request: InjectableRequest,
    requestingScope: InjectablesScope
  ): List<Injectable> {
    // we return merged collections
    if (request.type.frameworkKey.isEmpty() &&
      request.type.classifier == ctx.listClassifier) return emptyList()

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
          if (key.type.frameworkKey.isNotEmpty() &&
            candidate.type.frameworkKey != key.type.frameworkKey) continue
          val context = candidate.type.buildContext(key.type, key.staticTypeParameters, ctx = ctx)
          if (!context.isOk) continue
          this += CallableInjectable(
            this@InjectablesScope,
            candidate.substitute(context.fixedTypeVariables),
            key.type
          )
        }
      }
    }
  }

  fun frameworkInjectableForRequest(request: InjectableRequest): Injectable? = when {
    request.type.isFunctionType && !request.type.isProvide -> LambdaInjectable(this, request)
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
      TypeKeyInjectable(request.type, this)
    else -> null
  }

  private fun listElementsTypesForType(
    singleElementType: InjektType,
    collectionElementType: InjektType,
    key: CallableRequestKey
  ): List<InjektType> {
    if (injectables.isEmpty())
      return parent?.listElementsTypesForType(singleElementType, collectionElementType, key) ?: emptyList()

    return buildList {
      parent?.listElementsTypesForType(singleElementType, collectionElementType, key)
        ?.let { addAll(it) }

      for (candidate in injectables.toList()) {
        var context =
          candidate.type.buildContext(singleElementType, key.staticTypeParameters, ctx = ctx)
        if (!context.isOk)
          context = candidate.type.buildContext(collectionElementType, key.staticTypeParameters, ctx = ctx)
        if (!context.isOk) continue

        val substitutedCandidate = candidate.substitute(context.fixedTypeVariables)

        add(substitutedCandidate.type)
      }
    }
  }

  private fun spreadInjectables(spreadingInjectable: SpreadingInjectable) {
    for (candidate in allInjectables.toList())
      spreadInjectables(spreadingInjectable, candidate.type)
  }

  private fun spreadInjectables(spreadingInjectable: SpreadingInjectable, candidateType: InjektType) {
    if (!spreadingInjectable.processedCandidateTypes.add(candidateType) ||
      spreadingInjectable in spreadingInjectableChain) return

    val context = buildContextForSpreadingInjectable(
      spreadingInjectable.constraintType,
      candidateType,
      allStaticTypeParameters,
      ctx
    )
    if (!context.isOk) return

    val substitutedInjectable = spreadingInjectable.callable
      .copy(
        type = spreadingInjectable.callable.type
          .substitute(context.fixedTypeVariables),
        parameterTypes = spreadingInjectable.callable.parameterTypes
          .mapValues { it.value.substitute(context.fixedTypeVariables) },
        typeArguments = spreadingInjectable.callable
          .typeArguments
          .mapValues { it.value.substitute(context.fixedTypeVariables) }
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

    spreadingInjectableChain -= spreadingInjectable
  }

  override fun toString(): String = "InjectablesScope($name)"
}
