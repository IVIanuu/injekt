/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.compiler.resolution

import injekt.compiler.*
import org.jetbrains.kotlin.types.model.*

interface TypeCheckerContext {
  val ctx: InjektContext
  fun isDenotable(type: InjektType): Boolean
  fun addSubTypeConstraint(subType: InjektType, superType: InjektType): Boolean? = null
}

context(ctx: TypeCheckerContext)
fun InjektType.isEqualTo(other: InjektType): Boolean {
  if (this == other) return true

  if (ctx.isDenotable(this) && ctx.isDenotable(other)) {
    if (classifier != other.classifier) return false

    if (isMarkedNullable != other.isMarkedNullable) return false

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
      if (!thisParameter.isEqualTo(otherParameter))
        return false
    }

    return true
  }

  return isSubTypeOf(other) && other.isSubTypeOf(this)
}

context(ctx: TypeCheckerContext)
fun InjektType.isSubTypeOf(superType: InjektType): Boolean {
  if (this == superType) return true

  ctx.addSubTypeConstraint(this, superType)
    ?.let { return it }

  if (classifier.classId == InjektFqNames.Nothing &&
    (!isMarkedNullable || superType.isNullableType)
  ) return true

  if (superType.classifier.classId == InjektFqNames.Any &&
    (superType.isMarkedNullable || !isNullableType)) return true

  subtypeView(superType.classifier)
    ?.let { return it.isSubTypeOfSameClassifier(superType) }

  return false
}

context(ctx: TypeCheckerContext)
private fun InjektType.isSubTypeOfSameClassifier(superType: InjektType): Boolean {
  if (!superType.isMarkedNullable && isMarkedNullable) return false

  for (i in arguments.indices) {
    val argument = arguments[i]
    val parameter = superType.arguments[i]
    if (parameter.isStarProjection) continue
    if (argument.isStarProjection) return false
    val originalParameter = superType.classifier.defaultType.arguments[i]
    val argumentOk = when (effectiveVariance(parameter.variance, argument.variance, originalParameter.variance)) {
      TypeVariance.IN -> parameter.isSubTypeOf(argument)
      TypeVariance.OUT -> argument.isSubTypeOf(parameter)
      TypeVariance.INV -> argument.isEqualTo(parameter)
    }

    if (!argumentOk) return false
  }

  return true
}

sealed interface TypeContextError {
  data class ConstraintError(
    val subType: InjektType,
    val superType: InjektType,
    val kind: ConstraintKind
  ) : TypeContextError
}

class VariableWithConstraints(val typeVariable: InjektClassifier) {
  val constraints = mutableListOf<Constraint>()

  fun addConstraint(constraint: Constraint): Boolean {
    for (previousConstraint in constraints.toList())
      if (previousConstraint.type == constraint.type) {
        if (previousConstraint.kind == ConstraintKind.EQUAL ||
          previousConstraint.kind == constraint.kind)
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

    constraints += constraint

    return true
  }
}

data class Constraint(
  val typeVariable: InjektClassifier,
  val type: InjektType,
  val kind: ConstraintKind,
  val position: ConstraintPosition,
  val derivedFrom: Set<InjektClassifier>
)

enum class ConstraintKind { LOWER, UPPER, EQUAL }

sealed interface ConstraintPosition {
  data object FixVariable : ConstraintPosition
  data object DeclaredUpperBound : ConstraintPosition
  data object Unknown : ConstraintPosition
}

context(ctx: TypeCheckerContext)
fun runAddOnInjectableInference(
  constraintType: InjektType,
  candidateType: InjektType,
  staticTypeParameters: List<InjektClassifier>
): TypeContext {
  val candidateTypeParameters = mutableListOf<InjektClassifier>()
  candidateType.allTypes.forEach {
    if (it.classifier.isTypeParameter)
      candidateTypeParameters += it.classifier
  }
  return candidateType.runCandidateInference(
    constraintType,
    candidateTypeParameters + staticTypeParameters,
    true
  )
}

context(ctx: TypeCheckerContext)
fun InjektType.runCandidateInference(
  superType: InjektType,
  staticTypeParameters: List<InjektClassifier>,
  collectSuperTypeVariables: Boolean = false
): TypeContext {
  val typeCtx = TypeContext(ctx.ctx)
  staticTypeParameters.forEach { typeCtx.addStaticTypeParameter(it) }
  allTypes.forEach {
    if (it.classifier.isTypeParameter)
      typeCtx.addTypeVariable(it.classifier)
  }
  if (collectSuperTypeVariables)
    superType.allTypes.forEach {
      if (it.classifier.isTypeParameter)
        typeCtx.addTypeVariable(it.classifier)
    }

  typeCtx.addInitialSubTypeConstraint(this, superType)
  typeCtx.fixTypeVariables()
  return typeCtx
}

class TypeContext(override val ctx: InjektContext) : TypeCheckerContext {
  private val staticTypeParameters = mutableListOf<InjektClassifier>()
  private val typeVariables = mutableMapOf<InjektClassifier, VariableWithConstraints>()
  val fixedTypeVariables = mutableMapOf<InjektClassifier, InjektType>()

