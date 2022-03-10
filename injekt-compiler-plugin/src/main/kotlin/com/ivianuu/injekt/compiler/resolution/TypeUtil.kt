/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.calls.inference.*
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.*
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.storage.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

fun TypeProjection.substitute(map: Map<String, KotlinType>, ctx: Context): TypeProjection {
  if (map.isEmpty() || isStarProjection) return this
  return TypeProjectionImpl(projectionKind, type.substitute(map, ctx))
}

fun KotlinType.substitute(map: Map<String, KotlinType>, ctx: Context): KotlinType {
  if (map.isEmpty()) return this
  if (constructor.declarationDescriptor !is TypeParameterDescriptor) {
    if (arguments.isEmpty()) return this

    val newArguments = arguments.map { it.substitute(map, ctx) }
    if (newArguments != arguments)
      return replace(newArguments)

    return this
  }

  val uniqueKey = constructor.declarationDescriptor!!.uniqueKey(ctx)

  val substitution = map[uniqueKey] ?: return this

  val newNullability = isMarkedNullable || substitution.isMarkedNullable
  val newIsProvide = hasAnnotation(InjektFqNames.Provide) || substitution.hasAnnotation(
    InjektFqNames.Provide
  )
  val newIsInject = hasAnnotation(InjektFqNames.Inject) || substitution.hasAnnotation(InjektFqNames.Inject)
  return if (newNullability != substitution.isMarkedNullable ||
    newIsProvide != substitution.hasAnnotation(InjektFqNames.Provide) ||
    newIsInject != substitution.hasAnnotation(InjektFqNames.Inject)
  ) {
    KotlinTypeFactory.simpleType(
      if (!newIsProvide && !newIsInject) Annotations.EMPTY
      else Annotations.create(
        listOf(
          annotations.findAnnotation(InjektFqNames.Provide)
            ?: substitution.annotations.findAnnotation(InjektFqNames.Provide)!!,
          annotations.findAnnotation(InjektFqNames.Inject)
            ?: substitution.annotations.findAnnotation(InjektFqNames.Inject)!!
        )
      ),
      substitution.constructor,
      substitution.arguments,
      newNullability
    )
  } else substitution
}

data class TagTypeConstructor(private val delegate: TypeConstructor) : TypeConstructor by delegate {
  private val parameters = delegate.parameters + TypeParameterDescriptorImpl.createWithDefaultBound(
    delegate.declarationDescriptor!!,
    Annotations.EMPTY,
    false,
    Variance.OUT_VARIANCE,
    "\$T".asNameId(),
    delegate.parameters.size,
    LockBasedStorageManager.NO_LOCKS
  )

  override fun getParameters(): List<TypeParameterDescriptor> = parameters

  override fun getSupertypes(): Collection<KotlinType> = listOf(builtIns.anyType)
}

class InjektTypeConstructor(
  private val declarationDescriptor: ClassifierDescriptor,
  private val delegate: TypeConstructor
) : TypeConstructor by delegate {
  override fun getDeclarationDescriptor(): ClassifierDescriptor? = declarationDescriptor

  private val _supertypes by lazy(LazyThreadSafetyMode.NONE) {
    delegate.supertypes.map { it.prepare() }
  }
  override fun getSupertypes(): Collection<KotlinType> = _supertypes

  private val _parameters by lazy(LazyThreadSafetyMode.NONE) {
    delegate.parameters.map { it.prepare() as TypeParameterDescriptor }
  }
  override fun getParameters(): List<TypeParameterDescriptor> = _parameters
}

fun TypeConstructor.prepare(declarationDescriptor: ClassifierDescriptor): TypeConstructor =
  if (this is InjektTypeConstructor) this
  else InjektTypeConstructor(declarationDescriptor, this)

class InjektTypeParameterDescriptor(
  private val delegate: TypeParameterDescriptor
) : TypeParameterDescriptor by delegate {
  private val constructor by lazy(LazyThreadSafetyMode.NONE) { delegate.typeConstructor.prepare(this) }
  override fun getTypeConstructor(): TypeConstructor = constructor

  private val _defaultType by lazy(LazyThreadSafetyMode.NONE) {
    KotlinTypeFactory.simpleType(
      Annotations.EMPTY,
      constructor,
      constructor.parameters.map { it.defaultType.asTypeProjection() },
      false
    )
  }
  override fun getDefaultType(): SimpleType = _defaultType
}

class InjektClassDescriptor(private val delegate: ClassDescriptor) : ClassDescriptor by delegate {
  private val constructor by lazy(LazyThreadSafetyMode.NONE) { delegate.typeConstructor.prepare(this) }
  override fun getTypeConstructor(): TypeConstructor = constructor

