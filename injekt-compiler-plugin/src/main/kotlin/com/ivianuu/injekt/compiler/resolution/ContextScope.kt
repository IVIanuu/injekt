/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.DISPATCH_RECEIVER_INDEX
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.providersLookupName
import com.ivianuu.injekt.compiler.memberScopeForFqName
import com.ivianuu.injekt.compiler.nextFrameworkKey
import com.ivianuu.injekt.compiler.subProvidersLookupName
import com.ivianuu.injekt.compiler.uniqueKey
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.util.*

class ContextScope(
  val name: String,
  val parent: ContextScope?,
  val callContext: CallContext = CallContext.DEFAULT,
  val ownerDescriptor: DeclarationDescriptor? = null,
  val file: KtFile? = null,
  val typeScopeType: TypeRef? = null,
  val isDeclarationContainer: Boolean = true,
  val isEmpty: Boolean = false,
  val initialProviders: List<CallableRef> = emptyList(),
  val providersPredicate: (CallableRef) -> Boolean = { true },
  imports: List<ResolvedProviderImport> = emptyList(),
  val typeParameters: List<ClassifierRef> = emptyList(),
  val nesting: Int = parent?.nesting?.inc() ?: 0,
  val ctx: Context
) {
  val chain: MutableList<Pair<ContextRequest, Provider>> = parent?.chain ?: mutableListOf()
  val resultsByType = mutableMapOf<TypeRef, ResolutionResult>()
  val resultsByCandidate = mutableMapOf<Provider, ResolutionResult>()
  val typeScopes = mutableMapOf<TypeRefKey, ContextScope>()

  private val imports = imports.toMutableList()

  val providers = mutableListOf<CallableRef>()

  data class ProviderKey(
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

  private val spreadingProviders = mutableListOf<SpreadingProvider>()
  val spreadingProviderKeys = mutableSetOf<ProviderKey>()
  private val spreadingProviderCandidateTypes = mutableListOf<TypeRef>()

  data class SpreadingProvider(
    val callable: CallableRef,
    val constraintType: TypeRef = callable.typeParameters.single {
      it.isSpread
    }.defaultType.substitute(callable.typeArguments),
    val processedCandidateTypes: MutableSet<TypeRef> = mutableSetOf(),
    val resultingFrameworkKeys: MutableSet<String> = mutableSetOf()
  ) {
    fun copy() = SpreadingProvider(
      callable,
      constraintType,
      processedCandidateTypes.toMutableSet(),
      resultingFrameworkKeys.toMutableSet()
    )
  }

  val allScopes: List<ContextScope> = parent?.allScopes?.let { it + this } ?: listOf(this)

  private val allStaticTypeParameters = allScopes.flatMap { it.typeParameters }

  data class CallableRequestKey(val type: TypeRef, val staticTypeParameters: List<ClassifierRef>)
  private val providersByRequest = mutableMapOf<CallableRequestKey, List<CallableProvider>>()

  private val isNoOp: Boolean = parent?.allScopes?.any { it.isDeclarationContainer } == true &&
      typeParameters.isEmpty() &&
      (isEmpty || (initialProviders.isEmpty() && callContext == parent.callContext))

  val scopeToUse: ContextScope = if (isNoOp) parent!!.scopeToUse else this

  init {
    // we need them right here because otherwise we could possibly add duplicated spreading providers
    if (parent != null)
      spreadingProviderKeys.addAll(parent.spreadingProviderKeys)

    for (provider in initialProviders)
      provider.collectProviders(
        scope = this,
        addImport = { importFqName, packageFqName ->
          this.imports += ResolvedProviderImport(
            null,
            "${importFqName}.*",
            packageFqName
          )
        },
        addProvider = { callable ->
          providers += callable
          val typeWithFrameworkKey = callable.type
            .copy(frameworkKey = callable.callable.uniqueKey(ctx))
          providers += callable.copy(type = typeWithFrameworkKey)
          spreadingProviderCandidateTypes += typeWithFrameworkKey
        },
        addSpreadingProvider = { callable ->
          if (spreadingProviderKeys.add(ProviderKey(callable, ctx)))
            spreadingProviders += SpreadingProvider(callable)
        },
        ctx = ctx
      )

    val hasSpreadingProviders = spreadingProviders.isNotEmpty()
    val hasSpreadingProviderCandidates = spreadingProviderCandidateTypes.isNotEmpty()
    if (parent != null) {
      spreadingProviders.addAll(
        0,
        // we only need to copy the parent providers if we have any candidates to process
        if (hasSpreadingProviderCandidates) parent.spreadingProviders.map { it.copy() }
        else parent.spreadingProviders
      )
      spreadingProviderCandidateTypes.addAll(0, parent.spreadingProviderCandidateTypes)
    }

    // only run if there is something meaningful to process
    if ((hasSpreadingProviders && spreadingProviderCandidateTypes.isNotEmpty()) ||
      (hasSpreadingProviderCandidates && spreadingProviders.isNotEmpty())
    ) {
      spreadingProviderCandidateTypes
        .toList()
        .forEach { spreadProviders(it) }
    }
  }

  fun recordLookup(
    lookupLocation: LookupLocation,
    lookups: MutableSet<String>,
    visitedScopes: MutableSet<ContextScope> = mutableSetOf()
  ) {
    if (!visitedScopes.add(this)) return

    parent?.recordLookup(lookupLocation, lookups, visitedScopes)
    typeScopes.forEach { it.value.recordLookup(lookupLocation, lookups, visitedScopes) }
    for (import in imports) {
      memberScopeForFqName(import.packageFqName, lookupLocation, ctx)
        ?.first
        ?.recordLookup(providersLookupName, lookupLocation)
        ?.let { lookups += import.packageFqName.child(providersLookupName).asString() }
      if (import.importPath!!.endsWith(".**")) {
        memberScopeForFqName(import.packageFqName, lookupLocation, ctx)
          ?.first
          ?.recordLookup(subProvidersLookupName, lookupLocation)
          ?.let { lookups += import.packageFqName.child(subProvidersLookupName).asString() }
      }
    }
  }

  fun providersForRequest(
    request: ContextRequest,
    requestingScope: ContextScope
  ): List<Provider> {
    // we return merged collections
    if (request.type.frameworkKey.isEmpty() &&
      request.type.classifier == ctx.listClassifier) return emptyList()

    return providersForType(
      CallableRequestKey(request.type, requestingScope.allStaticTypeParameters)
    )
      .let { allProviders ->
        if (request.parameterIndex == DISPATCH_RECEIVER_INDEX) allProviders
        else allProviders.filter { it.callable.isValidForObjectRequest() }
      }

  }

  private fun providersForType(key: CallableRequestKey): List<CallableProvider> {
    if (providers.isEmpty())
      return parent?.providersForType(key) ?: emptyList()
    return providersByRequest.getOrPut(key) {
      buildList {
        parent?.providersForType(key)?.let { addAll(it) }

        for (candidate in providers) {
          if (candidate.type.frameworkKey != key.type.frameworkKey) continue
          val context = candidate.type.buildContext(key.type, key.staticTypeParameters, ctx = ctx)
          if (!context.isOk) continue
          this += CallableProvider(
            this@ContextScope,
            candidate.substitute(context.fixedTypeVariables)
          )
        }
      }
    }
  }

  fun frameworkProviderForRequest(request: ContextRequest): Provider? {
    when {
      request.type.isFunctionType -> {
        val finalCallContext = if (request.isInline) callContext
        else request.type.callContext
        return FunctionProvider(
          type = request.type,
          ownerScope = this,
          dependencyCallContext = finalCallContext,
          isInline = request.isInline
        )
      }
      request.type.classifier == ctx.listClassifier -> {
        fun createProvider(): ListProvider? {
          val singleElementType = request.type.arguments[0]
          val collectionElementType = ctx.collectionClassifier.defaultType
            .withArguments(listOf(singleElementType))

          val key = CallableRequestKey(request.type, allStaticTypeParameters)

          val elements = listElementsForType(singleElementType, collectionElementType, key)
            .values.map { it.type } + frameworkListElementsForType(singleElementType)

          return if (elements.isEmpty()) null
          else ListProvider(
            type = request.type,
            ownerScope = this,
            elements = elements,
            singleElementType = singleElementType,
            collectionElementType = collectionElementType
          )
        }

        val typeScope = TypeContextScope(request.type, this, ctx)
          .takeUnless { it.isEmpty }
        return if (typeScope != null) typeScope.frameworkProviderForRequest(request)
        else createProvider()
      }
      request.type.classifier.fqName == InjektFqNames.TypeKey ->
        return TypeKeyProvider(request.type, this)
      request.type.classifier.fqName == InjektFqNames.SourceKey ->
        return SourceKeyProvider(request.type, this)
      else -> return null
    }
  }

  private fun listElementsForType(
    singleElementType: TypeRef,
    collectionElementType: TypeRef,
    key: CallableRequestKey
  ): Map<ProviderKey, CallableRef> {
    if (providers.isEmpty())
      return parent?.listElementsForType(singleElementType, collectionElementType, key) ?: emptyMap()

    return buildMap {
      fun addThisProviders() {
        for (candidate in providers.toList()) {
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

          providers += finalCandidate

          this[ProviderKey(finalCandidate, ctx)] = finalCandidate
        }
      }

      // if we are a type scope we wanna appear in the list before the other scopes
      if (typeScopeType != null)
        addThisProviders()

      parent?.listElementsForType(singleElementType, collectionElementType, key)
        ?.let { parentElements ->
          for ((candidateKey, candidate) in parentElements)
            put(candidateKey, candidate)
        }

      if (typeScopeType == null)
        addThisProviders()
    }
  }

  private fun frameworkListElementsForType(singleElementType: TypeRef): List<TypeRef> =
    if (!singleElementType.isFunctionType) emptyList()
    else {
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

  private fun spreadProviders(candidateType: TypeRef) {
    for (spreadingProvider in spreadingProviders.toList())
      spreadProviders(spreadingProvider, candidateType)
  }

  private fun spreadProviders(
    spreadingProvider: SpreadingProvider,
    candidateType: TypeRef
  ) {
    if (candidateType.frameworkKey in spreadingProvider.resultingFrameworkKeys) return
    if (!spreadingProvider.processedCandidateTypes.add(candidateType)) return

    val (context, substitutionMap) = buildContextForSpreadingProvider(
      spreadingProvider.constraintType,
      candidateType,
      allStaticTypeParameters,
      ctx
    )
    if (!context.isOk) return

    val newProviderType = spreadingProvider.callable.type
      .substitute(substitutionMap)
      .copy(frameworkKey = "")
    val newProvider = spreadingProvider.callable
      .copy(
        type = newProviderType,
        originalType = newProviderType,
        parameterTypes = spreadingProvider.callable.parameterTypes
          .mapValues { it.value.substitute(substitutionMap) },
        typeArguments = spreadingProvider.callable
          .typeArguments
          .mapValues { it.value.substitute(substitutionMap) }
      )

    newProvider.collectProviders(
      scope = this,
      addImport = { importFqName, packageFqName ->
        imports += ResolvedProviderImport(
          null,
          "${importFqName}.*",
          packageFqName
        )
      },
      addProvider = { innerCallable ->
        val finalInnerCallable = innerCallable
          .copy(originalType = innerCallable.type)
        providers += finalInnerCallable
        val innerCallableWithFrameworkKey = finalInnerCallable.copy(
          type = finalInnerCallable.type.copy(
            frameworkKey = spreadingProvider.callable.type.frameworkKey
              .nextFrameworkKey(finalInnerCallable.callable.uniqueKey(ctx))
              .also { spreadingProvider.resultingFrameworkKeys += it }
          )
        )
        providers += innerCallableWithFrameworkKey
        spreadingProviderCandidateTypes += innerCallableWithFrameworkKey.type
        spreadProviders(innerCallableWithFrameworkKey.type)
      },
      addSpreadingProvider = { newInnerCallable ->
        val finalNewInnerProvider = newInnerCallable
          .copy(originalType = newInnerCallable.type)
        if (spreadingProviderKeys.add(ProviderKey(finalNewInnerProvider, ctx))) {
          val newSpreadingProvider = SpreadingProvider(finalNewInnerProvider)
          spreadingProviders += newSpreadingProvider
          for (candidate in spreadingProviderCandidateTypes.toList())
            spreadProviders(newSpreadingProvider, candidate)
        }
      },
      ctx = ctx
    )
  }

  /**
   * We add implicit providers for objects under some circumstances to allow
   * callables in it to resolve their dispatch receiver parameter
   * Here we ensure that the user cannot resolve such implicit object provider if they are not
   * provided by the user
   */
  private fun CallableRef.isValidForObjectRequest(): Boolean =
    !originalType.classifier.isObject ||
        (callable !is ReceiverParameterDescriptor ||
            callable.cast<ReceiverParameterDescriptor>()
              .value !is ImplicitClassReceiver ||
            originalType.classifier.descriptor!!.hasAnnotation(InjektFqNames.Provide))

  override fun toString(): String = "ContextScope($name)"
}
