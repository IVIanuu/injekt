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
import org.jetbrains.kotlin.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.*

interface TypeCheckerContext {
    val injektContext: InjektContext
    fun isDenotable(type: TypeRef): Boolean
    fun addSubTypeConstraint(subType: TypeRef, superType: TypeRef): Boolean? = null
}

fun TypeRef.isEqualTo(context: TypeCheckerContext, other: TypeRef): Boolean {
    if (this == other) return true

    if (context.isDenotable(this) && context.isDenotable(other)) {
        if (classifier != other.classifier) return false

        if (isMarkedNullable != other.isMarkedNullable) return false
        if (isMarkedComposable != other.isMarkedComposable) return false
        if (!qualifiers.areMatchingQualifiers(context, other.qualifiers)) return false

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

    return isSubTypeOf(context, other) && other.isSubTypeOf(context, this)
}

fun TypeRef.isSubTypeOf(
    context: TypeCheckerContext,
    superType: TypeRef
): Boolean {
    if (this == superType) return true

    context.addSubTypeConstraint(this, superType)
        ?.let { return it }

    val thisQualifiers = (subtypeView(context.injektContext.anyType.classifier) ?: this).qualifiers
    val superQualifiers = (superType.subtypeView(context.injektContext.anyType.classifier) ?: superType).qualifiers

    if (classifier.fqName == InjektFqNames.Nothing &&
        (!isMarkedNullable || superType.isNullableType) &&
        (qualifiers.isEmpty() || thisQualifiers.areMatchingQualifiers(context, superQualifiers)))
            return true

    if (superType.classifier.fqName == InjektFqNames.Any &&
        (superType.isMarkedNullable || !isNullableType) &&
        (superType.qualifiers.isEmpty() || thisQualifiers.areMatchingQualifiers(context, superQualifiers)))
            return true

    if (isNullableType && !superType.isNullableType) return false

    if (!thisQualifiers.areMatchingQualifiers(context, superQualifiers)) return false

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
            TypeVariance.IN -> parameter.isSubTypeOf(context, argument)
            TypeVariance.OUT -> argument.isSubTypeOf(context, parameter)
            TypeVariance.INV -> argument.isEqualTo(context, parameter)
        }

        if (!argumentOk) return false
    }

    return true
}