  private val _defaultType: SimpleType by lazy(LazyThreadSafetyMode.NONE) {
    TypeUtils.makeUnsubstitutedType(
      constructor,
      unsubstitutedMemberScope
    ) { _defaultType }
  }

  override fun getDefaultType(): SimpleType = _defaultType
}

fun ClassifierDescriptor.prepare(): ClassifierDescriptor =
  when (this) {
    is TypeParameterDescriptor -> if (this is InjektTypeParameterDescriptor) this
    else InjektTypeParameterDescriptor(this)
    is ClassDescriptor -> if (this is InjektClassDescriptor) this
    else InjektClassDescriptor(this)
    else -> this
  }

fun KotlinType.prepare(): KotlinType {
  val unwrapped = fullyAbbreviatedType
  val kotlinType = when {
    unwrapped.constructor.isDenotable -> unwrapped
    unwrapped.constructor.supertypes.isNotEmpty() -> CommonSupertypes
      .commonSupertype(unwrapped.constructor.supertypes)
    else -> return builtIns.nullableAnyType
  }

  val tags = kotlinType.getTags().map { it.prepare() }

  if (tags.isEmpty() && arguments.isEmpty())
    return kotlinType

  val finalArguments = arguments.map {
    if (it.isStarProjection) it
    else TypeProjectionImpl(it.projectionKind, it.type.prepare())
  }

  val finalType = KotlinTypeFactory.simpleType(
    if (annotations.isEmpty()) Annotations.EMPTY
    else Annotations.create(
      annotations.filter {
        it.fqName == InjektFqNames.Provide ||
            it.fqName == InjektFqNames.Inject
      }
    ),
    constructor,
    finalArguments,
    false
  )

  return tags.wrap(finalType)
    .let { if (isMarkedNullable) it.makeNullable() else it }
}

fun List<KotlinType>.wrap(type: KotlinType): KotlinType = foldRight(type) { nextTag, acc ->
  nextTag.wrap(acc)
}

fun KotlinType.wrap(type: KotlinType): KotlinType = if (constructor is TagTypeConstructor) {
  replace(newArguments = arguments.dropLast(1) + type.asTypeProjection())
} else {
  KotlinTypeFactory.simpleType(
    Annotations.EMPTY,
    TagTypeConstructor(constructor),
    arguments + type.asTypeProjection(),
    isMarkedNullable
  )
}

fun KotlinType.unwrapTags(): KotlinType = if (constructor !is TagTypeConstructor) this
else arguments.last().type.unwrapTags()

fun Annotated.getTags(): List<KotlinType> =
  annotations.filter { it.type.constructor.declarationDescriptor?.isTag == true }
    .map { it.type }

val ClassifierDescriptor.isTag: Boolean
  get() = hasAnnotation(InjektFqNames.Tag) || fqNameSafe == InjektFqNames.Composable

fun KotlinType.anyType(action: (KotlinType) -> Boolean): Boolean =
  action(this) || arguments.any { it.type.anyType(action) }

fun KotlinType.anySupertype(action: (KotlinType) -> Boolean): Boolean =
  action(this) || constructor.supertypes.any { it.anySupertype(action) }

fun KotlinType.firstSupertypeOrNull(action: (KotlinType) -> Boolean): KotlinType? =
  takeIf(action) ?: constructor.supertypes.firstNotNullOfOrNull { it.firstSupertypeOrNull(action) }

val KotlinType.fqName: FqName
  get() = constructor.declarationDescriptor?.fqNameSafe ?: FqName.ROOT

val KotlinType.typeSize: Int
  get() {
    var typeSize = 0
    val seen = mutableSetOf<KotlinType>()
    fun visit(type: KotlinType) {
      typeSize++
      if (seen.add(type))
        type.arguments.forEach { visit(it.type) }
    }
    visit(this)
    return typeSize
  }

val KotlinType.coveringSet: Set<ClassifierDescriptor>
  get() {
    val classifiers = mutableSetOf<ClassifierDescriptor>()
    val seen = mutableSetOf<KotlinType>()
    fun visit(type: KotlinType) {
      if (!seen.add(type)) return
      type.constructor.declarationDescriptor
        ?.let { classifiers += it }
      type.arguments.forEach { visit(it.type) }
    }
    visit(this)
    return classifiers
  }

val KotlinType.allTypes: List<KotlinType>
  get() {
    val result = mutableListOf<KotlinType>()
    val seen = mutableSetOf<KotlinType>()
    fun collect(inner: KotlinType) {
      if (!seen.add(inner)) return
      result += inner
      inner.arguments.forEach { collect(it.type) }
      inner.constructor.supertypes.forEach { collect(it) }
    }
    collect(this)
    return result
  }

