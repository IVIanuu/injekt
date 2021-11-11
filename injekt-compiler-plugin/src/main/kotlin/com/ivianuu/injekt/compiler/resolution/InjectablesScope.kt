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
import com.ivianuu.injekt.compiler.generateFrameworkKey
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.injectablesLookupName
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.isIde
import com.ivianuu.injekt.compiler.memberScopeForFqName
import com.ivianuu.injekt.compiler.subInjectablesLookupName
import com.ivianuu.shaded_injekt.Inject
import com.ivianuu.shaded_injekt.Provide
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.utils.addToStdlib.cast

class InjectablesScope(
  val name: String,
  val parent: InjectablesScope?,
  val callContext: CallContext = CallContext.DEFAULT,
  val ownerDescriptor: DeclarationDescriptor? = null,
  val file: KtFile? = null,
  val componentType: TypeRef? = null,
  val isTypeScope: Boolean = false,
  val isDeclarationContainer: Boolean = true,
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

  val allScopes: List<InjectablesScope> = parent?.allScopes?.let { it + this } ?: listOf(this)

  val allStaticTypeParameters = allScopes.flatMap { it.typeParameters }

  data class CallableRequestKey(val type: TypeRef, val staticTypeParameters: List<ClassifierRef>)

  private val injectablesByRequest = mutableMapOf<CallableRequestKey, List<CallableInjectable>>()

  private val listElementsByType = mutableMapOf<CallableRequestKey, List<TypeRef>>()

  private val components: MutableList<CallableRef> =
    parent?.components?.toMutableList() ?: mutableListOf()
  private val entryPoints: MutableList<CallableRef> =
    parent?.entryPoints?.toMutableList() ?: mutableListOf()

  init {
    initialInjectables
      .forEach { injectable ->
        injectable.collectInjectables(
          scope = this,
          addImport = { importFqName, packageFqName ->
            this.imports += ResolvedProviderImport(
              null,
              "${importFqName}.*",
              packageFqName
            )
          },
          addInjectable = { injectables += it },
          addComponent = { components += it },
          addEntryPoint = { entryPoints += it }
        )
      }
  }

  fun recordLookup(lookupLocation: LookupLocation) {
    if (isIde) return
    parent?.recordLookup(lookupLocation)
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
    if (request.type.frameworkKey == null &&
      request.type.classifier == ctx.listClassifier) return emptyList()

    return injectablesForType(CallableRequestKey(request.type, requestingScope.allStaticTypeParameters))
      .filter { it.isValidObjectRequest(request) }
  }

  private fun injectablesForType(key: CallableRequestKey): List<CallableInjectable> {
    if (injectables.isEmpty())
      return parent?.injectablesForType(key) ?: emptyList()
    return injectablesByRequest.getOrPut(key) {
      val thisInjectables = injectables
        .mapNotNull { candidate ->
          if (candidate.type.frameworkKey != key.type.frameworkKey)
            return@mapNotNull null
          val context = candidate.type.buildContext(key.type, key.staticTypeParameters)
          if (!context.isOk)
            return@mapNotNull null
          val finalCandidate = candidate.substitute(context.fixedTypeVariables)
          CallableInjectable(this, finalCandidate)
        }
      val parentInjectables = parent?.injectablesForType(key)
      if (parentInjectables != null) parentInjectables + thisInjectables
      else thisInjectables
    }
  }

  fun frameworkInjectableForRequest(request: InjectableRequest): Injectable? {
    when {
      request.type.isFunctionType -> {
        val finalCallContext = if (request.isInline) callContext
        else request.type.callContext
        return ProviderInjectable(
          type = request.type,
          ownerScope = this,
          dependencyCallContext = finalCallContext,
          isInline = request.isInline
        )
      }
      request.type.classifier == ctx.listClassifier -> {
        val singleElementType = request.type.arguments[0]
        val collectionElementType = ctx.collectionClassifier.defaultType
          .withArguments(listOf(singleElementType))

        var key = CallableRequestKey(request.type, allStaticTypeParameters)

        var elements = listElementsForType(singleElementType, collectionElementType, key) +
            frameworkListElementsForType(singleElementType, collectionElementType, key)
        if (elements.isEmpty() && singleElementType.isFunctionType) {
          val providerReturnType = singleElementType.arguments.last()
          key = CallableRequestKey(providerReturnType, allStaticTypeParameters)

          elements = (listElementsForType(
            providerReturnType, ctx.collectionClassifier
              .defaultType.withArguments(listOf(providerReturnType)), key) +
              frameworkListElementsForType(providerReturnType, ctx.collectionClassifier
                .defaultType.withArguments(listOf(providerReturnType)), key))
            .map { elementType ->
              singleElementType.copy(
                arguments = singleElementType.arguments
                  .dropLast(1) + elementType
              )
            }
        }

        return if (elements.isEmpty()) null
        else ListInjectable(
          type = request.type,
          ownerScope = this,
          elements = elements,
          singleElementType = singleElementType,
          collectionElementType = collectionElementType
        )
      }
      request.type.classifier.fqName == injektFqNames().typeKey ->
        return TypeKeyInjectable(request.type, this)
      request.type.classifier.fqName == injektFqNames().sourceKey ->
        return SourceKeyInjectable(request.type, this)
      request.type.unwrapTags().classifier.isComponent ->
        return componentForType(request.type)?.let {
          ComponentInjectable(it, entryPointsForType(it.type), this)
        }
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
    return listElementsByType.getOrPut(key) {
      val thisElements: List<TypeRef> = injectables
        .mapNotNull { candidate ->
          if (candidate.type.frameworkKey != key.type.frameworkKey)
            return@mapNotNull null
          var context =
            candidate.type.buildContext(singleElementType, key.staticTypeParameters)
          if (!context.isOk) {
            context = candidate.type.buildContext(collectionElementType, key.staticTypeParameters)
          }
          if (!context.isOk) return@mapNotNull null
          candidate.substitute(context.fixedTypeVariables)
        }
        .map { callable ->
          val typeWithFrameworkKey = callable.type.copy(
            frameworkKey = generateFrameworkKey()
          )
          injectables += callable.copy(type = typeWithFrameworkKey)
          typeWithFrameworkKey
        }
      val parentElements = parent?.listElementsForType(singleElementType, collectionElementType, key)
      if (parentElements != null) parentElements + thisElements
      else thisElements
    }
  }

  private fun frameworkListElementsForType(
    singleElementType: TypeRef,
    collectionElementType: TypeRef,
    key: CallableRequestKey
  ): List<TypeRef> {
    if (components.isEmpty()) return emptyList()
    return components
      .mapNotNull { candidate ->
        var context =
          candidate.type.buildContext(singleElementType, key.staticTypeParameters)
        if (!context.isOk) {
          context = candidate.type.buildContext(collectionElementType, key.staticTypeParameters)
        }
        if (!context.isOk) return@mapNotNull null
        candidate.substitute(context.fixedTypeVariables)
      }
      .map { candidate ->
        val typeWithFrameworkKey = candidate.type.copy(frameworkKey = generateFrameworkKey())
        components +=
          candidate.copy(type = typeWithFrameworkKey, originalType = typeWithFrameworkKey)
        typeWithFrameworkKey
      }
  }

  private fun entryPointsForType(componentType: TypeRef): List<CallableRef> {
    if (entryPoints.isEmpty()) return emptyList()
    return entryPoints
      .mapNotNull { candidate ->
        val context = candidate.type.classifier.entryPointComponentType!!
          .buildContext(componentType, allStaticTypeParameters)
        if (!context.isOk) return@mapNotNull null
        candidate.substitute(context.fixedTypeVariables)
      }
  }

  private fun componentForType(type: TypeRef): CallableRef? {
    if (components.isEmpty()) return null
    return components
      .firstNotNullOfOrNull { candidate ->
        val context = candidate.type.buildContext(type, allStaticTypeParameters)
        if (!context.isOk) return@firstNotNullOfOrNull null
        candidate.substitute(context.fixedTypeVariables)
      }
  }

  /**
   * We add implicit injectables for objects under some circumstances to allow
   * callables in it to resolve their dispatch receiver parameter
   * Here we ensure that the user cannot resolve such implicit object injectable if they are not
   * provided by the user
   */
  private fun Injectable.isValidObjectRequest(request: InjectableRequest): Boolean =
    !originalType.classifier.isObject ||
        request.parameterName == DISPATCH_RECEIVER_NAME ||
        (this !is CallableInjectable ||
            callable.callable !is ReceiverParameterDescriptor ||
            callable.callable.cast<ReceiverParameterDescriptor>()
              .value !is ImplicitClassReceiver ||
            originalType.classifier.descriptor!!.hasAnnotation(injektFqNames().provide))

  override fun toString(): String = "InjectablesScope($name)"
}
