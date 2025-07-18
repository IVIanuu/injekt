/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, DirectDeclarationsAccess::class)

package injekt.compiler.fir

import injekt.compiler.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.deserialization.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.utils.addToStdlib.*

// todo replace with a diagnostic suppressor once/if available
class TagAnnotationTargetPatcher(
  ctx: InjektContext
) : FirStatusTransformerExtension(ctx.session) {
  override fun needTransformStatus(declaration: FirDeclaration): Boolean =
    declaration is FirRegularClass &&
        declaration.hasAnnotationSafe(InjektFqNames.Tag, session)

  override fun transformStatus(
    status: FirDeclarationStatus,
    regularClass: FirRegularClass,
    containingClass: FirClassLikeSymbol<*>?,
    isLocal: Boolean
  ): FirDeclarationStatus {
    if (regularClass.hasAnnotation(InjektFqNames.Target, session))
      return super.transformStatus(status, regularClass, containingClass, isLocal)

    val targetSymbol = InjektFqNames.Target
      .toSymbol(session)!!
      .cast<FirClassSymbol<*>>()
    val targetConstructorSymbol =
      targetSymbol.declarationSymbols
        .filterIsInstance<FirConstructorSymbol>()
        .firstOrNull { it.isPrimary }
        ?: return super.transformStatus(status, regularClass, containingClass, isLocal)
    val allowedTargetsValueParameterSymbol =
      targetConstructorSymbol.valueParameterSymbols.single()

    regularClass.replaceAnnotations(
      regularClass.annotations + buildAnnotation {
        source = regularClass.source

        annotationTypeRef = targetSymbol.defaultType().toFirResolvedTypeRef()

        argumentMapping = buildAnnotationArgumentMapping {
          mapping[Name.identifier("allowedTargets")] =
            buildVarargArgumentsExpression {
              coneTypeOrNull = allowedTargetsValueParameterSymbol.resolvedReturnType
              coneElementTypeOrNull = allowedTargetsValueParameterSymbol.resolvedReturnType
                .typeArguments
                .single()
                .type

              arguments += buildEnumEntryDeserializedAccessExpression {
                enumClassId = StandardClassIds.AnnotationTarget
                enumEntryName = Name.identifier("CLASS")
              }
                .toQualifiedPropertyAccessExpression(session)

              arguments += buildEnumEntryDeserializedAccessExpression {
                enumClassId = StandardClassIds.AnnotationTarget
                enumEntryName = Name.identifier("CONSTRUCTOR")
              }
                .toQualifiedPropertyAccessExpression(session)

              arguments += buildEnumEntryDeserializedAccessExpression {
                enumClassId = StandardClassIds.AnnotationTarget
                enumEntryName = Name.identifier("TYPE")
              }
                .toQualifiedPropertyAccessExpression(session)
            }
        }
      }
    )

    return super.transformStatus(status, regularClass, containingClass, isLocal)
  }
}
