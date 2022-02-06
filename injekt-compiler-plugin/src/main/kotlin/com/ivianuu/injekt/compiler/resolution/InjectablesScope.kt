/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.util.*

@OptIn(ExperimentalStdlibApi::class)
class InjectablesScope(
  val name: String,
  val parent: InjectablesScope?,
  val callContext: CallContext = CallContext.DEFAULT,
  val ownerDescriptor: DeclarationDescriptor? = null,
  val file: KtFile? = null,
  val typeScopeType: TypeRef? = null,
  val isDeclarationContainer: Boolean = true,
  val isEmpty: Boolean = false,
  val initialInjectables: List<CallableRef> = emptyList(),
  val injectablesPredicate: (CallableRef) -> Boolean = { true },
  imports: List<ResolvedProviderImport> = emptyList(),
  val typeParameters: List<ClassifierRef> = emptyList(),
  val nesting: Int = parent?.nesting?.inc() ?: 0,
  val ctx: Context
) {
  val chain: MutableList<Pair<InjectableRequest, Injectable>> = parent?.chain ?: mutableListOf()
  val resultsByType = mutableMapOf<TypeRef, ResolutionResult>()
  val resultsByCandidate = mutableMapOf<Injectable, ResolutionResult>()
  val typeScopes = mutableMapOf<TypeRefKey, InjectablesScope>()

  private val imports = imports.toMutableList()

  val injectables = mutableListOf<CallableRef>()

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
  val spreadingInjectableKeys = mutableSetOf<InjectableKey>()
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

  private val isNoOp: Boolean = parent?.isDeclarationContainer == true &&
      typeParameters.isEmpty() &&
      (isEmpty || (initialInjectables.isEmpty() && callContext == parent.callContext))

  val scopeToUse: InjectablesScope = if (isNoOp) parent!!.scopeToUse else this

  init {
    // we need them right here because otherwise we could possibly add duplicated spreading injectables
    if (parent != null)
      spreadingInjectableKeys.addAll(parent.spreadingInjectableKeys)

    for (injectable in initialInjectables)
      injectable.collectInjectables(
        scope = this,
        addImport = { importFqName, packageFqName ->
          this.imports += ResolvedProviderImport(
            null,
            "${importFqName}.*",
            packageFqName
          )
        },
        addInjectable = { callable ->
          injectables += callable
          val typeWithFrameworkKey = callable.type
            .copy(frameworkKey = callable.callable.uniqueKey(ctx))
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

  fun recordLookup(
    lookupLocation: LookupLocation,
    visitedScopes: MutableSet<InjectablesScope> = mutableSetOf()
  ) {
    if (!visitedScopes.add(this)) return

    parent?.recordLookup(lookupLocation, visitedScopes)
    typeScopes.forEach { it.value.recordLookup(lookupLocation, visitedScopes) }
    for (import in imports) {
      memberScopeForFqName(import.packageFqName, lookupLocation, ctx)
        ?.first
        ?.recordLookup(injectablesLookupName, lookupLocation)
      if (import.importPath!!.endsWith(".**")) {
        memberScopeForFqName(import.packageFqName, lookupLocation, ctx)
          ?.first
          ?.recordLookup(subInjectablesLookupName, lookupLocation)
      }
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
    )
      .let { allInjectables ->
        if (request.parameterIndex == DISPATCH_RECEIVER_INDEX) allInjectables
        else allInjectables.filter { it.callable.isValidForObjectRequest() }
      }

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
            candidate.substitute(context.fixedTypeVariables, ctx)
          )
        }
      }
    }
  }

  fun frameworkInjectableForRequest(request: InjectableRequest): Injectable? {
    when {
      request.type.isFunctionType -> {
        val finalCallContext = if (request.isInline) callContext
        else request.type.callContext(ctx)
        return ProviderInjectable(
          type = request.type,
          ownerScope = this,
          dependencyCallContext = finalCallContext,
          isInline = request.isInline
        )
      }
      request.type.classifier == ctx.listClassifier -> {
        fun createInjectable(): ListInjectable? {
          val singleElementType = request.type.arguments[0]
          val collectionElementType = ctx.collectionClassifier.defaultType
            .withArguments(listOf(singleElementType))

          val key = CallableRequestKey(request.type, allStaticTypeParameters)

          val elements = listElementsForType(singleElementType, collectionElementType, key)
            .values.map { it.type } + frameworkListElementsForType(singleElementType)

          return if (elements.isEmpty()) null
          else ListInjectable(
            type = request.type,
            ownerScope = this,
            elements = elements,
            singleElementType = singleElementType,
            collectionElementType = collectionElementType
          )
        }

        val typeScope = TypeInjectablesScopeOrNull(request.type, this, ctx)
          .takeUnless { it.isEmpty }
        return if (typeScope != null) typeScope.frameworkInjectableForRequest(request)
        else createInjectable()
      }
      request.type.classifier.fqName == ctx.injektFqNames.typeKey ->
        return TypeKeyInjectable(request.type, this)
      request.type.classifier.fqName == ctx.injektFqNames.sourceKey ->
        return SourceKeyInjectable(request.type, this)
      else -> return null
    }
  }

  private fun listElementsForType(
    singleElementType: TypeRef,
    collectionElementType: TypeRef,
    key: CallableRequestKey
  ): Map<InjectableKey, CallableRef> {
    if (injectables.isEmpty())
      return parent?.listElementsForType(singleElementType, collectionElementType, key) ?: emptyMap()

    return buildMap {
      fun addThisInjectables() {
        for (candidate in injectables.toList()) {
          if (candidate.type.frameworkKey != key.type.frameworkKey) continue

          var context =
            candidate.type.buildContext(singleElementType, key.staticTypeParameters, ctx = ctx)
          if (!context.isOk)
            context = candidate.type.buildContext(collectionElementType, key.staticTypeParameters, ctx = ctx)
          if (!context.isOk) continue

          val substitutedCandidate = candidate.substitute(context.fixedTypeVariables, ctx)

          val typeWithFrameworkKey = substitutedCandidate.type.copy(
            frameworkKey = UUID.randomUUID().toString()
          )

          val finalCandidate = substitutedCandidate.copy(type = typeWithFrameworkKey)

          injectables += finalCandidate

          this[InjectableKey(finalCandidate, ctx)] = finalCandidate
        }
      }

      // if we are a type scope we wanna appear in the list before the other scopes
      if (typeScopeType != null)
        addThisInjectables()

      parent?.listElementsForType(singleElementType, collectionElementType, key)
        ?.let { parentElements ->
          for ((candidateKey, candidate) in parentElements)
            put(candidateKey, candidate)
        }

      if (typeScopeType == null)
        addThisInjectables()
    }
  }

  private fun frameworkListElementsForType(singleElementType: TypeRef): List<TypeRef> =
    when {
      singleElementType.isFunctionType -> {
        val providerReturnType = singleElementType.arguments.last()
        val innerKey = CallableRequestKey(providerReturnType, allStaticTypeParameters)

        buildList {
          fun TypeRef.add() {
            this@buildList += singleElementType.copy(
              arguments = singleElementType.arguments
                .dropLast(1) + this
            )
          }

          for (candidate in listElementsForType(
            providerReturnType, ctx.collectionClassifier
              .defaultType.withArguments(listOf(providerReturnType)), innerKey).values)
            candidate.type.add()

          for (candidateType in frameworkListElementsForType(providerReturnType))
            candidateType.add()
        }
      }
      singleElementType.classifier.fqName == ctx.injektFqNames.sourceKey -> listOf(
        ctx.sourceKeyClassifier!!.defaultType
          .copy(frameworkKey = UUID.randomUUID().toString())
      )
      singleElementType.classifier.fqName == ctx.injektFqNames.typeKey -> listOf(singleElementType)
      else -> emptyList()
    }

  private fun spreadInjectables(candidateType: TypeRef) {
    for (spreadingInjectable in spreadingInjectables.toList())
      spreadInjectables(spreadingInjectable, candidateType)
  }

  private fun spreadInjectables(
    spreadingInjectable: SpreadingInjectable,
    candidateType: TypeRef
  ) {
    if (candidateType.frameworkKey in spreadingInjectable.resultingFrameworkKeys) return
    if (!spreadingInjectable.processedCandidateTypes.add(candidateType)) return

    val (context, substitutionMap) = buildContextForSpreadingInjectable(
      spreadingInjectable.constraintType,
      candidateType,
      allStaticTypeParameters,
      ctx
    )
    if (!context.isOk) return

    val newInjectableType = spreadingInjectable.callable.type
      .substitute(substitutionMap)
      .copy(frameworkKey = "")
    val newInjectable = spreadingInjectable.callable
      .copy(
        type = newInjectableType,
        originalType = newInjectableType,
        parameterTypes = spreadingInjectable.callable.parameterTypes
          .mapValues { it.value.substitute(substitutionMap) },
        typeArguments = spreadingInjectable.callable
          .typeArguments
          .mapValues { it.value.substitute(substitutionMap) }
      )

    newInjectable.collectInjectables(
      scope = this,
      addImport = { importFqName, packageFqName ->
        imports += ResolvedProviderImport(
          null,
          "${importFqName}.*",
          packageFqName
        )
      },
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

  /**
   * We add implicit injectables for objects under some circumstances to allow
   * callables in it to resolve their dispatch receiver parameter
   * Here we ensure that the user cannot resolve such implicit object injectable if they are not
   * provided by the user
   */
  private fun CallableRef.isValidForObjectRequest(): Boolean =
    !originalType.classifier.isObject ||
        (callable !is ReceiverParameterDescriptor ||
            callable.cast<ReceiverParameterDescriptor>()
              .value !is ImplicitClassReceiver ||
            originalType.classifier.descriptor!!.hasAnnotation(ctx.injektFqNames.provide))

  override fun toString(): String = "InjectablesScope($name)"
}
