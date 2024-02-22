/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.calls.components.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.*

sealed interface Injectable {
  val type: KotlinType
  val dependencies: List<InjectableRequest> get() = emptyList()
  val dependencyScope: InjectablesScope? get() = null
  val callableFqName: FqName
  val ownerScope: InjectablesScope
}

class CallableInjectable(
  override val ownerScope: InjectablesScope,
  val callable: CallableRef,
  override val type: KotlinType
) : Injectable {
  override val dependencies = (if (callable.callable is ConstructorDescriptor) callable.callable.valueParameters
      else callable.callable.allParameters)
    .map { it.toInjectableRequest(callable, ownerScope.ctx) }
  override val callableFqName = if (callable.callable is ClassConstructorDescriptor)
    callable.callable.constructedClass.fqNameSafe
  else callable.callable.fqNameSafe
}

class ListInjectable(
  override val type: KotlinType,
  override val ownerScope: InjectablesScope,
  elements: List<KotlinType>,
  val singleElementType: KotlinType,
  val collectionElementType: KotlinType
) : Injectable {
  override val callableFqName = FqName("listOf")
  override val dependencies = elements
    .mapIndexed { index, element ->
      InjectableRequest(
        type = element,
        typeKey = element.injektHashCode(ownerScope.ctx),
        callableFqName = callableFqName,
        callableTypeArguments = type.constructor.parameters
          .zip(type.arguments)
          .toMap(),
        parameterName = "element$index".asNameId(),
        parameterIndex = index
      )
    }
}

class LambdaInjectable(
  override val ownerScope: InjectablesScope,
  request: InjectableRequest
) : Injectable {
  override val type = request.type
  override val callableFqName = FqName(request.parameterName.asString())
  override val dependencies = listOf(
    InjectableRequest(
      type = type.arguments.last().type,
      typeKey = type.arguments.last().type.injektHashCode(ownerScope.ctx),
      callableFqName = callableFqName,
      parameterName = "instance".asNameId(),
      parameterIndex = 0
    )
  )

  val parameterDescriptors = type
    .constructor
    .declarationDescriptor
    .cast<ClassDescriptor>()
    .unsubstitutedMemberScope
    .getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
    .first()
    .valueParameters
    .map { ParameterDescriptor(it, this) }

  override val dependencyScope = InjectableScopeOrParent(
    name = "LAMBDA $type",
    parent = ownerScope,
    ctx = ownerScope.ctx,
    initialInjectables = parameterDescriptors
      .mapIndexed { index, parameter ->
        parameter
          .toCallableRef(ownerScope.ctx)
          .copy(type = type.arguments[index].type)
      }
  )

  // required to distinct between individual lambdas in codegen
  class ParameterDescriptor(
    private val delegate: ValueParameterDescriptor,
    val lambdaInjectable: LambdaInjectable
  ) : ValueParameterDescriptor by delegate
}

data class InjectableRequest(
  val typeKey: Int,
  val type: KotlinType,
  val callableFqName: FqName,
  val callableTypeArguments: Map<TypeParameterDescriptor, TypeProjection> = emptyMap(),
  val parameterName: Name,
  val parameterIndex: Int,
  val isRequired: Boolean = true
)

fun ParameterDescriptor.toInjectableRequest(callable: CallableRef, ctx: Context): InjectableRequest =
  InjectableRequest(
    type = callable.parameterTypes[injektIndex()]!!,
    typeKey = callable.parameterTypes[injektIndex()]!!.injektHashCode(ctx),
    callableFqName = callable.callableFqName,
    callableTypeArguments = callable.typeArguments,
    parameterName = injektName(),
    parameterIndex = injektIndex(),
    isRequired = this !is ValueParameterDescriptor ||
        injektIndex() in callable.injectParameters || !hasDefaultValue()
  )
