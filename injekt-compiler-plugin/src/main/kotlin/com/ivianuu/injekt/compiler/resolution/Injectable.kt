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

import com.ivianuu.injekt.compiler.DISPATCH_RECEIVER_INDEX
import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.analysis.hasDefaultValueIgnoringInject
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.injektIndex
import com.ivianuu.injekt.compiler.injektName
import com.ivianuu.injekt.compiler.uniqueKey
import com.ivianuu.injekt_shaded.Inject
import com.ivianuu.injekt_shaded.Provide
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
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
  abstract val dependencies: List<InjectableRequest>
  abstract val dependencyScopes: Map<InjectableRequest, InjectablesScope>
  abstract val callableFqName: FqName
  abstract val callContext: CallContext
  abstract val ownerScope: InjectablesScope
  abstract val usageKey: Any
}

class CallableInjectable(
  override val type: TypeRef,
  override val dependencies: List<InjectableRequest>,
  @Provide override val ownerScope: InjectablesScope,
  val callable: CallableRef,
) : Injectable() {
  override val callableFqName: FqName = if (callable.callable is ClassConstructorDescriptor)
    callable.callable.constructedClass.fqNameSafe
  else callable.callable.fqNameSafe
  override val callContext: CallContext
    get() = callable.callable.callContext(ownerScope.context)
  override val dependencyScopes: Map<InjectableRequest, InjectablesScope> = emptyMap()
  override val originalType: TypeRef
    get() = callable.originalType
  override val scopeComponentType: TypeRef?
    get() = callable.scopeComponentType
  override val usageKey: Any
    get() = listOf(callable.callable.uniqueKey(), callable.parameterTypes, callable.type)
}

