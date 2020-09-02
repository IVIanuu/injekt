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
    val annotationContainer = element("AnnotationContainer", Other) {
        visitorSuperType = baseAstElement
        transformerType = baseAstElement
    }
    val type = element("Type", Type, annotationContainer) {
        visitorSuperType = baseAstElement
        transformerType = this
    }
    val symbolOwner = element("SymbolOwner", Other) {
        visitorSuperType = baseAstElement
        transformerType = baseAstElement
    }
    val varargElement = element("VarargElement", Other) {
        visitorSuperType = baseAstElement
        transformerType = this
    }

    val targetElement = element("TargetElement", Other) {
        visitorSuperType = baseAstElement
        transformerType = this
    }

    val statement = element("Statement", Expression, annotationContainer) {
        visitorSuperType = baseAstElement
        transformerType = this
    }
    val expression = element("Expression", Expression, statement, varargElement) {
        visitorSuperType = statement
        transformerType = statement
    }
    val declaration = element("Declaration", Declaration, statement, annotationContainer) {
        visitorSuperType = statement
        transformerType = statement
    }
    val anonymousInitializer = element(
        "AnonymousInitializer",
        Declaration,
        declaration,
        symbolOwner
    ) {
        visitorSuperType = declaration
        transformerType = statement
    }
    // todo remove?
    val callableDeclaration =
        element("CallableDeclaration", Declaration, declaration, symbolOwner) {
            visitorSuperType = declaration
            transformerType = statement
        }
    val typeParameter =
        element("TypeParameter", Declaration, declaration, symbolOwner) {
            visitorSuperType = declaration
            transformerType = statement
        }
    val typeParametersOwner = element("TypeParametersOwner", Declaration) {
        visitorSuperType = baseAstElement
        transformerType = baseAstElement
    }

    val variable =
        element("Variable", Declaration, callableDeclaration, declaration) {
            visitorSuperType = declaration
            transformerType = statement
        }
    val valueParameter = element("ValueParameter", Declaration, variable) {
        visitorSuperType = variable
        transformerType = statement
    }
    val property = element(
        "Property",
        Declaration,
        variable,
        typeParametersOwner,
        callableDeclaration
    ) {
        visitorSuperType = variable
        transformerType = statement
    }
    val field =
        element("Field", Declaration, variable, typeParametersOwner, callableDeclaration) {
            visitorSuperType = variable
            transformerType = statement
        }
    val enumEntry = element("EnumEntry", Declaration, variable, callableDeclaration) {
        visitorSuperType = variable
        transformerType = statement
    }

    val classLikeDeclaration =
        element("ClassLikeDeclaration", Declaration, declaration, symbolOwner) {
            visitorSuperType = declaration
            transformerType = statement
        }
    val klass =
        element("Class", Declaration, classLikeDeclaration) {
            visitorSuperType = classLikeDeclaration
            transformerType = statement
        }
    val regularClass = element(
        "RegularClass",
        Declaration,
        declaration,
        typeParametersOwner,
        klass
    ) {
        visitorSuperType = klass
        transformerType = statement
    }
    val typeAlias = element(
        "TypeAlias",
        Declaration,
        classLikeDeclaration,
        typeParametersOwner
    ) {
        visitorSuperType = classLikeDeclaration
        transformerType = statement
    }

    val function = element(
        "Function",
        Declaration,
        callableDeclaration,
        targetElement
    ) {
        visitorSuperType = declaration
        transformerType = statement
    }

    val namedFunction = element(
        "NamedFunction",
        Declaration,
        function,
        callableDeclaration,
        typeParametersOwner
    ) {
        visitorSuperType = function
        transformerType = statement
    }
    val propertyAccessor = element("PropertyAccessor", Declaration, function, typeParametersOwner) {
        visitorSuperType = function
        transformerType = statement
    }
    val constructor = element(
        "Constructor",
        Declaration,
        function,
        callableDeclaration
    ) {
        visitorSuperType = function
        transformerType = statement
    }

    val moduleFragment = element("ModuleFragment", Declaration) {
        visitorSuperType = baseAstElement
        transformerType = this
    }
    val file = element("File", Declaration, annotationContainer) {
        visitorSuperType = baseAstElement
        transformerType = this
    }

    val anonymousFunction =
        element("AnonymousFunction", Declaration, function, expression) {
            visitorSuperType = function
            transformerType = statement
        }
    val anonymousObject =
        element("AnonymousObject", Declaration, klass, expression) {
            visitorSuperType = klass
            transformerType = statement
        }

    val loop = element("Loop", Expression, expression, targetElement) {
        visitorSuperType = expression
        transformerType = statement
    }
    val doWhileLoop = element("DoWhileLoop", Expression, loop) {
        visitorSuperType = loop
        transformerType = statement
    }
    val whileLoop = element("WhileLoop", Expression, loop) {
        visitorSuperType = loop
        transformerType = statement
    }

    val block = element("Block", Expression, expression) {
        visitorSuperType = expression
        transformerType = statement
    }
    val loopJump = element("LoopJump", Expression, expression) {
        visitorSuperType = expression
        transformerType = statement
    }
    val breakExpression = element("Break", Expression, loopJump) {
        visitorSuperType = loopJump
        transformerType = statement
    }
    val continueExpression = element("Continue", Expression, loopJump) {
        visitorSuperType = loopJump
        transformerType = statement
    }
    val catchClause = element("Catch", Expression) {
        visitorSuperType = baseAstElement
        transformerType = this
    }
    val tryExpression = element("Try", Expression, expression) {
        visitorSuperType = expression
        transformerType = statement
    }
    val constExpression = element("Const", Expression, expression) {
        visitorSuperType = expression
        transformerType = statement
    }
    val typeProjection = element("TypeProjection", Type) {
        visitorSuperType = baseAstElement
        transformerType = this
    }
    val starProjection = element("StarProjection", Type, typeProjection) {
        visitorSuperType = typeProjection
        transformerType = typeProjection
    }
    val typeProjectionWithVariance = element("TypeProjectionWithVariance", Type, typeProjection) {
        visitorSuperType = typeProjection
        transformerType = typeProjection
    }
    val call = element(
        "Call",
        Expression,
        expression
    ) {
        visitorSuperType = expression
        transformerType = statement
    }
    val whenExpression = element("When", Expression, expression) {
        visitorSuperType = expression
        transformerType = statement
    }
    val whenBranch = element("WhenBranch", Expression) {
        visitorSuperType = baseAstElement
        transformerType = this
    }

    val classReference = element("ClassReference", Expression, expression) {
        visitorSuperType = expression
        transformerType = statement
    }
    val qualifiedAccess = element("QualifiedAccess", Expression, expression) {
        visitorSuperType = expression
        transformerType = statement
    }
    val functionCall = element("FunctionCall", Expression, qualifiedAccess, call) {
        visitorSuperType = qualifiedAccess
        transformerType = statement
    }
    val delegatedConstructorCall = element("DelegatedConstructorCall", Expression, call) {
        visitorSuperType = call
        transformerType = statement
    }
    val callableReference = element("CallableReference", Expression, qualifiedAccess) {
        visitorSuperType = qualifiedAccess
        transformerType = statement
    }

    val vararg = element("Vararg", Expression, expression) {
        visitorSuperType = expression
        transformerType = statement
    }
    val spreadElement = element("SpreadElement", Other, varargElement) {
        visitorSuperType = varargElement
        transformerType = varargElement
    }

    val returnExpression = element("Return", Expression, expression) {
        visitorSuperType = expression
        transformerType = statement
    }
    val throwExpression = element("Throw", Expression, expression) {
        visitorSuperType = expression
        transformerType = statement
    }
    val variableAssignment = element("VariableAssignment", Expression, qualifiedAccess) {
        visitorSuperType = qualifiedAccess
        transformerType = statement
    }

    val superReference = element("SuperReference", Expression, expression) {
        visitorSuperType = expression
        transformerType = statement
    }
    val thisReference = element("ThisReference", Expression, expression) {
        visitorSuperType = expression
        transformerType = statement
    }
    val backingFieldReference = element("BackingFieldReference", Expression, expression) {
        visitorSuperType = expression
        transformerType = statement
    }

    val simpleType = element("SimpleType", Type, type) {
        visitorSuperType = this@AstTreeBuilder.type
        transformerType = this@AstTreeBuilder.type
    }
}
