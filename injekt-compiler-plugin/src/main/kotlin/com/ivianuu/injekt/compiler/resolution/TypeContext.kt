/*
 * Copyright 2021 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt_shaded.Inject
import org.jetbrains.kotlin.types.model.TypeVariance

interface TypeCheckerContext {
  val ctx: InjektContext
  fun isDenotable(type: TypeRef): Boolean
  fun addSubTypeConstraint(subType: TypeRef, superType: TypeRef): Boolean? = null
}

fun TypeRef.isEqualTo(other: TypeRef, @Inject ctx: TypeCheckerContext): Boolean {
  if (this == other) return true

  if (ctx.isDenotable(this) && ctx.isDenotable(other)) {
    if (classifier != other.classifier) return false

    if (isMarkedNullable != other.isMarkedNullable) return false
    if (isMarkedComposable != other.isMarkedComposable) return false

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

fun TypeRef.isSubTypeOf(
  superType: TypeRef,
  @Inject ctx: TypeCheckerContext
): Boolean {
  if (this == superType) return true

  ctx.addSubTypeConstraint(this, superType)
    ?.let { return it }

  if (classifier.fqName == ctx.ctx.injektFqNames.nothing &&
    (!isMarkedNullable || superType.isNullableType)
  ) return true

  if (superType.classifier.fqName == ctx.ctx.injektFqNames.any &&
    (superType.isMarkedNullable || !isNullableType)
  ) return true

  subtypeView(superType.classifier)
    ?.let { return it.isSubTypeOfSameClassifier(superType) }

  return false
}

private fun TypeRef.isSubTypeOfSameClassifier(
  superType: TypeRef,
  @Inject ctx: TypeCheckerContext
): Boolean {
  if (!superType.isMarkedNullable && isMarkedNullable) return false
  if (isMarkedComposable != superType.isMarkedComposable) return false

  for (i in arguments.indices) {
    val argument = arguments[i]
    val parameter = superType.arguments[i]
    if (argument.isStarProjection || parameter.isStarProjection) continue
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

sealed class TypeContextError {
  data class ConstraintError(
    val subType: TypeRef,
    val superType: TypeRef,
    val kind: ConstraintKind
  ) : TypeContextError()

  object NotEnoughInformation : TypeContextError()
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

  fun copy() = VariableWithConstraints(typeVariable).apply {
    this.constraints += this@VariableWithConstraints.constraints
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

sealed class ConstraintPosition {
  object FixVariable : ConstraintPosition()
  object DeclaredUpperBound : ConstraintPosition()
  object Unknown : ConstraintPosition()
}

fun buildContextForSpreadingInjectable(
  constraintType: TypeRef,
  candidateType: TypeRef,
  staticTypeParameters: List<ClassifierRef>,
  @Inject ctx: TypeCheckerContext
): Pair<TypeContext, Map<ClassifierRef, TypeRef>> {
  val candidateTypeParameters = mutableListOf<ClassifierRef>()
  candidateType.allTypes.forEach {
    if (it.classifier.isTypeParameter)
      candidateTypeParameters += it.classifier
  }
  val typeCtx = candidateType.buildContext(
    constraintType,
    candidateTypeParameters + staticTypeParameters,
    true
  )

  val map = if (typeCtx.isOk) {
    val swapMap = mutableMapOf<ClassifierRef, TypeRef>()
    val rawMap = typeCtx.fixedTypeVariables
    rawMap.forEach { (key, value) ->
      if (value.classifier in candidateTypeParameters) {
        swapMap[value.classifier] = key.defaultType
      }
    }
    rawMap
      .filterKeys { it !in candidateTypeParameters }
      .mapValues { it.value.substitute(swapMap) }
  } else emptyMap()
  return typeCtx to map
}

fun TypeRef.buildContext(
  superType: TypeRef,
  staticTypeParameters: List<ClassifierRef>,
  collectSuperTypeVariables: Boolean = false,
  @Inject ctx: TypeCheckerContext
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

class TypeContext(override val ctx: InjektContext) : TypeCheckerContext {
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
      if (constraintType.isMarkedNullable && constraint.kind == ConstraintKind.LOWER)
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
      .filter { it.kind == ConstraintKind.EQUAL }
      .map { it.type }
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
    if (resultType.classifier.fqName == ctx.injektFqNames.nothing) return false
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
    ConstraintKind.EQUAL -> constraintType.isEqualTo(resultType)
    ConstraintKind.LOWER -> constraintType.isSubTypeOf(resultType)
    ConstraintKind.UPPER -> resultType.isSubTypeOf(constraintType)
  }

  private fun findSuperType(variableWithConstraints: VariableWithConstraints): TypeRef? {
    val upperConstraints = variableWithConstraints
      .constraints
      .filter { it.kind == ConstraintKind.UPPER }
    return if (upperConstraints.isNotEmpty()) {
      intersectTypes(upperConstraints.map { it.type })
    } else null
  }

  private fun findSubType(variableWithConstraints: VariableWithConstraints): TypeRef? {
    val lowerConstraintTypes = variableWithConstraints.constraints
      .filter { it.kind == ConstraintKind.LOWER }
      .map { it.type }
    return if (lowerConstraintTypes.isNotEmpty()) {
      commonSuperType(lowerConstraintTypes, ctx = ctx)
    } else null
  }

  private fun List<TypeRef>.singleBestRepresentative(): TypeRef? {
    if (size == 1) return first()
    return firstOrNull { candidate ->
      all { other -> candidate.isEqualTo(other) }
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

    if (typeVariable.isMarkedNullable) {
      result = superType.classifier in typeVariables ||
          ctx.nullableNothingType.isSubTypeOf(superType)
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
      val constraintsWhichConstraintMyVariable = typeVariableWithConstraint.constraints.filter {
        it.type.anyType { it.classifier == typeVariable }
      }
      constraintsWhichConstraintMyVariable.forEach {
        if (!isOk) return@forEach
        generateNewConstraint(typeVariableWithConstraint.typeVariable, it, typeVariable, constraint)
      }
    }
  }

  private fun runIsSubTypeOf(subType: TypeRef, superType: TypeRef) {
    if (!subType.isSubTypeOf(superType)) {
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
      ConstraintKind.UPPER -> otherConstraint.type.withVariance(TypeVariance.OUT)
      ConstraintKind.LOWER -> otherConstraint.type.withVariance(TypeVariance.IN)
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
  @Inject ctx: TypeCheckerContext
): TypeRef {
  types.singleOrNull()?.let { return it }
  val notAllNotNull = types.any { it.isNullableType }
  val notNullTypes = if (notAllNotNull) types.map { it.withNullability(false) } else types

  val commonSuperType = commonSuperTypeForNotNullTypes(notNullTypes, depth)
  return if (notAllNotNull)
    refineNullabilityForUndefinedNullability(types, commonSuperType)
      ?: commonSuperType.withNullability(true)
  else
    commonSuperType
}

private fun refineNullabilityForUndefinedNullability(
  types: List<TypeRef>,
  commonSuperType: TypeRef,
  @Inject ctx: TypeCheckerContext
): TypeRef? {
  if (!commonSuperType.classifier.isTypeParameter) return null

  /*val actuallyNotNull =
      types.all { hasPathByNotMarkedNullableNodes(it, commonSuperType.typeConstructor()) }
  return if (actuallyNotNull) commonSuperType else null*/
  return commonSuperType
}