class ComponentInjectable(
  override val type: TypeRef,
  @Provide override val ownerScope: InjectablesScope
) : Injectable() {
  override val callableFqName: FqName = FqName(type.classifier.fqName.asString() + "Impl")

  val entryPoints = ownerScope.entryPointsForType(type)

  @OptIn(ExperimentalStdlibApi::class)
  val requestCallables: List<CallableRef> = buildList<CallableRef> {
    val seen = mutableListOf<TypeRef>()
    fun visit(type: TypeRef) {
      if (type in seen) return
      seen += type

      type.collectComponentCallables()
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

    entryPoints.forEach { visit(it) }
  }

  val componentObserversRequest = InjectableRequest(
    type = ownerScope.context.setClassifier.defaultType.copy(
      arguments = listOf(
        ownerScope.context.componentObserverType.defaultType.copy(
          arguments = listOf(type)
        )
      )
    ),
    callableFqName = callableFqName.child("observers".asNameId()),
    parameterName = "observers".asNameId(),
    parameterIndex = -1,
    isRequired = false,
    isLazy = true
  )

  val componentAndEntryPointInjectables = entryPoints
    .map {
      it.classifier.descriptor.cast<ClassDescriptor>().injectableReceiver()
    } + type.classifier.descriptor.cast<ClassDescriptor>().injectableReceiver()

  val componentScope = InjectablesScope(
    name = "COMPONENT $callableFqName",
    parent = ownerScope,
    context = ownerScope.context,
    componentType = type,
    initialInjectables = componentAndEntryPointInjectables
  )

  val requestsByRequestCallables = requestCallables
    .withIndex()
    .associateWith { (index, requestCallable) ->
      InjectableRequest(
        type = requestCallable.type,
        callableFqName = callableFqName,
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

  override val dependencies = requestsByRequestCallables.values + componentObserversRequest

  val dependencyScopesByRequestCallable = requestCallables
    .associateWith { requestCallable ->
      InjectablesScope(
        name = "COMPONENT FUNCTION ${callableFqName.child(requestCallable.callable.name)}",
        parent = componentScope,
        context = componentScope.context,
        callContext = requestCallable.callable.callContext(),
        initialInjectables = requestCallable.callable.allParameters
          .filter { it != requestCallable.callable.dispatchReceiverParameter }
          .map { it.toCallableRef(componentScope.context) }
      )
    }

  @OptIn(ExperimentalStdlibApi::class)
  override val dependencyScopes: Map<InjectableRequest, InjectablesScope> = buildMap {
    dependencyScopesByRequestCallable.forEach {
      this[requestsByRequestCallables[it.key]!!] = it.value
    }
    this[componentObserversRequest] = componentScope
  }

  override val callContext: CallContext
    get() = CallContext.DEFAULT
  override val originalType: TypeRef
    get() = type
  override val scopeComponentType: TypeRef?
    get() = null
  override val usageKey: Any
    get() = type
}

class SetInjectable(
  override val type: TypeRef,
  override val ownerScope: InjectablesScope,
  override val dependencies: List<InjectableRequest>,
  val singleElementType: TypeRef,
  val collectionElementType: TypeRef
) : Injectable() {
  override val callableFqName: FqName =
    FqName("com.ivianuu.injekt.injectSetOf<${type.arguments[0].renderToString()}>")
  override val callContext: CallContext
    get() = CallContext.DEFAULT
  override val dependencyScopes: Map<InjectableRequest, InjectablesScope>
    get() = emptyMap()
  override val originalType: TypeRef
    get() = type.classifier.defaultType
  override val scopeComponentType: TypeRef?
    get() = null
  override val usageKey: Any
    get() = type
}

class ProviderInjectable(
  override val type: TypeRef,
  @Provide override val ownerScope: InjectablesScope,
  val isInline: Boolean,
  dependencyCallContext: CallContext
) : Injectable() {
  override val callableFqName: FqName = when (type.callContext) {
    CallContext.DEFAULT -> FqName("com.ivianuu.injekt.providerOf")
    CallContext.COMPOSABLE -> FqName("com.ivianuu.injekt.composableProviderOf")
    CallContext.SUSPEND -> FqName("com.ivianuu.injekt.suspendProviderOf")
  }
  override val dependencies: List<InjectableRequest> = listOf(
    InjectableRequest(
      type = type.arguments.last(),
      callableFqName = callableFqName,
      parameterName = "instance".asNameId(),
      parameterIndex = 0,
      isInline = isInline,
      isLazy = !isInline
    )
  )

  val parameterDescriptors = type
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
      context = ownerScope.context,
      callContext = dependencyCallContext,
      initialInjectables = parameterDescriptors
        .mapIndexed { index, parameter ->
          parameter
            .toCallableRef()
            .copy(isProvide = true, type = type.arguments[index])
        }
    )
  )
  override val callContext: CallContext
    get() = CallContext.DEFAULT
  override val originalType: TypeRef
    get() = type.classifier.defaultType
  override val scopeComponentType: TypeRef?
    get() = null
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
          type = ownerScope.context.injektContext.typeKeyType.defaultType
            .withArguments(listOf(typeParameter.defaultType)),
          callableFqName = callableFqName,
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
  override val usageKey: Any
    get() = type
}

fun CallableRef.getInjectableRequests(
  @Inject context: InjektContext
): List<InjectableRequest> = callable.allParameters
  .filter {
    callable !is ClassConstructorDescriptor || it.name.asString() != "<this>"
  }
  .filter {
    it === callable.dispatchReceiverParameter ||
        it === callable.extensionReceiverParameter ||
        it.isProvide()
  }
  .map { it.toInjectableRequest(this) }

data class InjectableRequest(
  val type: TypeRef,
  val callableFqName: FqName,
  val parameterName: Name,
  val parameterIndex: Int,
  val isRequired: Boolean = true,
  val isInline: Boolean = false,
  val isLazy: Boolean = false
)

fun ParameterDescriptor.toInjectableRequest(callable: CallableRef): InjectableRequest {
  val index = injektIndex()
  return InjectableRequest(
    type = callable.parameterTypes[index]!!,
    callableFqName = containingDeclaration.fqNameSafe,
    parameterName = injektName(),
    parameterIndex = injektIndex(),
    isRequired = this !is ValueParameterDescriptor || !hasDefaultValueIgnoringInject,
    isInline = callable.callable.safeAs<FunctionDescriptor>()?.isInline == true &&
        InlineUtil.isInlineParameter(this)
  )
}
