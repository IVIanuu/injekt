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
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.isTypeParameterTypeConstructor
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast

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
  override val dependencies = callable.callable.allParametersWithContext
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
        callableFqName = callableFqName,
        callableTypeArguments = type.constructor.parameters.zip(type.arguments.map { it.type }).toMap(),
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
      callableFqName = callableFqName,
      parameterName = "instance".asNameId(),
      parameterIndex = 0
    )
  )

  val parameterDescriptors = type
    .constructor
    .declarationDescriptor!!
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

class SourceKeyInjectable(
  override val type: KotlinType,
  override val ownerScope: InjectablesScope
) : Injectable {
  override val callableFqName = FqName("sourceKey")
}

class TypeKeyInjectable(
  override val type: KotlinType,
  override val ownerScope: InjectablesScope
) : Injectable {
  override val callableFqName = FqName("typeKeyOf<${type.renderToString()}>")
  override val dependencies = type.allTypes
    .filter { it.constructor.isTypeParameterTypeConstructor() }
    .mapIndexed { index, typeParameter ->
      InjectableRequest(
        type = ownerScope.ctx.typeKeyClassifier!!.defaultType
          .withArguments(listOf(typeParameter.asTypeProjection())),
        callableFqName = callableFqName,
        callableTypeArguments = type.constructor.parameters.zip(type.arguments.map { it.type }).toMap(),
        parameterName = "${typeParameter.constructor.declarationDescriptor!!.name}Key".asNameId(),
        parameterIndex = index
      )
    }
}

data class InjectableRequest(
  val type: KotlinType,
  val callableFqName: FqName,
  val callableTypeArguments: Map<TypeParameterDescriptor, KotlinType> = emptyMap(),
  val parameterName: Name,
  val parameterIndex: Int,
  val isRequired: Boolean = true
)

fun ParameterDescriptor.toInjectableRequest(callable: CallableRef, ctx: Context): InjectableRequest =
  InjectableRequest(
    type = callable.parameterTypes[injektIndex(ctx)]!!,
    callableFqName = callable.callableFqName,
    callableTypeArguments = callable.typeArguments,
    parameterName = injektName(ctx),
    parameterIndex = injektIndex(ctx),
    isRequired = this !is ValueParameterDescriptor || !hasDefaultValueIgnoringInject
  )