val KotlinType.allVisibleTypes: List<KotlinType>
  get() {
    val result = mutableListOf<KotlinType>()
    val seen = mutableSetOf<KotlinType>()
    fun collect(inner: KotlinType) {
      if (!seen.add(inner)) return
      result += inner
      inner.arguments.forEach { collect(it.type) }
    }
    collect(this)
    return result
  }

fun KotlinType.buildSystem(
  superType: KotlinType,
  staticTypeParameters: List<TypeParameterDescriptor>,
  ctx: Context
): Map<String, KotlinType>? {
  val builder = ConstraintSystemBuilderImpl()

  val seenTypeParameters = mutableSetOf<String>()
  val typeVariables = buildList {
    for (visibleType in allVisibleTypes)
      if (visibleType.constructor.declarationDescriptor is TypeParameterDescriptor &&
        visibleType.constructor.declarationDescriptor !in staticTypeParameters &&
        seenTypeParameters.add(visibleType.constructor.declarationDescriptor!!.uniqueKey(ctx)))
          add(visibleType.constructor.declarationDescriptor as TypeParameterDescriptor)
  }

  val substitutor = builder.registerTypeVariables(
    CallHandle.NONE,
    typeVariables
  )

  builder.addSubtypeConstraint(
    substitutor.substitute(this, Variance.INVARIANT) ?: this,
    superType,
    ConstraintPositionKind.SPECIAL.position()
  )

  builder.fixVariables()

  val system = builder.build()

  return if (system.status.hasContradiction()) null
  else {
    buildMap<String, KotlinType> {
      fun KotlinType.collect() {
        system.resultingSubstitutor.substitution[this]
          ?.let { put(constructor.declarationDescriptor!!.uniqueKey(ctx), it.type) }
      }

      allTypes.forEach { it.collect() }
      superType.allTypes.forEach { it.collect() }
    }
  }
}

fun buildContextForSpreadingInjectable(
  constraintType: KotlinType,
  candidateType: KotlinType,
  staticTypeParameters: List<TypeParameterDescriptor>,
  ctx: Context
): Map<String, KotlinType>? {
  val candidateTypeParameters = mutableListOf<TypeParameterDescriptor>()
  candidateType.allTypes.forEach {
    if (it.constructor.declarationDescriptor is TypeParameterDescriptor)
      candidateTypeParameters += it.constructor.declarationDescriptor as TypeParameterDescriptor
  }
  val result = candidateType.buildSystem(
    constraintType,
    candidateTypeParameters + staticTypeParameters,
    ctx
  )

  return result
}

fun KotlinType.renderToString() = buildString {
  asTypeProjection().render { append(it) }
}

fun TypeProjection.render(
  depth: Int = 0,
  renderType: (KotlinType) -> Boolean = { true },
  append: (String) -> Unit
) {
  if (depth > 15) return

  fun TypeProjection.inner() {
    if (!renderType(type)) return

    when {
      isStarProjection -> append("*")
      else -> append(type.constructor.declarationDescriptor!!.fqNameSafe.asString())
    }

    if (type.arguments.isNotEmpty()) {
      append("<")
      type.arguments.forEachIndexed { index, typeArgument ->
        typeArgument.render(depth = depth + 1, renderType, append)
        if (index != type.arguments.lastIndex) append(", ")
      }
      append(">")
    }

    if (type.isMarkedNullable && !isStarProjection) append("?")
  }

  inner()
}

val KotlinType.frameworkKey: String
  get() = annotations.findAnnotation(InjektFqNames.Any)
    ?.allValueArguments?.values?.single()?.value?.cast() ?: ""

fun KotlinType.withFrameworkKey(key: String, ctx: Context) = replace(
  newAnnotations = Annotations.create(
    annotations
      .filter { it.fqName != InjektFqNames.Any } +
        AnnotationDescriptorImpl(
          ctx.module.builtIns.anyType,
          mapOf("value".asNameId() to StringValue(key)),
          SourceElement.NO_SOURCE
        )
  )
)

fun String.nextFrameworkKey(next: String) = "$this:$next"

val KotlinType.isComposable: Boolean
  get() = hasAnnotation(InjektFqNames.Composable)

val KotlinType.isProvideFunctionType: Boolean
  get() = isFunctionType && hasAnnotation(InjektFqNames.Provide) && !isComposable

val KotlinType.isFunctionType: Boolean
  get() = fqName.asString().startsWith("kotlin.Function")

val KotlinType.fullyAbbreviatedType: KotlinType
  get() {
    val abbreviatedType = getAbbreviatedType()
    return if (abbreviatedType != null && abbreviatedType != this) abbreviatedType.fullyAbbreviatedType else this
  }
