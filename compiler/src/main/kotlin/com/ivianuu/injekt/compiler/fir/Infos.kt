/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.fir

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class CallableInfo(
  val symbol: FirCallableSymbol<*>,
  val type: InjektType,
  val parameterTypes: Map<Int, InjektType>,
  val injectParameters: Set<Int>
)

fun FirCallableSymbol<*>.callableInfo(ctx: InjektContext): CallableInfo =
  if (this is FirPropertyAccessorSymbol) propertySymbol.callableInfo(ctx)
  else ctx.cached("callable_info", uniqueKey(ctx)) {
    decodeDeclarationInfo<PersistedCallableInfo>(ctx)
      ?.toCallableInfo(ctx)
      ?.let { return@cached it }

    val type = run {
      val tags = if (this is FirConstructorSymbol)
        buildList {
          addAll(resolvedReturnType.toSymbol(ctx.session)!!.classifierInfo(ctx).tags)
          for (tagAnnotation in annotations.getTags(ctx))
            add(tagAnnotation.resolvedType.toInjektType(ctx))
        }
      else emptyList()
      tags.wrap(resolvedReturnType.toInjektType(ctx))
    }

    val parameterTypes = buildMap {
      if (dispatchReceiverType != null)
        this[DISPATCH_RECEIVER_INDEX] = dispatchReceiverType!!.toInjektType(ctx)
      if (receiverParameter != null)
        this[EXTENSION_RECEIVER_INDEX] = receiverParameter!!.typeRef.coneType.toInjektType(ctx)
      if (this@callableInfo is FirFunctionSymbol<*>)
        valueParameterSymbols.forEachIndexed { index, valueParameter ->
          this[index] = valueParameter.resolvedReturnType.toInjektType(ctx)
        }
    }

    val injectParameters = if (this !is FirFunctionSymbol<*>) emptySet()
    else valueParameterSymbols
      .filter {
        it.defaultValueSource?.getElementTextInContextForDebug() ==
            InjektFqNames.inject.callableName.asString()
      }
      .mapTo(mutableSetOf()) { valueParameterSymbols.indexOf(it) }

    CallableInfo(this, type, parameterTypes, injectParameters)
  }

fun CallableInfo.shouldBePersisted(ctx: InjektContext) = injectParameters.isNotEmpty() ||
    type.shouldBePersisted() ||
    parameterTypes.values.any { it.shouldBePersisted() } ||
    symbol.typeParameterSymbols.any { it.classifierInfo(ctx).shouldBePersisted(ctx) }

@Serializable data class PersistedCallableInfo(
  val callableKey: String,
  val callableFqName: String,
  val type: PersistedInjektType,
  val parameterTypes: Map<Int, PersistedInjektType>,
  val injectParameters: Set<Int>,
  val typeParameterInfos: List<PersistedClassifierInfo>
)

fun CallableInfo.toPersistedCallableInfo(ctx: InjektContext) = PersistedCallableInfo(
  callableKey = symbol.uniqueKey(ctx),
  callableFqName = symbol.fqName.asString(),
  type = type.toPersistedInjektType(ctx),
  parameterTypes = parameterTypes
    .mapValues { it.value.toPersistedInjektType(ctx) },
  injectParameters = injectParameters,
  typeParameterInfos = symbol.typeParameterSymbols.map {
    it.classifierInfo(ctx).toPersistedClassifierInfo(ctx)
  }
)

fun PersistedCallableInfo.toCallableInfo(ctx: InjektContext) = try {
  CallableInfo(
    symbol = findCallableForKey(callableKey, FqName(callableFqName), ctx),
    type = type.toInjektType(ctx),
    parameterTypes = parameterTypes.mapValues { it.value.toInjektType(ctx) },
    injectParameters = injectParameters
  )
} catch (e: Throwable) {
  throw IllegalStateException("Failed to restore $this", e)
}

class ClassifierInfo(
  val symbol: FirClassifierSymbol<*>,
  val tags: List<InjektType>,
  val lazySuperTypes: Lazy<List<InjektType>>,
) {
  val superTypes by lazySuperTypes
}

