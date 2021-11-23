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
import com.ivianuu.injekt.compiler.DISPATCH_RECEIVER_INDEX
import com.ivianuu.injekt.compiler.analysis.hasDefaultValueIgnoringInject
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.injektIndex
import com.ivianuu.injekt.compiler.injektName
import com.ivianuu.injekt.compiler.uniqueKey
import com.ivianuu.shaded_injekt.Inject
import com.ivianuu.shaded_injekt.Provide
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

sealed class Injectable {
  abstract val type: TypeRef
  abstract val originalType: TypeRef
  abstract val scopeComponentType: TypeRef?
  abstract val isEager: Boolean
  abstract val dependencies: List<InjectableRequest>
  abstract val dependencyScopes: Map<InjectableRequest, InjectablesScope>
  abstract val callableFqName: FqName
  abstract val callContext: CallContext
  abstract val ownerScope: InjectablesScope
  abstract val usageKey: Any
}

class CallableInjectable(
  @Provide override val ownerScope: InjectablesScope,
  val callable: CallableRef,
) : Injectable() {
  override val type: TypeRef
    get() = callable.type
  override val dependencies: List<InjectableRequest> = callable.getInjectableRequests()
  override val callableFqName: FqName = if (callable.callable is ClassConstructorDescriptor)
    callable.callable.constructedClass.fqNameSafe
  else callable.callable.fqNameSafe
  override val callContext: CallContext
    get() = callable.callable.callContext()
  override val dependencyScopes: Map<InjectableRequest, InjectablesScope> = emptyMap()
  override val originalType: TypeRef
    get() = callable.originalType
  override val scopeComponentType: TypeRef?
    get() = callable.scopeComponentType
  override val isEager: Boolean
    get() = callable.isEager
  override val usageKey: Any =
    listOf(callable.callable.uniqueKey(), callable.parameterTypes, callable.type)

  override fun equals(other: Any?): Boolean =
    other is CallableInjectable && other.usageKey == usageKey

  override fun hashCode(): Int = usageKey.hashCode()
}

@OptIn(ExperimentalStdlibApi::class)
class AbstractInjectable(
  val constructor: CallableRef,
  val entryPoints: List<CallableRef>,
  @Provide override val ownerScope: InjectablesScope,
  val isComponent: Boolean,
  parentScope: InjectablesScope
) : Injectable() {
  override val callableFqName: FqName = type.classifier.fqName

  val requestCallables: List<CallableRef> = buildList<CallableRef> {
    val seen = mutableListOf<TypeRef>()
    fun visit(type: TypeRef) {
      if (type in seen) return
      seen += type

      type.collectAbstractInjectableCallables()
        .forEach { callable ->
          if (none {
              it.callable.name == callable.callable.name &&
                  it.callable is PropertyDescriptor == callable.callable is PropertyDescriptor &&
                  it.type == callable.type &&
                  it.parameterTypes.filter { it.key != DISPATCH_RECEIVER_INDEX } ==
                  callable.parameterTypes.filter { it.key != DISPATCH_RECEIVER_INDEX }
            }) this += callable
        }

      type.superTypes.forEach { visit(it) }
    }

    visit(type)

    entryPoints.forEach { visit(it.type) }
  }

  val constructorDependencies = constructor.getInjectableRequests(true)

  val componentObserversRequest: InjectableRequest? = if (!isComponent) null
  else InjectableRequest(
    type = ownerScope.ctx.listClassifier.defaultType.copy(
      arguments = listOf(
        ownerScope.ctx.componentObserverClassifier!!.defaultType.copy(
          arguments = listOf(type)
        )
      )
    ),
    callableFqName = callableFqName,
    callableTypeArguments = type.classifier.typeParameters.zip(type.arguments).toMap(),
    parameterName = "observers".asNameId(),
    parameterIndex = 0,
    isRequired = false,
    failOnAllCandidateErrors = true,
    isLazy = true
  )

  private val abstractInjectableAndEntryPointInjectables = entryPoints
    .map {
      it.type.classifier.descriptor.cast<ClassDescriptor>().injectableReceiver(true)
    } + type.classifier.descriptor.cast<ClassDescriptor>().injectableReceiver(true)

  val initScope = InjectablesScope(
    name = "ABSTRACT INJECTABLE INIT $callableFqName",
    parent = parentScope,
    ctx = ownerScope.ctx,
    componentType = if (isComponent) type else null
  )

  val scope = InjectablesScope(
    name = "ABSTRACT INJECTABLE $callableFqName",
    parent = parentScope,
    ctx = ownerScope.ctx,
    componentType = if (isComponent) type else null,
    initialInjectables = abstractInjectableAndEntryPointInjectables,
    injectablesPredicate = { candidate ->
      requestCallables.none { it.callable == candidate.callable }
    }
  )

  val requestsByRequestCallables = requestCallables
    .withIndex()
    .associateWith { (index, requestCallable) ->
      InjectableRequest(
        type = requestCallable.type,
        callableFqName = callableFqName,
        callableTypeArguments = type.classifier.typeParameters.zip(type.arguments).toMap(),
        parameterName = requestCallable.callable.name,
        parameterIndex = index,
        isRequired = requestCallable.callable
          .cast<CallableMemberDescriptor>()
          .modality == Modality.ABSTRACT,
        isLazy = true
      )
    }
    .mapKeys { it.key.value }
    .mapValues { it.value }

  override val dependencies = buildList<InjectableRequest> {
    this += constructorDependencies
    this += requestsByRequestCallables.values
    if (componentObserversRequest != null)
      this += componentObserversRequest
  }

  val dependencyScopesByRequestCallable = requestCallables
    .associateWith { requestCallable ->
      InjectablesScope(
        name = "ABSTRACT INJECTABLE CALLABLE ${callableFqName.child(requestCallable.callable.name)}",
        parent = scope,
        ctx = scope.ctx,
        callContext = requestCallable.callable.callContext(),
        initialInjectables = requestCallable.callable.allParameters
          .filter { it != requestCallable.callable.dispatchReceiverParameter }
          .map {
            if (it is ReceiverParameterDescriptor)
              ComponentReceiverParameterDescriptor(it)
            else
              ComponentValueParameterDescriptor(it.cast())
          }
          .map { it.toCallableRef(scope.ctx) }
      )
    }

  @OptIn(ExperimentalStdlibApi::class)
  override val dependencyScopes: Map<InjectableRequest, InjectablesScope> = buildMap {
    constructorDependencies.forEach {
      this[it] = initScope
    }
    dependencyScopesByRequestCallable.forEach {
      this[requestsByRequestCallables[it.key]!!] = it.value
    }
    if (componentObserversRequest != null)
      this[componentObserversRequest] = scope
  }

  override val callContext: CallContext
    get() = CallContext.DEFAULT
  override val type: TypeRef
    get() = constructor.type
  override val originalType: TypeRef
    get() = type
  override val scopeComponentType: TypeRef?
    get() = null
  override val isEager: Boolean
    get() = false
  override val usageKey: Any
    get() = type

  // required to distinct between individual components in codegen
  class ComponentReceiverParameterDescriptor(
    private val delegate: ReceiverParameterDescriptor
  ) : ReceiverParameterDescriptor by delegate
  class ComponentValueParameterDescriptor(
    private val delegate: ValueParameterDescriptor
  ) : ValueParameterDescriptor by delegate
}