private fun uniquify(
  types: List<TypeRef>,
  @Inject ctx: TypeCheckerContext
): List<TypeRef> {
  val uniqueTypes = mutableListOf<TypeRef>()
  for (type in types) {
    val isNewUniqueType = uniqueTypes.none { it.isEqualTo(type) }
    if (isNewUniqueType) uniqueTypes += type
  }
  return uniqueTypes
}

private fun filterSupertypes(
  list: List<TypeRef>,
  @Inject ctx: TypeCheckerContext
): List<TypeRef> {
  val supertypes = list.toMutableList()
  val iterator = supertypes.iterator()
  while (iterator.hasNext()) {
    val potentialSubType = iterator.next()
    val isSubType = supertypes.any { supertype ->
      supertype !== potentialSubType &&
          potentialSubType.isSubTypeOf(supertype)
    }

    if (isSubType) iterator.remove()
  }

  return supertypes
}

private fun commonSuperTypeForNotNullTypes(
  types: List<TypeRef>,
  depth: Int,
  @Inject ctx: TypeCheckerContext
): TypeRef {
  if (types.size == 1) return types.single()

  val uniqueTypes = uniquify(types)
  if (uniqueTypes.size == 1) return uniqueTypes.single()

  val explicitSupertypes = filterSupertypes(uniqueTypes)
  if (explicitSupertypes.size == 1) return explicitSupertypes.single()

  return findSuperTypeConstructorsAndIntersectResult(explicitSupertypes, depth)
}

private fun findSuperTypeConstructorsAndIntersectResult(
  types: List<TypeRef>,
  depth: Int,
  @Inject ctx: TypeCheckerContext
): TypeRef = intersectTypes(
  allCommonSuperTypeClassifiers(types)
    .map { superTypeWithInjectableClassifier(types, it, depth) }
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
  @Inject ctx: TypeCheckerContext
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
          depth
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
  @Inject ctx: TypeCheckerContext
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
      return commonSuperType(arguments, depth + 1)
    }

    val equalToEachOtherType = arguments.firstOrNull { potentialSuperType ->
      arguments.all { it.isEqualTo(potentialSuperType) }
    }

    return if (equalToEachOtherType == null) {
      commonSuperType(arguments, depth + 1).withVariance(TypeVariance.OUT)
    } else {
      val thereIsNotInv = arguments.any { it.variance != TypeVariance.INV }
      equalToEachOtherType.withVariance(if (thereIsNotInv) TypeVariance.OUT else TypeVariance.INV)
    }
  } else {
    val type = intersectTypes(arguments)
    return if (parameter.variance != TypeVariance.INV) type else type.withVariance(TypeVariance.IN)
  }
}

internal fun intersectTypes(types: List<TypeRef>, @Inject ctx: TypeCheckerContext): TypeRef {
  if (types.size == 1) return types.single()

  val resultNullability = types.fold(ResultNullability.START, ResultNullability::combine)

  val correctNullability = types.mapTo(mutableSetOf()) {
    if (resultNullability == ResultNullability.NOT_NULL) {
      it.withNullability(false)
    } else it
  }

  return intersectTypesWithoutIntersectionType(correctNullability)
}

private fun intersectTypesWithoutIntersectionType(
  types: Set<TypeRef>,
  @Inject ctx: TypeCheckerContext
): TypeRef {
  if (types.size == 1) return types.single()

  val filteredEqualTypes = filterTypes(types) { lower, upper ->
    isStrictSupertype(lower, upper, ctx)
  }

  val filteredSuperAndEqualTypes = filterTypes(filteredEqualTypes) { lower, upper ->
    lower.isEqualTo(upper, ctx)
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
  @Inject ctx: TypeCheckerContext
): Boolean = subtype.isSubTypeOf(supertype, ctx) &&
    !supertype.isSubTypeOf(subtype, ctx)

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
      isMarkedNullable -> ACCEPT_NULL
      !isNullableType -> NOT_NULL
      else -> UNKNOWN
    }
}
