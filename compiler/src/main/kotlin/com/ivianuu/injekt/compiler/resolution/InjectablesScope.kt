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
  val resultsByType = mutableMapOf<KotlinType, ResolutionResult>()
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
    val processedCandidateTypes: MutableSet<KotlinType> = mutableSetOf()
  ) {
    fun copy() = SpreadingInjectable(
      callable,
      constraintType,
      processedCandidateTypes.toMutableSet()
    )
  }

  val allScopes: List<InjectablesScope> = parent?.allScopes?.let { it + this } ?: listOf(this)

  val allStaticTypeParameters = allScopes.flatMap { it.typeParameters }

  data class CallableRequestKey(val type: KotlinType, val staticTypeParameters: List<TypeParameterDescriptor>)
  private val injectablesByRequest = mutableMapOf<KotlinType, List<CallableInjectable>>()

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
      request.type.constructor.declarationDescriptor == ctx.listClassifier) return emptyList()

    return injectablesForType(
      CallableRequestKey(request.type, requestingScope.allStaticTypeParameters)
    ).filter { injectable -> allScopes.all { it.injectablesPredicate(injectable.callable) } }
  }

  private fun injectablesForType(key: CallableRequestKey): List<CallableInjectable> {
    if (injectables.isEmpty())
      return parent?.injectablesForType(key) ?: emptyList()
    return injectablesByRequest.getOrPut(key.type) {
      buildList {
        parent?.injectablesForType(key)?.let { addAll(it) }

        for (candidate in injectables) {
          if (key.type.frameworkKey.isNotEmpty() &&
            candidate.type.frameworkKey != key.type.frameworkKey) continue
          val substitutor = candidate.type.runCandidateInference(key.type, key.staticTypeParameters, ctx)
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
    request.type.isFunctionType && !request.type.isProvide -> LambdaInjectable(this, request)
    request.type.constructor.declarationDescriptor == ctx.listClassifier -> {
      val singleElementType = request.type.arguments[0].type
      val collectionElementType = ctx.collectionClassifier.defaultType
        .replace(request.type.arguments)

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
    request.type.constructor.declarationDescriptor!!.fqNameSafe == InjektFqNames.TypeKey ->
      TypeKeyInjectable(request.type, this)
    request.type.constructor.declarationDescriptor!!.fqNameSafe == InjektFqNames.SourceKey ->
      SourceKeyInjectable(request.type, this)
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
          candidate.type.runCandidateInference(singleElementType, key.staticTypeParameters, ctx)
            ?: candidate.type.runCandidateInference(collectionElementType, key.staticTypeParameters, ctx)
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
    if (!spreadingInjectable.processedCandidateTypes.add(candidateType) ||
      spreadingInjectable in spreadingInjectableChain) return

    val substitutor = runSpreadingInjectableInference(
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