  val errors = mutableSetOf<TypeContextError>()

  val isOk: Boolean get() = errors.isEmpty()

  private var possibleNewConstraints: MutableList<Constraint>? = null

  private fun addPossibleNewConstraint(constraint: Constraint) {
    (possibleNewConstraints ?: mutableListOf<Constraint>()
      .also { possibleNewConstraints = it }) += constraint
  }

  fun addStaticTypeParameter(typeParameter: InjektClassifier) {
    staticTypeParameters += typeParameter
  }

  fun addTypeVariable(typeParameter: InjektClassifier) {
    if (typeParameter in staticTypeParameters) return
    val variableWithConstraints = VariableWithConstraints(typeParameter)
    typeVariables[typeParameter] = variableWithConstraints
    typeParameter.superTypes.forEach {
      if (it != ctx.nullableAnyType)
        addInitialSubTypeConstraint(typeParameter.defaultType, it)
    }
  }

  fun addInitialSubTypeConstraint(subType: InjektType, superType: InjektType) {
    runIsSubTypeOf(subType, superType)
    processConstraints()
  }

  private fun addInitialEqualityConstraint(a: InjektType, b: InjektType) {
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
        if (shouldSkipConstraint(constraint)) continue
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

  private fun shouldSkipConstraint(constraint: Constraint): Boolean {
    if (constraint.kind == ConstraintKind.EQUAL)
      return false

    val constraintType = constraint.type

    if (constraintType.classifier == constraint.typeVariable)
      return !(constraintType.isMarkedNullable && constraint.kind == ConstraintKind.LOWER)

    return constraint.position is ConstraintPosition.DeclaredUpperBound &&
        constraint.kind == ConstraintKind.UPPER &&
        constraintType == ctx.nullableAnyType
  }

  fun fixTypeVariables() {
    while (isOk) {
      val unfixedTypeVariables = typeVariables
        .filterKeys { it !in fixedTypeVariables }
        .values
      if (unfixedTypeVariables.isEmpty()) break
      val typeVariableToFix = unfixedTypeVariables
        .firstOrNull { typeVariable ->
          typeVariable.getNestedTypeVariables()
            .all { it.classifier in fixedTypeVariables }
        } ?: unfixedTypeVariables.first()
      fixVariable(typeVariableToFix)
    }
  }

  private fun fixVariable(variableWithConstraints: VariableWithConstraints) {
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
  }

  private fun getFixedType(variableWithConstraints: VariableWithConstraints): InjektType {
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
    firstCandidate: InjektType?,
    secondCandidate: InjektType?,
    variableWithConstraints: VariableWithConstraints
  ): InjektType? {
    if (firstCandidate == null || secondCandidate == null) return firstCandidate ?: secondCandidate

    if (isSuitableType(firstCandidate, variableWithConstraints)) return firstCandidate

    return if (isSuitableType(secondCandidate, variableWithConstraints)) secondCandidate
    else firstCandidate
  }

  private fun isSuitableType(
    resultType: InjektType,
    variableWithConstraints: VariableWithConstraints
  ): Boolean {
    if (resultType.classifier.classId == InjektFqNames.Nothing) return false
    val filteredConstraints = variableWithConstraints.constraints
    for (constraint in filteredConstraints) {
      if (!checkConstraint(constraint.type, constraint.kind, resultType))
        return false
    }

    return true
  }

  private fun checkConstraint(
    constraintType: InjektType,
    kind: ConstraintKind,
    resultType: InjektType
  ): Boolean = when (kind) {
    ConstraintKind.EQUAL -> constraintType.isEqualTo(resultType)
    ConstraintKind.LOWER -> constraintType.isSubTypeOf(resultType)
    ConstraintKind.UPPER -> resultType.isSubTypeOf(constraintType)
  }

  private fun findSuperType(variableWithConstraints: VariableWithConstraints): InjektType? =
    variableWithConstraints
      .constraints
      .transform {
        if (it.kind == ConstraintKind.UPPER)
          add(it.type)
      }
      .takeIf { it.isNotEmpty() }
      ?.intersectTypes()

  private fun findSubType(variableWithConstraints: VariableWithConstraints): InjektType? =
    variableWithConstraints.constraints
      .transform {
        if (it.kind == ConstraintKind.LOWER)
          add(it.type)
      }
      .takeIf { it.isNotEmpty() }
      ?.commonSuperType()

  private fun List<InjektType>.singleBestRepresentative(): InjektType? {
    if (size == 1) return first()
    return firstOrNull { candidate ->
      all { other ->
        context(ctx) { candidate.isEqualTo(other) }
      }
    }
  }

  private fun VariableWithConstraints.getNestedTypeVariables(): List<InjektType> {
    val nestedTypeVariables = mutableListOf<InjektType>()
    constraints.forEach { constraint ->
      constraint.type.allTypes.forEach {
        if (it.classifier in typeVariables)
          nestedTypeVariables += it
      }
    }
    return nestedTypeVariables
  }

  override fun isDenotable(type: InjektType): Boolean = type.classifier !in typeVariables

  override fun addSubTypeConstraint(subType: InjektType, superType: InjektType): Boolean? {
    var answer: Boolean? = null

    if (superType.classifier in typeVariables)
      answer = addLowerConstraint(superType, subType)

    if (subType.classifier in typeVariables)
      return addUpperConstraint(subType, superType) && (answer ?: true)
    else
      subType.source?.let {
        return addUpperConstraint(it.defaultType, superType) && (answer ?: true)
      }

    return answer
  }

  private fun addUpperConstraint(typeVariable: InjektType, superType: InjektType): Boolean {
    addPossibleNewConstraint(
      Constraint(
        typeVariable.classifier, superType, ConstraintKind.UPPER,
        ConstraintPosition.Unknown,
        emptySet()
      )
    )

    var result = true

    if (typeVariable.isMarkedNullable)
      result = superType.classifier in typeVariables ||
          ctx.nullableNothingType.isSubTypeOf(superType)

    return result
  }

  private fun addLowerConstraint(typeVariable: InjektType, subType: InjektType): Boolean {
    addPossibleNewConstraint(
      Constraint(
        typeVariable.classifier,
        subType, ConstraintKind.LOWER, ConstraintPosition.Unknown,
        emptySet()
      )
    )
    return true
  }

  private fun directWithVariable(typeVariable: InjektClassifier, constraint: Constraint) {
    if (constraint.kind != ConstraintKind.LOWER)
      for (otherConstraint in typeVariables[typeVariable]!!.constraints.toList()) {
        if (!isOk) break
        if (otherConstraint.kind != ConstraintKind.UPPER)
          runIsSubTypeOf(otherConstraint.type, constraint.type)
      }

    if (constraint.kind != ConstraintKind.UPPER)
      for (otherConstraint in typeVariables[typeVariable]!!.constraints.toList()) {
        if (!isOk) break
        if (otherConstraint.kind != ConstraintKind.LOWER)
          runIsSubTypeOf(constraint.type, otherConstraint.type)
      }
  }

  private fun insideOtherConstraint(typeVariable: InjektClassifier, constraint: Constraint) {
    for (typeVariableWithConstraint in typeVariables.values) {
      if (!isOk) break
      for (variableConstraint in typeVariableWithConstraint.constraints) {
        if (!isOk) break
        if (variableConstraint.type.anyType { it.classifier == typeVariable })
          generateNewConstraint(typeVariableWithConstraint.typeVariable, variableConstraint, typeVariable, constraint)
      }
    }
  }

  private fun runIsSubTypeOf(subType: InjektType, superType: InjektType) {
    if (!subType.isSubTypeOf(superType))
      errors += TypeContextError.ConstraintError(subType, superType, ConstraintKind.UPPER)
  }

  private fun generateNewConstraint(
    targetVariable: InjektClassifier,
    baseConstraint: Constraint,
    otherVariable: InjektClassifier,
    otherConstraint: Constraint
  ) {
    val newConstraint = when (otherConstraint.kind) {
      ConstraintKind.EQUAL -> otherConstraint.type
      ConstraintKind.UPPER -> otherConstraint.type.copy(variance = TypeVariance.OUT, source = otherVariable)
      ConstraintKind.LOWER -> otherConstraint.type.copy(variance = TypeVariance.IN, source = otherVariable)
    }
    val substitutedType = baseConstraint.type.substitute(mapOf(otherVariable to newConstraint))
    if (baseConstraint.kind != ConstraintKind.LOWER)
      addNewConstraint(
        targetVariable, baseConstraint,
        otherVariable, otherConstraint, substitutedType, ConstraintKind.UPPER
      )
    if (baseConstraint.kind != ConstraintKind.UPPER)
      addNewConstraint(
        targetVariable, baseConstraint,
        otherVariable, otherConstraint, substitutedType, ConstraintKind.LOWER
      )
  }

  private fun addNewConstraint(
    targetVariable: InjektClassifier,
    baseConstraint: Constraint,
    otherVariable: InjektClassifier,
    otherConstraint: Constraint,
    newConstraint: InjektType,
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
}

context(ctx: TypeCheckerContext)
fun List<InjektType>.commonSuperType(
  depth: Int = -(maxOfOrNull { it.typeDepth } ?: 0)
): InjektType {
  singleOrNull()?.let { return it }
  val notAllNotNull = any { it.isNullableType }
  val notNullTypes = if (notAllNotNull) map { it.withNullability(false) } else this

  val commonSuperType = notNullTypes.commonSuperTypeForNotNullTypes(depth)
  return if (notAllNotNull)
    commonSuperType.takeIf { it.classifier.isTypeParameter }
      ?: commonSuperType.withNullability(true)
  else
    commonSuperType
}

context(ctx: TypeCheckerContext)
private fun List<InjektType>.uniquify(): List<InjektType> {
  val uniqueTypes = mutableListOf<InjektType>()
  for (type in this) {
    val isNewUniqueType = uniqueTypes.none { it.isEqualTo(type) }
    if (isNewUniqueType) uniqueTypes += type
  }
  return uniqueTypes
}

context(ctx: TypeCheckerContext)
private fun List<InjektType>.commonSuperTypeForNotNullTypes(depth: Int): InjektType {
  if (size == 1) return single()

  val uniqueTypes = uniquify()
  if (uniqueTypes.size == 1) return uniqueTypes.single()

  val explicitSupertypes = uniqueTypes.filterTypes { lower, upper ->
    lower.isSubTypeOf(upper)
  }
  if (explicitSupertypes.size == 1) return explicitSupertypes.single()

  return findSuperTypeConstructorsAndIntersectResult(explicitSupertypes, depth)
}

context(ctx: TypeCheckerContext)
private fun findSuperTypeConstructorsAndIntersectResult(
  types: List<InjektType>,
  depth: Int
): InjektType = types.allCommonSuperTypeClassifiers()
  .map { superTypeWithInjectableClassifier(types, it, depth) }
  .intersectTypes()

private fun List<InjektType>.allCommonSuperTypeClassifiers(): List<InjektClassifier> {
  val result = first().collectAllSupertypes()
  // retain all super constructors of the first type that are present in the supertypes of all other types
  for (type in this) {
    if (type === first()) continue

    result.retainAll(type.collectAllSupertypes())
  }
  // remove all constructors that have subtype(s) with constructors from the resulting set - they are less precise
  return result.filterNot { target ->
    result.any { other ->
      other != target && other.superTypes.any { it.classifier == target }
    }
  }
}

private fun InjektType.collectAllSupertypes() = mutableSetOf<InjektClassifier>().apply {
  anySuperType {
    add(it.classifier)
    false
  }
}

context(ctx: TypeCheckerContext)
private fun superTypeWithInjectableClassifier(
  types: List<InjektType>,
  classifier: InjektClassifier,
  depth: Int
): InjektType {
  if (classifier.typeParameters.isEmpty()) return classifier.defaultType

  val correspondingSuperTypes = types.map { it.subtypeView(classifier) }

  val arguments = mutableListOf<InjektType>()
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

    fun collapseRecursiveArgumentIfPossible(argument: InjektType): InjektType {
      if (argument.isStarProjection) return argument
      val argumentClassifier = argument.classifier
      return if (argument.variance == TypeVariance.OUT
        && argumentClassifier == classifier) STAR_PROJECTION_TYPE else argument
    }

    val argument = if (thereIsStar || typeArguments.isEmpty()) STAR_PROJECTION_TYPE
    else collapseRecursiveArgumentIfPossible(
      calculateArgument(
        parameter,
        typeArguments,
        depth
      )
    )

    arguments.add(argument)
  }
  return classifier.defaultType.withArguments(arguments)
}

context(ctx: TypeCheckerContext)
private fun calculateArgument(
  parameter: InjektClassifier,
  arguments: List<InjektType>,
  depth: Int
): InjektType {
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
      if (thereIsIn) return STAR_PROJECTION_TYPE
      else true
    } else !thereIsIn
  }

