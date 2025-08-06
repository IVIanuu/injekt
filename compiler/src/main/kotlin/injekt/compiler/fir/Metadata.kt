/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, SymbolInternals::class)

package injekt.compiler.fir

import injekt.compiler.*
import injekt.compiler.resolution.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
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

context(ctx: InjektContext)
fun FirCallableSymbol<*>.callableMetadata(): CallableMetadata =
  when {
    this != originalOrSelf() -> originalOrSelf().callableMetadata()
    this is FirPropertyAccessorSymbol -> propertySymbol.callableMetadata()
    else -> cached("callable_metadata", uniqueKey()) {
      decodeMetadata<PersistedCallableMetadata>()
        ?.toCallableMetadata()
        ?.let { return@cached it }

      val type = run {
        val tags = if (this is FirConstructorSymbol)
          buildList {
            addAll(resolvedReturnType.toSymbol(session)!!.classifierMetadata().tags)
            for (tagAnnotation in annotations.getTagAnnotations())
              add(tagAnnotation.resolvedType.toInjektType())
          }
        else emptyList()
        tags.wrap(resolvedReturnType.toInjektType())
      }

      val parameterTypes = buildMap {
        if (dispatchReceiverType != null)
          this[DISPATCH_RECEIVER_NAME] = dispatchReceiverType!!.toInjektType()
        this@callableMetadata.isExtension
        if (resolvedReceiverType != null)
          this[EXTENSION_RECEIVER_NAME] = resolvedReceiverType!!.toInjektType()
        contextParameterSymbols.forEach { contextParameter ->
          this[contextParameter.injektName()] = contextParameter.resolvedReturnType.toInjektType()
        }
        if (this@callableMetadata is FirFunctionSymbol<*>)
          valueParameterSymbols.forEach { valueParameter ->
            this[valueParameter.name] =
              valueParameter.resolvedReturnType.toInjektType()
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

context(ctx: InjektContext)
fun CallableMetadata.shouldBePersisted() = injectParameters.isNotEmpty() ||
    type.shouldBePersisted() ||
    parameterTypes.values.any { it.shouldBePersisted() } ||
    symbol.typeParameterSymbols.any { it.classifierMetadata().shouldBePersisted() }

@Serializable data class PersistedCallableMetadata(
  val callableKey: String,
  val callableFqName: String,
  val type: PersistedInjektType,
  val parameterTypes: Map<String, PersistedInjektType>,
  val injectParameters: Set<String>,
  val typeParameterMetadata: List<PersistedClassifierMetadata>
)

context(ctx: InjektContext)
fun CallableMetadata.toPersistedCallableMetadata() = PersistedCallableMetadata(
  callableKey = symbol.uniqueKey(),
  callableFqName = symbol.fqName.asString(),
  type = type.toPersistedInjektType(),
  parameterTypes = parameterTypes
    .mapKeys { it.key.asString() }
    .mapValues { it.value.toPersistedInjektType() },
  injectParameters = injectParameters.mapTo(mutableSetOf()) { it.asString() },
  typeParameterMetadata = symbol.typeParameterSymbols.map {
    it.classifierMetadata().toPersistedClassifierMetadata()
  }
)

context(ctx: InjektContext)
fun PersistedCallableMetadata.toCallableMetadata() = try {
  CallableMetadata(
    symbol = findCallableForKey(callableKey, FqName(callableFqName)),
    type = type.toInjektType(),
    parameterTypes = parameterTypes
      .mapKeys { it.key.asNameId() }
      .mapValues { it.value.toInjektType() },
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

context(ctx: InjektContext)
fun FirClassifierSymbol<*>.classifierMetadata(): ClassifierMetadata =
  cached("classifier_metadata", uniqueKey()) {
    when (this) {
      is FirTypeParameterSymbol -> {
        val index = containingDeclarationSymbol.typeParameterSymbols!!.indexOf(this)
        (containingDeclarationSymbol.safeAs<FirClassifierSymbol<*>>()
          ?.decodeMetadata<PersistedClassifierMetadata>()
          ?.typeParameterMetadata ?:
        containingDeclarationSymbol.safeAs<FirCallableSymbol<*>>()
          ?.decodeMetadata<PersistedCallableMetadata>()
          ?.typeParameterMetadata)
          ?.let {
            it.getOrNull(index)
              ?: error("Wtf $this $containingDeclarationSymbol ${containingDeclarationSymbol.typeParameterSymbols}")
          }
          ?.toClassifierMetadata()
          ?.let { return@cached it }
      }
      is FirTypeAliasSymbol -> session.symbolProvider.getTopLevelFunctionSymbols(
        classId.packageFqName,
        (classId.shortClassName.asString() + "\$MetadataHolder").asNameId()
      )
        // for some reason there are multiple holders sometimes
        // so just the first non null result
        .firstNotNullOfOrNull { it.decodeMetadata<PersistedClassifierMetadata>() }
        ?.toClassifierMetadata()
        ?.let { return@cached it }
      else -> {
        decodeMetadata<PersistedClassifierMetadata>()
          ?.toClassifierMetadata()
          ?.let { return@cached it }
      }
    }

    val expandedType = (this as? FirTypeAliasSymbol)?.resolvedExpandedTypeRef
      ?.coneType?.toInjektType()

    val isTag = hasAnnotation(InjektFqNames.Tag, session)

    val lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) {
      when {
        isTag -> listOf(ctx.anyType)
        expandedType != null -> listOf(expandedType)
        this is FirTypeParameterSymbol -> resolvedBounds.map { it.coneType.toInjektType() }
        else -> this.cast<FirClassLikeSymbol<*>>().getSuperTypes(session, recursive = false)
          .map { it.toInjektType() }
      }
    }

    val lazyTags = lazy(LazyThreadSafetyMode.NONE) {
      annotations.getTagAnnotations().map { it.resolvedType.toInjektType() }
    }
    ClassifierMetadata(this, lazyTags, lazySuperTypes)
  }

context(ctx: InjektContext)
fun ClassifierMetadata.shouldBePersisted(): Boolean =
  tags.any { it.shouldBePersisted() } || superTypes.any { it.shouldBePersisted() }  ||
      symbol.typeParameterSymbols?.any { it.classifierMetadata().shouldBePersisted() } == true

@Serializable data class PersistedClassifierMetadata(
  val classifierKey: String,
  val classifierFqName: String,
  val classifierClassId: String?,
  val tags: List<PersistedInjektType>,
  val superTypes: List<PersistedInjektType>,
  val typeParameterMetadata: List<PersistedClassifierMetadata>
)

context(ctx: InjektContext)
fun PersistedClassifierMetadata.toClassifierMetadata() = ClassifierMetadata(
  symbol = findClassifier(
    classifierKey,
    FqName(classifierFqName),
    ClassId.fromString(classifierKey)
  ),
  lazyTags = lazy(LazyThreadSafetyMode.NONE) { tags.map { it.toInjektType() } },
  lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) { superTypes.map { it.toInjektType() } }
)

context(ctx: InjektContext)
fun ClassifierMetadata.toPersistedClassifierMetadata(): PersistedClassifierMetadata = PersistedClassifierMetadata(
  classifierKey = symbol.uniqueKey(),
  classifierFqName = symbol.fqName.asString(),
  classifierClassId = symbol.safeAs<FirClassLikeSymbol<*>>()?.classId?.toString(),
  tags = tags.map { it.toPersistedInjektType() },
  superTypes = superTypes.map { it.toPersistedInjektType() },
  typeParameterMetadata = symbol.safeAs<FirClassLikeSymbol<*>>()
    ?.typeParameterSymbols
    ?.map { it.classifierMetadata().toPersistedClassifierMetadata() }
    ?: emptyList()
)

fun InjektType.shouldBePersisted(): Boolean = anyType { it.classifier.isTag }

val json = Json { ignoreUnknownKeys = true }
inline fun <reified T> T.encode(): String = json.encodeToString(this)
inline fun <reified T> String.decode(): T = json.decodeFromString(this)

context(ctx: InjektContext)
private inline fun <reified T> FirBasedSymbol<*>.decodeMetadata(): T? =
  getAnnotationByClassId(InjektFqNames.InjektMetadata, session)
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

context(ctx: InjektContext)
fun InjektType.toPersistedInjektType(): PersistedInjektType =
  PersistedInjektType(
    classifierKey = classifier.key,
    classifierFqName = classifier.fqName.asString(),
    classifierClassId = classifier.classId?.toString(),
    arguments = arguments.map { it.toPersistedInjektType() },
    isStarProjection = isStarProjection,
    variance = variance,
    isMarkedNullable = isMarkedNullable,
    isProvide = isProvide
  )

context(ctx: InjektContext)
fun PersistedInjektType.toInjektType(): InjektType =
  if (isStarProjection) STAR_PROJECTION_TYPE
  else InjektType(
    classifier = findClassifier(
      classifierKey,
      FqName(classifierFqName),
      classifierClassId?.let { ClassId.fromString(it) }
    )
      .toInjektClassifier(),
    arguments = arguments.map { it.toInjektType() },
    variance = variance,
    isMarkedNullable = isMarkedNullable,
    isProvide = isProvide
  )
