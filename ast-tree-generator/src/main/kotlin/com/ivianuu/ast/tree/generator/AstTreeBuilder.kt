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
    val declarationContainer = element("DeclarationContainer", Declaration) {
        visitorSuperType = baseAstElement
        transformerType = baseAstElement
    }
    val namedDeclaration = element("NamedDeclaration", Declaration, declaration) {
        visitorSuperType = declaration
        transformerType = statement
    }
    val memberDeclaration = element("MemberDeclaration", Declaration, namedDeclaration) {
        visitorSuperType = namedDeclaration
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
        element("TypeParameter", Declaration, namedDeclaration, symbolOwner) {
            visitorSuperType = namedDeclaration
            transformerType = statement
        }
    val typeParametersOwner = element("TypeParametersOwner", Declaration) {
        visitorSuperType = baseAstElement
        transformerType = baseAstElement
    }

    val variable =
        element("Variable", Declaration, callableDeclaration, namedDeclaration) {
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
        callableDeclaration,
        memberDeclaration
    ) {
        visitorSuperType = variable
        transformerType = statement
    }

    val classLikeDeclaration =
        element("ClassLikeDeclaration", Declaration, declaration, symbolOwner) {
            visitorSuperType = declaration
            transformerType = statement
        }
    val klass =
        element("Class", Declaration, classLikeDeclaration, declarationContainer) {
            visitorSuperType = classLikeDeclaration
            transformerType = statement
        }
    val regularClass = element(
        "RegularClass",
        Declaration,
        memberDeclaration,
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
        memberDeclaration,
        typeParametersOwner
    ) {
        visitorSuperType = classLikeDeclaration
        transformerType = statement
    }
    val enumEntry = element("EnumEntry", Declaration, klass, namedDeclaration) {
        visitorSuperType = klass
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
        memberDeclaration,
        callableDeclaration,
        typeParametersOwner
    ) {
        visitorSuperType = function
        transformerType = statement
    }
    val propertyAccessor = element("PropertyAccessor", Declaration, function, memberDeclaration, typeParametersOwner) {
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
    val packageFragment = element("PackageFragment", Declaration, declarationContainer) {
        visitorSuperType = declarationContainer
        transformerType = baseAstElement
    }
    val file = element("File", Declaration, packageFragment, annotationContainer) {
        visitorSuperType = packageFragment
        transformerType = baseAstElement
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
    val jump = element("Jump", Expression, expression) {
        visitorSuperType = expression
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
    val forLoop = element("ForLoop", Expression, loop) {
        visitorSuperType = loop
        transformerType = statement
    }

    val block = element("Block", Expression, expression) {
        visitorSuperType = expression
        transformerType = statement
    }
    val loopJump = element("LoopJump", Expression, jump) {
        visitorSuperType = jump
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
    val calleeReference = element("CalleeReference", Expression, expression) {
        visitorSuperType = expression
        transformerType = statement
    }
    val call = element(
        "Call",
        Expression,
        calleeReference
    ) {
        visitorSuperType = calleeReference
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

    val baseQualifiedAccess = element("BaseQualifiedAccess", Expression, expression) {
        visitorSuperType = expression
        transformerType = statement
    }
    val qualifiedAccess = element("QualifiedAccess", Expression, calleeReference, baseQualifiedAccess) {
        visitorSuperType = baseQualifiedAccess
        transformerType = statement
    }
    val functionCall = element("FunctionCall", Expression, baseQualifiedAccess, call) {
        visitorSuperType = baseQualifiedAccess
        transformerType = statement
    }
    val delegatedConstructorCall = element("DelegatedConstructorCall", Expression, call) {
        visitorSuperType = call
        transformerType = statement
    }
    val delegateInitializer = element("DelegateInitializer", Expression) {
        visitorSuperType = baseAstElement
        transformerType = baseAstElement
    }
    val callableReference = element("CallableReference", Expression, baseQualifiedAccess) {
        visitorSuperType = baseQualifiedAccess
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

    val returnExpression = element("Return", Expression, jump) {
        visitorSuperType = jump
        transformerType = statement
    }
    val throwExpression = element("Throw", Expression, expression) {
        visitorSuperType = expression
        transformerType = statement
    }
    val variableAssignment = element("VariableAssignment", Expression, baseQualifiedAccess) {
        visitorSuperType = baseQualifiedAccess
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
    val propertyBackingFieldReference = element("PropertyBackingFieldReference", Expression, expression) {
        visitorSuperType = expression
        transformerType = statement
    }
    val typeOperation = element("TypeOperation", Expression, expression) {
        visitorSuperType = expression
        transformerType = statement
    }

}
