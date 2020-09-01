/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.tree.generator

import com.ivianuu.ast.tree.generator.context.generatedType
import com.ivianuu.ast.tree.generator.context.type
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

val jumpTargetType = type("ast", "AstTarget")
val constKindType = generatedType("expressions", "AstConstKind")
val operationType = type("ast.expressions", "AstOperation")
val classKindType = type(ClassKind::class)
val varianceType = type(Variance::class)
val nameType = type(Name::class)
val visibilityType = type(null, "Visibility")
val modalityType = type(Modality::class)
val fqNameType = type(FqName::class)
val classIdType = type(ClassId::class)
val annotationUseSiteTargetType = type(AnnotationUseSiteTarget::class)
val operationKindType = type("ast.expressions", "LogicOperationKind")
val coneKotlinTypeType = type(ConeKotlinType::class)

val whenExpressionType = generatedType("expressions", "AstWhenExpression")
val expressionType = generatedType("expressions", "AstExpression")
val safeCallCheckedSubjectType = generatedType("expressions", "AstCheckedSafeCallSubject")

val whenRefType = generatedType("", "AstExpressionRef<AstWhenExpression>")
val safeCallOriginalReceiverReferenceType = generatedType("", "AstExpressionRef<AstExpression>")
val safeCallCheckedSubjectReferenceType =
    generatedType("", "AstExpressionRef<AstCheckedSafeCallSubject>")

val noReceiverExpressionType = generatedType("expressions.impl", "AstNoReceiverExpression")
val implicitTypeRefType = generatedType("types.impl", "AstImplicitTypeRefImpl")
val astQualifierPartType = type("ast.types", "AstQualifierPart")
val simpleNamedReferenceType = generatedType("references.impl", "AstSimpleNamedReference")
val explicitThisReferenceType = generatedType("references.impl", "AstExplicitThisReference")
val explicitSuperReferenceType = generatedType("references.impl", "AstExplicitSuperReference")
val implicitBooleanTypeRefType = generatedType("types.impl", "AstImplicitBooleanTypeRef")
val implicitNothingTypeRefType = generatedType("types.impl", "AstImplicitNothingTypeRef")
val implicitStringTypeRefType = generatedType("types.impl", "AstImplicitStringTypeRef")
val implicitUnitTypeRefType = generatedType("types.impl", "AstImplicitUnitTypeRef")
val stubReferenceType = generatedType("references.impl", "AstStubReference")
val compositeTransformResultType = type("ast.visitors", "CompositeTransformResult")

val abstractAstBasedSymbolType = type("ast.symbols", "AbstractAstBasedSymbol")
val backingFieldSymbolType = type("ast.symbols.impl", "AstBackingFieldSymbol")
val delegateFieldSymbolType = type("ast.symbols.impl", "AstDelegateFieldSymbol")
val classSymbolType = type("ast.symbols.impl", "AstClassSymbol")
val classLikeSymbolType = type("ast.symbols.impl", "AstClassLikeSymbol<*>")
val typeParameterSymbolType = type("ast.symbols.impl", "AstTypeParameterSymbol")
val anonymousInitializerSymbolType = type("ast.symbols.impl", "AstAnonymousInitializerSymbol")

val emptyArgumentListType = type("ast.expressions", "AstEmptyArgumentList")

val pureAbstractElementType = generatedType("AstPureAbstractElement")

val astImplementationDetailType = generatedType("AstImplementationDetail")
val declarationOriginType = generatedType("declarations", "AstDeclarationOrigin")
val declarationAttributesType = generatedType("declarations", "AstDeclarationAttributes")
val annotationResolveStatusType = generatedType("expressions", "AstAnnotationResolveStatus")
