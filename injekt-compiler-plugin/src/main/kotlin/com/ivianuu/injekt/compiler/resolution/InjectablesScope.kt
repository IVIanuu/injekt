/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.util.*

@OptIn(ExperimentalStdlibApi::class)
class InjectablesScope(
  val name: String,
  val parent: InjectablesScope?,
  val ownerDescriptor: DeclarationDescriptor? = null,
  val file: KtFile? = null,
  val typeScopeType: KotlinType? = null,
  val isDeclarationContainer: Boolean = true,
  val isEmpty: Boolean = false,
  val initialInjectables: List<CallableRef> = emptyList(),
  val injectablesPredicate: (CallableRef) -> Boolean = { true },
  imports: List<ResolvedProviderImport> = emptyList(),
  val typeParameters: List<TypeParameterDescriptor> = emptyList(),
  val nesting: Int = parent?.nesting?.inc() ?: 0,
  val ctx: Context
) {
  val chain: MutableList<Pair<InjectableRequest, Injectable>> = parent?.chain ?: mutableListOf()
  val resultsByType = mutableMapOf<KotlinType, ResolutionResult>()
  val resultsByCandidate = mutableMapOf<Injectable, ResolutionResult>()
  val typeScopes = mutableMapOf<KotlinType, InjectablesScope>()

  private val imports = imports.toMutableList()

  val injectables = mutableListOf<CallableRef>()

  data class InjectableKey(
    val uniqueKey: String,
    val originalType: KotlinType,
    val typeArguments: Map<TypeParameterDescriptor, KotlinType>,
    val parameterTypes: Map<Int, KotlinType>
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
  private val spreadingInjectableCandidateTypes = mutableListOf<KotlinType>()

  data class SpreadingInjectable(
    val callable: CallableRef,
    val ctx: Context,
    val constraintType: KotlinType = callable.typeParameters.single {
      it.hasAnnotation(InjektFqNames.Spread)
    }.defaultType.substitute(
      callable.typeArguments
        .mapKeys { it.key.uniqueKey(ctx) },
      ctx
    ),
    val processedCandidateTypes: MutableSet<KotlinType> = mutableSetOf(),
    val resultingFrameworkKeys: MutableSet<String> = mutableSetOf()
  ) {
    fun copy() = SpreadingInjectable(
      callable,
      ctx,
      constraintType,
      processedCandidateTypes.toMutableSet(),
      resultingFrameworkKeys.toMutableSet()
    )
  }

  val allScopes: List<InjectablesScope> = parent?.allScopes?.let { it + this } ?: listOf(this)

  private val allStaticTypeParameters = allScopes.flatMap { it.typeParameters }

  data class CallableRequestKey(val type: KotlinType, val staticTypeParameters: List<TypeParameterDescriptor>)
  private val injectablesByRequest = mutableMapOf<CallableRequestKey, List<CallableInjectable>>()

  private val isNoOp: Boolean = parent?.isDeclarationContainer == true &&
      typeParameters.isEmpty() &&
      (isEmpty || initialInjectables.isEmpty())

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
            .withFrameworkKey(callable.callable.uniqueKey(ctx), ctx)
          injectables += callable.copy(type = typeWithFrameworkKey)
          spreadingInjectableCandidateTypes += typeWithFrameworkKey
        },
        addSpreadingInjectable = { callable ->
          if (spreadingInjectableKeys.add(InjectableKey(callable, ctx)))
            spreadingInjectables += SpreadingInjectable(callable, ctx)
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
      request.type.fqName == ctx.module.builtIns.list.fqNameSafe) return emptyList()

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
          val substitutionMap =
            candidate.type.buildSystem(key.type, key.staticTypeParameters, ctx) ?: continue
          this += CallableInjectable(
            this@InjectablesScope,
            candidate.substitute(substitutionMap, ctx)
          )
        }
      }
    }
  }

  fun frameworkInjectableForRequest(request: InjectableRequest): Injectable? {
    when {
      request.type.isFunctionType -> return ProviderInjectable(
        type = request.type,
        ownerScope = this,
        isInline = request.isInline
      )
      request.type.fqName == ctx.module.builtIns.list.fqNameSafe -> {
        fun createInjectable(): ListInjectable? {
          val singleElementType = request.type.arguments[0].type
          val collectionElementType = ctx.module.builtIns.collection.defaultType
            .replace(newArguments = listOf(singleElementType.asTypeProjection()))

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
      request.type.fqName == InjektFqNames.TypeKey ->
        return TypeKeyInjectable(request.type, this)
      request.type.fqName == InjektFqNames.SourceKey ->
        return SourceKeyInjectable(request.type, this)
      else -> return null
    }
  }

  private fun listElementsForType(
    singleElementType: KotlinType,
    collectionElementType: KotlinType,
    key: CallableRequestKey
  ): Map<InjectableKey, CallableRef> {
    if (injectables.isEmpty())
      return parent?.listElementsForType(singleElementType, collectionElementType, key) ?: emptyMap()

    return buildMap {
      fun addThisInjectables() {
        for (candidate in injectables.toList()) {
          if (candidate.type.frameworkKey != key.type.frameworkKey) continue

          var substitutionMap =
            candidate.type.buildSystem(singleElementType, key.staticTypeParameters, ctx)
          if (substitutionMap == null ||
            !singleElementType.isSubtypeOf(singleElementType))
            substitutionMap = candidate.type.buildSystem(collectionElementType, key.staticTypeParameters, ctx)
          if (substitutionMap == null) continue

          val substitutedCandidate = candidate.substitute(substitutionMap, ctx)

          if (!substitutedCandidate.type.isSubtypeOf(singleElementType) &&
            !substitutedCandidate.type.isSubtypeOf(collectionElementType))
            continue

          val frameworkKey = UUID.randomUUID().toString()
          val typeWithFrameworkKey = substitutedCandidate.type.withFrameworkKey(
            frameworkKey,
            ctx
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

  private fun frameworkListElementsForType(singleElementType: KotlinType): List<KotlinType> =
    if (!singleElementType.isFunctionType) emptyList()
    else {
      val providerReturnType = singleElementType.arguments.last().type
      val innerKey = CallableRequestKey(providerReturnType, allStaticTypeParameters)

      buildList {
        fun KotlinType.add() {
          this@buildList += singleElementType.replace(
            singleElementType.arguments
              .dropLast(1) + this.asTypeProjection()
          )
        }

        for (candidate in listElementsForType(
          providerReturnType, ctx.module.builtIns.collection
            .defaultType.replace(newArguments = listOf(providerReturnType.asTypeProjection())), innerKey).values)
          candidate.type.add()

        for (candidateType in frameworkListElementsForType(providerReturnType))
          candidateType.add()
      }
    }

  private fun spreadInjectables(candidateType: KotlinType) {
    for (spreadingInjectable in spreadingInjectables.toList())
      spreadInjectables(spreadingInjectable, candidateType)
  }

  private fun spreadInjectables(
    spreadingInjectable: SpreadingInjectable,
    candidateType: KotlinType
  ) {
    if (candidateType.frameworkKey in spreadingInjectable.resultingFrameworkKeys) return
    if (!spreadingInjectable.processedCandidateTypes.add(candidateType)) return

    val substitutionMap = buildContextForSpreadingInjectable(
      spreadingInjectable.constraintType,
      candidateType,
      allStaticTypeParameters,
      ctx
    ) ?: return

    val newInjectableType = spreadingInjectable.callable.type.substitute(substitutionMap, ctx)
      .withFrameworkKey("", ctx)
    val newInjectable = spreadingInjectable.callable
      .copy(
        type = newInjectableType,
        originalType = newInjectableType,
        parameterTypes = spreadingInjectable.callable.parameterTypes
          .mapValues { it.value.substitute(substitutionMap, ctx) },
        typeArguments = spreadingInjectable.callable
          .typeArguments
          .mapValues { it.value.substitute(substitutionMap, ctx) }
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
          type = finalInnerCallable.type.withFrameworkKey(
            spreadingInjectable.callable.type.frameworkKey
              .nextFrameworkKey(finalInnerCallable.callable.uniqueKey(ctx))
              .also { spreadingInjectable.resultingFrameworkKeys += it },
            ctx
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
          val newSpreadingInjectable = SpreadingInjectable(finalNewInnerInjectable, ctx)
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
    originalType.constructor.declarationDescriptor?.safeAs<ClassDescriptor>()?.kind != ClassKind.OBJECT ||
        (callable !is ReceiverParameterDescriptor ||
            callable.cast<ReceiverParameterDescriptor>()
              .value !is ImplicitClassReceiver ||
            originalType.constructor.declarationDescriptor!!.hasAnnotation(InjektFqNames.Provide))

  override fun toString(): String = "InjectablesScope($name)"
}
