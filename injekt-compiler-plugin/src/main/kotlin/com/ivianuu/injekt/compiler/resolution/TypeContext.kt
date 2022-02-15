/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.types.model.*

interface TypeCheckerContext {
  val ctx: Context
  fun isDenotable(type: TypeRef): Boolean
  fun addSubTypeConstraint(subType: TypeRef, superType: TypeRef): Boolean? = null
}

fun TypeRef.isEqualTo(other: TypeRef, ctx: TypeCheckerContext): Boolean {
  if (this == other) return true

  if (ctx.isDenotable(this) && ctx.isDenotable(other)) {
    if (classifier != other.classifier) return false

    if (isNullable != other.isNullable) return false

    for (i in arguments.indices) {
      val thisParameter = arguments[i]
      val otherParameter = other.arguments[i]
      if (thisParameter.isStarProjection &&
        otherParameter.isStarProjection
      ) continue
      if (effectiveVariance(thisParameter.variance, otherParameter.variance, TypeVariance.INV) !=
        effectiveVariance(otherParameter.variance, thisParameter.variance, TypeVariance.INV)
      )
        return false
      if (!thisParameter.isEqualTo(otherParameter, ctx))
        return false
    }

    return true
  }

  return isSubTypeOf(other, ctx) && other.isSubTypeOf(this, ctx)
}

fun TypeRef.isSubTypeOf(
  superType: TypeRef,
  ctx: TypeCheckerContext
): Boolean {
  if (this == superType) return true

  ctx.addSubTypeConstraint(this, superType)
    ?.let { return it }

  if (classifier.fqName == InjektFqNames.Nothing &&
    (!isNullable || superType.isNullableType)
  ) return true

  if (superType.classifier.fqName == InjektFqNames.Any &&
    (superType.isNullable || !isNullableType)
  ) return true

  if (superType.isComposableType && !isComposableType) return false

  subtypeView(superType.classifier)
    ?.let { return it.isSubTypeOfSameClassifier(superType, ctx) }

  return false
}

private fun TypeRef.isSubTypeOfSameClassifier(
  superType: TypeRef,
  ctx: TypeCheckerContext
): Boolean {
  if (!superType.isNullable && isNullable) return false

  for (i in arguments.indices) {
    val argument = arguments[i]
    val parameter = superType.arguments[i]
    if (argument.isStarProjection || parameter.isStarProjection) continue
    val originalParameter = superType.classifier.defaultType.arguments[i]
    val argumentOk = when (effectiveVariance(parameter.variance, argument.variance, originalParameter.variance)) {
      TypeVariance.IN -> parameter.isSubTypeOf(argument, ctx)
      TypeVariance.OUT -> argument.isSubTypeOf(parameter, ctx)
      TypeVariance.INV -> argument.isEqualTo(parameter, ctx)
    }

    if (!argumentOk) return false
  }

  return true
}

sealed interface TypeContextError {
  data class ConstraintError(
    val subType: TypeRef,
    val superType: TypeRef,
    val kind: ConstraintKind
  ) : TypeContextError

  object NotEnoughInformation : TypeContextError
}

class VariableWithConstraints(val typeVariable: ClassifierRef) {
  val constraints = mutableListOf<Constraint>()

  fun addConstraint(constraint: Constraint): Boolean {
    for (previousConstraint in constraints.toList()) {
      if (previousConstraint.type == constraint.type) {
        if (newConstraintIsUseless(previousConstraint, constraint))
          return false

        val isMatchingForSimplification = when (previousConstraint.kind) {
          ConstraintKind.EQUAL -> true
          ConstraintKind.LOWER -> constraint.kind == ConstraintKind.UPPER
          ConstraintKind.UPPER -> constraint.kind == ConstraintKind.LOWER
        }
        if (isMatchingForSimplification) {
          val actualConstraint = if (constraint.kind != ConstraintKind.EQUAL) {
            Constraint(
              typeVariable,
              constraint.type,
              ConstraintKind.EQUAL,
              constraint.position,
              constraint.derivedFrom
            )
          } else constraint
          constraints.removeAll { it.type == actualConstraint.type }
          constraints += actualConstraint
          return true
        }
      }
    }

    constraints += constraint

    return true
  }

