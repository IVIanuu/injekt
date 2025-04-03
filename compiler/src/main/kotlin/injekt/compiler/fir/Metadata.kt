/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package injekt.compiler.fir

import injekt.compiler.*
import injekt.compiler.resolution.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class CallableMetadata(
  val symbol: FirCallableSymbol<*>,
  val type: InjektType,
  val parameterTypes: Map<Name, InjektType>,
  val injectParameters: Set<Name>
)

fun FirCallableSymbol<*>.callableMetadata(ctx: InjektContext): CallableMetadata =
  when {
    this != originalOrSelf() -> originalOrSelf().callableMetadata(ctx)
    this is FirPropertyAccessorSymbol -> propertySymbol.callableMetadata(ctx)
    else -> ctx.cached("callable_metadata", uniqueKey(ctx)) {
      decodeMetadata<PersistedCallableMetadata>(ctx)
        ?.toCallableMetadata(ctx)
        ?.let { return@cached it }

      val type = run {
        val tags = if (this is FirConstructorSymbol)
          buildList {
            addAll(resolvedReturnType.toSymbol(ctx.session)!!.classifierMetadata(ctx).tags)
            for (tagAnnotation in annotations.getTagAnnotations(ctx))
              add(tagAnnotation.resolvedType.toInjektType(ctx))
          }
        else emptyList()
        tags.wrap(resolvedReturnType.toInjektType(ctx))
      }

      val parameterTypes = buildMap {
        if (dispatchReceiverType != null)
          this[DISPATCH_RECEIVER_NAME] = dispatchReceiverType!!.toInjektType(ctx)
        if (receiverParameter != null)
          this[EXTENSION_RECEIVER_NAME] = receiverParameter!!.typeRef.coneType.toInjektType(ctx)
        resolvedContextParameters.forEach { contextParameter ->
          this[contextParameter.symbol.injektName()] = contextParameter.returnTypeRef.coneType.toInjektType(ctx)
        }
        if (this@callableMetadata is FirFunctionSymbol<*>)
          valueParameterSymbols.forEach { valueParameter ->
            this[valueParameter.name] =
              valueParameter.resolvedReturnType.toInjektType(ctx)
          }
      }

      val injectParameters = if (this !is FirFunctionSymbol<*>) emptySet()
      else valueParameterSymbols
        .filter {
          it.defaultValueSource?.getElementTextInContextForDebug() ==
              InjektFqNames.inject.callableName.asString()
        }
        .mapTo(mutableSetOf()) { it.name }

      CallableMetadata(this, type, parameterTypes, injectParameters)
    }
  }

fun CallableMetadata.shouldBePersisted(ctx: InjektContext) = injectParameters.isNotEmpty() ||
    type.shouldBePersisted() ||
    parameterTypes.values.any { it.shouldBePersisted() } ||
    symbol.typeParameterSymbols.any { it.classifierMetadata(ctx).shouldBePersisted(ctx) }

@Serializable data class PersistedCallableMetadata(
  val callableKey: String,
  val callableFqName: String,
  val type: PersistedInjektType,
  val parameterTypes: Map<String, PersistedInjektType>,
  val injectParameters: Set<String>,
  val typeParameterMetadata: List<PersistedClassifierMetadata>
)

fun CallableMetadata.toPersistedCallableMetadata(ctx: InjektContext) = PersistedCallableMetadata(
  callableKey = symbol.uniqueKey(ctx),
  callableFqName = symbol.fqName.asString(),
  type = type.toPersistedInjektType(ctx),
  parameterTypes = parameterTypes
    .mapKeys { it.key.asString() }
    .mapValues { it.value.toPersistedInjektType(ctx) },
  injectParameters = injectParameters.mapTo(mutableSetOf()) { it.asString() },
  typeParameterMetadata = symbol.typeParameterSymbols.map {
    it.classifierMetadata(ctx).toPersistedClassifierMetadata(ctx)
  }
)

fun PersistedCallableMetadata.toCallableMetadata(ctx: InjektContext) = try {
  CallableMetadata(
    symbol = findCallableForKey(callableKey, FqName(callableFqName), ctx),
    type = type.toInjektType(ctx),
    parameterTypes = parameterTypes
      .mapKeys { it.key.asNameId() }
      .mapValues { it.value.toInjektType(ctx) },
    injectParameters = injectParameters.mapTo(mutableSetOf()) { it.asNameId() }
  )
} catch (e: Throwable) {
  throw IllegalStateException("Failed to restore $this", e)
}

class ClassifierMetadata(
  val symbol: FirClassifierSymbol<*>,
  val lazyTags: Lazy<List<InjektType>>,
  val lazySuperTypes: Lazy<List<InjektType>>,
) {
  val superTypes by lazySuperTypes
  val tags by lazyTags
}

