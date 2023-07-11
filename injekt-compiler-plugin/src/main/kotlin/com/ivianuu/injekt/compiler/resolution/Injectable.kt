/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.allParametersWithContext
import com.ivianuu.injekt.compiler.analysis.hasDefaultValueIgnoringInject
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.injektIndex
import com.ivianuu.injekt.compiler.injektName
import com.ivianuu.injekt.compiler.transform
import com.ivianuu.injekt.compiler.uniqueKey
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

sealed interface Injectable {
  val type: TypeRef
  val originalType: TypeRef get() = type
  val dependencies: List<InjectableRequest> get() = emptyList()
  val dependencyScope: InjectablesScope? get() = null
  val callableFqName: FqName
  val ownerScope: InjectablesScope
  val usageKey: Any get() = type
}

context(Context) class CallableInjectable(
  override val ownerScope: InjectablesScope,
  val callable: CallableRef
) : Injectable {
  override val type: TypeRef get() = callable.type
  override val dependencies = callable.getInjectableRequests()
  override val callableFqName = if (callable.callable is ClassConstructorDescriptor)
    callable.callable.constructedClass.fqNameSafe
  else callable.callable.fqNameSafe
  override val originalType: TypeRef get() = callable.originalType
  override val usageKey =
    listOf(callable.callable.uniqueKey(), callable.parameterTypes, callable.type)

  override fun equals(other: Any?): Boolean =
    other is CallableInjectable && other.usageKey == usageKey

  override fun hashCode(): Int = usageKey.hashCode()
}

class ListInjectable(
  override val type: TypeRef,
  override val ownerScope: InjectablesScope,
  elements: List<TypeRef>,
  val singleElementType: TypeRef,
  val collectionElementType: TypeRef
) : Injectable {
  override val callableFqName = FqName("listOf")
  override val dependencies = elements
    .mapIndexed { index, element ->
      InjectableRequest(
        type = element,
        callableFqName = callableFqName,
        callableTypeArguments = type.classifier.typeParameters.zip(type.arguments).toMap(),
        parameterName = "element$index".asNameId(),
        parameterIndex = index
      )
    }
}

context(Context) class ProviderInjectable(
  override val type: TypeRef,
  override val ownerScope: InjectablesScope
) : Injectable {
  override val callableFqName = FqName("providerOf")
  override val dependencies = listOf(
    InjectableRequest(
      type = type.arguments.last(),
      callableFqName = callableFqName,
      parameterName = "instance".asNameId(),
      parameterIndex = 0
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

  override val dependencyScope = InjectableScopeOrParent(
    name = "PROVIDER $type",
    parent = ownerScope,
    initialInjectables = parameterDescriptors
      .mapIndexed { index, parameter ->
        parameter
          .toCallableRef()
          .copy(type = type.arguments[index])
      }
  )

  override val originalType: TypeRef
    get() = type.classifier.defaultType

  // required to distinct between individual providers in codegen
  class ProviderValueParameterDescriptor(
    private val delegate: ValueParameterDescriptor
  ) : ValueParameterDescriptor by delegate
}

class SourceKeyInjectable(
  override val type: TypeRef,
  override val ownerScope: InjectablesScope
) : Injectable {
  override val callableFqName = FqName("com.ivianuu.injekt.common.sourceKey")
}

context(Context) class TypeKeyInjectable(
  override val type: TypeRef,
  override val ownerScope: InjectablesScope
) : Injectable {
  override val callableFqName = FqName("com.ivianuu.injekt.common.typeKeyOf<${type.renderToString()}>")
  override val dependencies = type.allTypes
    .filter { it.classifier.isTypeParameter }
    .mapIndexed { index, typeParameter ->
      InjectableRequest(
        type = typeKeyClassifier!!.defaultType
          .withArguments(listOf(typeParameter.classifier.defaultType)),
        callableFqName = callableFqName,
        callableTypeArguments = type.classifier.typeParameters.zip(type.arguments).toMap(),
        parameterName = "${typeParameter.classifier.fqName.shortName()}Key".asNameId(),
        parameterIndex = index
      )
    }
}

context(Context) fun CallableRef.getInjectableRequests(): List<InjectableRequest> = callable.allParametersWithContext
  .transform {
    if (it === callable.dispatchReceiverParameter ||
      it === callable.extensionReceiverParameter ||
      it in callable.contextReceiverParameters ||
      it.isProvide() ||
      parameterTypes[it.injektIndex()]?.isInject == true)
      add(it.toInjectableRequest(this@getInjectableRequests))
  }

data class InjectableRequest(
  val type: TypeRef,
  val callableFqName: FqName,
  val callableTypeArguments: Map<ClassifierRef, TypeRef> = emptyMap(),
  val parameterName: Name,
  val parameterIndex: Int,
  val isRequired: Boolean = true
)

context(Context) fun ParameterDescriptor.toInjectableRequest(callable: CallableRef) =
  InjectableRequest(
    type = callable.parameterTypes[injektIndex()]!!,
    callableFqName = containingDeclaration.safeAs<ConstructorDescriptor>()
      ?.constructedClass?.fqNameSafe ?: containingDeclaration.fqNameSafe,
    callableTypeArguments = callable.typeArguments,
    parameterName = injektName(),
    parameterIndex = injektIndex(),
    isRequired = this !is ValueParameterDescriptor || !hasDefaultValueIgnoringInject
  )
