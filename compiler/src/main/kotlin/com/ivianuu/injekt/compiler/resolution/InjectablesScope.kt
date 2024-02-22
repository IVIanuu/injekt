/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.inference.components.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*

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
  val resultsByType = mutableMapOf<Int, ResolutionResult>()
  val resultsByCandidate = mutableMapOf<Injectable, ResolutionResult>()

  private val injectables = mutableListOf<CallableRef>()
  private val allInjectables get() = allScopes.flatMap { injectables }

  private val spreadingInjectables: MutableList<SpreadingInjectable> =
    parent?.spreadingInjectables?.mapTo(mutableListOf()) { it.copy() } ?: mutableListOf()

  private val spreadingInjectableChain: MutableList<SpreadingInjectable> =
    parent?.spreadingInjectables ?: mutableListOf()

  data class SpreadingInjectable(
    val callable: CallableRef,
    val constraintType: KotlinType = callable.typeArguments.keys.single {
      it.typeConstructor.declarationDescriptor!!.hasAnnotation(InjektFqNames.Spread)
    }.defaultType.substitute(
      NewTypeSubstitutorByConstructorMap(
        callable.typeArguments
          .mapKeys { it.key.typeConstructor }
          .mapValues { it.value.type.unwrap() }
      )
    ),
    val processedCandidateTypes: MutableSet<Int> = mutableSetOf()
  ) {
    fun copy() = SpreadingInjectable(
      callable,
      constraintType,
      processedCandidateTypes.toMutableSet()
    )
  }

  val allScopes: List<InjectablesScope> = parent?.allScopes?.let { it + this } ?: listOf(this)

  val allStaticTypeParameters = allScopes.flatMap { it.typeParameters }

  data class CallableRequestKey(
    val type: KotlinType,
    val typeKey: Int,
    val staticTypeParameters: List<TypeParameterDescriptor>,
  )
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
    if (request.type.uniqueId == null &&
      request.type.constructor.declarationDescriptor
        ?.fqNameSafe == StandardNames.FqNames.list) return emptyList()

    return injectablesForType(
      CallableRequestKey(request.type, request.typeKey, requestingScope.allStaticTypeParameters)
    ).filter { injectable -> allScopes.all { it.injectablesPredicate(injectable.callable) } }
  }

  private fun injectablesForType(key: CallableRequestKey): List<CallableInjectable> {
    if (injectables.isEmpty())
      return parent?.injectablesForType(key) ?: emptyList()
    return injectablesByRequest.getOrPut(key) {
      buildList {
        parent?.injectablesForType(key)?.let { addAll(it) }

        for (candidate in injectables) {
          if (key.type.uniqueId != null &&
            candidate.type.uniqueId != key.type.uniqueId) continue
          val substitutor = candidate.type.runCandidateInference(key.type, key.staticTypeParameters, ctx = ctx)
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

  fun builtInInjectableForRequest(request: InjectableRequest): Injectable? = when {
    request.type.isFunctionType &&
        !request.type.hasAnnotation(InjektFqNames.Provide) -> LambdaInjectable(this, request)
    request.type.constructor.declarationDescriptor?.fqNameSafe == StandardNames.FqNames.list -> {
      val singleElementType = request.type.arguments[0].type
      val collectionElementType = ctx.collectionClassifier.defaultType
        .replace(listOf(singleElementType.asTypeProjection()))

      val key = CallableRequestKey(request.type, request.typeKey, allStaticTypeParameters)

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

        add(candidate.substitute(substitutor).type)
      }
    }
  }

  private fun spreadInjectables(spreadingInjectable: SpreadingInjectable) {
    for (candidate in allInjectables.toList())
      spreadInjectables(spreadingInjectable, candidate.type)
  }

  private fun spreadInjectables(spreadingInjectable: SpreadingInjectable, candidateType: KotlinType) {
    if (!spreadingInjectable.processedCandidateTypes.add(candidateType.injektHashCode(ctx)) ||
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
