/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.psi.*

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
  val resolutionChain: MutableList<Injectable> = parent?.resolutionChain ?: mutableListOf()
  val resultsByType = mutableMapOf<TypeRef, ResolutionResult>()
  val resultsByCandidate = mutableMapOf<Injectable, ResolutionResult>()

  private val injectables = mutableListOf<CallableRef>()
  private val allInjectables get() = allScopes.flatMap { injectables }

  private val addOnInjectables: MutableList<AddOnInjectable> =
    parent?.addOnInjectables?.mapTo(mutableListOf()) { it.copy() } ?: mutableListOf()

  private val addOnInjectableChain: MutableList<AddOnInjectable> =
    parent?.addOnInjectables ?: mutableListOf()

  data class AddOnInjectable(
    val callable: CallableRef,
    val constraintType: TypeRef = callable.typeParameters.single {
      it.isAddOn
    }.defaultType.substitute(callable.typeArguments),
    val processedCandidateTypes: MutableSet<TypeRef> = mutableSetOf()
  ) {
    fun copy() = AddOnInjectable(
      callable,
      constraintType,
      processedCandidateTypes.toMutableSet()
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
        addInjectable = { injectables += it },
        addAddOnInjectable = { addOnInjectables += AddOnInjectable(it) },
        ctx = ctx
      )

    addOnInjectables.toList().forEach { collectAddOnInjectables(it) }
  }

  fun injectablesForRequest(
    request: InjectableRequest,
    requestingScope: InjectablesScope
  ): List<Injectable> {
    // we return merged collections
    if (request.type.uniqueId == null &&
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
          if (key.type.uniqueId != null && candidate.type.uniqueId != key.type.uniqueId) continue
          val context = candidate.type.runCandidateInference(key.type, key.staticTypeParameters, ctx = ctx)
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

  fun builtInInjectableForRequest(request: InjectableRequest): Injectable? = when {
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
        var context =
          candidate.type.runCandidateInference(singleElementType, key.staticTypeParameters, ctx = ctx)
        if (!context.isOk)
          context = candidate.type.runCandidateInference(collectionElementType, key.staticTypeParameters, ctx = ctx)
        if (!context.isOk) continue

        val substitutedCandidate = candidate.substitute(context.fixedTypeVariables)

        add(substitutedCandidate.type)
      }
    }
  }

  private fun collectAddOnInjectables(addOnInjectable: AddOnInjectable) {
    for (candidate in allInjectables.toList())
      collectAddOnInjectables(addOnInjectable, candidate.type)
  }

  private fun collectAddOnInjectables(candidateType: TypeRef) {
    for (addOnInjectable in addOnInjectables.toList())
      collectAddOnInjectables(addOnInjectable, candidateType)
  }

  private fun collectAddOnInjectables(addOnInjectable: AddOnInjectable, candidateType: TypeRef) {
    if (!addOnInjectable.processedCandidateTypes.add(candidateType) ||
      addOnInjectable in addOnInjectableChain) return

    val context = runAddOnInjectableInference(
      addOnInjectable.constraintType,
      candidateType,
      allStaticTypeParameters,
      ctx
    )
    if (!context.isOk) return

    val substitutedInjectable = addOnInjectable.callable.substitute(context.fixedTypeVariables)
    addOnInjectableChain += addOnInjectable

    substitutedInjectable.collectInjectables(
      scope = this,
      addInjectable = { next ->
        injectables += next
        collectAddOnInjectables(next.type)
      },
      addAddOnInjectable = { next ->
        AddOnInjectable(next)
          .also { collectAddOnInjectables(it) }
      },
      ctx = ctx
    )

    addOnInjectableChain -= addOnInjectable
  }

  override fun toString(): String = "InjectablesScope($name)"
}