  private fun newConstraintIsUseless(old: Constraint, new: Constraint): Boolean =
    old.kind == new.kind || when (old.kind) {
      ConstraintKind.EQUAL -> true
      ConstraintKind.LOWER -> new.kind == ConstraintKind.LOWER
      ConstraintKind.UPPER -> new.kind == ConstraintKind.UPPER
    }
}

data class Constraint(
  val typeVariable: ClassifierRef,
  val type: TypeRef,
  val kind: ConstraintKind,
  val position: ConstraintPosition,
  val derivedFrom: Set<ClassifierRef>
)

enum class ConstraintKind {
  LOWER, UPPER, EQUAL
}

sealed interface ConstraintPosition {
  object FixVariable : ConstraintPosition
  object DeclaredUpperBound : ConstraintPosition
  object Unknown : ConstraintPosition
}

fun TypeRef.buildContext(
  superType: TypeRef,
  staticTypeParameters: List<ClassifierRef>,
  collectSuperTypeVariables: Boolean = false,
  ctx: TypeCheckerContext
): TypeContext {
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
  return typeCtx
}

class TypeContext(override val ctx: Context) : TypeCheckerContext {
  private val staticTypeParameters = mutableListOf<ClassifierRef>()
  private val typeVariables = mutableMapOf<ClassifierRef, VariableWithConstraints>()
  val fixedTypeVariables = mutableMapOf<ClassifierRef, TypeRef>()

  val errors = mutableSetOf<TypeContextError>()

  val isOk: Boolean get() = errors.isEmpty()

  private var possibleNewConstraints: MutableList<Constraint>? = null

  private fun addPossibleNewConstraint(constraint: Constraint) {
    (possibleNewConstraints ?: mutableListOf<Constraint>()
      .also { possibleNewConstraints = it }) += constraint
  }

  fun addStaticTypeParameter(typeParameter: ClassifierRef) {
    staticTypeParameters += typeParameter
  }

  fun addTypeVariable(typeParameter: ClassifierRef) {
    if (typeParameter in staticTypeParameters) return
    val variableWithConstraints = VariableWithConstraints(typeParameter)
    typeVariables[typeParameter] = variableWithConstraints
    typeParameter.superTypes.forEach {
      if (it != ctx.nullableAnyType)
        addInitialSubTypeConstraint(typeParameter.defaultType, it)
    }
  }

  fun addInitialSubTypeConstraint(subType: TypeRef, superType: TypeRef) {
    runIsSubTypeOf(subType, superType)
    processConstraints()
  }

  private fun addInitialEqualityConstraint(a: TypeRef, b: TypeRef) {
    val (typeVariable, equalType) = when {
      a.classifier.isTypeParameter -> a to b
      b.classifier.isTypeParameter -> b to a
      else -> return
    }

    addPossibleNewConstraint(
      Constraint(
        typeVariable.classifier, equalType,
        ConstraintKind.EQUAL, ConstraintPosition.FixVariable,
        emptySet()
      )
    )

    processConstraints()
  }

  private fun processConstraints() {
    while (possibleNewConstraints != null) {
      if (!isOk) break
      val constraintsToProcess = possibleNewConstraints!!
        .also { possibleNewConstraints = null }
      var anyAdded = false
      for (constraint in constraintsToProcess) {
        if (shouldWeSkipConstraint(constraint)) continue
        val variableWithConstraints = typeVariables[constraint.typeVariable]!!
        val wasAdded = variableWithConstraints.addConstraint(constraint)
        anyAdded = anyAdded || wasAdded
        if (wasAdded) {
          directWithVariable(constraint.typeVariable, constraint)
          insideOtherConstraint(constraint.typeVariable, constraint)
        }
      }
      if (!anyAdded) break
    }
  }

