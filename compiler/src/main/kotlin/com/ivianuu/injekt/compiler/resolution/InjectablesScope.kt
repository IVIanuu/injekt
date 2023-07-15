/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.psi.KtElement
import java.util.UUID

class InjectablesScope(
  val name: String,
  val parent: InjectablesScope?,
  val owner: KtElement? = null,
  val initialInjectables: List<CallableRef> = emptyList(),
  val injectablesPredicate: (CallableRef) -> Boolean = { true },
  val typeParameters: List<ClassifierRef> = emptyList(),
  val nesting: Int = parent?.nesting?.inc() ?: 0,
  val ctx: Context
) {
  val chain: MutableList<Pair<InjectableRequest, Injectable>> = parent?.chain ?: mutableListOf()
  val resultsByType = mutableMapOf<TypeRef, ResolutionResult>()
  val resultsByCandidate = mutableMapOf<Injectable, ResolutionResult>()

  private val injectables = mutableListOf<CallableRef>()

  private val spreadingInjectables = mutableListOf<SpreadingInjectable>()
  private val spreadingInjectableCandidateTypes = mutableListOf<TypeRef>()

  data class SpreadingInjectable(
    val callable: CallableRef,
    val constraintType: TypeRef = callable.typeParameters.single {
      it.isSpread
    }.defaultType.substitute(callable.typeArguments),
    val processedCandidateTypes: MutableSet<TypeRef> = mutableSetOf(),
    val resultingFrameworkKeys: MutableSet<String> = mutableSetOf()
  ) {
    fun copy() = SpreadingInjectable(
      callable,
      constraintType,
      processedCandidateTypes.toMutableSet(),
      resultingFrameworkKeys.toMutableSet()
    )
  }

  val allScopes: List<InjectablesScope> = parent?.allScopes?.let { it + this } ?: listOf(this)

  val allStaticTypeParameters = allScopes.flatMap { it.typeParameters }

  data class CallableRequestKey(val type: TypeRef, val staticTypeParameters: List<ClassifierRef>)
  private val injectablesByRequest = mutableMapOf<CallableRequestKey, List<CallableInjectable>>()

  init {
    for (injectable in initialInjectables)
      injectable.collectInjectables(
        scope = this,
        addInjectable = { next, unique ->
          injectables += next
          if (unique)
            spreadingInjectableCandidateTypes += next.type
        },
        addSpreadingInjectable = { spreadingInjectables += SpreadingInjectable(it) },
        ctx = ctx
      )

    val hasSpreadingInjectables = spreadingInjectables.isNotEmpty()
    val hasSpreadingInjectableCandidates = spreadingInjectableCandidateTypes.isNotEmpty()
    if (parent != null) {
      spreadingInjectables.addAll(
        0,
        // we only need to copy the parent injectables if we have any candidates to process
        if (hasSpreadingInjectableCandidates) parent.spreadingInjectables.map { it.copy() }
        else parent.spreadingInjectables
      )
      spreadingInjectableCandidateTypes.addAll(0, parent.spreadingInjectableCandidateTypes)
    }

    // only run if there is something meaningful to process
    if ((hasSpreadingInjectables && spreadingInjectableCandidateTypes.isNotEmpty()) ||
      (hasSpreadingInjectableCandidates && spreadingInjectables.isNotEmpty())
    ) {
      spreadingInjectableCandidateTypes
        .toList()
        .forEach { spreadInjectables(it) }
    }
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
          if (candidate.type.frameworkKey != key.type.frameworkKey) continue
          val context = candidate.type.buildContext(key.type, key.staticTypeParameters, ctx = ctx)
          if (!context.isOk) continue
          this += CallableInjectable(
            this@InjectablesScope,
            candidate.substitute(context.fixedTypeVariables)
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
    request.type.classifier.fqName == InjektFqNames.TypeKey ->
      TypeKeyInjectable(request.type, this)
    request.type.classifier.fqName == InjektFqNames.SourceKey ->
      SourceKeyInjectable(request.type, this)
    else -> null
  }

  private fun listElementsTypesForType(
    singleElementType: TypeRef,
    collectionElementType: TypeRef,
    key: CallableRequestKey
  ): List<TypeRef> {
    if (injectables.isEmpty())
      return parent?.listElementsTypesForType(singleElementType, collectionElementType, key) ?: emptyList()

    return buildList {
      parent?.listElementsTypesForType(singleElementType, collectionElementType, key)
        ?.let { addAll(it) }

      for (candidate in injectables.toList()) {
        if (candidate.type.frameworkKey.isNotEmpty()) continue

        var context =
          candidate.type.buildContext(singleElementType, key.staticTypeParameters, ctx = ctx)
        if (!context.isOk)
          context = candidate.type.buildContext(collectionElementType, key.staticTypeParameters, ctx = ctx)
        if (!context.isOk) continue

        val substitutedCandidate = candidate.substitute(context.fixedTypeVariables)

        val typeWithFrameworkKey = substitutedCandidate.type.copy(
          frameworkKey = UUID.randomUUID().toString()
        )

        injectables += substitutedCandidate.copy(type = typeWithFrameworkKey)

        add(typeWithFrameworkKey)
      }
    }
  }

  private fun spreadInjectables(candidateType: TypeRef) {
    for (spreadingInjectable in spreadingInjectables.toList())
      spreadInjectables(spreadingInjectable, candidateType)
  }

  private fun spreadInjectables(spreadingInjectable: SpreadingInjectable, candidateType: TypeRef) {
    if (candidateType.frameworkKey in spreadingInjectable.resultingFrameworkKeys) return
    if (!spreadingInjectable.processedCandidateTypes.add(candidateType)) return

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

    substitutedInjectable.collectInjectables(
      scope = this,
      addInjectable = { next, unique ->
        injectables += next
        if (unique) {
          spreadingInjectableCandidateTypes += next.type
          spreadInjectables(next.type)
        }
      },
      addSpreadingInjectable = { next ->
        val newSpreadingInjectable = SpreadingInjectable(next)
        spreadingInjectables += newSpreadingInjectable
        for (candidate in spreadingInjectableCandidateTypes.toList())
          spreadInjectables(newSpreadingInjectable, candidate)
      },
      ctx = ctx
    )
  }

  override fun toString(): String = "InjectablesScope($name)"
}
