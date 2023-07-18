/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.fullyAbbreviatedType
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.FlexibleType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeAttribute
import org.jetbrains.kotlin.types.TypeAttributes
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.isTypeParameterTypeConstructor
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast
import kotlin.reflect.KClass

fun KotlinType.prepareForInjekt(ctx: Context): KotlinType {
  val unwrapped = getAbbreviation() ?: this
  return when {
    unwrapped.constructor.isDenotable -> unwrapped
    unwrapped.constructor.supertypes.isNotEmpty() -> CommonSupertypes
      .commonSupertype(unwrapped.constructor.supertypes)
    else -> return ctx.nullableAnyType
  }.fullyAbbreviatedType
}

fun KotlinType.substitute(
  substitutor: TypeSubstitutor,
  variance: Variance = Variance.INVARIANT,
): KotlinType = substitutor.safeSubstitute(this, variance)

val KotlinType.allTypes: Set<KotlinType> get() {
  val allTypes = mutableSetOf<KotlinType>()
  fun collect(inner: KotlinType) {
    if (!allTypes.add(inner)) return
    inner.arguments.forEach { collect(it.type) }
    inner.constructor.supertypes.forEach { collect(it) }
  }
  collect(this)
  return allTypes
}

fun KotlinType.subtypeView(descriptor: ClassifierDescriptor): KotlinType? {
  if (constructor.declarationDescriptor == descriptor) return this
  return constructor.supertypes
    .firstNotNullOfOrNull { it.subtypeView(descriptor) }
    ?.let { return it }
}

fun KotlinType.withArguments(arguments: List<TypeProjection>): KotlinType =
  if (this.arguments == arguments) this
  else replace(newArguments = arguments)

fun KotlinType.withNullability(isMarkedNullable: Boolean) =
  if (isMarkedNullable) makeNullable() else makeNotNullable()

fun KotlinType.anyType(action: (KotlinType) -> Boolean): Boolean =
  action(this) || arguments.any { it.type.anyType(action) }

fun KotlinType.firstSuperTypeOrNull(action: (KotlinType) -> Boolean): KotlinType? =
  takeIf(action) ?: constructor.supertypes.firstNotNullOfOrNull { it.firstSuperTypeOrNull(action) }

fun KotlinType.renderToString() = buildString {
  render { append(it) }
}

fun KotlinType.render(
  depth: Int = 0,
  renderType: (KotlinType) -> Boolean = { true },
  append: (String) -> Unit
) {
  if (depth > 15) return

  fun TypeProjection.render(depth: Int) {
    if (isStarProjection) append("*")
    else type.render(depth, renderType, append)
  }

  fun KotlinType.inner() {
    if (!renderType(this)) return

    append(constructor.declarationDescriptor!!.fqNameSafe.asString())

    if (arguments.isNotEmpty()) {
      append("<")
      arguments.forEachIndexed { index, typeArgument ->
        typeArgument.render(depth = depth + 1)
        if (index != arguments.lastIndex) append(", ")
      }
      append(">")
    }
    if (isMarkedNullable) append("?")
  }
  inner()
}

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
      classifiers += type.constructor.declarationDescriptor!!
      type.arguments.forEach { visit(it.type) }
    }
    visit(this)
    return classifiers
  }

fun KotlinType.isProvideFunctionType(ctx: Context): Boolean =
  isProvide && isSubtypeOf(ctx.functionType)

val KotlinType.isFunctionType: Boolean
  get() = constructor.declarationDescriptor!!.fqNameSafe.asString().startsWith("kotlin.Function")

fun KotlinType.isUnconstrained(staticTypeParameters: List<TypeParameterDescriptor>): Boolean =
  constructor.isTypeParameterTypeConstructor() &&
      constructor.declarationDescriptor !in staticTypeParameters &&
      constructor.supertypes.all {
        it.constructor.declarationDescriptor?.fqNameSafe == InjektFqNames.Any ||
            it.isUnconstrained(staticTypeParameters)
      }

fun buildContextForSpreadingInjectable(
  constraintType: KotlinType,
  candidateType: KotlinType,
  staticTypeParameters: List<TypeParameterDescriptor>,
  ctx: Context
): TypeSubstitutor? {
  val candidateTypeParameters = candidateType.allTypes
    .filter { it.constructor.isTypeParameterTypeConstructor() }
    .map { it.constructor.declarationDescriptor.cast<TypeParameterDescriptor>() }
  return candidateType.buildSubstitutor(
    constraintType,
    candidateTypeParameters + staticTypeParameters,
    true,
    ctx
  )
}

fun KotlinType.buildSubstitutor(
  superType: KotlinType,
  staticTypeParameters: List<TypeParameterDescriptor>,
  collectSuperTypeVariables: Boolean = false,
  ctx: Context
): TypeSubstitutor? {
  return if (isSubtypeOf(superType)) TypeSubstitutor.EMPTY else null
  /*val constraintSystem = SimpleConstraintSystemImpl(

  )
  val typeCtx = TypeContext(ctx.ctx)
  staticTypeParameters.forEach { typeCtx.addStaticTypeParameter(it) }
  allTypes.forEach {
    if (it.classifier.isTypeParameter)
      typeCtx.addTypeVariable(it.classifier)
  }

  if (collectSuperTypeVariables) {
    superType.allTypes.forEach {
      if (it.classifier.isTypeParameter)
        typeCtx.addTypeVariable(it.classifier)
    }
  }
  typeCtx.addInitialSubTypeConstraint(this, superType)
  typeCtx.fixTypeVariables()
  return typeCtx*/
}

data class FrameworkKeyAttribute(val value: String?) : TypeAttribute<FrameworkKeyAttribute>() {
  override val key: KClass<out FrameworkKeyAttribute>
    get() = FrameworkKeyAttribute::class
  override fun add(other: FrameworkKeyAttribute?) = this
  override fun intersect(other: FrameworkKeyAttribute?) = this
  override fun union(other: FrameworkKeyAttribute?) = this
  override fun isSubtypeOf(other: FrameworkKeyAttribute?) = true
}

val TypeAttributes.frameworkKeyAttribute: FrameworkKeyAttribute? by TypeAttributes.attributeAccessor()
val KotlinType.frameworkKey: String? get() = attributes.frameworkKeyAttribute?.value
fun KotlinType.withFrameworkKey(value: String?): KotlinType =
  if (frameworkKey == value) this
  else {
    val newAttributes = attributes + FrameworkKeyAttribute(value)
    when (val unwrapped = unwrap()) {
      is FlexibleType -> KotlinTypeFactory.flexibleType(
        unwrapped.lowerBound.withFrameworkKey(value).cast(),
        unwrapped.upperBound.withFrameworkKey(value).cast()
      )
      is SimpleType -> unwrapped.replaceAttributes(newAttributes)
    }
  }

val KotlinType.isInject: Boolean
  get() = hasAnnotation(InjektFqNames.Inject)

val KotlinType.isProvide: Boolean
  get() = hasAnnotation(InjektFqNames.Provide)

fun KotlinType.toAnnotation() = AnnotationDescriptorImpl(
  this,
  emptyMap(),
  SourceElement.NO_SOURCE
)
