/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.getTags
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import org.jetbrains.kotlin.resolve.calls.inference.components.SimpleConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.components.TypeVariableDirectionCalculator
import org.jetbrains.kotlin.resolve.calls.inference.model.FixVariableConstraintPositionImpl
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.FlexibleType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.isTypeParameterTypeConstructor
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.safeSubstitute
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast

fun KotlinType.prepare(): KotlinType {
  val unwrapped = unwrap()
    .let { unwrapped ->
      if (unwrapped.arguments.isEmpty()) unwrapped
      else {
        val newArguments = unwrapped.arguments.map {
          TypeProjectionImpl(it.projectionKind, it.type.prepare())
        }
        when (unwrapped) {
          is SimpleType -> replace(newArguments)
          is FlexibleType -> KotlinTypeFactory.flexibleType(
            unwrapped.lowerBound.replace(newArguments),
            unwrapped.upperBound.replace(newArguments)
          )
        }
      }
    }
  val tags = getTags().map { it.prepare() }
  return tags.wrap(unwrapped.replaceAnnotations(
    Annotations.create(
      unwrapped.annotations.filterNot { it.type.constructor.declarationDescriptor.isTag }
    )
  ))
}

fun List<KotlinType>.wrap(type: KotlinType): KotlinType = foldRight(type) { nextTag, acc ->
  nextTag.wrap(acc)
}

val ClassifierDescriptor?.isTag: Boolean
  get() = this?.hasAnnotation(InjektFqNames.Tag) == true

fun KotlinType.unwrapTags(): KotlinType = if (constructor.declarationDescriptor.isTag) this
else arguments.last().type.unwrapTags()

fun KotlinType.wrap(type: KotlinType): KotlinType {
  val newArguments = if (arguments.size < constructor.parameters.size)
    arguments + type.asTypeProjection()
  else arguments.dropLast(1) + type.asTypeProjection()
  return replace(newArguments)
}

inline fun buildSubstitutor(block: MutableMap<TypeConstructor, UnwrappedType>.() -> Unit) =
  NewTypeSubstitutorByConstructorMap(buildMap(block))

fun KotlinType.substitute(
  substitutor: NewTypeSubstitutor
): KotlinType = substitutor.safeSubstitute(unwrap())

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

fun runSpreadingInjectableInference(
  constraintType: KotlinType,
  candidateType: KotlinType,
  staticTypeParameters: List<TypeParameterDescriptor>,
  ctx: Context
): NewTypeSubstitutor? {
  val candidateTypeParameters = candidateType.allTypes
    .filter { it.constructor.isTypeParameterTypeConstructor() }
    .map { it.constructor.declarationDescriptor.cast<TypeParameterDescriptor>() }
  return candidateType.runCandidateInference(
    constraintType,
    candidateTypeParameters + staticTypeParameters,
    ctx,
    true
  )
}

fun KotlinType.runCandidateInference(
  superType: KotlinType,
  staticTypeParameters: List<TypeParameterDescriptor>,
  ctx: Context,
  collectSuperTypeVariables: Boolean = false,
): NewTypeSubstitutor? {
  val constraintSystem = SimpleConstraintSystemImpl(
    ctx.constraintInjector,
    ctx.module.builtIns,
    KotlinTypeRefiner.Default,
    LanguageVersionSettingsImpl.DEFAULT
  )

  val substitutor = constraintSystem.registerTypeVariables(
    buildList {
      allTypes.forEach { type ->
        if (type.constructor.isTypeParameterTypeConstructor() &&
          type.constructor.declarationDescriptor !in staticTypeParameters)
          add(type.constructor.declarationDescriptor!!.cast())
      }

      if (collectSuperTypeVariables)
        superType.allTypes.forEach { type ->
          if (type.constructor.isTypeParameterTypeConstructor() &&
            type.constructor.declarationDescriptor !in staticTypeParameters)
            add(type.constructor.declarationDescriptor!!.cast())
        }
    }
  )

  val finalSubType = substitutor.safeSubstitute(this.unwrap())
  val finalSuperType = substitutor.safeSubstitute(superType.unwrap())

  constraintSystem.addSubtypeConstraint(finalSubType, finalSuperType)

  while (constraintSystem.system.notFixedTypeVariables.isNotEmpty()) {
    constraintSystem.system.notFixedTypeVariables.toList().forEach {
      constraintSystem.system.fixVariable(
        it.second.typeVariable,
        ctx.resultTypeResolver.findResultType(constraintSystem.system, it.second, TypeVariableDirectionCalculator.ResolveDirection.TO_SUBTYPE),
        FixVariableConstraintPositionImpl(it.second.typeVariable, null)
      )
    }
  }

  val isOk = !constraintSystem.hasContradiction()

  return if (constraintSystem.hasContradiction()) null else NewTypeSubstitutorByConstructorMap(
    constraintSystem.system
      .fixedTypeVariables
      .mapKeys { it.key.cast<TypeConstructor>() }
      .mapValues { it.value.cast() }
  )
}

val KotlinType.frameworkKey: String get() = ""

fun KotlinType.withFrameworkKey(value: String) = this

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