  private fun shouldWeSkipConstraint(constraint: Constraint): Boolean {
    if (constraint.kind == ConstraintKind.EQUAL)
      return false

    val constraintType = constraint.type

    if (constraintType.classifier == constraint.typeVariable) {
      if (constraintType.isNullable && constraint.kind == ConstraintKind.LOWER)
        return false
      return true
    }
    if (constraint.position is ConstraintPosition.DeclaredUpperBound &&
      constraint.kind == ConstraintKind.UPPER &&
      constraintType == ctx.nullableAnyType
    ) return true

    return false
  }

  fun fixTypeVariables() {
    while (true) {
      if (!isOk) break
      val unfixedTypeVariables = typeVariables
        .filterKeys { it !in fixedTypeVariables }
        .values
      if (unfixedTypeVariables.isEmpty()) break
      var typeVariableToFix = unfixedTypeVariables
        .firstOrNull { typeVariable ->
          typeVariable.getNestedTypeVariables()
            .all { it.classifier in fixedTypeVariables }
        }
      if (typeVariableToFix == null) {
        typeVariableToFix = unfixedTypeVariables.first()
      }
      if (!fixVariable(typeVariableToFix)) {
        addError(TypeContextError.NotEnoughInformation)
        break
      }
    }
  }

  private fun fixVariable(variableWithConstraints: VariableWithConstraints): Boolean {
    val type = getFixedType(variableWithConstraints)

    addInitialEqualityConstraint(variableWithConstraints.typeVariable.defaultType, type)

    typeVariables
      .filterNot { it.key == variableWithConstraints.typeVariable || it.key in fixedTypeVariables }
      .forEach { (_, otherVariableWithConstraints) ->
        otherVariableWithConstraints.constraints.removeAll { otherConstraint ->
          otherConstraint.type.anyType {
            it.classifier == variableWithConstraints.typeVariable
          }
        }
      }

    fixedTypeVariables[variableWithConstraints.typeVariable] = type

    return true
  }

  private fun getFixedType(variableWithConstraints: VariableWithConstraints): TypeRef {
    variableWithConstraints.constraints
      .transform {
        if (it.kind == ConstraintKind.EQUAL)
          add(it.type)
      }
      .singleBestRepresentative()
      ?.let { return it }

    val subType = findSubType(variableWithConstraints)
    val superType = findSuperType(variableWithConstraints)
    resultType(subType, superType, variableWithConstraints)
      ?.let { return it }

    return ctx.nullableAnyType
  }

  private fun resultType(
    firstCandidate: TypeRef?,
    secondCandidate: TypeRef?,
    variableWithConstraints: VariableWithConstraints
  ): TypeRef? {
    if (firstCandidate == null || secondCandidate == null) return firstCandidate ?: secondCandidate

    if (isSuitableType(firstCandidate, variableWithConstraints)) return firstCandidate

    return if (isSuitableType(secondCandidate, variableWithConstraints)) {
      secondCandidate
    } else {
      firstCandidate
    }
  }

  private fun isSuitableType(
    resultType: TypeRef,
    variableWithConstraints: VariableWithConstraints
  ): Boolean {
    if (resultType.classifier.fqName == InjektFqNames.Nothing) return false
    val filteredConstraints = variableWithConstraints.constraints
    for (constraint in filteredConstraints) {
      if (!checkConstraint(constraint.type, constraint.kind, resultType))
        return false
    }

    return true
  }

  private fun checkConstraint(
    constraintType: TypeRef,
    kind: ConstraintKind,
    resultType: TypeRef
  ): Boolean = when (kind) {
    ConstraintKind.EQUAL -> constraintType.isEqualTo(resultType, this)
    ConstraintKind.LOWER -> constraintType.isSubTypeOf(resultType, this)
    ConstraintKind.UPPER -> resultType.isSubTypeOf(constraintType, this)
  }

