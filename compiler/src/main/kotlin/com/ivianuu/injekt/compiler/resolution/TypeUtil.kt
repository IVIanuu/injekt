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
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import org.jetbrains.kotlin.resolve.calls.inference.components.SimpleConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.components.TypeVariableDirectionCalculator
import org.jetbrains.kotlin.resolve.calls.inference.model.FixVariableConstraintPositionImpl
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.FlexibleType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.asSimpleType
import org.jetbrains.kotlin.types.checker.KotlinTypePreparator
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.isTypeParameterTypeConstructor
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.safeSubstitute
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun KotlinType.prepare(): KotlinType {
  return this
  if (constructor is InjektTypeConstructor) return this
  return withConstructor(
    InjektTypeConstructor(
      delegate = constructor,
      frameworkKey = "",
      tags = (this as Annotated).getTags()
    )
  ).replace(
    newArguments = arguments.map { TypeProjectionImpl(it.projectionKind, it.type.prepare()) }
  )
}

fun KotlinType.withConstructor(constructor: TypeConstructor): KotlinType {
  return this
  if (this.constructor == constructor) return this
  return when (val unwrapped = unwrap()) {
    is FlexibleType -> KotlinTypeFactory.flexibleType(
      unwrapped.lowerBound.prepare().cast(),
      unwrapped.upperBound.prepare().cast()
    ).asSimpleType()
    is SimpleType -> KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
      attributes = attributes,
      constructor = constructor,
      arguments = arguments,
      nullable = isMarkedNullable,
      memberScope = memberScope
    )
  }
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
      val subTypeTags = subType.cast<KotlinType>().tags
      val superTypeTags = superType.cast<KotlinType>().tags

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

  return if (constraintSystem.hasContradiction()) null else NewTypeSubstitutorByConstructorMap(
    constraintSystem.system
      .fixedTypeVariables
      .mapKeys { it.key.cast<TypeConstructor>() }
      .mapValues { it.value.cast() }
  )
}

val KotlinType.frameworkKey: String
  get() = constructor.safeAs<InjektTypeConstructor>()?.frameworkKey ?: ""

fun KotlinType.withFrameworkKey(value: String): KotlinType =
  if (frameworkKey == value) this
  else withConstructor(
    InjektTypeConstructor(
      delegate = constructor.safeAs<InjektTypeConstructor>()?.delegate ?: constructor,
      frameworkKey = value,
      tags = tags
    )
  )

val KotlinType.tags: List<KotlinType>
  get() = constructor.safeAs<InjektTypeConstructor>()?.tags ?: emptyList()

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

data class InjektTypeConstructor(
  val delegate: TypeConstructor,
  val frameworkKey: String,
  val tags: List<KotlinType>
) : TypeConstructor by delegate
