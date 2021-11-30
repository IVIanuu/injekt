/*
 * Copyright 2021 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.DISPATCH_RECEIVER_NAME
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injectablesLookupName
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.isIde
import com.ivianuu.injekt.compiler.memberScopeForFqName
import com.ivianuu.injekt.compiler.nextFrameworkKey
import com.ivianuu.injekt.compiler.subInjectablesLookupName
import com.ivianuu.injekt.compiler.uniqueKey
import com.ivianuu.shaded_injekt.Inject
import com.ivianuu.shaded_injekt.Provide
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.util.UUID

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
  @Inject @Provide val ctx: Context
) {
  val chain: MutableList<Pair<InjectableRequest, Injectable>> = parent?.chain ?: mutableListOf()
  val resultsByType = mutableMapOf<TypeRef, ResolutionResult>()
  val resultsByCandidate = mutableMapOf<Injectable, ResolutionResult>()
  val typeScopes = mutableMapOf<TypeRefKey, InjectablesScope>()

  private val imports = imports.toMutableList()

  val injectables = mutableListOf<CallableRef>()

  val spreadingInjectables = mutableListOf<SpreadingInjectable>()
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

  val isNoOp: Boolean = parent?.isDeclarationContainer == true &&
      typeParameters.isEmpty() &&
      (isEmpty || (initialInjectables.isEmpty() && callContext == parent.callContext))

  val scopeToUse: InjectablesScope = if (isNoOp) parent!!.scopeToUse else this

  init {
    for (injectable in initialInjectables) {
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
            .copy(frameworkKey = callable.callable.uniqueKey())
          injectables += callable.copy(type = typeWithFrameworkKey)
          spreadingInjectableCandidateTypes += typeWithFrameworkKey
        },
        addSpreadingInjectable = { spreadingInjectables += SpreadingInjectable(it) }
      )
    }

    val hasSpreadingInjectables = spreadingInjectables.isNotEmpty()
    val hasSpreadingInjectableCandidates = spreadingInjectableCandidateTypes.isNotEmpty()
    if (parent != null) {
      spreadingInjectables.addAll(
        0,
        if (hasSpreadingInjectableCandidates) parent.spreadingInjectables.map { it.copy() }
        else parent.spreadingInjectables
      )
      spreadingInjectableCandidateTypes.addAll(0, parent.spreadingInjectableCandidateTypes)
    }

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
    if (isIde) return
    if (this in visitedScopes) return
    visitedScopes += this

    parent?.recordLookup(lookupLocation, visitedScopes)
    typeScopes.forEach { it.value.recordLookup(lookupLocation, visitedScopes) }
    for (import in imports) {
      memberScopeForFqName(import.packageFqName, lookupLocation)
        ?.first
        ?.recordLookup(injectablesLookupName, lookupLocation)
      if (import.importPath!!.endsWith(".**")) {
        memberScopeForFqName(import.packageFqName, lookupLocation)
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
    ).filter { it.callable.isValidObjectRequest(request) }
  }

  private fun injectablesForType(key: CallableRequestKey): List<CallableInjectable> {
    if (injectables.isEmpty())
      return parent?.injectablesForType(key) ?: emptyList()
    return injectablesByRequest.getOrPut(key) {
      buildList {
        parent?.injectablesForType(key)?.let { addAll(it) }

        for (candidate in injectables) {
          if (candidate.type.frameworkKey != key.type.frameworkKey) continue
          val context = candidate.type.buildContext(key.type, key.staticTypeParameters)
          if (!context.isOk) continue
          this += CallableInjectable(
            this@InjectablesScope,
            candidate.substitute(context.fixedTypeVariables)
          )
        }
      }
    }
  }

  fun frameworkInjectableForRequest(request: InjectableRequest): Injectable? {
    when {
      request.type.isFunctionType -> {
        val finalCallContext = if (request.isInline) callContext
        else request.type.callContext()
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

          val elements = listElementsForType(singleElementType, collectionElementType, key) +
              frameworkListElementsForType(singleElementType)

          return if (elements.isEmpty()) null
          else ListInjectable(
            type = request.type,
            ownerScope = this,
            elements = elements,
            singleElementType = singleElementType,
            collectionElementType = collectionElementType
          )
        }

        val typeScope = TypeInjectablesScopeOrNull(request.type, this)
          .takeUnless { it.isEmpty }
        return if (typeScope != null) typeScope.frameworkInjectableForRequest(request)
        else createInjectable()
      }
      request.type.classifier.fqName == injektFqNames().typeKey ->
        return TypeKeyInjectable(request.type, this)
      request.type.classifier.fqName == injektFqNames().sourceKey ->
        return SourceKeyInjectable(request.type, this)
      else -> return null
    }
  }

  private fun listElementsForType(
    singleElementType: TypeRef,
    collectionElementType: TypeRef,
    key: CallableRequestKey
  ): List<TypeRef> {
    if (injectables.isEmpty())
      return parent?.listElementsForType(singleElementType, collectionElementType, key) ?: emptyList()

    return buildList {
      fun addThisInjectables() {
        for (candidate in injectables.toList()) {
          if (candidate.type.frameworkKey != key.type.frameworkKey) continue

          var context =
            candidate.type.buildContext(singleElementType, key.staticTypeParameters)
          if (!context.isOk)
            context = candidate.type.buildContext(collectionElementType, key.staticTypeParameters)
          if (!context.isOk) continue

          val finalCandidate = candidate.substitute(context.fixedTypeVariables)

          val typeWithFrameworkKey = finalCandidate.type.copy(
            frameworkKey = finalCandidate.callable.uniqueKey()
          )

          injectables += finalCandidate.copy(type = typeWithFrameworkKey)

          this += typeWithFrameworkKey
        }
      }

      // if we are a type scope we wanna appear in the list before the other scopes
      if (typeScopeType != null)
        addThisInjectables()

      parent?.listElementsForType(singleElementType, collectionElementType, key)
        ?.let { addAll(it) }

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

          for (candidateType in listElementsForType(
            providerReturnType, ctx.collectionClassifier
              .defaultType.withArguments(listOf(providerReturnType)), innerKey))
            candidateType.add()

          for (candidateType in frameworkListElementsForType(providerReturnType))
            candidateType.add()
        }
      }
      singleElementType.classifier.fqName == injektFqNames().sourceKey -> listOf(
        ctx.sourceKeyClassifier!!.defaultType
          .copy(frameworkKey = UUID.randomUUID().toString())
      )
      singleElementType.classifier.fqName == injektFqNames().typeKey -> listOf(singleElementType)
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
    if (candidateType in spreadingInjectable.processedCandidateTypes) return
    spreadingInjectable.processedCandidateTypes += candidateType
    val (context, substitutionMap) = buildContextForSpreadingInjectable(
      spreadingInjectable.constraintType,
      candidateType,
      allStaticTypeParameters
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
        this.imports += ResolvedProviderImport(
          null,
          "${importFqName}.*",
          packageFqName
        )
      },
      addInjectable = { newInnerInjectable ->
        val finalNewInnerInjectable = newInnerInjectable
          .copy(originalType = newInnerInjectable.type)
        injectables += finalNewInnerInjectable
        val newInnerInjectableWithFrameworkKey = finalNewInnerInjectable.copy(
          type = finalNewInnerInjectable.type.copy(
            frameworkKey = spreadingInjectable.callable.type.frameworkKey
              .nextFrameworkKey(finalNewInnerInjectable.callable.uniqueKey())
              .also { spreadingInjectable.resultingFrameworkKeys += it }
          )
        )
        injectables += newInnerInjectableWithFrameworkKey
        spreadingInjectableCandidateTypes += newInnerInjectableWithFrameworkKey.type
        spreadInjectables(newInnerInjectableWithFrameworkKey.type)
      },
      addSpreadingInjectable = { newInnerInjectable ->
        val finalNewInnerInjectable = newInnerInjectable
          .copy(originalType = newInnerInjectable.type)
        val newSpreadingInjectable = SpreadingInjectable(finalNewInnerInjectable)
        spreadingInjectables += newSpreadingInjectable
        for (candidate in spreadingInjectableCandidateTypes.toList())
          spreadInjectables(newSpreadingInjectable, candidate)
      }
    )
  }

  /**
   * We add implicit injectables for objects under some circumstances to allow
   * callables in it to resolve their dispatch receiver parameter
   * Here we ensure that the user cannot resolve such implicit object injectable if they are not
   * provided by the user
   */
  private fun CallableRef.isValidObjectRequest(request: InjectableRequest): Boolean =
    !originalType.classifier.isObject ||
        request.parameterName == DISPATCH_RECEIVER_NAME ||
        (callable !is ReceiverParameterDescriptor ||
            callable.cast<ReceiverParameterDescriptor>()
              .value !is ImplicitClassReceiver ||
            originalType.classifier.descriptor!!.hasAnnotation(injektFqNames().provide))

  override fun toString(): String = "InjectablesScope($name)"
}