  private fun findSuperType(variableWithConstraints: VariableWithConstraints): TypeRef? =
    variableWithConstraints
      .constraints
      .transform {
        if (it.kind == ConstraintKind.UPPER)
          add(it.type)
      }
      .takeIf { it.isNotEmpty() }
      ?.let { intersectTypes(it, this) }

  private fun findSubType(variableWithConstraints: VariableWithConstraints): TypeRef? =
    variableWithConstraints.constraints
      .transform {
        if (it.kind == ConstraintKind.LOWER)
          add(it.type)
      }
      .takeIf { it.isNotEmpty() }
      ?.let { commonSuperType(it, ctx = this) }

  private fun List<TypeRef>.singleBestRepresentative(): TypeRef? {
    if (size == 1) return first()
    return firstOrNull { candidate ->
      all { other -> candidate.isEqualTo(other, ctx = ctx) }
    }
  }

  private fun VariableWithConstraints.getNestedTypeVariables(): List<TypeRef> {
    val nestedTypeVariables = mutableListOf<TypeRef>()
    constraints.forEach { constraint ->
      constraint.type.allTypes.forEach {
        if (it.classifier in typeVariables)
          nestedTypeVariables += it
      }
    }
    return nestedTypeVariables
  }

  override fun isDenotable(type: TypeRef): Boolean = type.classifier !in typeVariables

  override fun addSubTypeConstraint(subType: TypeRef, superType: TypeRef): Boolean? {
    var answer: Boolean? = null

    if (superType.classifier in typeVariables)
      answer = addLowerConstraint(superType, subType)

    if (subType.classifier in typeVariables)
      return addUpperConstraint(subType, superType) && (answer ?: true)
    else {
      subType.source?.let {
        return addUpperConstraint(it.defaultType, superType) && (answer ?: true)
      }
    }

    return answer
  }

  private fun addUpperConstraint(typeVariable: TypeRef, superType: TypeRef): Boolean {
    addPossibleNewConstraint(
      Constraint(
        typeVariable.classifier, superType, ConstraintKind.UPPER,
        ConstraintPosition.Unknown,
        emptySet()
      )
    )

    var result = true

    if (typeVariable.isNullable) {
      result = superType.classifier in typeVariables ||
          ctx.nullableNothingType.isSubTypeOf(superType, this)
    }

    return result
  }

  private fun addLowerConstraint(typeVariable: TypeRef, subType: TypeRef): Boolean {
    addPossibleNewConstraint(
      Constraint(
        typeVariable.classifier,
        subType, ConstraintKind.LOWER, ConstraintPosition.Unknown,
        emptySet()
      )
    )
    return true
  }

  private fun directWithVariable(typeVariable: ClassifierRef, constraint: Constraint) {
    if (constraint.kind != ConstraintKind.LOWER) {
      typeVariables[typeVariable]!!.constraints.toList().forEach {
        if (!isOk) return@forEach
        if (it.kind != ConstraintKind.UPPER) {
          runIsSubTypeOf(it.type, constraint.type)
        }
      }
    }

    if (constraint.kind != ConstraintKind.UPPER) {
      typeVariables[typeVariable]!!.constraints.toList().forEach {
        if (!isOk) return@forEach
        if (it.kind != ConstraintKind.LOWER) {
          runIsSubTypeOf(constraint.type, it.type)
        }
      }
    }
  }

  private fun insideOtherConstraint(typeVariable: ClassifierRef, constraint: Constraint) {
    for (typeVariableWithConstraint in typeVariables.values) {
      if (!isOk) break
      for (variableConstraint in typeVariableWithConstraint.constraints) {
        if (!isOk) break
        if (variableConstraint.type.anyType { it.classifier == typeVariable })
          generateNewConstraint(typeVariableWithConstraint.typeVariable, variableConstraint, typeVariable, constraint)
      }
    }
  }

  private fun runIsSubTypeOf(subType: TypeRef, superType: TypeRef) {
    if (!subType.isSubTypeOf(superType, this)) {
      addError(TypeContextError.ConstraintError(subType, superType, ConstraintKind.UPPER))
    }
  }