class ListInjectable(
  override val type: TypeRef,
  override val ownerScope: InjectablesScope,
  elements: List<TypeRef>,
  val singleElementType: TypeRef,
  val collectionElementType: TypeRef
) : Injectable() {
  override val callableFqName: FqName = FqName("com.ivianuu.injekt.injectListOf")
  override val dependencies: List<InjectableRequest> = elements
    .mapIndexed { index, element ->
      InjectableRequest(
        type = element,
        callableFqName = callableFqName,
        callableTypeArguments = type.classifier.typeParameters.zip(type.arguments).toMap(),
        parameterName = "element$index".asNameId(),
        parameterIndex = index
      )
    }
  override val callContext: CallContext
    get() = CallContext.DEFAULT
  override val dependencyScopes = dependencies.associateWith { ownerScope }
  override val originalType: TypeRef
    get() = type.classifier.defaultType
  override val scopeComponentType: TypeRef?
    get() = null
  override val isEager: Boolean
    get() = false
  override val usageKey: Any
    get() = type
}

class ProviderInjectable(
  override val type: TypeRef,
  @Provide override val ownerScope: InjectablesScope,
  val isInline: Boolean,
  dependencyCallContext: CallContext
) : Injectable() {
  override val callableFqName: FqName = when (type.callContext()) {
    CallContext.DEFAULT -> FqName("providerOf")
    CallContext.COMPOSABLE -> FqName("composableProviderOf")
    CallContext.SUSPEND -> FqName("suspendProviderOf")
  }
  override val dependencies: List<InjectableRequest> = listOf(
    InjectableRequest(
      type = type.unwrapTags().arguments.last(),
      callableFqName = callableFqName,
      parameterName = "instance".asNameId(),
      parameterIndex = 0,
      isInline = isInline,
      isLazy = !isInline,
      isRequired = !type.unwrapTags().arguments.last().isNullableType
    )
  )

  val parameterDescriptors = type
    .unwrapTags()
    .classifier
    .descriptor!!
    .cast<ClassDescriptor>()
    .unsubstitutedMemberScope
    .getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
    .first()
    .valueParameters
    .map { ProviderValueParameterDescriptor(it) }

  override val dependencyScopes = mapOf(
    dependencies.single() to InjectablesScope(
      name = "PROVIDER $type",
      parent = ownerScope,
      ctx = ownerScope.ctx,
      callContext = dependencyCallContext,
      initialInjectables = parameterDescriptors
        .mapIndexed { index, parameter ->
          parameter
            .toCallableRef()
            .copy(type = type.unwrapTags().arguments[index])
        }
    )
  )
  override val callContext: CallContext
    get() = CallContext.DEFAULT
  override val originalType: TypeRef
    get() = type.unwrapTags().classifier.defaultType
  override val scopeComponentType: TypeRef?
    get() = null
  override val isEager: Boolean
    get() = false
  override val usageKey: Any
    get() = type

  // required to distinct between individual providers in codegen
  class ProviderValueParameterDescriptor(
    private val delegate: ValueParameterDescriptor
  ) : ValueParameterDescriptor by delegate
}

