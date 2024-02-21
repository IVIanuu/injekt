/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, DfaInternals::class, UnresolvedExpressionTypeAccess::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.dfa.*
import org.jetbrains.kotlin.fir.resolve.inference.*
import org.jetbrains.kotlin.fir.resolve.inference.model.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.resolve.substitution.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.calls.inference.components.*
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.utils.addToStdlib.*

fun ConeKotlinType.prepare(session: FirSession): ConeKotlinType {
  val tags = customAnnotations.filter { it.isTag(session) }

  if (tags.isEmpty()) return this

  val result = tags.fold(withAttributes(attributes.remove(attributes.custom!!))) { acc, tag ->
    session.symbolProvider.getClassLikeSymbolByClassId(
      ClassId(
        tag.coneTypeOrNull!!.classId!!.packageFqName,
        tag.coneTypeOrNull!!.classId!!.relativeClassName.child(InjektFqNames.TagWrapper),
        false
      )
    )!!
      .constructType(
        (tag.coneTypeOrNull!!.typeArguments.toList() +
            acc.toTypeProjection(ProjectionKind.OUT)).toTypedArray(),
        isNullable
      )
  }

  return result
}

fun FirAnnotation.isTag(session: FirSession): Boolean =
  coneTypeOrNull?.toRegularClassSymbol(session)?.hasAnnotation(InjektFqNames.Tag, session) == true

fun ConeTypeProjection.renderToString() = buildString {
  render { append(it) }
}

fun ConeTypeProjection.render(
  depth: Int = 0,
  renderType: (ConeTypeProjection) -> Boolean = { true },
  append: (String) -> Unit
) {
  if (depth > 15) return
  fun ConeTypeProjection.inner() {
    if (!renderType(this)) return

    when (this) {
      is ConeStarProjection -> append("*")
      is ConeClassLikeType -> append(lookupTag.classId.asSingleFqName().asString())
      is ConeLookupTagBasedType -> append(lookupTag.name.asString())
      else -> error("Unexpected type ${this@render} -> $this")
    }

    if (type?.typeArguments?.isNotEmpty() == true) {
      append("<")
      type!!.typeArguments.forEachIndexed { index, typeArgument ->
        typeArgument.render(depth = depth + 1, renderType, append)
        if (index != type!!.typeArguments.lastIndex) append(", ")
      }
      append(">")
    }

    if (type!!.isNullable) append("?")
  }
  inner()
}

fun testSpreadCandidateAndCreateSubstitutor(
  constraintType: ConeKotlinType,
  candidateType: ConeKotlinType,
  staticTypeParameters: List<FirTypeParameterSymbol>,
  session: FirSession
): ConeSubstitutor? {
  val candidateTypeParameters = mutableListOf<FirTypeParameterSymbol>()
  candidateType.forEachType {
    if (it is ConeTypeParameterType)
      candidateTypeParameters += it.lookupTag.typeParameterSymbol
  }
  return candidateType.testCandidateAndCreateSubstitutor(
    constraintType,
    candidateTypeParameters + staticTypeParameters,
    true,
    session
  )
}