  private fun generateNewConstraint(
    targetVariable: ClassifierRef,
    baseConstraint: Constraint,
    otherVariable: ClassifierRef,
    otherConstraint: Constraint
  ) {
    val newConstraint = when (otherConstraint.kind) {
      ConstraintKind.EQUAL -> otherConstraint.type
      ConstraintKind.UPPER -> otherConstraint.type.copy(variance = TypeVariance.OUT, source = otherVariable)
      ConstraintKind.LOWER -> otherConstraint.type.copy(variance = TypeVariance.IN, source = otherVariable)
    }
    val substitutedType = baseConstraint.type.substitute(mapOf(otherVariable to newConstraint))
    if (baseConstraint.kind != ConstraintKind.LOWER) {
      addNewConstraint(
        targetVariable, baseConstraint,
        otherVariable, otherConstraint, substitutedType, ConstraintKind.UPPER
      )
    }
    if (baseConstraint.kind != ConstraintKind.UPPER) {
      addNewConstraint(
        targetVariable, baseConstraint,
        otherVariable, otherConstraint, substitutedType, ConstraintKind.LOWER
      )
    }
  }

  private fun addNewConstraint(
    targetVariable: ClassifierRef,
    baseConstraint: Constraint,
    otherVariable: ClassifierRef,
    otherConstraint: Constraint,
    newConstraint: TypeRef,
    kind: ConstraintKind
  ) {
    val derivedFrom = baseConstraint.derivedFrom.toMutableSet()
      .also { it.addAll(otherConstraint.derivedFrom) }
    if (otherVariable in derivedFrom) return

    derivedFrom.add(otherVariable)
    addPossibleNewConstraint(
      Constraint(
        targetVariable, newConstraint,
        kind, ConstraintPosition.Unknown,
        derivedFrom
      )
    )
  }

  private fun addError(error: TypeContextError) {
    errors += error
  }
}

fun commonSuperType(
  types: List<TypeRef>,
  depth: Int = -(types.maxOfOrNull { it.typeDepth } ?: 0),
  ctx: TypeCheckerContext
): TypeRef {
  types.singleOrNull()?.let { return it }
  val notAllNotNull = types.any { it.isNullableType }
  val notNullTypes = if (notAllNotNull) types.map { it.withNullability(false) } else types

  val commonSuperType = commonSuperTypeForNotNullTypes(notNullTypes, depth, ctx)
  return if (notAllNotNull)
    refineNullabilityForUndefinedNullability(types, commonSuperType, ctx)
      ?: commonSuperType.withNullability(true)
  else
    commonSuperType
}

private fun refineNullabilityForUndefinedNullability(
  types: List<TypeRef>,
  commonSuperType: TypeRef,
  ctx: TypeCheckerContext
): TypeRef? {
  if (!commonSuperType.classifier.isTypeParameter) return null

  /*val actuallyNotNull =
      types.all { hasPathByNotMarkedNullableNodes(it, commonSuperType.typeConstructor()) }
  return if (actuallyNotNull) commonSuperType else null*/
  return commonSuperType
}

private fun uniquify(
  types: List<TypeRef>,
  ctx: TypeCheckerContext
): List<TypeRef> {
  val uniqueTypes = mutableListOf<TypeRef>()
  for (type in types) {
    val isNewUniqueType = uniqueTypes.none { it.isEqualTo(type, ctx) }
    if (isNewUniqueType) uniqueTypes += type
  }
  return uniqueTypes
}

private fun filterSupertypes(
  list: List<TypeRef>,
  ctx: TypeCheckerContext
): List<TypeRef> {
  val supertypes = list.toMutableList()
  val iterator = supertypes.iterator()
  while (iterator.hasNext()) {
    val potentialSubType = iterator.next()
    val isSubType = supertypes.any { supertype ->
      supertype !== potentialSubType &&
          potentialSubType.isSubTypeOf(supertype, ctx)
    }

    if (isSubType) iterator.remove()
  }

  return supertypes
}