class SourceKeyInjectable(
  override val type: TypeRef,
  override val ownerScope: InjectablesScope
) : Injectable() {
  override val callableFqName: FqName = FqName("com.ivianuu.injekt.common.sourceKey")
  override val dependencies: List<InjectableRequest> = emptyList()
  override val dependencyScopes: Map<InjectableRequest, InjectablesScope>
    get() = emptyMap()
  override val callContext: CallContext
    get() = CallContext.DEFAULT
  override val originalType: TypeRef
    get() = type
  override val scopeComponentType: TypeRef?
    get() = null
  override val isEager: Boolean
    get() = false
  override val usageKey: Any
    get() = type
}

class TypeKeyInjectable(
  override val type: TypeRef,
  override val ownerScope: InjectablesScope
) : Injectable() {
  override val callableFqName: FqName = FqName("com.ivianuu.injekt.common.typeKeyOf<${type.renderToString()}>")
  override val dependencies: List<InjectableRequest> = run {
    val typeParameterDependencies = mutableSetOf<ClassifierRef>()
    type.allTypes.forEach {
      if (it.classifier.isTypeParameter)
        typeParameterDependencies += it.classifier
    }
    typeParameterDependencies
      .mapIndexed { index, typeParameter ->
        InjectableRequest(
          type = ownerScope.ctx.typeKeyClassifier!!.defaultType
            .withArguments(listOf(typeParameter.defaultType)),
          callableFqName = callableFqName,
          callableTypeArguments = type.classifier.typeParameters.zip(type.arguments).toMap(),
          parameterName = "${typeParameter.fqName.shortName()}Key".asNameId(),
          parameterIndex = index
        )
      }
  }
  override val dependencyScopes: Map<InjectableRequest, InjectablesScope> =
    dependencies.associateWith { ownerScope }
  override val callContext: CallContext
    get() = CallContext.DEFAULT
  override val originalType: TypeRef
    get() = type
  override val scopeComponentType: TypeRef?
    get() = null
  override val isEager: Boolean
    get() = false
  override val usageKey: Any
    get() = type
}

fun CallableRef.getInjectableRequests(
  ignoreInject: Boolean = false,
  @Inject ctx: Context
): List<InjectableRequest> = callable.allParameters
  .filter { callable !is ClassConstructorDescriptor || it.name.asString() != "<this>" }
  .filter {
    ignoreInject ||
        it === callable.dispatchReceiverParameter ||
        it === callable.extensionReceiverParameter ||
        it.isProvide()
  }
  .map { it.toInjectableRequest(this) }

data class InjectableRequest(
  val type: TypeRef,
  val callableFqName: FqName,
  val callableTypeArguments: Map<ClassifierRef, TypeRef> = emptyMap(),
  val parameterName: Name,
  val parameterIndex: Int,
  val isRequired: Boolean = true,
  val failOnAllCandidateErrors: Boolean = false,
  val isInline: Boolean = false,
  val isLazy: Boolean = false,
  val customErrorMessages: () -> CustomErrorMessages? = { null }
)

fun ParameterDescriptor.toInjectableRequest(
  callable: CallableRef,
  @Inject ctx: Context
): InjectableRequest = InjectableRequest(
  type = callable.parameterTypes[injektIndex()]!!,
  callableFqName = containingDeclaration.safeAs<ConstructorDescriptor>()
    ?.constructedClass?.fqNameSafe ?: containingDeclaration.fqNameSafe,
  callableTypeArguments = callable.typeArguments,
  parameterName = injektName(),
  parameterIndex = injektIndex(),
  isRequired = this !is ValueParameterDescriptor || !hasDefaultValueIgnoringInject,
  isInline = callable.callable.safeAs<FunctionDescriptor>()?.isInline == true &&
      InlineUtil.isInlineParameter(this),
  customErrorMessages = { toCallableRef().customErrorMessages() }
)
