/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.tree.generator

import com.ivianuu.ast.tree.generator.context.AbstractAstTreeBuilder
import com.ivianuu.ast.tree.generator.model.Element.Kind.Declaration
import com.ivianuu.ast.tree.generator.model.Element.Kind.Expression
import com.ivianuu.ast.tree.generator.model.Element.Kind.Other
import com.ivianuu.ast.tree.generator.model.Element.Kind.Type

@Suppress("unused", "MemberVisibilityCanBePrivate")
object AstTreeBuilder : AbstractAstTreeBuilder() {
    val annotationContainer = element("AnnotationContainer", Other)
    val type = element("Type", Type, annotationContainer)
    val symbolOwner = element("SymbolOwner", Other)
    val varargElement = element("VarargElement", Other)

    val targetElement = element("TargetElement", Other)

    val statement = element("Statement", Expression, annotationContainer)
    val expression = element("Expression", Expression, statement, varargElement)
    val declaration = element("Declaration", Declaration)
    val anonymousInitializer = element(
        "AnonymousInitializer",
        Declaration,
        declaration,
        symbolOwner
    )
    // todo remove?
    val callableDeclaration =
        element("CallableDeclaration", Declaration, declaration, symbolOwner)
    val typeParameter =
        element("TypeParameter", Declaration, declaration, annotationContainer, symbolOwner)
    val typeParametersOwner = element("TypeParametersOwner", Declaration)

    val variable =
        element("Variable", Declaration, callableDeclaration, declaration, annotationContainer, statement)
    val valueParameter = element("ValueParameter", Declaration, variable)
    val property = element(
        "Property",
        Declaration,
        variable,
        typeParametersOwner,
        callableDeclaration
    )
    val field =
        element("Field", Declaration, variable, typeParametersOwner, callableDeclaration)
    val enumEntry = element("EnumEntry", Declaration, variable, callableDeclaration)

    val classLikeDeclaration =
        element("ClassLikeDeclaration", Declaration, declaration, annotationContainer, statement, symbolOwner)
    val klass =
        element("Class", Declaration, classLikeDeclaration, statement)
    val regularClass = element(
        "RegularClass",
        Declaration,
        callableDeclaration,
        typeParametersOwner,
        klass
    )
    val typeAlias = element(
        "TypeAlias",
        Declaration,
        classLikeDeclaration,
        callableDeclaration,
        typeParametersOwner
    )

    val function = element(
        "Function",
        Declaration,
        callableDeclaration,
        targetElement,
        statement
    )

    val namedFunction = element(
        "NamedFunction",
        Declaration,
        function,
        callableDeclaration,
        typeParametersOwner
    )
    val propertyAccessor = element("PropertyAccessor", Declaration, function, typeParametersOwner)
    val constructor = element(
        "Constructor",
        Declaration,
        function,
        callableDeclaration
    )

    val moduleFragment = element("ModuleFragment", Declaration)
    val file = element("File", Declaration, annotationContainer)

    val anonymousFunction =
        element("AnonymousFunction", Declaration, function, expression)
    val anonymousObject =
        element("AnonymousObject", Declaration, klass, expression)

    val loop = element("Loop", Expression, statement, targetElement)
    val doWhileLoop = element("DoWhileLoop", Expression, loop)
    val whileLoop = element("WhileLoop", Expression, loop)

    val block = element("Block", Expression, expression)
    val binaryLogicOperation = element("BinaryLogicOperation", Expression, expression)
    val jump = element("Jump", Expression, expression)
    val loopJump = element("LoopJump", Expression, jump)
    val breakExpression = element("Break", Expression, loopJump)
    val continueExpression = element("Continue", Expression, loopJump)
    val catchClause = element("Catch", Expression)
    val tryExpression = element("Try", Expression, expression)
    val constExpression = element("Const", Expression, expression)
    val typeProjection = element("TypeProjection", Type)
    val starProjection = element("StarProjection", Type, typeProjection)
    val typeProjectionWithVariance = element("TypeProjectionWithVariance", Type, typeProjection)
    val call = element(
        "Call",
        Expression,
        statement
    )
    val comparisonOperation = element("ComparisonOperation", Expression, expression)
    val typeOperation = element("TypeOperation", Expression, expression)
    val assignmentOperatorStatement = element("AssignmentOperatorStatement", Expression, statement)
    val equalityOperation = element("EqualityOperation", Expression, expression)
    val whenExpression = element("When", Expression, expression)
    val whenBranch = element("WhenBranch", Expression)

    val classReference = element("ClassReference", Expression, expression)
    val qualifiedAccess = element("QualifiedAccess", Expression, expression)
    val functionCall = element("FunctionCall", Expression, qualifiedAccess, call)
    val delegatedConstructorCall = element("DelegatedConstructorCall", Expression, call)
    val callableReference = element("CallableReference", Expression, qualifiedAccess)

    val vararg = element("Vararg", Expression, expression)
    val spreadElement = element("SpreadElement", Other, varargElement)

    val returnExpression = element("Return", Expression, jump)
    val throwExpression = element("Throw", Expression, expression)
    val variableAssignment = element("VariableAssignment", Expression, qualifiedAccess)

    val superReference = element("SuperReference", Expression, expression)
    val thisReference = element("ThisReference", Expression, expression)
    val backingFieldReference = element("BackingFieldReference", Expression, expression)

    val simpleType = element("SimpleType", Type, type)
}