fun ConeKotlinType.testCandidateAndCreateSubstitutor(
  superType: ConeKotlinType,
  staticTypeParameters: List<FirTypeParameterSymbol>,
  collectSuperTypeVariables: Boolean = false,
  session: FirSession
): ConeSubstitutor? {
  val constraintSystem = session.inferenceComponents.createConstraintSystem()

  val typeVariables = buildMap<FirTypeParameterSymbol, ConeTypeVariable> {
    staticTypeParameters.forEach {
      put(it, ConeTypeParameterBasedTypeVariable(it))
    }

    forEachType {
      if (it is ConeTypeParameterType) {
        val tpSymbol = it.lookupTag.typeParameterSymbol
        put(tpSymbol, ConeTypeParameterBasedTypeVariable(tpSymbol))
      }
    }

    if (collectSuperTypeVariables)
      superType.forEachType {
        if (it is ConeTypeParameterType) {
          val tpSymbol = it.lookupTag.typeParameterSymbol
          put(tpSymbol, ConeTypeParameterBasedTypeVariable(tpSymbol))
        }
      }
  }

  if (typeVariables.isEmpty())
    return if (isSubtypeOf(superType, session)) ConeSubstitutor.Empty else null

  val toTypeVariablesSubstitutor =
    substitutorByMap(typeVariables.mapValues { it.value.defaultType }, session)

  typeVariables.forEach { constraintSystem.registerVariable(it.value) }

  constraintSystem.addSubtypeConstraint(
    toTypeVariablesSubstitutor.substituteOrSelf(superType),
    toTypeVariablesSubstitutor.substituteOrSelf(this),
    SimpleConstraintSystemConstraintPosition
  )

  while (true) {
    if (constraintSystem.hasContradiction ||
      constraintSystem.notFixedTypeVariables.isEmpty()) break

    fun MutableVariableWithConstraints.getNestedTypeVariables(): Set<ConeKotlinType> {
      val nestedTypeVariables = mutableSetOf<ConeKotlinType>()
      constraints.forEach { constraint ->
        constraint.type.cast<ConeKotlinType>().forEachType {
          if (it is ConeLookupTagBasedType && it.lookupTag in constraintSystem.allTypeVariables)
            nestedTypeVariables += it
        }
      }
      return nestedTypeVariables
    }

    val typeVariableToFix = constraintSystem.notFixedTypeVariables.values
      .firstOrNull { typeVariable ->
        typeVariable.getNestedTypeVariables()
          .all { it.cast<ConeLookupTagBasedType>().lookupTag in constraintSystem.fixedTypeVariables }
      } ?: constraintSystem.notFixedTypeVariables.values.first()

    val resultType = session.inferenceComponents.resultTypeResolver.findResultType(
      constraintSystem,
      constraintSystem.notFixedTypeVariables.values.first(),
      TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN
    )

    constraintSystem.fixVariable(
      typeVariableToFix.typeVariable,
      resultType,
      ConeFixVariableConstraintPosition(typeVariableToFix.typeVariable)
    )
  }

  return if (constraintSystem.hasContradiction) null
  else substitutorByMap(
    typeVariables.mapValues {
      constraintSystem.fixedTypeVariables[it.value.typeConstructor]!!.cast()
    },
    session
  )
}

val ConeKotlinType.typeSize: Int
  get() {
    var typeSize = 0
    val seen = mutableSetOf<ConeKotlinType>()
    fun visit(type: ConeKotlinType) {
      typeSize++
      if (seen.add(type))
        type.typeArguments.forEach { it.type?.let { visit(it) } }
    }
    visit(this)
    return typeSize
  }

val ConeKotlinType.coveringSet: Set<ConeClassifierLookupTag>
  get() {
    val tags = mutableSetOf<ConeClassifierLookupTag>()
    val seen = mutableSetOf<ConeKotlinType>()
    fun visit(type: ConeKotlinType) {
      if (!seen.add(type)) return
      type.safeAs<ConeLookupTagBasedType>()?.let { tags += it.lookupTag }
      type.typeArguments.forEach { it.type?.let { visit(it) } }
    }
    visit(this)
    return tags
  }

class FrameworkKeyConeAttribute(val value: String) : ConeAttribute<FrameworkKeyConeAttribute>() {
  override val keepInInferredDeclarationType: Boolean
    get() = true

  override fun union(other: FrameworkKeyConeAttribute?) = other
  override fun intersect(other: FrameworkKeyConeAttribute?) = this
  override fun add(other: FrameworkKeyConeAttribute?) = this

  override fun isSubtypeOf(other: FrameworkKeyConeAttribute?): Boolean = true

  override val key = FrameworkKeyConeAttribute::class

  override fun toString(): String = "FrameworkKey($value)"
}

val ConeKotlinType.frameworkKey get() = attributes.frameworkKey?.value

val ConeAttributes.frameworkKey by ConeAttributes.attributeAccessor<FrameworkKeyConeAttribute>()

fun ConeKotlinType.withFrameworkKey(value: String?): ConeKotlinType = when {
  attributes.frameworkKey?.value == value -> this
  value == null -> withAttributes(attributes.remove(attributes.frameworkKey!!))
  else -> withAttributes(attributes + FrameworkKeyConeAttribute(value))
}

fun ConeKotlinType.isUnconstrained(staticTypeParameters: List<FirTypeParameterSymbol>): Boolean =
  false
  /*classifier.isTypeParameter &&
      classifier !in staticTypeParameters &&
      classifier.superTypes.all {
        it.classifier.fqName == InjektFqNames.Any ||
            it.isUnconstrained(staticTypeParameters)
      }*/

val FirSession.collectionType get() = symbolProvider.getClassLikeSymbolByClassId(
  InjektFqNames.Collection
).cast<FirClassSymbol<*>>().defaultType()

val FirSession.listType get() = symbolProvider.getClassLikeSymbolByClassId(
  InjektFqNames.List
).cast<FirClassSymbol<*>>().defaultType()
