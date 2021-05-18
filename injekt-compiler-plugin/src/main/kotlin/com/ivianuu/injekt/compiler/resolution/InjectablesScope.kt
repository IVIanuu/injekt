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

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.com.intellij.openapi.progress.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.callUtil.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.resolve.scopes.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class InjectablesScope(
  val name: String,
  val parent: InjectablesScope?,
  val context: InjektContext,
  val callContext: CallContext,
  val ownerDescriptor: DeclarationDescriptor?,
  val trace: BindingTrace,
  val initialInjectables: List<CallableRef>,
  imports: List<ResolvedProviderImport>,
  val typeParameters: List<ClassifierRef>
) {
  val chain: MutableList<Pair<InjectableRequest, Injectable>> = parent?.chain ?: mutableListOf()
  val resultsByType = mutableMapOf<TypeRef, ResolutionResult>()
  val resultsByCandidate = mutableMapOf<Injectable, ResolutionResult>()

  private data class InjectableKey(
    val type: TypeRef,
    val callable: CallableDescriptor,
    val source: CallableRef?
  )

  private val CallableRef.injectableKey: InjectableKey
    get() = InjectableKey(type, callable, source)

  /**
   * There should be only one injectable for a type + callable combination
   * If there are duplicates we choose the best version
   */
  private fun addInjectableIfAbsentOrBetter(callable: CallableRef) {
    val key = callable.injectableKey
    val existing = injectables[key]
    if (compareCallable(callable, existing) < 0)
      injectables[key] = callable
  }

  private val imports = imports.toMutableList()

  private val injectables = mutableMapOf<InjectableKey, CallableRef>()

  private val spreadingInjectables = mutableListOf<SpreadingInjectable>()
  private val spreadingInjectableCandidates = mutableListOf<SpreadingInjectableCandidate>()

  private data class SpreadingInjectable(
    val callable: CallableRef,
    val constraintType: TypeRef = callable.typeParameters.single {
      it.isSpread
    }.defaultType.substitute(callable.typeArguments),
    val processedCandidateTypes: MutableSet<TypeRef> = mutableSetOf(),
    val resultingFrameworkKeys: MutableSet<Int> = mutableSetOf()
  ) {
    fun copy() = SpreadingInjectable(
      callable,
      constraintType,
      processedCandidateTypes.toMutableSet(),
      resultingFrameworkKeys.toMutableSet()
    )
  }

  private data class SpreadingInjectableCandidate(
    val type: TypeRef,
    val rawType: TypeRef,
    val source: CallableRef?
  )

  val allParents: List<InjectablesScope> = parent?.allScopes ?: emptyList()
  val allScopes: List<InjectablesScope> = allParents + this

  val allStaticTypeParameters = allScopes.flatMap { it.typeParameters }

  data class CallableRequestKey(
    val type: TypeRef,
    val staticTypeParameters: List<ClassifierRef>
  )

  private val injectablesByRequest = mutableMapOf<CallableRequestKey, List<Injectable>?>()

  private val setElementsByType = mutableMapOf<CallableRequestKey, List<TypeRef>?>()

  private data class ProviderRequestKey(val type: TypeRef, val callContext: CallContext)

  private val providerInjectablesByRequest = mutableMapOf<ProviderRequestKey, ProviderInjectable>()
  private val setInjectablesByType = mutableMapOf<TypeRef, SetInjectable?>()

  init {
    measureTimeMillisWithResult {
      initialInjectables
        .forEach { injectable ->
          injectable.collectInjectables(
            context = context,
            scope = this,
            trace = trace,
            addImport = { importFqName, packageFqName ->
              this.imports += ResolvedProviderImport(
                null,
                "${importFqName}.*",
                packageFqName
              )
            },
            addInjectable = { callable ->
              addInjectableIfAbsentOrBetter(callable.copy(source = injectable))
              val typeWithFrameworkKey = callable.type
                .copy(frameworkKey = generateFrameworkKey())
              addInjectableIfAbsentOrBetter(callable.copy(type = typeWithFrameworkKey, source = injectable))
              spreadingInjectableCandidates += SpreadingInjectableCandidate(
                type = typeWithFrameworkKey,
                rawType = callable.type,
                source = injectable
              )
            },
            addSpreadingInjectable = { spreadingInjectables += SpreadingInjectable(it) }
          )
        }

      val hasSpreadingInjectables = spreadingInjectables.isNotEmpty()
      val hasSpreadingInjectableCandidates = spreadingInjectableCandidates.isNotEmpty()
      if (parent != null) {
        spreadingInjectables.addAll(
          0,
          parent.spreadingInjectables
            .map { if (hasSpreadingInjectableCandidates) it.copy() else it }
        )
        spreadingInjectableCandidates.addAll(0, parent.spreadingInjectableCandidates)
      }

      if ((hasSpreadingInjectables && spreadingInjectableCandidates.isNotEmpty()) ||
        (hasSpreadingInjectableCandidates && spreadingInjectables.isNotEmpty())
      ) {
        spreadingInjectableCandidates
          .toList()
          .forEach { spreadInjectables(it) }
      }
    }.let {
      println("initializing scope $name took ${it.first} ms")
    }
  }

  fun recordLookup(lookupLocation: LookupLocation) {
    if (isIde) return
    parent?.recordLookup(lookupLocation)
    fun recordLookup(declaration: DeclarationDescriptor) {
      if (declaration is ConstructorDescriptor) {
        recordLookup(declaration.constructedClass)
        return
      }
      if (declaration is ReceiverParameterDescriptor &&
          declaration.value is ImplicitClassReceiver) {
        recordLookup(declaration.value.cast<ImplicitClassReceiver>().classDescriptor)
        return
      }
      when (val containingDeclaration = declaration.containingDeclaration) {
        is ClassDescriptor -> containingDeclaration.unsubstitutedMemberScope
        is PackageFragmentDescriptor -> containingDeclaration.getMemberScope()
        else -> null
      }?.recordLookup(declaration.name, lookupLocation)
    }
    injectables.forEach {
      if (it.value.type.frameworkKey == 0)
        recordLookup(it.value.callable)
    }
    spreadingInjectables.forEach {
      if (it.callable.type.frameworkKey == 0)
        recordLookup(it.callable.callable)
    }
    imports.forEach { import ->
        context.memberScopeForFqName(import.packageFqName, lookupLocation)
          ?.recordLookup(
            injectablesLookupName(
              FqName(import.importPath!!.removeSuffix(".*")),
              import.packageFqName
            ),
            lookupLocation
          )
      }
  }

  fun injectablesForRequest(
    request: InjectableRequest,
    requestingScope: InjectablesScope
  ): List<Injectable>? {
    // we return merged collections
    if (request.type.frameworkKey == 0 &&
      request.type.classifier == context.setClassifier
    ) return null
    return injectablesForType(CallableRequestKey(request.type, requestingScope.allStaticTypeParameters))
      ?.filter { it -> it.isValidObjectRequest(request) }
      ?.takeIf { it.isNotEmpty() }
  }

  private fun injectablesForType(key: CallableRequestKey): List<Injectable>? {
    if (injectables.isEmpty()) return parent?.injectablesForType(key)
    return injectablesByRequest.getOrPut(key) {
      val thisInjectables = injectables
        .asSequence()
        .mapNotNull { (_, candidate) ->
          if (candidate.type.frameworkKey != key.type.frameworkKey)
            return@mapNotNull null
          val context = candidate.type.buildContext(context, key.staticTypeParameters, key.type)
          if (!context.isOk) return@mapNotNull null
          val substitutionMap = context.fixedTypeVariables
          val finalCandidate = candidate.substitute(substitutionMap)
          CallableInjectable(
            key.type,
            finalCandidate.getInjectableRequests(this.context, trace),
            this,
            finalCandidate
          )
        }
        .toList()
        .takeIf { it.isNotEmpty() }
      val parentInjectables = parent?.injectablesForType(key)
      if (parentInjectables != null && thisInjectables != null) parentInjectables + thisInjectables
      else thisInjectables ?: parentInjectables
    }
  }

  fun frameworkInjectableForRequest(request: InjectableRequest): Injectable? {
    if (request.type.frameworkKey != 0) return null
    if (request.type.isFunctionType) {
      val finalCallContext = if (request.isInline) callContext
      else request.type.callContext
      return providerInjectablesByRequest.getOrPut(
        ProviderRequestKey(request.type, finalCallContext)
      ) {
        ProviderInjectable(
          type = request.type,
          ownerScope = this,
          dependencyCallContext = finalCallContext
        )
      }
    } else if (request.type.classifier == context.setClassifier) {
      return setInjectablesByType.getOrPut(request.type) {
        val singleElementType = request.type.arguments[0]
        val collectionElementType = context.collectionClassifier.defaultType
          .withArguments(listOf(singleElementType))

        var elements = setElementsForType(
          singleElementType, collectionElementType,
          CallableRequestKey(request.type, allStaticTypeParameters)
        )
        if (elements == null && singleElementType.isFunctionType) {
          val providerReturnType = singleElementType.arguments.last()
          elements = setElementsForType(
            providerReturnType, context.collectionClassifier
              .defaultType.withArguments(listOf(providerReturnType)),
            CallableRequestKey(providerReturnType, allStaticTypeParameters)
          )
            ?.map { elementType ->
              singleElementType.copy(
                arguments = singleElementType.arguments
                  .dropLast(1) + elementType
              )
            }
        }

        if (elements != null) {
          val elementRequests = elements
            .mapIndexed { index, element ->
              InjectableRequest(
                type = element,
                defaultStrategy = if (request.type.ignoreElementsWithErrors)
                  InjectableRequest.DefaultStrategy.DEFAULT_ON_ALL_ERRORS
                else InjectableRequest.DefaultStrategy.NONE,
                callableFqName = FqName("com.ivianuu.injekt.injectSetOf<${request.type.arguments[0].render()}>"),
                parameterName = "element$index".asNameId(),
                isInline = false,
                isLazy = false
              )
            }
          SetInjectable(
            type = request.type,
            ownerScope = this,
            dependencies = elementRequests,
            singleElementType = singleElementType,
            collectionElementType = collectionElementType
          )
        } else null
      }
    }

    return null
  }

  private fun setElementsForType(
    singleElementType: TypeRef,
    collectionElementType: TypeRef,
    key: CallableRequestKey
  ): List<TypeRef>? {
    if (injectables.isEmpty())
      return parent?.setElementsForType(singleElementType, collectionElementType, key)
    return setElementsByType.getOrPut(key) {
      val thisElements: List<TypeRef>? = injectables
        .toList()
        .asSequence()
        .mapNotNull { (_, candidate) ->
          if (candidate.type.frameworkKey != key.type.frameworkKey)
            return@mapNotNull null
          var context =
            candidate.type.buildContext(context, key.staticTypeParameters, singleElementType)
          if (!context.isOk) {
            context = candidate.type.buildContext(
              this.context,
              key.staticTypeParameters,
              collectionElementType
            )
          }
          if (!context.isOk) return@mapNotNull null
          val substitutionMap = context.fixedTypeVariables
          candidate.substitute(substitutionMap)
        }
        .map { callable ->
          val typeWithFrameworkKey = callable.type.copy(
            frameworkKey = generateFrameworkKey()
          )
          addInjectableIfAbsentOrBetter(callable.copy(type = typeWithFrameworkKey))
          typeWithFrameworkKey
        }
        .toList()
        .takeIf { it.isNotEmpty() }
      val parentElements = parent?.setElementsForType(singleElementType, collectionElementType, key)
      if (parentElements != null && thisElements != null) parentElements + thisElements
      else thisElements ?: parentElements
    }
  }

  private fun spreadInjectables(candidate: SpreadingInjectableCandidate) {
    for (spreadingInjectable in spreadingInjectables.toList()) {
      checkCancelled()
      spreadInjectables(spreadingInjectable, candidate)
    }
  }

  private fun spreadInjectables(
    spreadingInjectable: SpreadingInjectable,
    candidate: SpreadingInjectableCandidate
  ) {
    if (candidate.type.frameworkKey in spreadingInjectable.resultingFrameworkKeys) return
    if (candidate.type in spreadingInjectable.processedCandidateTypes) return
    spreadingInjectable.processedCandidateTypes += candidate.type
    val (context, substitutionMap) = buildContextForSpreadingInjectable(
      context,
      spreadingInjectable.constraintType,
      candidate.type,
      allStaticTypeParameters
    )
    if (!context.isOk) return

    val newInjectableType = spreadingInjectable.callable.type
      .substitute(substitutionMap)
      .copy(frameworkKey = 0)
    val newInjectable = spreadingInjectable.callable
      .copy(
        type = newInjectableType,
        originalType = newInjectableType,
        parameterTypes = spreadingInjectable.callable.parameterTypes
          .mapValues { it.value.substitute(substitutionMap) },
        typeArguments = spreadingInjectable.callable
          .typeArguments
          .mapValues { it.value.substitute(substitutionMap) },
        source = candidate.source
      )

    newInjectable.collectInjectables(
      context = this.context,
      scope = this,
      trace = trace,
      addImport = { importFqName, packageFqName ->
        this.imports += ResolvedProviderImport(
          null,
          "${importFqName}.*",
          packageFqName
        )
      },
      addInjectable = { newInnerInjectable ->
        val finalNewInnerInjectable = newInnerInjectable
          .copy(
            source = candidate.source,
            originalType = newInnerInjectable.type
          )
        addInjectableIfAbsentOrBetter(finalNewInnerInjectable)
        val newInnerInjectableWithFrameworkKey = finalNewInnerInjectable.copy(
          type = finalNewInnerInjectable.type.copy(
            frameworkKey = generateFrameworkKey()
              .also { spreadingInjectable.resultingFrameworkKeys += it }
          )
        )
        addInjectableIfAbsentOrBetter(newInnerInjectableWithFrameworkKey)
        val newCandidate = SpreadingInjectableCandidate(
          type = newInnerInjectableWithFrameworkKey.type,
          rawType = finalNewInnerInjectable.type,
          source = candidate.source
        )
        spreadingInjectableCandidates += newCandidate
        spreadInjectables(newCandidate)
      },
      addSpreadingInjectable = { newInnerInjectable ->
        val finalNewInnerInjectable = newInnerInjectable
          .copy(
            source = candidate.source,
            originalType = newInnerInjectable.type
          )
        val newSpreadingInjectable = SpreadingInjectable(finalNewInnerInjectable)
        spreadingInjectables += newSpreadingInjectable
        spreadingInjectableCandidates
          .toList()
          .forEach {
            spreadInjectables(newSpreadingInjectable, it)
          }
      }
    )
  }

  /**
   * We add implicit injectables for objects under some circumstances to allow
   * callables in it to resolve their dispatch receiver parameter
   *
   * Here we ensure that the user cannot resolve such implicit object injectable if they are not
   * provided by the user
   */
  private fun Injectable.isValidObjectRequest(request: InjectableRequest): Boolean {
    if (!request.type.classifier.isObject) return true
    return request.parameterName.asString() == DISPATCH_RECEIVER_NAME ||
        (this !is CallableInjectable ||
            callable.callable !is ReceiverParameterDescriptor ||
            callable.callable.cast<ReceiverParameterDescriptor>()
              .value !is ImplicitClassReceiver ||
            request.type.classifier.descriptor!!.hasAnnotation(InjektFqNames.Provide))
  }

  override fun toString(): String = "InjectablesScope($name)"
}
