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

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.resolve.calls.inference.components.*
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.*
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.*

interface TypeCheckerContext {
    val injektContext: InjektContext
    fun isDenotable(type: TypeRef): Boolean
    fun addSubTypeConstraint(
        subType: TypeRef,
        superType: TypeRef,
        exactQualifiers: Boolean
    ): Boolean? = null
}

fun TypeRef.isEqualTo(context: TypeCheckerContext, other: TypeRef): Boolean {
    if (this == other) return true

    if (context.isDenotable(this) && context.isDenotable(other)) {
        if (classifier != other.classifier) return false

        if (isMarkedNullable != other.isMarkedNullable) return false
        if (isMarkedComposable != other.isMarkedComposable) return false
        if (!qualifiers.areMatchingQualifiers(context, other.qualifiers, true)) return false

        for (i in arguments.indices) {
            val thisParameter = arguments[i]
            val otherParameter = other.arguments[i]
            if (thisParameter.isStarProjection &&
                otherParameter.isStarProjection) continue
            if (effectiveVariance(thisParameter.variance, otherParameter.variance, TypeVariance.INV) !=
                effectiveVariance(otherParameter.variance, thisParameter.variance, TypeVariance.INV))
                return false
            if (!thisParameter.isEqualTo(context, otherParameter))
                return false
        }

        return true
    }

    return isSubTypeOf(context, other, true) &&
            other.isSubTypeOf(context, this, true)
}

fun TypeRef.isSubTypeOf(
    context: TypeCheckerContext,
    superType: TypeRef,
    exactQualifiers: Boolean
): Boolean {
    if (this == superType) return true

    context.addSubTypeConstraint(this, superType, exactQualifiers)
        ?.let { return it }

    if (isNullableType && !superType.isNullableType) return false

    val thisQualifiers = (subtypeView(context.injektContext.anyType.classifier) ?: this).qualifiers
    val superQualifiers = (superType.subtypeView(context.injektContext.anyType.classifier) ?: superType).qualifiers

    if (!thisQualifiers.areMatchingQualifiers(context, superQualifiers, exactQualifiers) &&
        classifier.fqName != InjektFqNames.Nothing)
            return false

    if (classifier.fqName == InjektFqNames.Nothing)
        return true

    if (superType.classifier.fqName == InjektFqNames.Any)
        return true

    subtypeView(superType.classifier)
        ?.let { return it.isSubTypeOfSameClassifier(context, superType) }

    return false
}

private fun TypeRef.isSubTypeOfSameClassifier(
    context: TypeCheckerContext,
    superType: TypeRef
): Boolean {
    if (isMarkedComposable != superType.isMarkedComposable) return false

    for (i in arguments.indices) {
        val argument = arguments[i]
        val parameter = superType.arguments[i]
        if (argument.isStarProjection || parameter.isStarProjection) continue
        val originalParameter = superType.classifier.defaultType.arguments[i]
        val variance = effectiveVariance(parameter.variance, argument.variance, originalParameter.variance)
        val argumentOk = when (variance) {
            TypeVariance.IN -> parameter.isSubTypeOf(context, argument, false)
            TypeVariance.OUT -> argument.isSubTypeOf(context, parameter, false)
            TypeVariance.INV -> argument.isEqualTo(context, parameter)
        }

        if (!argumentOk) return false
    }

    return true
}

private fun List<TypeRef>.areMatchingQualifiers(
    context: TypeCheckerContext,
    other: List<TypeRef>,
    exact: Boolean
): Boolean {
    if (exact && size != other.size) return false
    for (otherQualifier in other) {
        val thisQualifier = firstOrNull { it.classifier == otherQualifier.classifier }
        if (thisQualifier == null ||
            !thisQualifier.isSubTypeOf(context, otherQualifier, false))
            return false
    }

    return true
}

fun TypeRef.subtypeView(classifier: ClassifierRef): TypeRef? {
    if (this.classifier == classifier) return this
    return superTypes
        .firstNotNullResult { it.subtypeView(classifier) }
        ?.let { return it }
}

sealed class TypeContextError {
    data class ConstraintError(
        val subType: TypeRef,
        val superType: TypeRef,
        val kind: Constraint.Kind
    ) : TypeContextError()
    object NotEnoughInformation : TypeContextError()
}

