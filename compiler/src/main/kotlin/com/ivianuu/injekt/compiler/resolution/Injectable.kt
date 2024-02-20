/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, SymbolInternals::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.utils.addToStdlib.*

sealed interface Injectable {
  val type: ConeKotlinType
  val dependencies: List<InjectableRequest> get() = emptyList()
  val dependencyScope: InjectablesScope? get() = null
  val chainFqName: FqName
  val ownerScope: InjectablesScope
}

class CallableInjectable(
  override val ownerScope: InjectablesScope,
  val callable: InjektCallable,
  override val type: ConeKotlinType
) : Injectable {
  override val dependencies = callable.parameterTypes.map { (index, type) ->
    InjectableRequest(
      type = type,
      chainFqName = callable.chainFqName,
      parameterName = when (index) {
        DISPATCH_RECEIVER_INDEX -> DISPATCH_RECEIVER_NAME
        EXTENSION_RECEIVER_INDEX -> EXTENSION_RECEIVER_NAME
        else -> callable.callable.cast<FirFunctionSymbol<*>>().valueParameterSymbols[index].name
      },
      parameterIndex = index,
      isRequired = callable.callable.safeAs<FirFunctionSymbol<*>>()
        ?.valueParameterSymbols?.getOrNull(index)?.hasDefaultValue != false
    )
  }

  override val chainFqName get() = callable.chainFqName
}

class ListInjectable(
  override val type: ConeKotlinType,
  override val ownerScope: InjectablesScope,
  elements: List<ConeKotlinType>,
  val singleElementType: ConeKotlinType,
  val collectionElementType: ConeKotlinType
) : Injectable {
  override val chainFqName = FqName("listOf")
  override val dependencies = elements
    .mapIndexed { index, element ->
      InjectableRequest(
        type = element,
        chainFqName = chainFqName,
        //callableTypeArguments = type.classifier.typeParameters.zip(type.arguments).toMap(),
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
  override val chainFqName = FqName(request.parameterName.asString())
  override val dependencies = listOf(
    InjectableRequest(
      type = type.typeArguments.last().type!!,
      chainFqName = chainFqName,
      parameterName = "instance".asNameId(),
      parameterIndex = 0
    )
  )

  /*val parameterDescriptors = type
    .classifier
    .descriptor!!
    .cast<ClassDescriptor>()
    .unsubstitutedMemberScope
    .getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
    .first()
    .valueParameters
    .map { ParameterDescriptor(it, this) }*/

  override val dependencyScope = /*InjectableScopeOrParent(
    name = "LAMBDA $type",
    parent = ownerScope,
    ctx = ownerScope.ctx,
    initialInjectables = parameterDescriptors
      .mapIndexed { index, parameter ->
        parameter
          .toInjektCallable(ownerScope.ctx)
          .copy(type = type.arguments[index])
      }
  )*/ null

  // required to distinct between individual lambdas in codegen
  class ParameterDescriptor(
    private val delegate: ValueParameterDescriptor,
    val lambdaInjectable: LambdaInjectable
  ) : ValueParameterDescriptor by delegate
}

class TypeKeyInjectable(
  override val type: ConeKotlinType,
  override val ownerScope: InjectablesScope
) : Injectable {
  override val chainFqName = FqName("typeKeyOf<${type.renderReadableWithFqNames()}>")
  override val dependencies = buildList {
    var index = 0
    type.forEachType {
      if (it is ConeTypeParameterType)
        this += InjectableRequest(
          type = ownerScope.session.symbolProvider.getClassLikeSymbolByClassId(
            InjektFqNames.TypeKey
          )!!.constructType(arrayOf(it.type), false),
          chainFqName = chainFqName,
          parameterName = "${it.lookupTag.name}Key".asNameId(),
          parameterIndex = index++
        )
    }
  }
}

data class InjectableRequest(
  val type: ConeKotlinType,
  val chainFqName: FqName,
  val parameterName: Name,
  val parameterIndex: Int,
  val isRequired: Boolean = true
)
