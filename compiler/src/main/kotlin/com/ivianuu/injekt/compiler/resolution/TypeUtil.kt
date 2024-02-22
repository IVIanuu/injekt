@file:OptIn(TypeRefinement::class, UnsafeCastFunction::class, UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.fir.resolve.inference.model.*
import org.jetbrains.kotlin.resolve.calls.components.*
import org.jetbrains.kotlin.resolve.calls.inference.components.*
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.*
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.isTypeParameterTypeConstructor
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

fun KotlinType.prepareForInjekt(): UnwrappedType {
  val unwrapped = unwrap()
    .let { unwrapped ->
      if (unwrapped.arguments.isEmpty()) unwrapped
      else {
        val newArguments = unwrapped.arguments.map {
          TypeProjectionImpl(it.projectionKind, it.type.prepareForInjekt())
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

  return getTags().map { it.type.prepareForInjekt() }.wrapTags(unwrapped).unwrap()
}

fun KotlinType.injektHashCode(ctx: Context): Int {
  var result = constructor.declarationDescriptor?.uniqueKey(ctx).hashCode()
  result = 31 * result + isMarkedNullable.hashCode()
  arguments.forEach { result = 31 * result + it.type.injektHashCode(ctx) }
  result = 31 * result + uniqueId.hashCode()
  return result
}

fun KotlinType.substitute(substitutor: NewTypeSubstitutor) = substitutor.safeSubstitute(unwrap())

fun TypeProjection.substitute(substitutor: NewTypeSubstitutor) =
  substitute { it.substitute(substitutor) }

fun KotlinType.allTypes(ctx: Context): Collection<KotlinType> {
  val allTypes = mutableMapOf<Int, KotlinType>()
  fun collect(inner: KotlinType) {
    val key = inner.injektHashCode(ctx)
    if (key in allTypes) return
    allTypes[key] = inner
    inner.arguments.forEach { collect(it.type) }
    inner.constructor.supertypes.forEach { collect(it) }
  }
  collect(this)
  return allTypes.values
}

fun KotlinType.withNullability(isMarkedNullable: Boolean) =
  if (isMarkedNullable) makeNullable() else makeNotNullable()

fun KotlinType.forEachTypeWithSuperTypes(action: (KotlinType) -> Unit) {
  action(this)
  arguments.forEach { it.type.forEachTypeWithSuperTypes(action) }
  supertypes().forEach { it.forEachTypeWithSuperTypes(action) }
}

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

fun KotlinType.subtypeView(descriptor: ClassifierDescriptor): KotlinType? {
  if (constructor.declarationDescriptor == descriptor) return this
  return constructor.supertypes
    .firstNotNullOfOrNull { it.subtypeView(descriptor) }
    ?.let { return it }
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
  hasAnnotation(InjektFqNames.Provide) && isSubtypeOf(ctx.functionType)

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
  val candidateTypeParameters = candidateType.allTypes(ctx)
    .filter { it.constructor.isTypeParameterTypeConstructor() }
    .map { it.constructor.declarationDescriptor!!.cast<TypeParameterDescriptor>() }
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
  val typeSystemContextDelegate = ClassicTypeSystemContextForCS(builtIns, ctx.module.getKotlinTypeRefiner())

  fun injektTypeCheckerState(
    errorTypesEqualToAnything: Boolean,
    stubTypesEqualToAnything: Boolean
  ) = object : TypeCheckerState(
    errorTypesEqualToAnything,
    stubTypesEqualToAnything,
    true,
    SimpleClassicTypeSystemContext,
    KotlinTypePreparator.Default,
    ctx.module.getKotlinTypeRefiner()
  ) {
    override fun customIsSubtypeOf(subType: KotlinTypeMarker, superType: KotlinTypeMarker): Boolean {
      val subTags = subType.cast<KotlinType>().getTags()
      val superTags = superType.cast<KotlinType>().getTags()
      if (subTags.size != superTags.size) return false

      for (i in subTags.indices) {
        val subTag = subTags[i].type
        val superTag = superTags[i].type
        if (!AbstractTypeChecker.isSubtypeOf(this, subTag, superTag))
          return false
      }

      return true
    }
  }

  val constraintSystem = NewConstraintSystemImpl(
    ctx.constraintInjector,
    object : TypeSystemInferenceExtensionContext by typeSystemContextDelegate {
      override fun newTypeCheckerState(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean
      ): TypeCheckerState = injektTypeCheckerState(errorTypesEqualToAnything, stubTypesEqualToAnything)
    },
    LanguageVersionSettingsImpl.DEFAULT
  )

  val typeVariables = buildMap {
    forEachTypeWithSuperTypes {
      if (it.constructor.isTypeParameterTypeConstructor()) {
        val typeParameter = it.constructor.declarationDescriptor.cast<TypeParameterDescriptor>()
        if (typeParameter !in staticTypeParameters)
          put(typeParameter, TypeVariableFromCallableDescriptor(typeParameter))
      }
    }

    if (collectSuperTypeVariables)
      superType.forEachTypeWithSuperTypes {
        if (it.constructor.isTypeParameterTypeConstructor()) {
          val typeParameter = it.constructor.declarationDescriptor.cast<TypeParameterDescriptor>()
          if (typeParameter !in staticTypeParameters)
            put(typeParameter, TypeVariableFromCallableDescriptor(typeParameter))
        }
      }
  }

  if (typeVariables.isEmpty())
    return if (AbstractTypeChecker.isSubtypeOf(
        injektTypeCheckerState(false, false),
      this, superType
    )) EmptySubstitutor else null

  val toTypeVariablesSubstitutor = NewTypeSubstitutorByConstructorMap(
    typeVariables.mapKeys { it.key.typeConstructor }.mapValues {
      it.value.defaultType(constraintSystem.typeSystemContext).cast()
    }
  )

  typeVariables.forEach { constraintSystem.registerVariable(it.value) }

  constraintSystem.addSubtypeConstraint(
    superType.substitute(toTypeVariablesSubstitutor),
    this.substitute(toTypeVariablesSubstitutor),
    SimpleConstraintSystemConstraintPosition
  )

  while (true) {
    if (constraintSystem.hasContradiction || constraintSystem.notFixedTypeVariables.isEmpty())
      break

    fun MutableVariableWithConstraints.getNestedTypeVariables(): Collection<KotlinType> {
      val nestedTypeVariables = mutableMapOf<Int, KotlinType>()
      constraints.forEach { constraint ->
        constraint.type.cast<KotlinType>().forEachTypeWithSuperTypes {
          if (it.constructor.declarationDescriptor in typeVariables)
            nestedTypeVariables[it.injektHashCode(ctx)] = it
        }
      }
      return nestedTypeVariables.values
    }

    val typeVariableToFix = constraintSystem.notFixedTypeVariables.values
      .firstOrNull { typeVariable ->
        typeVariable.getNestedTypeVariables()
          .all { it.constructor in constraintSystem.fixedTypeVariables }
      } ?: constraintSystem.notFixedTypeVariables.values.first()

    val resultType = ctx.resultTypeResolver.findResultType(
      constraintSystem,
      typeVariableToFix,
      TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN
    )

    constraintSystem.fixVariable(
      typeVariableToFix.typeVariable,
      resultType,
      ConeFixVariableConstraintPosition(typeVariableToFix.typeVariable)
    )
  }

  return if (constraintSystem.hasContradiction) null
  else NewTypeSubstitutorByConstructorMap(
    constraintSystem.fixedTypeVariables
      .mapKeys { it.key.cast<TypeVariableTypeConstructor>().originalTypeParameter!!.typeConstructor }
      .mapValues { it.value.cast() }
  )
}

class UniqueIdTypeAttribute(val value: String) : TypeAttribute<UniqueIdTypeAttribute>() {
  override fun union(other: UniqueIdTypeAttribute?) = other
  override fun intersect(other: UniqueIdTypeAttribute?) = this
  override fun add(other: UniqueIdTypeAttribute?) = this

  override fun isSubtypeOf(other: UniqueIdTypeAttribute?): Boolean = true

  override val key = UniqueIdTypeAttribute::class

  override fun toString(): String = "UniqueId($value)"
}

val KotlinType.uniqueId get() = attributes.uniqueId?.value

val TypeAttributes.uniqueId by TypeAttributes.attributeAccessor<UniqueIdTypeAttribute>()

fun KotlinType.withUniqueId(value: String?): KotlinType = when {
  attributes.uniqueId?.value == value -> this
  value == null -> unwrap().replaceAttributes(attributes.remove(attributes.uniqueId!!))
  else -> unwrap().replaceAttributes(attributes + UniqueIdTypeAttribute(value))
}

fun List<KotlinType>.wrapTags(type: KotlinType): KotlinType = if (isEmpty()) type
else type.replace(
  newAnnotations = Annotations.create(
    type.annotations
      .filter { it.type.constructor.declarationDescriptor?.hasAnnotation(InjektFqNames.Tag) != true } +
        map { AnnotationDescriptorImpl(it, emptyMap(), SourceElement.NO_SOURCE) }
  )
)
