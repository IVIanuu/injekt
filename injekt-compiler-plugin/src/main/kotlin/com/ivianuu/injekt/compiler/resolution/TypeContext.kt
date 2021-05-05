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
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.*

interface TypeCheckerContext {
    fun addSubTypeConstraint(
        subType: TypeRef,
        superType: TypeRef,
        exactQualifiers: Boolean
    ): Boolean? = null
    companion object : TypeCheckerContext
}

fun TypeRef.isEqualTo(context: TypeCheckerContext, other: TypeRef): Boolean {
    if (this == other) return true

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

fun TypeRef.isSubTypeOf(
    context: TypeCheckerContext,
    superType: TypeRef,
    exactQualifiers: Boolean
): Boolean {
    if (this == superType) return true

    context.addSubTypeConstraint(this, superType, exactQualifiers)
        ?.let { return it }

    if (isNullableType && !superType.isNullableType) return false

    if (classifier.fqName == InjektFqNames.Nothing) return true
    if (superType.classifier.fqName == InjektFqNames.Any)
        return superType.qualifiers.isEmpty() ||
                (qualifiers.isNotEmpty() &&
                        qualifiers.areMatchingQualifiers(context, superType.qualifiers, exactQualifiers))

    subtypeView(superType.classifier)
        ?.let { return it.isSubTypeOfSameClassifier(context, superType, exactQualifiers) }

    return false
}

private fun TypeRef.isSubTypeOfSameClassifier(
    context: TypeCheckerContext,
    superType: TypeRef,
    exactQualifiers: Boolean
): Boolean {
    if (!qualifiers.areMatchingQualifiers(context, superType.qualifiers, exactQualifiers))
        return false
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

data class ConstraintError(
    val subType: TypeRef,
    val superType: TypeRef,
    val kind: Constraint.Kind
)

class TypeVariableWithConstraints(val typeVariable: ClassifierRef) {
    val constraints = mutableListOf<Constraint>()
}

data class Constraint(
    val typeVariable: ClassifierRef,
    val type: TypeRef,
    val kind: Kind
) {
    enum class Kind {
        LOWER, UPPER, EQUAL
    }
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
    return context
}

fun TypeContext.getSubstitutionMap(): Map<ClassifierRef, TypeRef> {
    /*check(isOk)
    return typeParameters.mapNotNull { typeParameter ->
        val values = getValues(typeParameter)
        values.singleOrNull()
            ?.let { typeParameter to it }
    }.toMap()*/
    return emptyMap()
}

class TypeContext(val injektContext: InjektContext) : TypeCheckerContext {
    private val staticTypeParameters = mutableListOf<ClassifierRef>()
    private val typeVariables = mutableMapOf<ClassifierRef, TypeVariableWithConstraints>()

    val errors = mutableListOf<ConstraintError>()

    val isOk: Boolean get() = errors.isEmpty()

    fun addStaticTypeParameter(typeParameter: ClassifierRef) {
        staticTypeParameters += typeParameter
    }

    fun addTypeVariable(typeParameter: ClassifierRef) {
        if (typeParameter in staticTypeParameters) return
        val variableWithConstraints = TypeVariableWithConstraints(typeParameter)
        typeVariables[typeParameter] = variableWithConstraints
        typeParameter.superTypes.forEach {
            addConstraint(typeParameter.defaultType, it, Constraint.Kind.UPPER)
        }
    }

    fun addInitialSubTypeConstraint(subType: TypeRef, superType: TypeRef, exactQualifiers: Boolean) {
        checkConstraint(subType, superType, exactQualifiers)
    }

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
        addConstraint(typeVariable, superType, Constraint.Kind.UPPER)

        /*if (typeVariable.isMarkedNullable) {
            // here is important that superType is singleClassifierType
            return simplifiedSuperType.classifier in typeVariables ||
                    isSubtypeOfByTypeChecker(nullableNothingType(), simplifiedSuperType)
        }*/

        return true
    }

    private fun addLowerConstraint(typeVariable: TypeRef, subType: TypeRef): Boolean {
        val lowerConstraint = if (typeVariable.isMarkedNullable) {
           /* val typeVariableTypeConstructor = typeVariable.typeConstructor()
            val subTypeConstructor = subType.typeConstructor()
            val needToMakeDefNotNull = subTypeConstructor.isTypeVariable() ||
                    typeVariableTypeConstructor !is TypeVariableTypeConstructorMarker ||
                    !typeVariableTypeConstructor.isContainedInInvariantOrContravariantPositions()

            val resultType = if (needToMakeDefNotNull) {
                subType.makeDefinitelyNotNullOrNotNull()
            } else {
                subType.withNullability(false)
            }
            resultType*/
            subType
        } else subType

        addConstraint(typeVariable, lowerConstraint, Constraint.Kind.LOWER)

        return true
    }

    private fun addConstraint(
        typeVariable: TypeRef,
        type: TypeRef,
        kind: Constraint.Kind
    ) {
        check(typeVariable.classifier.isTypeParameter)
        val constraints = typeVariables[typeVariable.classifier]!!.constraints
        val constraint = Constraint(typeVariable.classifier, type, kind)
        if (constraint in constraints) return
        constraints += constraint
        directWithVariable(typeVariable.classifier, constraint)
        insideOtherConstraint(typeVariable.classifier, constraint)
    }

    private fun directWithVariable(typeVariable: ClassifierRef, constraint: Constraint) {
        if (constraint.kind != Constraint.Kind.LOWER) {
            typeVariables[typeVariable]!!.constraints.toList().forEach {
                if (it.kind != Constraint.Kind.UPPER) {
                    addNewIncorporatedConstraint(it.type, constraint.type)
                }
            }
        }

        if (constraint.kind != Constraint.Kind.UPPER) {
            typeVariables[typeVariable]!!.constraints.toList().forEach {
                if (it.kind != Constraint.Kind.LOWER) {
                    addNewIncorporatedConstraint(constraint.type, it.type)
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

    private fun addNewIncorporatedConstraint(subType: TypeRef, superType: TypeRef) {
        checkConstraint(subType, superType, false)
    }

    private fun checkConstraint(subType: TypeRef, superType: TypeRef, exactQualifiers: Boolean) {
        if (!subType.isSubTypeOf(this, superType, exactQualifiers)) {
            errors += ConstraintError(subType, superType, Constraint.Kind.UPPER)
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
            Constraint.Kind.UPPER -> {
                if (baseConstraint.kind == Constraint.Kind.LOWER && !isBaseGenericType) {
                    injektContext.nothingType
                } else if (baseConstraint.kind == Constraint.Kind.UPPER && !isBaseGenericType) {
                    otherConstraint.type
                } else {
                    otherConstraint.type
                }
            }
            Constraint.Kind.LOWER -> {
                if (baseConstraint.kind == Constraint.Kind.UPPER && !isBaseGenericType) {
                    injektContext.nullableAnyType
                } else {
                    otherConstraint.type
                }
            }
        }

        if (baseConstraint.kind != Constraint.Kind.LOWER) {
            addConstraint(targetVariable.defaultType, type, Constraint.Kind.UPPER)
        }
        if (baseConstraint.kind != Constraint.Kind.UPPER) {
            addConstraint(targetVariable.defaultType, type, Constraint.Kind.LOWER)
        }
    }
}
