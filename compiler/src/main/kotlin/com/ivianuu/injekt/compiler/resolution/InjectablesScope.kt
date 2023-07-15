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

  val allScopes: List<InjectablesScope> = parent?.allScopes?.let { it + this } ?: listOf(this)

  val allStaticTypeParameters = allScopes.flatMap { it.typeParameters }

  data class CallableRequestKey(val type: TypeRef, val staticTypeParameters: List<ClassifierRef>)
  private val injectablesByRequest = mutableMapOf<CallableRequestKey, List<CallableInjectable>>()

  init {
    for (injectable in initialInjectables)
      injectable.collectInjectables(
        scope = this,
        addInjectable = { injectables += it },
        ctx = ctx
      )
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
          for (result in buildContextForCandidate(candidate, key.type, key.staticTypeParameters))
            this += CallableInjectable(
              this@InjectablesScope,
              candidate.substitute(result)
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
        for (result in buildContextForCandidate(candidate, singleElementType, key.staticTypeParameters) +
            buildContextForCandidate(candidate, collectionElementType, key.staticTypeParameters)) {
          val substitutedCandidate = candidate.substitute(result)
          val uniqueType = substitutedCandidate.type.copy(frameworkKey = UUID.randomUUID().toString())
          injectables += substitutedCandidate.copy(type = uniqueType)
          add(uniqueType)
        }
      }
    }
  }

  private fun buildContextForCandidate(
    candidate: CallableRef,
    type: TypeRef,
    staticTypeParameters: List<ClassifierRef>
  ): List<Map<ClassifierRef, TypeRef>> = if (candidate.constraintType == null) {
    listOfNotNull(
      candidate.type.buildContext(type, staticTypeParameters, ctx = ctx)
        .takeIf { it.isOk }
        ?.fixedTypeVariables
    )
  } else {
    allScopes
      .flatMap { it.injectables }
      .mapNotNull { constraintCandidate ->
        val constraintResult = buildContextForSpreadingInjectable(
          candidate.constraintType,
          constraintCandidate.type,
          staticTypeParameters,
          ctx
        ).takeIf { it.isOk }
          ?.fixedTypeVariables
          ?: return@mapNotNull null

        val result = candidate.type.substitute(constraintResult)
          .buildContext(type, staticTypeParameters, ctx = ctx)
          .takeIf { it.isOk }
          ?.fixedTypeVariables
          ?: return@mapNotNull null

        constraintResult + result
      }
  }

  override fun toString(): String = "InjectablesScope($name)"
}