fun FirClassifierSymbol<*>.classifierMetadata(ctx: InjektContext): ClassifierMetadata =
  ctx.cached("classifier_metadata", uniqueKey(ctx)) {
    when (this) {
      is FirTypeParameterSymbol -> {
        val index = containingDeclarationSymbol.typeParameterSymbols!!.indexOf(this)
        (containingDeclarationSymbol.safeAs<FirClassifierSymbol<*>>()
          ?.decodeMetadata<PersistedClassifierMetadata>(ctx)
          ?.typeParameterMetadata ?:
        containingDeclarationSymbol.safeAs<FirCallableSymbol<*>>()
          ?.decodeMetadata<PersistedCallableMetadata>(ctx)
          ?.typeParameterMetadata)
          ?.let {
            it.getOrNull(index)
              ?: error("Wtf $this $containingDeclarationSymbol ${containingDeclarationSymbol.typeParameterSymbols}")
          }
          ?.toClassifierMetadata(ctx)
          ?.let { return@cached it }
      }
      is FirTypeAliasSymbol -> {
        ctx.session.symbolProvider.getTopLevelFunctionSymbols(
          classId.packageFqName,
          (classId.shortClassName.asString() + "\$MetadataHolder").asNameId()
        )
          // for some reason there are multiple holders sometimes
          // so just the first non null result
          .firstNotNullOfOrNull { it.decodeMetadata<PersistedClassifierMetadata>(ctx) }
          ?.toClassifierMetadata(ctx)
          ?.let { return@cached it }
      }
      else -> {
        decodeMetadata<PersistedClassifierMetadata>(ctx)
          ?.toClassifierMetadata(ctx)
          ?.let { return@cached it }
      }
    }

    val expandedType = (this as? FirTypeAliasSymbol)?.resolvedExpandedTypeRef
      ?.coneType?.toInjektType(ctx)

    val isTag = hasAnnotation(InjektFqNames.Tag, ctx.session)

    val lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) {
      when {
        isTag -> listOf(ctx.anyType)
        expandedType != null -> listOf(expandedType)
        this is FirTypeParameterSymbol -> resolvedBounds.map { it.coneType.toInjektType(ctx) }
        else -> cast<FirClassLikeSymbol<*>>().getSuperTypes(ctx.session, recursive = false)
          .map { it.toInjektType(ctx) }
      }
    }

    val lazyTags = lazy(LazyThreadSafetyMode.NONE) {
      annotations.getTagAnnotations(ctx).map { it.resolvedType.toInjektType(ctx) }
    }
    ClassifierMetadata(this, lazyTags, lazySuperTypes)
  }

fun ClassifierMetadata.shouldBePersisted(ctx: InjektContext): Boolean =
  tags.any { it.shouldBePersisted() } || superTypes.any { it.shouldBePersisted() }  ||
      symbol.typeParameterSymbols?.any { it.classifierMetadata(ctx).shouldBePersisted(ctx) } == true

@Serializable data class PersistedClassifierMetadata(
  val classifierKey: String,
  val classifierFqName: String,
  val classifierClassId: String?,
  val tags: List<PersistedInjektType>,
  val superTypes: List<PersistedInjektType>,
  val typeParameterMetadata: List<PersistedClassifierMetadata>
)

fun PersistedClassifierMetadata.toClassifierMetadata(ctx: InjektContext) = ClassifierMetadata(
  symbol = findClassifier(classifierKey, FqName(classifierFqName), ClassId.fromString(classifierKey), ctx),
  lazyTags = lazy(LazyThreadSafetyMode.NONE) { tags.map { it.toInjektType(ctx) } },
  lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) { superTypes.map { it.toInjektType(ctx) } }
)

fun ClassifierMetadata.toPersistedClassifierMetadata(ctx: InjektContext): PersistedClassifierMetadata = PersistedClassifierMetadata(
  classifierKey = symbol.uniqueKey(ctx),
  classifierFqName = symbol.fqName.asString(),
  classifierClassId = symbol.safeAs<FirClassLikeSymbol<*>>()?.classId?.toString(),
  tags = tags.map { it.toPersistedInjektType(ctx) },
  superTypes = superTypes.map { it.toPersistedInjektType(ctx) },
  typeParameterMetadata = symbol.safeAs<FirClassLikeSymbol<*>>()
    ?.typeParameterSymbols
    ?.map { it.classifierMetadata(ctx).toPersistedClassifierMetadata(ctx) }
    ?: emptyList()
)

fun InjektType.shouldBePersisted(): Boolean = anyType { it.classifier.isTag }

val json = Json { ignoreUnknownKeys = true }
inline fun <reified T> T.encode(): String = json.encodeToString(this)
inline fun <reified T> String.decode(): T = json.decodeFromString(this)

private inline fun <reified T> FirBasedSymbol<*>.decodeMetadata(ctx: InjektContext): T? =
  getAnnotationByClassId(InjektFqNames.InjektMetadata, ctx.session)
    ?.argumentMapping
    ?.mapping
    ?.values
    ?.singleOrNull()
    ?.safeAs<FirLiteralExpression>()
    ?.value
    ?.safeAs<String>()
    ?.decode<T>()

@Serializable data class PersistedInjektType(
  val classifierKey: String,
  val classifierFqName: String,
  val classifierClassId: String?,
  val arguments: List<PersistedInjektType>,
  val isStarProjection: Boolean,
  val variance: TypeVariance,
  val isMarkedNullable: Boolean,
  val isProvide: Boolean
)

fun InjektType.toPersistedInjektType(ctx: InjektContext): PersistedInjektType =
  PersistedInjektType(
    classifierKey = classifier.key,
    classifierFqName = classifier.fqName.asString(),
    classifierClassId = classifier.classId?.toString(),
    arguments = arguments.map { it.toPersistedInjektType(ctx) },
    isStarProjection = isStarProjection,
    variance = variance,
    isMarkedNullable = isMarkedNullable,
    isProvide = isProvide
  )

fun PersistedInjektType.toInjektType(ctx: InjektContext): InjektType =
  if (isStarProjection) STAR_PROJECTION_TYPE
  else InjektType(
    classifier = findClassifier(
      classifierKey,
      FqName(classifierFqName),
      classifierClassId?.let { ClassId.fromString(it) },
      ctx
    )
      .toInjektClassifier(ctx),
    arguments = arguments.map { it.toInjektType(ctx) },
    variance = variance,
    isMarkedNullable = isMarkedNullable,
    isProvide = isProvide
  )