private fun commonSuperTypeForNotNullTypes(
  types: List<TypeRef>,
  depth: Int,
  ctx: TypeCheckerContext
): TypeRef {
  if (types.size == 1) return types.single()

  val uniqueTypes = uniquify(types, ctx)
  if (uniqueTypes.size == 1) return uniqueTypes.single()

  val explicitSupertypes = filterSupertypes(uniqueTypes, ctx)
  if (explicitSupertypes.size == 1) return explicitSupertypes.single()

  return findSuperTypeConstructorsAndIntersectResult(explicitSupertypes, depth, ctx)
}

private fun findSuperTypeConstructorsAndIntersectResult(
  types: List<TypeRef>,
  depth: Int,
  ctx: TypeCheckerContext
): TypeRef = intersectTypes(
  allCommonSuperTypeClassifiers(types)
    .map { superTypeWithInjectableClassifier(types, it, depth, ctx) },
  ctx
)

private fun allCommonSuperTypeClassifiers(types: List<TypeRef>): List<ClassifierRef> {
  val result = collectAllSupertypes(types.first())
  // retain all super constructors of the first type that are present in the supertypes of all other types
  for (type in types) {
    if (type === types.first()) continue

    result.retainAll(collectAllSupertypes(type))
  }
  // remove all constructors that have subtype(s) with constructors from the resulting set - they are less precise
  return result.filterNot { target ->
    result.any { other ->
      other != target && other.superTypes.any { it.classifier == target }
    }
  }
}

private fun collectAllSupertypes(type: TypeRef) = mutableSetOf<ClassifierRef>().apply {
  type.anySuperType {
    add(it.classifier)
    false
  }
}

private fun superTypeWithInjectableClassifier(
  types: List<TypeRef>,
  classifier: ClassifierRef,
  depth: Int,
  ctx: TypeCheckerContext
): TypeRef {
  if (classifier.typeParameters.isEmpty()) return classifier.defaultType

  val correspondingSuperTypes = types.map { it.subtypeView(classifier) }

  val arguments = mutableListOf<TypeRef>()
  for (index in classifier.typeParameters.indices) {
    val parameter = classifier.typeParameters[index]
    var thereIsStar = false
    val typeArguments = correspondingSuperTypes.mapNotNull {
      val typeArgument = it?.arguments?.get(index) ?: return@mapNotNull null
      when {
        typeArgument.isStarProjection -> {
          thereIsStar = true
          null
        }
        else -> typeArgument
      }
    }

    fun collapseRecursiveArgumentIfPossible(argument: TypeRef): TypeRef {
      if (argument.isStarProjection) return argument
      val argumentClassifier = argument.classifier
      return if (argument.variance == TypeVariance.OUT &&
        argumentClassifier == classifier
      ) {
        STAR_PROJECTION_TYPE
      } else {
        argument
      }
    }

    val argument = if (thereIsStar || typeArguments.isEmpty()) {
      STAR_PROJECTION_TYPE
    } else {
      collapseRecursiveArgumentIfPossible(
        calculateArgument(
          parameter,
          typeArguments,
          depth,
          ctx
        )
      )
    }

    arguments.add(argument)
  }
  return classifier.defaultType.withArguments(arguments)
}

