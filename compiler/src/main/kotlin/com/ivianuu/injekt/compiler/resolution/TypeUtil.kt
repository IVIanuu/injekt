/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.getTags
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.uniqueKey
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.components.EmptySubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.FlexibleType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.checker.KotlinTypePreparator
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.isTypeParameterTypeConstructor
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast

inline fun buildSubstitutor(block: MutableMap<TypeConstructor, UnwrappedType>.() -> Unit) =
  NewTypeSubstitutorByConstructorMap(buildMap(block))

fun KotlinType.substitute(
  substitutor: NewTypeSubstitutor
): KotlinType = substitutor.safeSubstitute(unwrap())

fun KotlinType.allTypes(ctx: Context): List<KotlinType> {
  val allTypes = mutableMapOf<Int, KotlinType>()
  fun collect(inner: KotlinType) {
    val key = inner.injektHashCode(ctx)
    if (key in allTypes) return
    allTypes[key] = inner
    inner.getTags().forEach { collect(it) }
    inner.arguments.forEach { collect(it.type) }
    inner.constructor.supertypes.forEach { collect(it) }
  }
  collect(this)
  return allTypes.values.toList()
}

val KotlinType.allTypesWithoutSuperTypes: List<KotlinType>
  get() {
    val types = mutableListOf<KotlinType>()
    fun collect(inner: KotlinType) {
      inner.getTags().forEach { collect(it) }
      inner.arguments.forEach { collect(it.type) }
    }
    collect(this)
    return types
  }

fun KotlinType.subtypeView(descriptor: ClassifierDescriptor): KotlinType? {
  if (constructor.declarationDescriptor == descriptor) return this
  return constructor.supertypes
    .firstNotNullOfOrNull { it.subtypeView(descriptor) }
    ?.let { return it }
}

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

    getTags().forEach {
      if (!renderType(it)) return@forEach
      append("@")
      it.render(depth, renderType, append)
      append(" ")
    }

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

fun KotlinType.typeSize(ctx: Context): Int {
  var typeSize = 0
  val seen = mutableSetOf<Int>()
  fun visit(type: KotlinType) {
    typeSize++
    if (seen.add(type.injektHashCode(ctx)))
      type.arguments.forEach { visit(it.type) }
  }
  visit(this)
  return typeSize
}

fun KotlinType.coveringSet(ctx: Context): Set<ClassifierDescriptor> {
  val classifiers = mutableSetOf<ClassifierDescriptor>()
  val seen = mutableSetOf<Int>()
  fun visit(type: KotlinType) {
    if (!seen.add(type.injektHashCode(ctx))) return
    classifiers += type.constructor.declarationDescriptor!!
    type.arguments.forEach { visit(it.type) }
  }
  visit(this)
  return classifiers
}

fun KotlinType.isProvideFunctionType(ctx: Context): Boolean =
  isProvide && isInjektSubtypeOf(ctx.functionType)

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
): NewTypeSubstitutor? {
  val candidateTypeParameters = candidateType.allTypes(ctx)
    .filter { it.constructor.isTypeParameterTypeConstructor() }
    .map { it.constructor.declarationDescriptor.cast<TypeParameterDescriptor>() }
  return candidateType.runCandidateInference(
    constraintType,
    candidateTypeParameters + staticTypeParameters,
    true,
    ctx
  )
}

class KotlinTypeKey(val type: KotlinType, val ctx: Context) {
  private var _hashCode: Int = 0
  override fun hashCode(): Int {
    if (_hashCode != 0) return _hashCode
    _hashCode = type.injektHashCode(ctx)
    return _hashCode
  }

  override fun equals(other: Any?): Boolean =
    other is KotlinTypeKey && hashCode() == other.hashCode()
}

fun KotlinType?.injektEquals(other: KotlinType?, ctx: Context) =
  injektHashCode(ctx) == other.injektHashCode(ctx)

fun KotlinType?.injektHashCode(ctx: Context): Int {
  return if (this == null) 0
  else allTypesWithoutSuperTypes.fold(0) { acc, next ->
    var result = acc
    result = 31 * result + next.constructor.declarationDescriptor!!.uniqueKey(ctx).hashCode()
    result = 31 * result + next.isProvide.hashCode()
    result = 31 * result + next.isInject.hashCode()
    result = 31 * result + next.frameworkKey.hashCode()
    return result
  }
}

fun KotlinType.isInjektSubtypeOf(superType: KotlinType): Boolean = AbstractTypeChecker.isSubtypeOf(
  object : TypeCheckerState(
    false,
    false,
    true,
    SimpleClassicTypeSystemContext,
    KotlinTypePreparator.Default,
    KotlinTypeRefiner.Default
  ) {
    override fun customIsSubtypeOf(subType: KotlinTypeMarker, superType: KotlinTypeMarker): Boolean {
      val subTypeTags = subType.cast<KotlinType>().getTags()
      val superTypeTags = superType.cast<KotlinType>().getTags()

      if (subTypeTags.size != superTypeTags.size)
        return false

      for (index in subTypeTags.indices) {
        val subTypeTag = subTypeTags[index]
        val superTypeTag = superTypeTags[index]
        if (!AbstractTypeChecker.isSubtypeOf(this, subTypeTag, superTypeTag))
          return false
      }

      return true
    }
  },
  this,
  superType
)

fun KotlinType.runCandidateInference(
  superType: KotlinType,
  staticTypeParameters: List<TypeParameterDescriptor>,
  collectSuperTypeVariables: Boolean = false,
  ctx: Context
): NewTypeSubstitutor? {
  return if (isInjektSubtypeOf(superType)) EmptySubstitutor else null
  /*val constraintSystem = SimpleConstraintSystemImpl(
    ConstraintInjector(
      ConstraintIncorporator(

      )
    ),
    ctx.module.builtIns,
    KotlinTypeRefiner.Default,
    LanguageVersionSettingsImpl.DEFAULT
  )
  staticTypeParameters.forEach {
    typeCtx.addStaticTypeParameter(it)
  }
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

val KotlinType.frameworkKey: String?
  get() = annotations.findAnnotation(InjektFqNames.FrameworkKey)
    ?.allValueArguments?.values?.single()?.value?.cast<String>()

fun KotlinType.withFrameworkKey(value: String?, ctx: Context): KotlinType =
  if (frameworkKey == value) this
  else {
    when (val unwrapped = unwrap()) {
      is FlexibleType -> KotlinTypeFactory.flexibleType(
        unwrapped.lowerBound.withFrameworkKey(value, ctx).cast(),
        unwrapped.upperBound.withFrameworkKey(value, ctx).cast()
      )
      is SimpleType -> unwrapped.replaceAnnotations(
        newAnnotations = Annotations.create(
          buildList {
            annotations.forEach {
              if (it.type != ctx.frameworkKeyType)
                add(it)
            }

            if (value != null)
              add(ctx.frameworkKeyType.toAnnotation(
                mapOf("value".asNameId() to StringValue(value))
              ))
          }
        )
      )
    }
  }

val KotlinType.isInject: Boolean
  get() = hasAnnotation(InjektFqNames.Inject)

val KotlinType.isProvide: Boolean
  get() = hasAnnotation(InjektFqNames.Provide)

fun KotlinType.toAnnotation(
  valueArguments: Map<Name, ConstantValue<*>> = emptyMap()
) = AnnotationDescriptorImpl(
  this,
  valueArguments,
  SourceElement.NO_SOURCE
)
