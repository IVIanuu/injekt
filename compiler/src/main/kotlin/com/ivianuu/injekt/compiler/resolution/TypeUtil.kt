package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.InjektClassIds
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypePreparator
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.customAnnotations
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeRefiner
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

fun ConeKotlinType.isSubtypeOfInjekt(
  superType: ConeKotlinType,
  session: FirSession
): Boolean {
  val state = object : TypeCheckerState(
    false,
    false,
    true,
    session.typeContext,
    ConeTypePreparator(session),
    AbstractTypeRefiner.Default
  ) {
    override fun customIsSubtypeOf(
      subType: KotlinTypeMarker,
      superType: KotlinTypeMarker
    ): Boolean {
      subType as ConeKotlinType
      superType as ConeKotlinType
      val subTypeTags = subType.getTags(session)
      val superTypeTags = superType.getTags(session)

      if (subTypeTags.size != superTypeTags.size) return false

      for (index in subTypeTags.indices) {
        val subTypeTag = subTypeTags[index]
        val superTypeTag = superTypeTags[index]
        if (!AbstractTypeChecker.isSubtypeOf(this, subTypeTag, superTypeTag))
          return false
      }

      return true
    }
  }

  return AbstractTypeChecker.isSubtypeOf(
    state,
    this,
    superType,
  )
}

fun ConeKotlinType.getTags(session: FirSession): List<ConeKotlinType> =
  attributes.customAnnotations.filter {
    it.typeRef.toClassLikeSymbol(session)!!.hasAnnotation(ClassId.topLevel(InjektClassIds.Tag), session)
  }.map { it.typeRef.coneType }
