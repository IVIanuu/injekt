/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.tree.generator

import com.ivianuu.ast.tree.generator.context.generatedType
import com.ivianuu.ast.tree.generator.context.type
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

val constKindType = generatedType("expressions", "AstConstKind")
val typeOperatorType = generatedType("expressions", "AstTypeOperator")
val delegatedConstructorCallKindType = generatedType("expressions", "AstDelegatedConstructorCallKind")
val classKindType = type(ClassKind::class)
val varianceType = type(Variance::class)
val nameType = type(Name::class)
val visibilityType = type("ast", "Visibility")
val visibilitiesType = type("ast", "Visibilities")
val modalityType = type(Modality::class)
val fqNameType = type(FqName::class)
val classIdType = type(ClassId::class)
val platformStatusType = type("ast", "PlatformStatus")
val targetType = type("ast", "AstTarget")

val compositeTransformResultType = type("ast.visitors", "CompositeTransformResult")

val astSymbolType = type("ast.symbols", "AstSymbol")
val callableSymbolType = type("ast.symbols.impl", "AstCallableSymbol")
val classSymbolType = type("ast.symbols.impl", "AstClassSymbol")
val classLikeSymbolType = type("ast.symbols.impl", "AstClassLikeSymbol")
val classifierSymbolType = type("ast.symbols.impl", "AstClassifierSymbol")
val constructorSymbolType = type("ast.symbols.impl", "AstConstructorSymbol")
val enumEntrySymbolType = type("ast.symbols.impl", "AstEnumEntrySymbol")
val functionSymbolType = type("ast.symbols.impl", "AstFunctionSymbol")
val propertySymbolType = type("ast.symbols.impl", "AstPropertySymbol")
val typeParameterSymbolType = type("ast.symbols.impl", "AstTypeParameterSymbol")
val anonymousInitializerSymbolType = type("ast.symbols.impl", "AstAnonymousInitializerSymbol")
val valueParameterSymbol = type("ast.symbols.impl", "AstValueParameterSymbol")
val variableSymbol = type("ast.symbols.impl", "AstVariableSymbol")

val pureAbstractElementType = generatedType("AstPureAbstractElement")

val astImplementationDetailType = generatedType("AstImplementationDetail")
val declarationOriginType = generatedType("declarations", "AstDeclarationOrigin")
val declarationAttributesType = generatedType("declarations", "AstDeclarationAttributes")

val builderType = type("builder", "AstBuilder")
val contextType = type("ast", "AstContext")
