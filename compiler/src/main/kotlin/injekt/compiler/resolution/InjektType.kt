/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package injekt.compiler.resolution

import injekt.compiler.*
import injekt.compiler.fir.*
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.*

data class InjektClassifier(
  val key: String,
  val fqName: FqName,
  val classId: ClassId?,
  val typeParameters: List<InjektClassifier> = emptyList(),
  val lazySuperTypes: Lazy<List<InjektType>> = lazyOf(emptyList()),
  val isTypeParameter: Boolean = false,
  val isObject: Boolean = false,
  val isTag: Boolean = false,
  val symbol: FirClassifierSymbol<*>? = null,
  val tags: List<InjektType> = emptyList(),
  val isAddOn: Boolean = false,
  val variance: TypeVariance = TypeVariance.INV
) {
  val superTypes by lazySuperTypes

  val defaultType: InjektType = tags.wrap(
    InjektType(
      classifier = this,
      arguments = typeParameters.map { it.defaultType },
      variance = variance
    )
  )

  override fun equals(other: Any?): Boolean = (other is InjektClassifier) && key == other.key
  override fun hashCode(): Int = key.hashCode()
  override fun toString(): String = key
}

fun List<InjektType>.wrap(type: InjektType): InjektType = foldRight(type) { nextTag, acc ->
  nextTag.wrap(acc)
}

fun InjektType.unwrapTags(): InjektType = if (!classifier.isTag) this
else arguments.last().unwrapTags()

fun InjektType.wrap(type: InjektType): InjektType {
  val newArguments = if (arguments.size < classifier.typeParameters.size)
    arguments + type
  else arguments.dropLast(1) + type
  return withArguments(newArguments)
}

fun FirClassifierSymbol<*>.toInjektClassifier(ctx: InjektContext): InjektClassifier =
  ctx.cached("injekt_classifier", this) {
    val info = classifierInfo(ctx)

    val typeParameters = typeParameterSymbols
      ?.mapTo(mutableListOf()) { it.toInjektClassifier(ctx) }

    if (typeParameters != null && isTag(ctx))
      typeParameters += InjektClassifier(
        key = "${uniqueKey(ctx)}.\$TT",
        fqName = fqName.child("\$TT".asNameId()),
        classId = null,
        isTypeParameter = true,
        lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) { listOf(ctx.nullableAnyType) },
        variance = TypeVariance.OUT
      )

    InjektClassifier(
      key = uniqueKey(ctx),
      fqName = fqName,
      classId = safeAs<FirClassLikeSymbol<*>>()?.classId,
      typeParameters = typeParameters ?: emptyList(),
      lazySuperTypes = info.lazySuperTypes,
      isTypeParameter = this is FirTypeParameterSymbol,
      isObject = this is FirRegularClassSymbol && classKind == ClassKind.OBJECT,
      isTag = isTag(ctx),
      symbol = this,
      tags = info.tags,
      isAddOn = hasAnnotation(InjektFqNames.AddOn, ctx.session),
      variance = (this as? FirTypeParameterSymbol)?.variance?.convertVariance() ?: TypeVariance.INV
    )
  }

fun ConeTypeProjection.toInjektType(ctx: InjektContext): InjektType = when (kind) {
  ProjectionKind.STAR -> STAR_PROJECTION_TYPE
  ProjectionKind.IN -> type!!.toInjektType(ctx, TypeVariance.IN)
  ProjectionKind.OUT -> type!!.toInjektType(ctx, TypeVariance.OUT)
  ProjectionKind.INVARIANT -> type!!.toInjektType(ctx, TypeVariance.INV)
}

