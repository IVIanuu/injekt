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
  val resultsByType = mutableMapOf<TypeKey, ResolutionResult>()
  val resultsByCandidate = mutableMapOf<Injectable, ResolutionResult>()
  val typeScopes = mutableMapOf<TypeKey, InjectablesScope>()

  private val imports = imports.toMutableList()

  val injectables = mutableListOf<CallableRef>()

  private val injectableByFrameworkKey = mutableMapOf<String, CallableRef>()

  data class InjectableKey(
    val uniqueKey: String,
    val originalTypeKey: TypeKey,
    val typeArguments: Map<TypeParameterDescriptor, TypeProjection>,
    val parameterTypes: Map<Int, KotlinType>
  ) {
    constructor(callable: CallableRef, ctx: Context) : this(
      callable.callable.uniqueKey(ctx),
      callable.originalType.toTypeKey(),
      callable.typeArguments,
      callable.parameterTypes
    )
  }

  val allScopes: List<InjectablesScope> = parent?.allScopes?.let { it + this } ?: listOf(this)

  private val allStaticTypeParameters = allScopes.flatMap { it.typeParameters }

  data class CallableRequestKey(
    val typeKey: TypeKey,
    val staticTypeParameters: List<TypeParameterDescriptor>
  )
  private val injectablesByRequest = mutableMapOf<CallableRequestKey, List<CallableInjectable>>()

  private val isNoOp: Boolean = parent?.isDeclarationContainer == true &&
      typeParameters.isEmpty() &&
      (isEmpty || initialInjectables.isEmpty())

  val scopeToUse: InjectablesScope = if (isNoOp) parent!!.scopeToUse else this

  init {
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
        addInjectable = {
          injectables += it
          if (it.type.frameworkKey.isNotEmpty())
            injectableByFrameworkKey[it.type.frameworkKey] = it
        },
        ctx = ctx
      )
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
      CallableRequestKey(request.type.toTypeKey(), requestingScope.allStaticTypeParameters)
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

        val frameworkKey = key.typeKey.frameworkKey
        if (frameworkKey.isEmpty()) {
          for (candidate in injectables) {
            if (candidate.type.frameworkKey != key.typeKey.frameworkKey) continue
            val system = candidate.type.buildSystem(key.typeKey.type, key.staticTypeParameters)
            if (system.status.hasContradiction()) continue
            val substitutedCandidate = candidate.substitute(system.resultingSubstitutor)
            if (!substitutedCandidate.type.isSubtypeOf(key.typeKey.type))
              continue
            this += CallableInjectable(this@InjectablesScope, substitutedCandidate)
          }
        } else {
          injectableByFrameworkKey[frameworkKey]
            ?.let { this += CallableInjectable(this@InjectablesScope, it) }
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

          val key = CallableRequestKey(request.type.toTypeKey(), allStaticTypeParameters)

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
          var system =
            candidate.type.buildSystem(singleElementType, key.staticTypeParameters)
          if (system.status.hasContradiction() ||
              !singleElementType.isSubtypeOf(singleElementType))
            system = candidate.type.buildSystem(collectionElementType, key.staticTypeParameters)
          if (system.status.hasContradiction()) continue

          val substitutedCandidate = candidate.substitute(system.resultingSubstitutor)

          if (!substitutedCandidate.type.isSubtypeOf(singleElementType) &&
              !substitutedCandidate.type.isSubtypeOf(collectionElementType))
            continue

          val frameworkKey = UUID.randomUUID().toString()
          val typeWithFrameworkKey = substitutedCandidate.type.withFrameworkKey(
            frameworkKey,
            ctx
          )

          val finalCandidate = substitutedCandidate.copy(type = typeWithFrameworkKey)

          injectableByFrameworkKey[frameworkKey] = finalCandidate

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
    when {
      singleElementType.isFunctionType -> {
        val providerReturnType = singleElementType.arguments.last().type
        val innerKey = CallableRequestKey(providerReturnType.toTypeKey(), allStaticTypeParameters)

        buildList {
          fun KotlinType.add() {
            this@buildList += singleElementType.replace(
              newArguments = singleElementType.arguments
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
      else -> emptyList()
    }

  /**
   * We add implicit injectables for objects under some circumstances to allow
   * callables in it to resolve their dispatch receiver parameter
   * Here we ensure that the user cannot resolve such implicit object injectable if they are not
   * provided by the user
   */
  private fun CallableRef.isValidForObjectRequest(): Boolean =
    originalType.constructor.declarationDescriptor.safeAs<ClassDescriptor>()?.kind != ClassKind.OBJECT ||
        (callable !is ReceiverParameterDescriptor ||
            callable.cast<ReceiverParameterDescriptor>()
              .value !is ImplicitClassReceiver ||
            originalType.constructor.declarationDescriptor!!.hasAnnotation(InjektFqNames.Provide))

  override fun toString(): String = "InjectablesScope($name)"
}