private fun List<TypeRef>.areMatchingQualifiers(
    context: TypeCheckerContext,
    other: List<TypeRef>
): Boolean {
    if (size != other.size) return false
    for (otherQualifier in other) {
        val thisQualifier = firstOrNull { it.classifier == otherQualifier.classifier }
        if (thisQualifier == null ||
            !thisQualifier.isSubTypeOf(context, otherQualifier))
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

fun ConstraintKind.opposite() = when (this) {
    ConstraintKind.UPPER -> ConstraintKind.LOWER
    ConstraintKind.LOWER -> ConstraintKind.UPPER
    ConstraintKind.EQUAL -> ConstraintKind.EQUAL
}

fun TypeRef.buildContext(
    injektContext: InjektContext,
    staticTypeParameters: List<ClassifierRef>,
    superType: TypeRef
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
    context.addInitialSubTypeConstraint(this, superType)
    context.fixTypeVariables()
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
            if (it != injektContext.nullableAnyType)
                addInitialSubTypeConstraint(typeParameter.defaultType, it)
        }
    }

    fun addInitialSubTypeConstraint(subType: TypeRef, superType: TypeRef) {
        runIsSubTypeOf(subType, superType)
        processConstraints()
    }

    fun addInitialEqualityConstraint(a: TypeRef, b: TypeRef) {
        val (typeVariable, equalType) = when {
            a.classifier.isTypeParameter -> a to b
            b.classifier.isTypeParameter -> b to a
            else -> return
        }

        addPossibleNewConstraint(Constraint(
            typeVariable.classifier, equalType, ConstraintKind.EQUAL,
            ConstraintPosition.FixVariable, emptySet()))
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
            constraintType == injektContext.nullableAnyType
        ) return true

        return false
    }

    fun fixTypeVariables() {
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
            .filter { it.kind == ConstraintKind.EQUAL }
            .map { it.type }
            .singleBestRepresentative()
            ?.let { return it }

        val subType = findSubType(variableWithConstraints)
        val superType = findSuperType(variableWithConstraints)
        resultType(subType, superType, variableWithConstraints)
            ?.let { return it }

        return injektContext.nullableAnyType
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
        if (resultType.classifier.fqName == InjektFqNames.Nothing) return false
        val filteredConstraints = variableWithConstraints.constraints//.filter { isProperTypeForFixation(it.type) }
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
        ConstraintKind.EQUAL -> constraintType.isEqualTo(this, resultType)
        ConstraintKind.LOWER -> constraintType.isSubTypeOf(this, resultType)
        ConstraintKind.UPPER -> resultType.isSubTypeOf(this, constraintType)
    }

    private fun findSuperType(variableWithConstraints: VariableWithConstraints): TypeRef? {
        val upperConstraints = variableWithConstraints
            .constraints
            .filter { it.kind == ConstraintKind.UPPER }
        return if (upperConstraints.isNotEmpty()) {
            intersectTypes(this, upperConstraints.map { it.type })
        } else null
    }

    private fun findSubType(variableWithConstraints: VariableWithConstraints): TypeRef? {
        val lowerConstraintTypes = variableWithConstraints.constraints
            .filter { it.kind == ConstraintKind.LOWER }
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
        constraints.forEach { constraint ->
            constraint.type.visitRecursive {
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
        addPossibleNewConstraint(Constraint(
            typeVariable.classifier, superType, ConstraintKind.UPPER,
            ConstraintPosition.Unknown, emptySet()))

        var result = true

        if (typeVariable.isMarkedNullable) {
            result = superType.classifier in typeVariables ||
                    injektContext.nullableNothingType.isSubTypeOf(this, superType)
        }

        if (typeVariable.qualifiers.isNotEmpty()) {
            result = result && (superType.isSubTypeOf(
                        this,
                        injektContext.nullableAnyType.copy(qualifiers = typeVariable.qualifiers)) || superType.classifier in typeVariables)
        }

        return result
    }

    private fun addLowerConstraint(typeVariable: TypeRef, subType: TypeRef): Boolean {
        val lowerConstraint = if (typeVariable.qualifiers.isNotEmpty() &&
            subType.qualifiers.isNotEmpty()) {
            // trigger comparison of qualifiers
            injektContext.nullableAnyType.copy(qualifiers = subType.qualifiers)
                .isSubTypeOf(this,
                    injektContext.nullableAnyType.copy(qualifiers = typeVariable.qualifiers))

            subType.copy(
                qualifiers = subType.qualifiers
                    .filterNot { subQualifier ->
                        typeVariable.qualifiers.any {
                            subQualifier.classifier == it.classifier
                        }
                    }
            )
        } else subType

        addPossibleNewConstraint(Constraint(typeVariable.classifier,
            lowerConstraint, ConstraintKind.LOWER, ConstraintPosition.Unknown, emptySet()))

        return true
    }

    private fun directWithVariable(typeVariable: ClassifierRef, constraint: Constraint) {
        if (constraint.kind != ConstraintKind.LOWER) {
            typeVariables[typeVariable]!!.constraints.toList().forEach {
                if (it.kind != ConstraintKind.UPPER) {
                    runIsSubTypeOf(it.type, constraint.type)
                }
            }
        }

        if (constraint.kind != ConstraintKind.UPPER) {
            typeVariables[typeVariable]!!.constraints.toList().forEach {
                if (it.kind != ConstraintKind.LOWER) {
                    runIsSubTypeOf(constraint.type, it.type)
                }
            }
        }
    }

    private fun insideOtherConstraint(typeVariable: ClassifierRef, constraint: Constraint) {
        for (typeVariableWithConstraint in typeVariables.values) {
            val constraintsWhichConstraintMyVariable = typeVariableWithConstraint.constraints.filter {
                it.type.anyType(false) { it.classifier == typeVariable }
            }
            constraintsWhichConstraintMyVariable.forEach {
                generateNewConstraint(typeVariableWithConstraint.typeVariable, it, typeVariable, constraint)
            }
        }
    }

    private fun runIsSubTypeOf(subType: TypeRef, superType: TypeRef) {
        if (!subType.isSubTypeOf(this, superType)) {
            addError(TypeContextError.ConstraintError(subType, superType, ConstraintKind.UPPER))
        }
    }

    private fun generateNewConstraint(
        targetVariable: ClassifierRef,
        baseConstraint: Constraint,
        otherVariable: ClassifierRef,
        otherConstraint: Constraint
    ) {
        val substitutedType = baseConstraint.type.substitute(mapOf(otherVariable to otherConstraint.type))
        if (baseConstraint.kind != ConstraintKind.LOWER) {
            addNewConstraint(targetVariable, baseConstraint,
                otherVariable, otherConstraint, substitutedType, ConstraintKind.UPPER)
        }
        if (baseConstraint.kind != ConstraintKind.UPPER) {
            addNewConstraint(targetVariable, baseConstraint,
                otherVariable, otherConstraint, substitutedType, ConstraintKind.LOWER)
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
        if (targetVariable in getNestedTypeVariables(newConstraint))
            return

        val derivedFrom = baseConstraint.derivedFrom.toMutableSet().also {
            it.addAll(otherConstraint.derivedFrom)
        }
        if (otherVariable in derivedFrom)
            return

        derivedFrom.add(otherVariable)

        addPossibleNewConstraint(Constraint(targetVariable, newConstraint,
            kind, ConstraintPosition.Unknown, derivedFrom))
    }

    private fun addError(error: TypeContextError) {
        errors += error
    }

    private fun getNestedTypeVariables(type: TypeRef): List<ClassifierRef> {
        val nestedVariables = mutableListOf<ClassifierRef>()
        type.visitRecursive {
            if (it.classifier in typeVariables)
                nestedVariables += it.classifier
        }
        return nestedVariables
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
        val potentialSubType = iterator.next()
        val isSubType = supertypes.any { supertype ->
            supertype !== potentialSubType &&
                    potentialSubType.isSubTypeOf(context, supertype)
        }

        if (isSubType) iterator.remove()
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
): Boolean = subtype.isSubTypeOf(context.injektContext, supertype) &&
        !supertype.isSubTypeOf(context.injektContext, subtype)

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