  if (asOut) {
    val parameterIsNotInv = parameter.variance != TypeVariance.INV

    if (parameterIsNotInv)
      return arguments.commonSuperType(depth + 1)

    val equalToEachOtherType = arguments.firstOrNull { potentialSuperType ->
      arguments.all { it.isEqualTo(potentialSuperType) }
    }

    return if (equalToEachOtherType == null)
      arguments.commonSuperType(depth + 1).withVariance(TypeVariance.OUT)
    else {
      val thereIsNotInv = arguments.any { it.variance != TypeVariance.INV }
      equalToEachOtherType.withVariance(if (thereIsNotInv) TypeVariance.OUT else TypeVariance.INV)
    }
  } else {
    val type = arguments.intersectTypes()
    return if (parameter.variance != TypeVariance.INV) type else type.withVariance(TypeVariance.IN)
  }
}

context(ctx: TypeCheckerContext)
private fun List<InjektType>.intersectTypes(): InjektType {
  if (size == 1) return single()

  val resultNullability = fold(false) { nullability, type ->
    nullability || type.isNullableType
  }

  val correctNullability = mapTo(mutableSetOf()) {
    it.withNullability(resultNullability)
  }

  return correctNullability.intersectTypesWithoutIntersectionType()
}

context(ctx: TypeCheckerContext)
private fun Set<InjektType>.intersectTypesWithoutIntersectionType(): InjektType {
  if (size == 1) return single()

  val filteredEqualTypes = filterTypes { lower, upper ->
    isStrictSupertype(lower, upper)
  }

  val filteredSuperAndEqualTypes = filteredEqualTypes.filterTypes { lower, upper ->
    lower.isEqualTo(upper)
  }

  if (filteredSuperAndEqualTypes.size < 2) return filteredSuperAndEqualTypes.single()

  val allNotNull = filteredSuperAndEqualTypes.none { it.isNullableType }

  return if (allNotNull) ctx.ctx.anyType
  else ctx.ctx.nullableAnyType
}

private fun Collection<InjektType>.filterTypes(predicate: (InjektType, InjektType) -> Boolean) =
  filterNot { upper ->
    any { lower -> lower !== upper && predicate(lower, upper) }
  }

context(ctx: TypeCheckerContext)
private fun isStrictSupertype(
  subtype: InjektType,
  supertype: InjektType
): Boolean = context(ctx.ctx) {
  subtype.isSubTypeOf(supertype) && !supertype.isSubTypeOf(subtype)
}