fun ConeKotlinType.toInjektType(
  ctx: InjektContext,
  variance: TypeVariance = TypeVariance.INV,
): InjektType {
  if (this is ConeErrorType) return ctx.nullableAnyType
  val unwrapped = when(val abbreviatedOrSelf = abbreviatedTypeOrSelf) {
    is ConeCapturedType -> abbreviatedOrSelf.lowerType ?: return STAR_PROJECTION_TYPE
    is ConeDefinitelyNotNullType -> abbreviatedOrSelf.original.unwrapLowerBound()
    is ConeFlexibleType -> abbreviatedOrSelf.lowerBound.unwrapLowerBound()
    is ConeSimpleKotlinType -> abbreviatedOrSelf
  }

  val classifier = unwrapped.safeAs<ConeLookupTagBasedType>()?.lookupTag
    ?.toSymbol(ctx.session)?.toInjektClassifier(ctx)
    ?: return ctx.nullableAnyType

  val rawType = InjektType(
    classifier = classifier,
    isMarkedNullable = unwrapped.isMarkedNullable,
    arguments = unwrapped.typeArguments
      .map { it.toInjektType(ctx) }
      .let {
        if (classifier.isTag && it.size != classifier.typeParameters.size)
          it + List(classifier.typeParameters.size - it.size) { ctx.nullableAnyType }
        else it
      },
    isProvide = unwrapped.customAnnotations.hasAnnotation(InjektFqNames.Provide, ctx.session),
    isStarProjection = false,
    uniqueId = null,
    variance = variance
  )

  val tags = unwrapped.customAnnotations.getTags(ctx)
  var result = if (tags.isNotEmpty()) {
    tags
      .map { it.resolvedType.toInjektType(ctx) }
      .map {
        it.copy(
          arguments = it.arguments,
          isMarkedNullable = rawType.isMarkedNullable,
          isProvide = rawType.isProvide,
          variance = rawType.variance
        )
      }
      .wrap(rawType)
  } else rawType

  // expand the type
  while (result.unwrapTags().classifier.symbol is FirTypeAliasSymbol) {
    val expanded = result.unwrapTags().superTypes.single()
    result = if (result.classifier.isTag) result.wrap(expanded) else expanded
  }

  return result
}

data class InjektType(
  val classifier: InjektClassifier,
  val isMarkedNullable: Boolean = false,
  val arguments: List<InjektType> = emptyList(),
  val isProvide: Boolean = false,
  val isStarProjection: Boolean = false,
  val uniqueId: String? = null,
  val variance: TypeVariance = TypeVariance.INV,
  val source: InjektClassifier? = null
) {
  override fun toString(): String = renderToString()

  override fun equals(other: Any?) =
    other is InjektType && other.hashCode() == hashCode()

  private var _hashCode: Int = 0

  init {
    check(arguments.size == classifier.typeParameters.size) {
      "Argument size mismatch ${classifier.fqName} " +
          "params: ${classifier.typeParameters.map { it.fqName }} " +
          "args: ${arguments.map { it.renderToString() }}"
    }
  }

  private var _superTypes: List<InjektType>? = null
  val superTypes: List<InjektType> get() {
    if (_superTypes == null) {
      val substitutionMap = buildMap {
        for ((index, parameter) in classifier.typeParameters.withIndex())
          this[parameter] = arguments[index]
      }
      _superTypes = if (substitutionMap.isEmpty()) classifier.superTypes
        .map { it.withNullability(isMarkedNullable) }
      else classifier.superTypes.map {
        it.substitute(substitutionMap)
          .withNullability(isMarkedNullable)
      }
    }
    return _superTypes!!
  }

  private var _allTypes: Set<InjektType>? = null
  val allTypes: Set<InjektType> get() {
    if (_allTypes == null) {
      val allTypes = mutableSetOf<InjektType>()
      fun collect(inner: InjektType) {
        if (!allTypes.add(inner)) return
        inner.arguments.forEach { collect(it) }
        inner.superTypes.forEach { collect(it) }
      }
      collect(this)
      _allTypes = allTypes
    }
    return _allTypes!!
  }

  private var _isNullableType: Boolean? = null
  val isNullableType: Boolean get() {
    if (_isNullableType == null) {
      fun inner(): Boolean {
        if (isMarkedNullable) return true
        for (superType in superTypes)
          if (superType.isNullableType) return true
        return false
      }
      _isNullableType = inner()
    }
    return _isNullableType!!
  }

  private val subtypeViews = mutableMapOf<InjektClassifier, InjektType?>()
  fun subtypeView(classifier: InjektClassifier): InjektType? = subtypeViews.getOrPut(classifier) {
    fun InjektType.inner(): InjektType? {
      if (this.classifier == classifier) return this
      return superTypes
        .firstNotNullOfOrNull { it.subtypeView(classifier) }
        ?.let { return it }
    }
    inner()
  }

  override fun hashCode(): Int {
    if (_hashCode == 0) {
      var result = classifier.hashCode()
      result = 31 * result + isMarkedNullable.hashCode()
      result = 31 * result + arguments.hashCode()
      result = 31 * result + isProvide.hashCode()
      result = 31 * result + isStarProjection.hashCode()
      result = 31 * result + uniqueId.hashCode()
      result = 31 * result + variance.hashCode()
      _hashCode = result
      return result
    }
    return _hashCode
  }
}

