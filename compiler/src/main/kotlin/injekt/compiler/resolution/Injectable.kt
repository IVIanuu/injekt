/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, SymbolInternals::class, DirectDeclarationsAccess::class)

package injekt.compiler.resolution

import injekt.compiler.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.utils.addToStdlib.*

sealed interface Injectable {
  val type: InjektType
  val callContext: CallContext get() = CallContext.DEFAULT
  val dependencies: List<InjectableRequest> get() = emptyList()
  val dependencyScope: InjectablesScope? get() = null
  val chainFqName: FqName
  val ownerScope: InjectablesScope
}

class CallableInjectable(
  override val ownerScope: InjectablesScope,
  val callable: InjektCallable,
  override val type: InjektType
) : Injectable {
  override val dependencies = callable.injectableRequests(emptySet(), ownerScope.ctx)
  override val callContext: CallContext = callable.callContext
  override val chainFqName = callable.chainFqName
}

class ListInjectable(
  override val type: InjektType,
  override val ownerScope: InjectablesScope,
  elements: List<InjektType>,
  val singleElementType: InjektType,
  val collectionElementType: InjektType
) : Injectable {
  override val chainFqName = FqName("listOf")
  override val dependencies = elements
    .mapIndexed { index, element ->
      InjectableRequest(
        type = element,
        callableFqName = chainFqName,
        callableTypeArguments = type.classifier.typeParameters.zip(type.arguments).toMap(),
        parameterName = "element$index".asNameId()
      )
    }
}

class LambdaInjectable(
  override val ownerScope: InjectablesScope,
  request: InjectableRequest
) : Injectable {
  val dependencyCallContext = if (request.isInline) callContext
  else request.type.callContext

  override val type = request.type
  override val chainFqName = FqName(request.parameterName.asString())
  override val dependencies = listOf(
    InjectableRequest(
      type = type.arguments.last(),
      callableFqName = chainFqName,
      parameterName = "instance".asNameId(),
      isInline = request.isInline
    )
  )

  val valueParameterSymbols = findClassifier(
    type.classifier.key,
    type.classifier.fqName,
    type.classifier.classId,
    ownerScope.ctx
  )
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

  override val dependencyScope = if (valueParameterSymbols.isEmpty() &&
    dependencyCallContext == ownerScope.callContext) null
  else InjectablesScope(
    name = "LAMBDA $type",
    parent = ownerScope,
    initialInjectables = valueParameterSymbols
      .mapIndexed { index, parameter ->
        parameter
          .toInjektCallable(ownerScope.ctx, chainFqName.child(parameter.name))
          .copy(type = type.arguments[index])
      },
    callContext = dependencyCallContext,
    ctx = ownerScope.ctx
  )
}

class SourceKeyInjectable(
  override val type: InjektType,
  override val ownerScope: InjectablesScope
) : Injectable {
  override val chainFqName = FqName("sourceKey")
}

class TypeKeyInjectable(
  override val type: InjektType,
  override val ownerScope: InjectablesScope
) : Injectable {
  override val chainFqName = FqName("typeKeyOf<${type.renderToString()}>")
  override val dependencies = type.allTypes
    .filter { it.classifier.isTypeParameter }
    .map { typeParameter ->
      InjectableRequest(
        type = ownerScope.ctx.typeKeyClassifier!!.defaultType
          .withArguments(listOf(typeParameter.classifier.defaultType)),
        callableFqName = chainFqName,
        callableTypeArguments = type.classifier.typeParameters.zip(type.arguments).toMap(),
        parameterName = "${typeParameter.classifier.fqName.shortName()}Key".asNameId()
      )
    }
}

data class InjectableRequest(
  val type: InjektType,
  val callableFqName: FqName,
  val callableTypeArguments: Map<InjektClassifier, InjektType> = emptyMap(),
  val parameterName: Name,
  val isRequired: Boolean = true,
  val isInline: Boolean = false
)

fun InjektCallable.injectableRequests(exclude: Set<Name>, ctx: InjektContext): List<InjectableRequest> =
  parameterTypes.map { (name, type) ->
    if (name in exclude) return@map null
    val valueParameter = symbol.safeAs<FirFunctionSymbol<*>>()?.valueParameterSymbols
      ?.firstOrNull { it.name == name }
      ?: symbol.contextParameterSymbols
        .firstOrNull { it.name == name }
    InjectableRequest(
      type = type,
      callableFqName = chainFqName,
      callableTypeArguments = typeArguments,
      parameterName = name,
      isRequired = valueParameter == null ||
          valueParameter.name in injectParameters ||
          !valueParameter.hasDefaultValue,
      isInline = symbol.isInline &&
          valueParameter?.isNoinline != true &&
          valueParameter?.isCrossinline != true &&
          valueParameter?.resolvedReturnType?.toInjektType(ctx)?.isNonKFunctionType() == true
    )
  }.filterNotNull()
