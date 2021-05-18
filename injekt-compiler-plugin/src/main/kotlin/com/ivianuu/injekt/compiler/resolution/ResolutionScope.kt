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

class ResolutionScope(
  val name: String,
  val parent: ResolutionScope?,
  val context: InjektContext,
  val callContext: CallContext,
  val ownerDescriptor: DeclarationDescriptor?,
  val trace: BindingTrace,
  val initialGivens: List<CallableRef>,
  imports: List<ResolvedGivenImport>,
  val typeParameters: List<ClassifierRef>
) {
  val chain: MutableList<Pair<GivenRequest, GivenNode>> = parent?.chain ?: mutableListOf()
  val resultsByType = mutableMapOf<TypeRef, ResolutionResult>()
  val resultsByCandidate = mutableMapOf<GivenNode, ResolutionResult>()

  private data class GivenKey(
    val type: TypeRef,
    val callable: CallableDescriptor,
    val source: CallableRef?
  )

  private val CallableRef.givenKey: GivenKey
    get() = GivenKey(type, callable, source)

  /**
   * There should be only one given for a type + callable combination
   * If there are duplicates we choose the best version
   */
  private fun addGivenIfAbsentOrBetter(callable: CallableRef) {
    val key = callable.givenKey
    val existing = givens[key]
    if (compareCallable(callable, existing) < 0)
      givens[key] = callable
  }

  private val imports = imports.toMutableList()

  private val givens = mutableMapOf<GivenKey, CallableRef>()

  private val spreadingGivens = mutableListOf<SpreadingGivenNode>()
  private val spreadingGivenCandidates = mutableListOf<SpreadingGivenCandidate>()

  private data class SpreadingGivenNode(
    val callable: CallableRef,
    val constraintType: TypeRef = callable.typeParameters.single {
      it.isSpread
    }.defaultType.substitute(callable.typeArguments),
    val processedCandidateTypes: MutableSet<TypeRef> = mutableSetOf(),
    val resultingFrameworkKeys: MutableSet<Int> = mutableSetOf()
  ) {
    fun copy() = SpreadingGivenNode(
      callable,
      constraintType,
      processedCandidateTypes.toMutableSet(),
      resultingFrameworkKeys.toMutableSet()
    )
  }

  private data class SpreadingGivenCandidate(
    val type: TypeRef,
    val rawType: TypeRef,
    val source: CallableRef?
  )

  val allParents: List<ResolutionScope> = parent?.allScopes ?: emptyList()
  val allScopes: List<ResolutionScope> = allParents + this

  val allStaticTypeParameters = allScopes.flatMap { it.typeParameters }

  data class CallableRequestKey(
    val type: TypeRef,
    val staticTypeParameters: List<ClassifierRef>
  )

  private val givensByRequest = mutableMapOf<CallableRequestKey, List<GivenNode>?>()

  private val setElementsByType = mutableMapOf<CallableRequestKey, List<TypeRef>?>()

  private data class ProviderRequestKey(val type: TypeRef, val callContext: CallContext)

  private val providerGivensByRequest = mutableMapOf<ProviderRequestKey, ProviderGivenNode>()
  private val setGivensByType = mutableMapOf<TypeRef, SetGivenNode?>()

  init {
    measureTimeMillisWithResult {
      initialGivens
        .forEach { given ->
          given.collectGivens(
            context = context,
            scope = this,
            trace = trace,
            addImport = { importFqName, packageFqName ->
              this.imports += ResolvedGivenImport(
                null,
                "${importFqName}.*",
                packageFqName
              )
            },
            addGiven = { callable ->
              addGivenIfAbsentOrBetter(callable.copy(source = given))
              val typeWithFrameworkKey = callable.type
                .copy(frameworkKey = generateFrameworkKey())
              addGivenIfAbsentOrBetter(callable.copy(type = typeWithFrameworkKey, source = given))
              spreadingGivenCandidates += SpreadingGivenCandidate(
                type = typeWithFrameworkKey,
                rawType = callable.type,
                source = given
              )
            },
            addSpreadingGiven = { spreadingGivens += SpreadingGivenNode(it) }
          )
        }

      val hasSpreadingGivens = spreadingGivens.isNotEmpty()
      val hasSpreadingGivenCandidates = spreadingGivenCandidates.isNotEmpty()
      if (parent != null) {
        spreadingGivens.addAll(
          0,
          parent.spreadingGivens
            .map { if (hasSpreadingGivenCandidates) it.copy() else it }
        )
        spreadingGivenCandidates.addAll(0, parent.spreadingGivenCandidates)
      }

      if ((hasSpreadingGivens && spreadingGivenCandidates.isNotEmpty()) ||
        (hasSpreadingGivenCandidates && spreadingGivens.isNotEmpty())
      ) {
        spreadingGivenCandidates
          .toList()
          .forEach { spreadGivens(it) }
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
    givens.forEach {
      if (it.value.type.frameworkKey == 0)
        recordLookup(it.value.callable)
    }
    spreadingGivens.forEach {
      if (it.callable.type.frameworkKey == 0)
        recordLookup(it.callable.callable)
    }
    imports.forEach { import ->
        context.memberScopeForFqName(import.packageFqName, lookupLocation)
          ?.recordLookup(
            givensLookupName(
              FqName(import.importPath!!.removeSuffix(".*")),
              import.packageFqName
            ),
            lookupLocation
          )
      }
  }

  fun givensForRequest(request: GivenRequest, requestingScope: ResolutionScope): List<GivenNode>? {
    // we return merged collections
    if (request.type.frameworkKey == 0 &&
      request.type.classifier == context.setClassifier
    ) return null
    return givensForType(CallableRequestKey(request.type, requestingScope.allStaticTypeParameters))
      ?.filter { it -> it.isValidObjectRequest(request) }
      ?.takeIf { it.isNotEmpty() }
  }

  private fun givensForType(key: CallableRequestKey): List<GivenNode>? {
    if (givens.isEmpty()) return parent?.givensForType(key)
    return givensByRequest.getOrPut(key) {
      val thisGivens = givens
        .asSequence()
        .mapNotNull { (_, candidate) ->
          if (candidate.type.frameworkKey != key.type.frameworkKey)
            return@mapNotNull null
          val context = candidate.type.buildContext(context, key.staticTypeParameters, key.type)
          if (!context.isOk) return@mapNotNull null
          val substitutionMap = context.fixedTypeVariables
          val finalCandidate = candidate.substitute(substitutionMap)
          CallableGivenNode(
            key.type,
            finalCandidate.getGivenRequests(this.context, trace),
            this,
            finalCandidate
          )
        }
        .toList()
        .takeIf { it.isNotEmpty() }
      val parentGivens = parent?.givensForType(key)
      if (parentGivens != null && thisGivens != null) parentGivens + thisGivens
      else thisGivens ?: parentGivens
    }
  }

  fun frameworkGivenForRequest(request: GivenRequest): GivenNode? {
    if (request.type.frameworkKey != 0) return null
    if (request.type.isFunctionTypeWithOnlyGivenParameters) {
      val finalCallContext = if (request.isInline) callContext
      else request.type.callContext
      return providerGivensByRequest.getOrPut(
        ProviderRequestKey(request.type, finalCallContext)
      ) {
        ProviderGivenNode(
          type = request.type,
          ownerScope = this,
          dependencyCallContext = finalCallContext
        )
      }
    } else if (request.type.classifier == context.setClassifier) {
      return setGivensByType.getOrPut(request.type) {
        val singleElementType = request.type.arguments[0]
        val collectionElementType = context.collectionClassifier.defaultType
          .withArguments(listOf(singleElementType))

        var elements = setElementsForType(
          singleElementType, collectionElementType,
          CallableRequestKey(request.type, allStaticTypeParameters)
        )
        if (elements == null &&
          singleElementType.isFunctionTypeWithOnlyGivenParameters
        ) {
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
              GivenRequest(
                type = element,
                defaultStrategy = if (request.type.ignoreElementsWithErrors)
                  GivenRequest.DefaultStrategy.DEFAULT_ON_ALL_ERRORS
                else GivenRequest.DefaultStrategy.NONE,
                callableFqName = FqName("com.ivianuu.injekt.summonSetOf<${request.type.arguments[0].render()}>"),
                parameterName = "element$index".asNameId(),
                isInline = false,
                isLazy = false
              )
            }
          SetGivenNode(
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
    if (givens.isEmpty())
      return parent?.setElementsForType(singleElementType, collectionElementType, key)
    return setElementsByType.getOrPut(key) {
      val thisElements: List<TypeRef>? = givens
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
          addGivenIfAbsentOrBetter(callable.copy(type = typeWithFrameworkKey))
          typeWithFrameworkKey
        }
        .toList()
        .takeIf { it.isNotEmpty() }
      val parentElements = parent?.setElementsForType(singleElementType, collectionElementType, key)
      if (parentElements != null && thisElements != null) parentElements + thisElements
      else thisElements ?: parentElements
    }
  }

  private fun spreadGivens(candidate: SpreadingGivenCandidate) {
    for (spreadingGiven in spreadingGivens.toList()) {
      checkCancelled()
      spreadGivens(spreadingGiven, candidate)
    }
  }

  private fun spreadGivens(
    spreadingGiven: SpreadingGivenNode,
    candidate: SpreadingGivenCandidate
  ) {
    if (candidate.type.frameworkKey in spreadingGiven.resultingFrameworkKeys) return
    if (candidate.type in spreadingGiven.processedCandidateTypes) return
    spreadingGiven.processedCandidateTypes += candidate.type
    val (context, substitutionMap) = buildContextForSpreadingGiven(
      context,
      spreadingGiven.constraintType,
      candidate.type,
      allStaticTypeParameters
    )
    if (!context.isOk) return

    val newGivenType = spreadingGiven.callable.type
      .substitute(substitutionMap)
      .copy(frameworkKey = 0)
    val newGiven = spreadingGiven.callable
      .copy(
        type = newGivenType,
        originalType = newGivenType,
        parameterTypes = spreadingGiven.callable.parameterTypes
          .mapValues { it.value.substitute(substitutionMap) },
        typeArguments = spreadingGiven.callable
          .typeArguments
          .mapValues { it.value.substitute(substitutionMap) },
        source = candidate.source
      )

    newGiven.collectGivens(
      context = this.context,
      scope = this,
      trace = trace,
      addImport = { importFqName, packageFqName ->
        this.imports += ResolvedGivenImport(
          null,
          "${importFqName}.*",
          packageFqName
        )
      },
      addGiven = { newInnerGiven ->
        val finalNewInnerGiven = newInnerGiven
          .copy(
            source = candidate.source,
            originalType = newInnerGiven.type
          )
        addGivenIfAbsentOrBetter(finalNewInnerGiven)
        val newInnerGivenWithFrameworkKey = finalNewInnerGiven.copy(
          type = finalNewInnerGiven.type.copy(
            frameworkKey = generateFrameworkKey()
              .also { spreadingGiven.resultingFrameworkKeys += it }
          )
        )
        addGivenIfAbsentOrBetter(newInnerGivenWithFrameworkKey)
        val newCandidate = SpreadingGivenCandidate(
          type = newInnerGivenWithFrameworkKey.type,
          rawType = finalNewInnerGiven.type,
          source = candidate.source
        )
        spreadingGivenCandidates += newCandidate
        spreadGivens(newCandidate)
      },
      addSpreadingGiven = { newInnerSpreadingGiven ->
        val finalNewInnerSpreadingGiven = newInnerSpreadingGiven
          .copy(
            source = candidate.source,
            originalType = newInnerSpreadingGiven.type
          )
        val newSpreadingGivenNode = SpreadingGivenNode(finalNewInnerSpreadingGiven)
        spreadingGivens += newSpreadingGivenNode
        spreadingGivenCandidates
          .toList()
          .forEach {
            spreadGivens(newSpreadingGivenNode, it)
          }
      }
    )
  }

  /**
   * We add implicit givens for objects under some circumstances to allow
   * object callables to resolve their dispatch receiver parameter
   *
   * Here we ensure that the user cannot resolve such implicit object givens if they are not
   * marked as given
   */
  private fun GivenNode.isValidObjectRequest(request: GivenRequest): Boolean {
    if (!request.type.classifier.isObject) return true
    return request.parameterName.asString() == DISPATCH_RECEIVER_NAME || (
        this !is CallableGivenNode ||
            callable.callable !is ReceiverParameterDescriptor ||
            callable.callable.cast<ReceiverParameterDescriptor>()
              .value !is ImplicitClassReceiver ||
            request.type.classifier.descriptor!!.hasAnnotation(InjektFqNames.Given)
        )
  }

  override fun toString(): String = "ResolutionScope($name)"
}