fun InjektType.withArguments(arguments: List<InjektType>): InjektType =
  if (this.arguments == arguments) this
  else copy(arguments = arguments)

fun InjektType.withNullability(isMarkedNullable: Boolean) =
  if (this.isMarkedNullable == isMarkedNullable) this
  else copy(isMarkedNullable = isMarkedNullable)

fun InjektType.withVariance(variance: TypeVariance) =
  if (this.variance == variance) this
  else copy(variance = variance)

val STAR_PROJECTION_TYPE = InjektType(
  classifier = InjektClassifier("*", StandardNames.FqNames.any.toSafe(), null),
  isStarProjection = true,
)

fun InjektType.anyType(action: (InjektType) -> Boolean): Boolean =
  action(this) || arguments.any { it.anyType(action) }

fun InjektType.anySuperType(action: (InjektType) -> Boolean): Boolean =
  action(this) || superTypes.any { it.anySuperType(action) }

fun List<InjektClassifier>.substitute(map: Map<InjektClassifier, InjektType>): List<InjektClassifier> {
  if (map.isEmpty()) return this
  val allNewSuperTypes = map { mutableListOf<InjektType>() }
  val newClassifiers = mapIndexed { index, classifier ->
    classifier.copy(lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) { allNewSuperTypes[index] })
  }
  val combinedMap = map + zip(newClassifiers.map { it.defaultType })
  for (i in indices) {
    val newSuperTypes = allNewSuperTypes[i]
    val oldClassifier = this[i]
    for (oldSuperType in oldClassifier.superTypes) {
      newSuperTypes += oldSuperType.substitute(combinedMap)
    }
  }
  return newClassifiers
}

fun InjektType.substitute(map: Map<InjektClassifier, InjektType>): InjektType {
  if (map.isEmpty()) return this
  map[classifier]?.let { substitution ->
    val newNullability = if (isStarProjection) substitution.isMarkedNullable
    else isMarkedNullable || substitution.isMarkedNullable
    val newIsProvide = isProvide || substitution.isProvide
    val newVariance = if (substitution.variance != TypeVariance.INV) substitution.variance
    else variance
    return if (newNullability != substitution.isMarkedNullable ||
      newIsProvide != substitution.isProvide ||
      newVariance != substitution.variance
    ) {
      substitution.copy(
        isMarkedNullable = newNullability,
        isProvide = newIsProvide,
        variance = newVariance
      )
    } else substitution
  }

  if (arguments.isEmpty()) return this

  val newArguments = arguments.map { it.substitute(map) }
  if (newArguments != arguments)
    return copy(arguments = newArguments)

  return this
}

fun InjektType.renderToString() = buildString {
  render { append(it) }
}

fun InjektType.render(
  depth: Int = 0,
  renderType: (InjektType) -> Boolean = { true },
  append: (String) -> Unit
) {
  if (depth > 15) return
  fun InjektType.inner() {
    if (!renderType(this)) return

    when {
      isStarProjection -> append("*")
      else -> append(classifier.fqName.asString())
    }
    if (arguments.isNotEmpty()) {
      append("<")
      arguments.forEachIndexed { index, typeArgument ->
        typeArgument.render(depth = depth + 1, renderType, append)
        if (index != arguments.lastIndex) append(", ")
      }
      append(">")
    }
    if (isMarkedNullable && !isStarProjection) append("?")
  }
  inner()
}

val InjektType.typeDepth: Int get() = (arguments.maxOfOrNull { it.typeDepth } ?: 0) + 1

fun InjektType.isProvideFunctionType(ctx: InjektContext): Boolean =
  isProvide && isSubTypeOf(ctx.functionType, ctx)

fun InjektType.isNonKFunctionType(ctx: InjektContext): Boolean =
  classifier.fqName.asString().let {
    it.startsWith(InjektFqNames.function) ||
        it.startsWith(InjektFqNames.suspendFunction) ||
        it.startsWith(InjektFqNames.composableFunction)
  }

fun InjektType.isUnconstrained(staticTypeParameters: List<InjektClassifier>): Boolean =
  classifier.isTypeParameter &&
      classifier !in staticTypeParameters &&
      classifier.superTypes.all {
        it.classifier.classId == InjektFqNames.Any ||
            it.isUnconstrained(staticTypeParameters)
      }

fun effectiveVariance(
  declared: TypeVariance,
  useSite: TypeVariance,
  originalDeclared: TypeVariance
): TypeVariance {
  if (declared != TypeVariance.INV) return declared
  if (useSite != TypeVariance.INV) return useSite
  return originalDeclared
}