class VariableWithConstraints(val typeVariable: ClassifierRef) {
    val constraints = mutableListOf<Constraint>()

    fun addConstraint(constraint: Constraint): Boolean {
        if (constraint in constraints) return false

        for (previousConstraint in constraints) {
            if (previousConstraint.type == constraint.type) {
                if (newConstraintIsUseless(previousConstraint, constraint)) {
                    return false
                }

                val isMatchingForSimplification = when (previousConstraint.kind) {
                    Constraint.Kind.LOWER -> constraint.kind == Constraint.Kind.UPPER
                    Constraint.Kind.UPPER -> constraint.kind == Constraint.Kind.LOWER
                    Constraint.Kind.EQUAL -> true
                }
                if (isMatchingForSimplification) {
                    val actualConstraint = if (constraint.kind != Constraint.Kind.EQUAL) {
                        Constraint(
                            typeVariable,
                            constraint.type,
                            Constraint.Kind.EQUAL,
                            constraint.position
                        )
                    } else constraint
                    constraints += actualConstraint
                    return true
                }
            }
        }

        constraints += constraint

        return true
    }

    private fun newConstraintIsUseless(old: Constraint, new: Constraint): Boolean =
        when (old.kind) {
            Constraint.Kind.EQUAL -> true
            Constraint.Kind.LOWER -> new.kind == Constraint.Kind.LOWER
            Constraint.Kind.UPPER -> new.kind == Constraint.Kind.UPPER
        }
}

data class Constraint(
    val typeVariable: ClassifierRef,
    val type: TypeRef,
    val kind: Kind,
    val position: ConstraintPosition
) {
    val initTrace: String = try {
        error("")
    } catch (e: Throwable) {
        e.stackTraceToString()
    }
    enum class Kind {
        LOWER, UPPER, EQUAL
    }
}

sealed class ConstraintPosition {
    object FixVariable : ConstraintPosition()
    object DeclaredUpperBound : ConstraintPosition()
    object Unknown : ConstraintPosition()
}

fun Constraint.Kind.opposite() = when (this) {
    Constraint.Kind.UPPER -> Constraint.Kind.LOWER
    Constraint.Kind.LOWER -> Constraint.Kind.UPPER
    Constraint.Kind.EQUAL -> Constraint.Kind.EQUAL
}

fun TypeRef.buildContext(
    injektContext: InjektContext,
    staticTypeParameters: List<ClassifierRef>,
    superType: TypeRef,
    exactQualifiers: Boolean
): TypeContext {
    val context = TypeContext(injektContext)
    staticTypeParameters.forEach { context.addStaticTypeParameter(it) }
    visitRecursive {
        if (it.classifier.isTypeParameter)
            context.addTypeVariable(it.classifier)
    }
    superType.visitRecursive {
        if (it.classifier.isTypeParameter)
            context.addTypeVariable(it.classifier)
    }
    context.addInitialSubTypeConstraint(this, superType, exactQualifiers)
    context.complete()
    return context
}

fun TypeContext.getSubstitutionMap(): Map<ClassifierRef, TypeRef> = fixedTypeVariables

