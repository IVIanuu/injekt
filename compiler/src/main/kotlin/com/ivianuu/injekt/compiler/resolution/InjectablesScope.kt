/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.nextFrameworkKey
import com.ivianuu.injekt.compiler.uniqueKey
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
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

  data class InjectableKey(
    val uniqueKey: String,
    val originalType: TypeRef,
    val typeArguments: Map<ClassifierRef, TypeRef>,
    val parameterTypes: Map<Int, TypeRef>
  ) {
    constructor(callable: CallableRef, ctx: Context) : this(
      callable.callable.uniqueKey(ctx),
      callable.originalType,
      callable.typeArguments,
      callable.parameterTypes
    )
  }

  private val spreadingInjectables = mutableListOf<SpreadingInjectable>()
  private val spreadingInjectableKeys: MutableSet<InjectableKey> =
    parent?.spreadingInjectableKeys?.toMutableSet() ?: mutableSetOf()
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
        addInjectable = { callable ->
          injectables += callable
          val typeWithFrameworkKey = callable.type
            .copy(frameworkKey = callable.callable.uniqueKey(ctx))
          if (callable.type.frameworkKey != typeWithFrameworkKey.frameworkKey)
            injectables += callable.copy(type = typeWithFrameworkKey)
          spreadingInjectableCandidateTypes += typeWithFrameworkKey
        },
        addSpreadingInjectable = { callable ->
          if (spreadingInjectableKeys.add(InjectableKey(callable, ctx)))
            spreadingInjectables += SpreadingInjectable(callable)
        },
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
    request.type.isFunctionType -> LambdaInjectable(
      type = request.type,
      ownerScope = this
    )
    request.type.classifier == ctx.listClassifier -> {
      val singleElementType = request.type.arguments[0]
      val collectionElementType = ctx.collectionClassifier.defaultType
        .withArguments(listOf(singleElementType))

      val key = CallableRequestKey(request.type, allStaticTypeParameters)

      val elements = listElementsForType(singleElementType, collectionElementType, key)
        .values.map { it.type }

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

  private fun listElementsForType(
    singleElementType: TypeRef,
    collectionElementType: TypeRef,
    key: CallableRequestKey
  ): Map<InjectableKey, CallableRef> {
    if (injectables.isEmpty())
      return parent?.listElementsForType(singleElementType, collectionElementType, key) ?: emptyMap()

    return buildMap {
      parent?.listElementsForType(singleElementType, collectionElementType, key)
        ?.let { parentElements ->
          for ((candidateKey, candidate) in parentElements)
            put(candidateKey, candidate)
        }

      for (candidate in injectables.toList()) {
        if (candidate.type.frameworkKey != key.type.frameworkKey) continue

        var context =
          candidate.type.buildContext(singleElementType, key.staticTypeParameters, ctx = ctx)
        if (!context.isOk)
          context = candidate.type.buildContext(collectionElementType, key.staticTypeParameters, ctx = ctx)
        if (!context.isOk) continue

        val substitutedCandidate = candidate.substitute(context.fixedTypeVariables)

        val typeWithFrameworkKey = substitutedCandidate.type.copy(
          frameworkKey = UUID.randomUUID().toString()
        )

        val finalCandidate = substitutedCandidate.copy(type = typeWithFrameworkKey)

        injectables += finalCandidate

        this[InjectableKey(finalCandidate, ctx)] = finalCandidate
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

    val newInjectableType = spreadingInjectable.callable.type
      .substitute(context.fixedTypeVariables)
      .copy(frameworkKey = "")
    val newInjectable = spreadingInjectable.callable
      .copy(
        type = newInjectableType,
        originalType = newInjectableType,
        parameterTypes = spreadingInjectable.callable.parameterTypes
          .mapValues { it.value.substitute(context.fixedTypeVariables) },
        typeArguments = spreadingInjectable.callable
          .typeArguments
          .mapValues { it.value.substitute(context.fixedTypeVariables) }
      )

    newInjectable.collectInjectables(
      scope = this,
      addInjectable = { innerCallable ->
        val finalInnerCallable = innerCallable
          .copy(originalType = innerCallable.type)
        injectables += finalInnerCallable
        val innerCallableWithFrameworkKey = finalInnerCallable.copy(
          type = finalInnerCallable.type.copy(
            frameworkKey = spreadingInjectable.callable.type.frameworkKey
              .nextFrameworkKey(finalInnerCallable.callable.uniqueKey(ctx))
              .also { spreadingInjectable.resultingFrameworkKeys += it }
          )
        )
        injectables += innerCallableWithFrameworkKey
        spreadingInjectableCandidateTypes += innerCallableWithFrameworkKey.type
        spreadInjectables(innerCallableWithFrameworkKey.type)
      },
      addSpreadingInjectable = { newInnerCallable ->
        val finalNewInnerInjectable = newInnerCallable
          .copy(originalType = newInnerCallable.type)
        if (spreadingInjectableKeys.add(InjectableKey(finalNewInnerInjectable, ctx))) {
          val newSpreadingInjectable = SpreadingInjectable(finalNewInnerInjectable)
          spreadingInjectables += newSpreadingInjectable
          for (candidate in spreadingInjectableCandidateTypes.toList())
            spreadInjectables(newSpreadingInjectable, candidate)
        }
      },
      ctx = ctx
    )
  }

  override fun toString(): String = "InjectablesScope($name)"
}