fun FirClassifierSymbol<*>.classifierInfo(ctx: InjektContext): ClassifierInfo =
  ctx.cached("classifier_info", uniqueKey(ctx)) {
    if (this is FirTypeParameterSymbol) {
      val index = containingDeclarationSymbol.typeParameterSymbols!!.indexOf(this)
      (containingDeclarationSymbol.safeAs<FirClassifierSymbol<*>>()
        ?.decodeDeclarationInfo<PersistedClassifierInfo>(ctx)
        ?.typeParameterInfos ?:
      containingDeclarationSymbol.safeAs<FirCallableSymbol<*>>()
        ?.decodeDeclarationInfo<PersistedCallableInfo>(ctx)
        ?.typeParameterInfos)
        ?.let {
          it.getOrNull(index)
            ?: error("Wtf $this $containingDeclarationSymbol ${containingDeclarationSymbol.typeParameterSymbols}")
        }
        ?.toClassifierInfo(ctx)
        ?.let { return@cached it }
    } else
      decodeDeclarationInfo<PersistedClassifierInfo>(ctx)
        ?.toClassifierInfo(ctx)
        ?.let { return@cached it }

    val expandedType = (this as? FirTypeAliasSymbol)?.resolvedExpandedTypeRef
      ?.type?.toInjektType(ctx)

    val isTag = hasAnnotation(InjektFqNames.Tag, ctx.session)

    val lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) {
      when {
        expandedType != null -> listOf(expandedType)
        isTag -> listOf(ctx.anyType)
        this is FirTypeParameterSymbol -> resolvedBounds.map { it.type.toInjektType(ctx) }
        else -> cast<FirClassLikeSymbol<*>>().getSuperTypes(ctx.session, recursive = false)
          .map { it.toInjektType(ctx) }
      }
    }

    val tags = annotations.getTags(ctx).map { it.resolvedType.toInjektType(ctx) }
    ClassifierInfo(this, tags, lazySuperTypes)
  }

fun ClassifierInfo.shouldBePersisted(ctx: InjektContext): Boolean =
  tags.any { it.shouldBePersisted() } || superTypes.any { it.shouldBePersisted() }  ||
      symbol.typeParameterSymbols?.any { it.classifierInfo(ctx).shouldBePersisted(ctx) } == true

@Serializable data class PersistedClassifierInfo(
  val classifierKey: String,
  val classifierFqName: String,
  val tags: List<PersistedInjektType>,
  val superTypes: List<PersistedInjektType>,
  val typeParameterInfos: List<PersistedClassifierInfo>
)

fun PersistedClassifierInfo.toClassifierInfo(ctx: InjektContext) = ClassifierInfo(
  symbol = findClassifierForKey(classifierKey, FqName(classifierFqName), ctx),
  tags = tags.map { it.toInjektType(ctx) },
  lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) { superTypes.map { it.toInjektType(ctx) } }
)

fun ClassifierInfo.toPersistedClassifierInfo(ctx: InjektContext): PersistedClassifierInfo = PersistedClassifierInfo(
  classifierKey = symbol.uniqueKey(ctx),
  classifierFqName = symbol.fqName.asString(),
  tags = tags.map { it.toPersistedInjektType(ctx) },
  superTypes = superTypes.map { it.toPersistedInjektType(ctx) },
  typeParameterInfos = safeAs<FirClassLikeSymbol<*>>()
    ?.typeParameterSymbols
    ?.map { it.classifierInfo(ctx).toPersistedClassifierInfo(ctx) }
    ?: emptyList()
)

fun InjektType.shouldBePersisted(): Boolean = anyType {
  it.classifier.isTag && it.arguments.size > 1
}

val json = Json { ignoreUnknownKeys = true }
inline fun <reified T> T.encode(): String = json.encodeToString(this)
inline fun <reified T> String.decode(): T = json.decodeFromString(this)

private inline fun <reified T> FirBasedSymbol<*>.decodeDeclarationInfo(ctx: InjektContext) =
  getAnnotationByClassId(InjektFqNames.DeclarationInfo, ctx.session)
    ?.argumentMapping
    ?.mapping
    ?.values
    ?.singleOrNull()
    ?.safeAs<FirLiteralExpression<String>>()
    ?.value
    ?.decode<T>()

@Serializable data class PersistedInjektType(
  val classifierKey: String,
  val classifierFqName: String,
  val arguments: List<PersistedInjektType> = emptyList(),
  val isStarProjection: Boolean,
  val variance: TypeVariance,
  val isMarkedNullable: Boolean,
  val isProvide: Boolean
)

fun InjektType.toPersistedInjektType(ctx: InjektContext): PersistedInjektType =
  PersistedInjektType(
    classifierKey = classifier.key,
    classifierFqName = classifier.fqName.asString(),
    arguments = arguments.map { it.toPersistedInjektType(ctx) },
    isStarProjection = isStarProjection,
    variance = variance,
    isMarkedNullable = isMarkedNullable,
    isProvide = isProvide
  )

fun PersistedInjektType.toInjektType(ctx: InjektContext): InjektType =
  if (isStarProjection) STAR_PROJECTION_TYPE
  else InjektType(
    classifier = findClassifierForKey(classifierKey, FqName(classifierFqName), ctx)
      .toInjektClassifier(ctx),
    arguments = arguments.map { it.toInjektType(ctx) },
    variance = variance,
    isMarkedNullable = isMarkedNullable,
    isProvide = isProvide
  )
