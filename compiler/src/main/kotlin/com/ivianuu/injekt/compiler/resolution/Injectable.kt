/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.calls.components.*
import org.jetbrains.kotlin.utils.addToStdlib.*

sealed interface Injectable {
  val type: InjektType
  val dependencies: List<InjectableRequest> get() = emptyList()
  val dependencyScope: InjectablesScope? get() = null
  val callableFqName: FqName
  val ownerScope: InjectablesScope
}

class CallableInjectable(
  override val ownerScope: InjectablesScope,
  val callable: InjektCallable,
  override val type: InjektType
) : Injectable {
  override val dependencies = callable.injectableRequests(emptySet())
  override val callableFqName = callable.callableFqName
}

class ListInjectable(
  override val type: InjektType,
  override val ownerScope: InjectablesScope,
  elements: List<InjektType>,
  val singleElementType: InjektType,
  val collectionElementType: InjektType
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

@OptIn(SymbolInternals::class) class LambdaInjectable(
  override val ownerScope: InjectablesScope,
  request: InjectableRequest
) : Injectable {
  override val type = request.type
  override val callableFqName = FqName(request.parameterName.asString())
  override val dependencies = listOf(
    InjectableRequest(
      type = type.arguments.last(),
      callableFqName = callableFqName,
      parameterName = "instance".asNameId(),
      parameterIndex = 0
    )
  )

  val valueParameterSymbols = findClassifierSymbol(type.classifier.key, type.classifier.fqName, ownerScope.ctx)
    .cast<FirRegularClassSymbol>()
    .declarationSymbols
    .filterIsInstance<FirFunctionSymbol<*>>()
    .single { it.name.asString() == "invoke" }
    .valueParameterSymbols
    .map { original ->
      buildValueParameterCopy(original.fir) {
        symbol = FirValueParameterSymbol(original.name)
      }.symbol
    }

  override val dependencyScope = InjectableScopeOrParent(
    name = "LAMBDA $type",
    parent = ownerScope,
    initialInjectables = valueParameterSymbols
      .mapIndexed { index, parameter ->
        parameter
          .toInjektCallable(ownerScope.ctx)
          .copy(type = type.arguments[index])
      },
    ctx = ownerScope.ctx
  )
}

data class InjectableRequest(
  val type: InjektType,
  val callableFqName: FqName,
  val callableTypeArguments: Map<InjektClassifier, InjektType> = emptyMap(),
  val parameterName: Name,
  val parameterIndex: Int,
  val isRequired: Boolean = true
)

fun ParameterDescriptor.toInjectableRequest(callable: InjektCallable): InjectableRequest =
  InjectableRequest(
    type = callable.parameterTypes[injektIndex()]!!,
    callableFqName = callable.callableFqName,
    callableTypeArguments = callable.typeArguments,
    parameterName = injektName(),
    parameterIndex = injektIndex(),
    isRequired = this !is ValueParameterDescriptor ||
        injektIndex() in callable.injectParameters || !hasDefaultValue()
  )

fun InjektCallable.injectableRequests(exclude: Set<Int>): List<InjectableRequest> =
  parameterTypes.map { (index, type) ->
    if (index in exclude) return@map null
    val valueParameter = symbol?.safeAs<FirFunctionSymbol<*>>()?.valueParameterSymbols
      ?.getOrNull(index)
    InjectableRequest(
      type = type,
      callableFqName = callableFqName,
      callableTypeArguments = typeArguments,
      parameterName = when (index) {
        DISPATCH_RECEIVER_INDEX -> DISPATCH_RECEIVER_NAME
        EXTENSION_RECEIVER_INDEX -> EXTENSION_RECEIVER_NAME
        else -> valueParameter!!.name
      },
      parameterIndex = index,
      isRequired = valueParameter?.hasDefaultValue != true
    )
  }.filterNotNull()