private fun calculateArgument(
  parameter: ClassifierRef,
  arguments: List<TypeRef>,
  depth: Int,
  ctx: TypeCheckerContext
): TypeRef {
  if (depth > 0) return STAR_PROJECTION_TYPE

  if (parameter.variance == TypeVariance.INV && arguments.all { it.variance == TypeVariance.INV }) {
    val first = arguments.first()
    if (arguments.all { it == first }) return first
  }

  val asOut = if (parameter.variance != TypeVariance.INV) {
    parameter.variance == TypeVariance.OUT
  } else {
    val thereIsOut = arguments.any { it.variance == TypeVariance.OUT }
    val thereIsIn = arguments.any { it.variance == TypeVariance.IN }
    if (thereIsOut) {
      if (thereIsIn) {
        return STAR_PROJECTION_TYPE
      } else {
        true
      }
    } else {
      !thereIsIn
    }
  }

  if (asOut) {
    val parameterIsNotInv = parameter.variance != TypeVariance.INV

    if (parameterIsNotInv) {
      return commonSuperType(arguments, depth + 1, ctx)
    }

    val equalToEachOtherType = arguments.firstOrNull { potentialSuperType ->
      arguments.all { it.isEqualTo(potentialSuperType, ctx) }
    }

    return if (equalToEachOtherType == null) {
      commonSuperType(arguments, depth + 1, ctx).withVariance(TypeVariance.OUT)
    } else {
      val thereIsNotInv = arguments.any { it.variance != TypeVariance.INV }
      equalToEachOtherType.withVariance(if (thereIsNotInv) TypeVariance.OUT else TypeVariance.INV)
    }
  } else {
    val type = intersectTypes(arguments, ctx)
    return if (parameter.variance != TypeVariance.INV) type else type.withVariance(TypeVariance.IN)
  }
}

internal fun intersectTypes(types: List<TypeRef>, ctx: TypeCheckerContext): TypeRef {
  if (types.size == 1) return types.single()

  val resultNullability = types.fold(ResultNullability.START, ResultNullability::combine)

  val correctNullability = types.mapTo(mutableSetOf()) {
    if (resultNullability == ResultNullability.NOT_NULL) {
      it.withNullability(false)
    } else it
  }

  return intersectTypesWithoutIntersectionType(correctNullability, ctx)
}

private fun intersectTypesWithoutIntersectionType(
  types: Set<TypeRef>,
  ctx: TypeCheckerContext
): TypeRef {
  if (types.size == 1) return types.single()

  val filteredEqualTypes = filterTypes(types) { lower, upper ->
    isStrictSupertype(lower, upper, ctx.ctx)
  }

  val filteredSuperAndEqualTypes = filterTypes(filteredEqualTypes) { lower, upper ->
    lower.isEqualTo(upper, ctx.ctx)
  }

  if (filteredSuperAndEqualTypes.size < 2) return filteredSuperAndEqualTypes.single()

  val allNotNull = filteredSuperAndEqualTypes.none { it.isNullableType }

  return if (allNotNull) ctx.ctx.anyType
  else ctx.ctx.nullableAnyType
}

private fun filterTypes(
  types: Collection<TypeRef>,
  predicate: (TypeRef, TypeRef) -> Boolean
): Collection<TypeRef> {
  val filteredTypes = types.toMutableList()
  val iterator = filteredTypes.iterator()
  while (iterator.hasNext()) {
    val upper = iterator.next()
    val shouldFilter = filteredTypes.any { lower -> lower !== upper && predicate(lower, upper) }
    if (shouldFilter) iterator.remove()
  }
  return filteredTypes
}

private fun isStrictSupertype(
  subtype: TypeRef,
  supertype: TypeRef,
  ctx: TypeCheckerContext
): Boolean = subtype.isSubTypeOf(supertype, ctx.ctx) &&
    !supertype.isSubTypeOf(subtype, ctx.ctx)

private enum class ResultNullability {
  START {
    override fun combine(nextType: TypeRef) = nextType.resultNullability
  },
  ACCEPT_NULL {
    override fun combine(nextType: TypeRef) = nextType.resultNullability
  },
  UNKNOWN {
    override fun combine(nextType: TypeRef) =
      nextType.resultNullability.let {
        if (it == ACCEPT_NULL) this else it
      }
  },
  NOT_NULL {
    override fun combine(nextType: TypeRef) = this
  };

  abstract fun combine(nextType: TypeRef): ResultNullability

  protected val TypeRef.resultNullability: ResultNullability
    get() = when {
      isNullable -> ACCEPT_NULL
      !isNullableType -> NOT_NULL
      else -> UNKNOWN
    }
}
