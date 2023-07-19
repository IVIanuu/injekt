/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.replace

class InjectablesScope(
  val name: String,
  val parent: InjectablesScope?,
  val owner: KtElement? = null,
  val initialInjectables: List<CallableRef> = emptyList(),
  val injectablesPredicate: (CallableRef) -> Boolean = { true },
  val typeParameters: List<TypeParameterDescriptor> = emptyList(),
  val nesting: Int = parent?.nesting?.inc() ?: 0,
  val ctx: Context
) {
  val resolutionChain: MutableList<Injectable> = parent?.resolutionChain ?: mutableListOf()
  val resultsByType = mutableMapOf<KotlinTypeKey, ResolutionResult>()
  val resultsByCandidate = mutableMapOf<Injectable, ResolutionResult>()

  private val injectables = mutableListOf<CallableRef>()
  private val allInjectables get() = allScopes.flatMap { injectables }

  private val spreadingInjectables: MutableList<SpreadingInjectable> =
    parent?.spreadingInjectables?.mapTo(mutableListOf()) { it.copy() } ?: mutableListOf()

  private val spreadingInjectableChain: MutableList<SpreadingInjectable> =
    parent?.spreadingInjectables ?: mutableListOf()

  data class SpreadingInjectable(
    val callable: CallableRef,
    val constraintType: KotlinType = callable.typeParameters.single {
      it.hasAnnotation(InjektFqNames.Spread)
    }.defaultType
      .substitute(
        buildSubstitutor {
          putAll(
            callable.typeArguments
              .mapKeys { it.key.typeConstructor }
              .mapValues { it.value.unwrap() }
          )
        }
      ),
    val processedCandidateTypes: MutableSet<KotlinTypeKey> = mutableSetOf()
  ) {
    fun copy() = SpreadingInjectable(
      callable,
      constraintType,
      processedCandidateTypes.toMutableSet()
    )
  }

  val allScopes: List<InjectablesScope> = parent?.allScopes?.let { it + this } ?: listOf(this)

  val allStaticTypeParameters = allScopes.flatMap { it.typeParameters }

  data class CallableRequestKey(val typeKey: KotlinTypeKey, val staticTypeParameters: List<TypeParameterDescriptor>)
  private val injectablesByRequest = mutableMapOf<KotlinTypeKey, List<CallableInjectable>>()

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
    if (request.key.type.frameworkKey == null &&
      request.key.type.constructor.declarationDescriptor == ctx.listClassifier) return emptyList()

    return injectablesForType(
      CallableRequestKey(request.key, requestingScope.allStaticTypeParameters)
    ).filter { injectable -> allScopes.all { it.injectablesPredicate(injectable.callable) } }
  }

  private fun injectablesForType(key: CallableRequestKey): List<CallableInjectable> {
    if (injectables.isEmpty())
      return parent?.injectablesForType(key) ?: emptyList()
    return injectablesByRequest.getOrPut(key.typeKey) {
      buildList {
        parent?.injectablesForType(key)?.let { addAll(it) }

        for (candidate in injectables) {
          if (key.typeKey.type.frameworkKey != null &&
            candidate.type.frameworkKey != key.typeKey.type.frameworkKey) continue
          val substitutor = candidate.type.runCandidateInference(key.typeKey.type, key.staticTypeParameters, ctx = ctx)
            ?: continue
          this += CallableInjectable(
            this@InjectablesScope,
            candidate.substitute(substitutor),
            key.typeKey.type
          )
        }
      }
    }
  }

  fun frameworkInjectableForRequest(request: InjectableRequest): Injectable? = when {
    request.key.type.isFunctionType && !request.key.type.isProvide -> LambdaInjectable(this, request)
    request.key.type.constructor.declarationDescriptor == ctx.listClassifier -> {
      val singleElementType = request.key.type.arguments[0].type
      val collectionElementType = ctx.collectionClassifier.defaultType
        .replace(request.key.type.arguments)

      val key = CallableRequestKey(request.key, allStaticTypeParameters)

      val elements = listElementsTypesForType(singleElementType, collectionElementType, key)

      if (elements.isEmpty()) null
      else ListInjectable(
        type = request.key.type,
        ownerScope = this,
        elements = elements,
        singleElementType = singleElementType,
        collectionElementType = collectionElementType
      )
    }
    request.key.type.constructor.declarationDescriptor!!.fqNameSafe == InjektFqNames.TypeKey ->
      TypeKeyInjectable(request.key.type, this)
    request.key.type.constructor.declarationDescriptor!!.fqNameSafe == InjektFqNames.SourceKey ->
      SourceKeyInjectable(request.key.type, this)
    else -> null
  }

  private fun listElementsTypesForType(
    singleElementType: KotlinType,
    collectionElementType: KotlinType,
    key: CallableRequestKey
  ): List<KotlinType> {
    if (injectables.isEmpty())
      return parent?.listElementsTypesForType(singleElementType, collectionElementType, key) ?: emptyList()

    return buildList {
      parent?.listElementsTypesForType(singleElementType, collectionElementType, key)
        ?.let { addAll(it) }

      for (candidate in injectables.toList()) {
        val substitutor =
          candidate.type.runCandidateInference(singleElementType, key.staticTypeParameters, ctx = ctx)
            ?: candidate.type.runCandidateInference(collectionElementType, key.staticTypeParameters, ctx = ctx)
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

  private fun spreadInjectables(spreadingInjectable: SpreadingInjectable, candidateType: KotlinType) {
    if (!spreadingInjectable.processedCandidateTypes.add(KotlinTypeKey(candidateType, ctx)) ||
      spreadingInjectable in spreadingInjectableChain) return

    val substitutor = buildContextForSpreadingInjectable(
      spreadingInjectable.constraintType,
      candidateType,
      allStaticTypeParameters,
      ctx
    ) ?: return

    val substitutedInjectable = spreadingInjectable.callable
      .copy(
        type = spreadingInjectable.callable.type
          .substitute(substitutor),
        parameterTypes = spreadingInjectable.callable.parameterTypes
          .mapValues { it.value.substitute(substitutor) },
        typeArguments = spreadingInjectable.callable
          .typeArguments
          .mapValues { it.value.substitute(substitutor) }
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