class TypeContext(override val injektContext: InjektContext) : TypeCheckerContext {
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
            addInitialSubTypeConstraint(typeParameter.defaultType, it, false)
        }
    }

    fun addInitialSubTypeConstraint(subType: TypeRef, superType: TypeRef, exactQualifiers: Boolean) {
        runIsSubTypeOf(subType, superType, exactQualifiers)
        processConstraints()
    }

    fun addInitialEqualityConstraint(a: TypeRef, b: TypeRef) {
        val (typeVariable, equalType) = when {
            a.classifier.isTypeParameter -> a to b
            b.classifier.isTypeParameter -> b to a
            else -> return
        }

        addPossibleNewConstraint(Constraint(
            typeVariable.classifier, equalType, Constraint.Kind.EQUAL, ConstraintPosition.FixVariable))
        processConstraints()
    }

    private fun processConstraints() {
        while (possibleNewConstraints != null) {
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
        if (constraint.kind == Constraint.Kind.EQUAL)
            return false

        val constraintType = constraint.type

        if (constraintType.classifier == constraint.typeVariable) {
            if (constraintType.isMarkedNullable && constraint.kind == Constraint.Kind.LOWER)
                return false
            return true
        }
        if (constraint.position is ConstraintPosition.DeclaredUpperBound &&
            constraint.kind == Constraint.Kind.UPPER &&
            constraintType.classifier.fqName == InjektFqNames.Any &&
            constraintType.isMarkedNullable
        )
            return true

        return false
    }

    fun complete() {
        if (!isOk) return
        while (true) {
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
            ?: return false

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

    private fun getFixedType(variableWithConstraints: VariableWithConstraints): TypeRef? {
        variableWithConstraints.constraints
            .filter { it.kind == Constraint.Kind.EQUAL }
            .map { it.type }
            .singleBestRepresentative()
            ?.let { return it }

        val subType = findSubType(variableWithConstraints)
        val superType = findSuperType(variableWithConstraints)
        return resultType(subType, superType, variableWithConstraints)
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

    private fun isSuitableType(resultType: TypeRef, variableWithConstraints: VariableWithConstraints): Boolean {
        val filteredConstraints = variableWithConstraints.constraints//.filter { isProperTypeForFixation(it.type) }
        for (constraint in filteredConstraints) {
            if (!checkConstraint(constraint.type, constraint.kind, resultType)) return false
        }
        /*if (!trivialConstraintTypeInferenceOracle.isSuitableResultedType(resultType)) {
            if (resultType.isNullableType() && checkSingleLowerNullabilityConstraint(filteredConstraints)) return false
        }*/

        return true
    }

    private fun checkConstraint(
        constraintType: TypeRef,
        kind: Constraint.Kind,
        resultType: TypeRef
    ): Boolean = when (kind) {
        Constraint.Kind.EQUAL -> constraintType.isEqualTo(this, resultType)
        Constraint.Kind.LOWER -> constraintType.isSubTypeOf(this, resultType, false)
        Constraint.Kind.UPPER -> resultType.isSubTypeOf(this, constraintType, false)
    }

    private fun findSuperType(variableWithConstraints: VariableWithConstraints): TypeRef? {
        val upperConstraints = variableWithConstraints.constraints.filter {
            it.kind == Constraint.Kind.UPPER
        }
        return if (upperConstraints.isNotEmpty()) {
            intersectTypes(this, upperConstraints.map { it.type })
        } else null
    }

    private fun findSubType(variableWithConstraints: VariableWithConstraints): TypeRef? {
        val lowerConstraintTypes = variableWithConstraints.constraints
            .filter { it.kind == Constraint.Kind.LOWER }
            .map { it.type }
        return if (lowerConstraintTypes.isNotEmpty()) {
            commonSuperType(injektContext, lowerConstraintTypes)
        } else null
    }

    private fun List<TypeRef>.singleBestRepresentative(): TypeRef? {
        if (size == 1) return first()
        return firstOrNull { candidate ->
            all { other ->
                candidate.isEqualTo(this@TypeContext, other)
            }
        }
    }

    private fun VariableWithConstraints.getNestedTypeVariables(): List<TypeRef> {
        val nestedTypeVariables = mutableListOf<TypeRef>()
        constraints.forEach {
            if (it.type.classifier in typeVariables)
                nestedTypeVariables += it.type
        }
        return nestedTypeVariables
    }

    override fun isDenotable(type: TypeRef): Boolean = type.classifier !in typeVariables

    override fun addSubTypeConstraint(
        subType: TypeRef,
        superType: TypeRef,
        exactQualifiers: Boolean
    ): Boolean? {
        var answer: Boolean? = null

        if (superType.classifier in typeVariables)
            answer = addLowerConstraint(superType, subType)

        if (subType.classifier in typeVariables)
            return addUpperConstraint(subType, superType) && (answer ?: true)

        return answer
    }

    private fun addUpperConstraint(typeVariable: TypeRef, superType: TypeRef): Boolean {
        addPossibleNewConstraint(Constraint(
            typeVariable.classifier, superType, Constraint.Kind.UPPER, ConstraintPosition.Unknown))

        var result = true

        if (typeVariable.isMarkedNullable) {
            result = superType.classifier in typeVariables ||
                    injektContext.nullableNothingType.isSubTypeOf(this, superType, false)
        }

        if (typeVariable.qualifiers.isNotEmpty()) {
            result = result && superType.classifier in typeVariables ||
                    superType.isSubTypeOf(
                        this,
                        injektContext.nullableAnyType.copy(qualifiers = typeVariable.qualifiers),
                        false
                    )
        }

        return result
    }

    private fun addLowerConstraint(typeVariable: TypeRef, subType: TypeRef): Boolean {
        var lowerConstraint = subType

        addPossibleNewConstraint(Constraint(typeVariable.classifier,
            lowerConstraint, Constraint.Kind.LOWER, ConstraintPosition.Unknown))

        return true
    }

    private fun directWithVariable(typeVariable: ClassifierRef, constraint: Constraint) {
        if (constraint.kind != Constraint.Kind.LOWER) {
            typeVariables[typeVariable]!!.constraints.toList().forEach {
                if (it.kind != Constraint.Kind.UPPER) {
                    runIsSubTypeOf(it.type, constraint.type, false)
                }
            }
        }

        if (constraint.kind != Constraint.Kind.UPPER) {
            typeVariables[typeVariable]!!.constraints.toList().forEach {
                if (it.kind != Constraint.Kind.LOWER) {
                    runIsSubTypeOf(constraint.type, it.type, false)
                }
            }
        }
    }

    private fun insideOtherConstraint(typeVariable: ClassifierRef, constraint: Constraint) {
        for (typeVariableWithConstraint in typeVariables.values) {
            val constraintsWhichConstraintMyVariable = typeVariableWithConstraint.constraints.filter {
                it.type.anyType { it.classifier == typeVariable }
            }
            constraintsWhichConstraintMyVariable.forEach {
                generateNewConstraint(typeVariableWithConstraint.typeVariable, it, constraint)
            }
        }
    }

    private fun runIsSubTypeOf(subType: TypeRef, superType: TypeRef, exactQualifiers: Boolean) {
        if (!subType.isSubTypeOf(this, superType, exactQualifiers)) {
            addError(TypeContextError.ConstraintError(subType, superType, Constraint.Kind.UPPER))
        }
    }

    private fun generateNewConstraint(
        targetVariable: ClassifierRef,
        baseConstraint: Constraint,
        otherConstraint: Constraint
    ) {
        val isBaseGenericType = baseConstraint.type.arguments.isNotEmpty()
        val type = when (otherConstraint.kind) {
            Constraint.Kind.EQUAL -> otherConstraint.type
                .copy(
                    isMarkedNullable = baseConstraint.type.isMarkedNullable,
                    qualifiers = baseConstraint.type.qualifiers
                )
            Constraint.Kind.UPPER -> {
                if (baseConstraint.kind == Constraint.Kind.LOWER && !isBaseGenericType) {
                    injektContext.nothingType
                }  else {
                    otherConstraint.type
                        .copy(
                            isMarkedNullable = baseConstraint.type.isMarkedNullable,
                            qualifiers = baseConstraint.type.qualifiers
                        )
                }
            }
            Constraint.Kind.LOWER -> {
                if (baseConstraint.kind == Constraint.Kind.UPPER && !isBaseGenericType) {
                    injektContext.nullableAnyType
                } else {
                    otherConstraint.type
                        .copy(
                            isMarkedNullable = baseConstraint.type.isMarkedNullable,
                            qualifiers = baseConstraint.type.qualifiers
                        )
                }
            }
        }

        if (baseConstraint.kind != Constraint.Kind.LOWER) {
            if (!isGeneratedConstraintTrivial(
                    baseConstraint,
                    otherConstraint,
                    type,
                    Constraint.Kind.UPPER
            )) {
                addPossibleNewConstraint(Constraint(targetVariable, type,
                    Constraint.Kind.UPPER, ConstraintPosition.Unknown))
            }
        }
        if (baseConstraint.kind != Constraint.Kind.UPPER) {
            if (!isGeneratedConstraintTrivial(
                    baseConstraint,
                    otherConstraint,
                    type,
                    Constraint.Kind.LOWER
                )) {
                addPossibleNewConstraint(Constraint(targetVariable, type,
                    Constraint.Kind.LOWER, ConstraintPosition.Unknown))
            }
        }
    }

    private fun isGeneratedConstraintTrivial(
        baseConstraint: Constraint,
        otherConstraint: Constraint,
        generatedConstraintType: TypeRef,
        kind: Constraint.Kind
    ): Boolean {
        if (kind == Constraint.Kind.UPPER && generatedConstraintType.classifier ==
            injektContext.nothingType.classifier) return true
        if (kind == Constraint.Kind.LOWER && generatedConstraintType == injektContext.nullableAnyType)
            return true

        // If types from constraints that will be used to generate new constraint already contains `Nothing(?)`,
        // then we can't decide that resulting constraint will be useless
        if (baseConstraint.type.anyType {
                it == injektContext.nothingType ||
                        it == injektContext.nullableNothingType
        }) return false
        if (otherConstraint.type.anyType {
                it == injektContext.nothingType ||
                        it == injektContext.nullableNothingType
        }) return false

        // It's important to preserve constraints with nullable Nothing: `Nothing? <: T` (see implicitNothingConstraintFromReturn.kt test)
        //if (generatedConstraintType.containsOnlyNonNullableNothing()) return true

        return false
    }

    private fun addError(error: TypeContextError) {
        errors += error
    }
}

private fun commonSuperType(
    context: TypeCheckerContext,
    types: List<TypeRef>,
    depth: Int = -(types.maxOfOrNull { it.typeDepth } ?: 0)
): TypeRef {
    types.singleOrNull()?.let { return it }
    val notAllNotNull =
        types.any { it.isNullableType }
    val notNullTypes = if (notAllNotNull) types.map { it.copy(isMarkedNullable = false) } else types

    val commonSuperType = commonSuperTypeForNotNullTypes(context, notNullTypes, depth)
    return if (notAllNotNull)
        refineNullabilityForUndefinedNullability(context, types, commonSuperType)
            ?: commonSuperType.copy(isMarkedNullable = true)
    else
        commonSuperType
}

private fun refineNullabilityForUndefinedNullability(
    context: TypeCheckerContext,
    types: List<TypeRef>,
    commonSuperType: TypeRef
): TypeRef? {
    if (!commonSuperType.classifier.isTypeParameter) return null

    /*val actuallyNotNull =
        types.all { hasPathByNotMarkedNullableNodes(it, commonSuperType.typeConstructor()) }
    return if (actuallyNotNull) commonSuperType else null*/
    return commonSuperType
}

private fun uniquify(
    context: TypeCheckerContext,
    types: List<TypeRef>
): List<TypeRef> {
    val uniqueTypes = mutableListOf<TypeRef>()
    for (type in types) {
        val isNewUniqueType = uniqueTypes.none {
            it.isEqualTo(context, type)
        }
        if (isNewUniqueType) uniqueTypes += type
    }
    return uniqueTypes
}

private fun filterSupertypes(
    context: TypeCheckerContext,
    list: List<TypeRef>
): List<TypeRef> {
    val supertypes = list.toMutableList()
    val iterator = supertypes.iterator()
    while (iterator.hasNext()) {
        val potentialSubtype = iterator.next()
        val isSubtype = supertypes.any { supertype ->
            supertype !== potentialSubtype &&
                    potentialSubtype.isSubTypeOf(context, supertype, false)
        }

        if (isSubtype) iterator.remove()
    }

    return supertypes
}

private fun commonSuperTypeForNotNullTypes(
    context: TypeCheckerContext,
    types: List<TypeRef>,
    depth: Int
): TypeRef {
    if (types.size == 1) return types.single()

    val uniqueTypes = uniquify(context, types)
    if (uniqueTypes.size == 1) return uniqueTypes.single()

    val explicitSupertypes = filterSupertypes(context, uniqueTypes)
    if (explicitSupertypes.size == 1) return explicitSupertypes.single()

    return findSuperTypeConstructorsAndIntersectResult(context, explicitSupertypes, depth)
}

private fun findSuperTypeConstructorsAndIntersectResult(
    context: TypeCheckerContext,
    types: List<TypeRef>,
    depth: Int
): TypeRef = intersectTypes(
    context,
    allCommonSuperTypeClassifiers(types)
        .map { superTypeWithGivenClassifier(context, types, it, depth) }
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

private fun superTypeWithGivenClassifier(
    context: TypeCheckerContext,
    types: List<TypeRef>,
    classifier: ClassifierRef,
    depth: Int
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
                argumentClassifier == classifier) {
                STAR_PROJECTION_TYPE
            } else {
                argument
            }
        }

        val argument = if (thereIsStar || typeArguments.isEmpty()) {
            STAR_PROJECTION_TYPE
        } else {
            collapseRecursiveArgumentIfPossible(calculateArgument(context, parameter, typeArguments, depth))
        }

        arguments.add(argument)
    }
    return classifier.defaultType.typeWith(arguments)
}

private fun calculateArgument(
    context: TypeCheckerContext,
    parameter: ClassifierRef,
    arguments: List<TypeRef>,
    depth: Int
): TypeRef {
    if (depth > 0) return STAR_PROJECTION_TYPE

    if (parameter.variance == TypeVariance.INV && arguments.all { it.variance == TypeVariance.INV }) {
        val first = arguments.first()
        if (arguments.all { it == first }) return first
    }

    val asOut: Boolean
    if (parameter.variance != TypeVariance.INV) {
        asOut = parameter.variance == TypeVariance.OUT
    } else {
        val thereIsOut = arguments.any { it.variance == TypeVariance.OUT }
        val thereIsIn = arguments.any { it.variance == TypeVariance.IN }
        if (thereIsOut) {
            if (thereIsIn) {
                return STAR_PROJECTION_TYPE
            } else {
                asOut = true
            }
        } else {
            asOut = !thereIsIn
        }
    }

    if (asOut) {
        val parameterIsNotInv = parameter.variance != TypeVariance.INV

        if (parameterIsNotInv) {
            return commonSuperType(context, arguments, depth + 1)
        }

        val equalToEachOtherType = arguments.firstOrNull { potentialSuperType ->
            arguments.all { it.isEqualTo(context, potentialSuperType) }
        }

        return if (equalToEachOtherType == null) {
            commonSuperType(context, arguments, depth + 1).copy(variance = TypeVariance.OUT)
        } else {
            val thereIsNotInv = arguments.any { it.variance != TypeVariance.INV }
            equalToEachOtherType.copy(variance = if (thereIsNotInv) TypeVariance.OUT else TypeVariance.INV)
        }
    } else {
        val type = intersectTypes(context, arguments)
        return if (parameter.variance != TypeVariance.INV) type else type.copy(variance = TypeVariance.IN)
    }
}

internal fun intersectTypes(context: TypeCheckerContext, types: List<TypeRef>): TypeRef {
    if (types.size == 1) return types.single()

    val resultNullability = types.fold(ResultNullability.START, ResultNullability::combine)

    val correctNullability = types.mapTo(mutableSetOf()) {
        if (resultNullability == ResultNullability.NOT_NULL) {
            it.copy(isMarkedNullable = false)
        } else it
    }

    return intersectTypesWithoutIntersectionType(context, correctNullability)
}

private fun intersectTypesWithoutIntersectionType(context: TypeCheckerContext, types: Set<TypeRef>): TypeRef {
    if (types.size == 1) return types.single()

    val filteredEqualTypes = filterTypes(types) { lower, upper ->
        isStrictSupertype(context, lower, upper)
    }

    val filteredSuperAndEqualTypes = filterTypes(filteredEqualTypes) { lower, upper ->
        lower.isEqualTo(context.injektContext, upper)
    }

    if (filteredSuperAndEqualTypes.size < 2) return filteredSuperAndEqualTypes.single()

    val allNotNull = filteredSuperAndEqualTypes.none { it.isNullableType }

    return if (allNotNull) context.injektContext.anyType
    else context.injektContext.nullableAnyType
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
    context: TypeCheckerContext,
    subtype: TypeRef,
    supertype: TypeRef
): Boolean = subtype.isSubTypeOf(context.injektContext, supertype, false) &&
        !supertype.isSubTypeOf(context.injektContext, subtype, false)

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
