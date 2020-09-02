/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.tree.generator

import com.ivianuu.ast.tree.generator.context.AbstractAstTreeBuilder
import com.ivianuu.ast.tree.generator.model.Element.Kind.Declaration
import com.ivianuu.ast.tree.generator.model.Element.Kind.Expression
import com.ivianuu.ast.tree.generator.model.Element.Kind.Other
import com.ivianuu.ast.tree.generator.model.Element.Kind.Reference
import com.ivianuu.ast.tree.generator.model.Element.Kind.Type

@Suppress("unused", "MemberVisibilityCanBePrivate")
object AstTreeBuilder : AbstractAstTreeBuilder() {
    val annotationContainer = element("AnnotationContainer", Other)
    val type = element("Type", Type, annotationContainer)
    val reference = element("Reference", Reference)
    val label = element("Label", Other)
    val symbolOwner = element("SymbolOwner", Other)
    val resolvable = element("Resolvable", Expression)

    val targetElement = element("TargetElement", Other)

    val declarationStatus = element("DeclarationStatus", Declaration)

    val statement = element("Statement", Expression, annotationContainer)
    val expression = element("Expression", Expression, statement)
    val declaration = element("Declaration", Declaration)
    val annotatedDeclaration =
        element("AnnotatedDeclaration", Declaration, declaration, annotationContainer)
    val anonymousInitializer = element(
        "AnonymousInitializer",
        Declaration,
        declaration,
        symbolOwner
    )
    val typedDeclaration = element("TypedDeclaration", Declaration, annotatedDeclaration)
    val callableDeclaration =
        element("CallableDeclaration", Declaration, typedDeclaration, symbolOwner)
    val typeParameterRef = element("TypeParameterRef", Declaration)
    val typeParameter =
        element("TypeParameter", Declaration, typeParameterRef, annotatedDeclaration, symbolOwner)
    val typeParameterRefsOwner = element("TypeParameterRefsOwner", Declaration)
    val typeParametersOwner = element("TypeParametersOwner", Declaration, typeParameterRefsOwner)
    val memberDeclaration =
        element("MemberDeclaration", Declaration, annotatedDeclaration, typeParameterRefsOwner)
    val callableMemberDeclaration =
        element("CallableMemberDeclaration", Declaration, callableDeclaration, memberDeclaration)

    val variable =
        element("Variable", Declaration, callableDeclaration, annotatedDeclaration, statement)
    val valueParameter = element("ValueParameter", Declaration, variable)
    val property = element(
        "Property",
        Declaration,
        variable,
        typeParametersOwner,
        callableMemberDeclaration
    )
    val field =
        element("Field", Declaration, variable, typeParametersOwner, callableMemberDeclaration)
    val enumEntry = element("EnumEntry", Declaration, variable, callableMemberDeclaration)

    val classLikeDeclaration =
        element("ClassLikeDeclaration", Declaration, annotatedDeclaration, statement, symbolOwner)
    val klass =
        element("Class", Declaration, classLikeDeclaration, statement, typeParameterRefsOwner)
    val regularClass = element(
        "RegularClass",
        Declaration,
        memberDeclaration,
        typeParameterRefsOwner,
        klass
    )
    val typeAlias = element(
        "TypeAlias",
        Declaration,
        classLikeDeclaration,
        memberDeclaration,
        typeParametersOwner
    )

    val function = element(
        "Function",
        Declaration,
        callableDeclaration,
        targetElement,
        typeParameterRefsOwner,
        statement
    )

    val simpleFunction = element(
        "SimpleFunction",
        Declaration,
        function,
        callableMemberDeclaration,
        typeParametersOwner
    )
    val propertyAccessor = element("PropertyAccessor", Declaration, function, typeParametersOwner)
    val constructor = element(
        "Constructor",
        Declaration,
        function,
        callableMemberDeclaration,
        typeParameterRefsOwner
    )

    val moduleFragment = element("ModuleFragment", Declaration)
    val file = element("File", Declaration, annotationContainer)

    val anonymousFunction =
        element("AnonymousFunction", Declaration, function, expression, typeParametersOwner)
    val anonymousObject =
        element("AnonymousObject", Declaration, klass, expression)

    val loop = element("Loop", Expression, statement, targetElement)
    val doWhileLoop = element("DoWhileLoop", Expression, loop)
    val whileLoop = element("WhileLoop", Expression, loop)

    val block = element("Block", Expression, expression)
    val binaryLogicExpression = element("BinaryLogicExpression", Expression, expression)
    val jump = element("Jump", Expression, expression)
    val loopJump = element("LoopJump", Expression, jump)
    val breakExpression = element("BreakExpression", Expression, loopJump)
    val continueExpression = element("ContinueExpression", Expression, loopJump)
    val catchClause = element("Catch", Expression)
    val tryExpression = element("TryExpression", Expression, expression, resolvable)
    val constExpression = element("ConstExpression", Expression, expression)
    val typeProjection = element("TypeProjection", Type)
    val starProjection = element("StarProjection", Type, typeProjection)
    val typeProjectionWithVariance = element("TypeProjectionWithVariance", Type, typeProjection)
    val argumentList = element("ArgumentList", Expression)
    val call = element(
        "Call",
        Expression,
        statement
    ) // TODO: may smth like `CallWithArguments` or `ElementWithArguments`?
    val annotationCall = element("AnnotationCall", Expression, expression, call, resolvable)
    val comparisonExpression = element("ComparisonExpression", Expression, expression)
    val typeOperatorCall = element("TypeOperatorCall", Expression, expression, call)
    val assignmentOperatorStatement = element("AssignmentOperatorStatement", Expression, statement)
    val equalityOperatorCall = element("EqualityOperatorCall", Expression, expression, call)
    val whenExpression = element("WhenExpression", Expression, expression, resolvable)
    val whenBranch = element("WhenBranch", Expression)
    val qualifiedAccess = element("QualifiedAccess", Expression, resolvable, statement)
    val elvisExpression = element("ElvisExpression", Expression, expression, resolvable)

    val classReferenceExpression = element("ClassReferenceExpression", Expression, expression)
    val qualifiedAccessExpression =
        element("QualifiedAccessExpression", Expression, expression, qualifiedAccess)
    val functionCall = element("FunctionCall", Expression, qualifiedAccessExpression, call)
    val delegatedConstructorCall = element("DelegatedConstructorCall", Expression, resolvable, call)
    val callableReferenceAccess =
        element("CallableReferenceAccess", Expression, qualifiedAccessExpression)
    val thisReceiverExpression =
        element("ThisReceiverExpression", Expression, qualifiedAccessExpression)
    val expressionWithSmartcast =
        element("ExpressionWithSmartcast", Expression, qualifiedAccessExpression)
    val safeCallExpression = element("SafeCallExpression", Expression, expression)
    val checkedSafeCallSubject = element("CheckedSafeCallSubject", Expression, expression)
    val getClassCall = element("GetClassCall", Expression, expression, call)
    val wrappedExpression = element("WrappedExpression", Expression, expression)
    val wrappedArgumentExpression =
        element("WrappedArgumentExpression", Expression, wrappedExpression)
    val lambdaArgumentExpression =
        element("LambdaArgumentExpression", Expression, wrappedArgumentExpression)
    val spreadArgumentExpression =
        element("SpreadArgumentExpression", Expression, wrappedArgumentExpression)
    val namedArgumentExpression =
        element("NamedArgumentExpression", Expression, wrappedArgumentExpression)
    val varargArgumentsExpression = element("VarargArgumentsExpression", Expression, expression)

    val resolvedQualifier = element("ResolvedQualifier", Expression, expression)
    val resolvedReifiedParameterReference =
        element("ResolvedReifiedParameterReference", Expression, expression)
    val returnExpression = element("ReturnExpression", Expression, jump)
    val stringConcatenationCall = element("StringConcatenationCall", Expression, call, expression)
    val throwExpression = element("ThrowExpression", Expression, expression)
    val variableAssignment = element("VariableAssignment", Expression, qualifiedAccess)
    val whenSubjectExpression = element("WhenSubjectExpression", Expression, expression)

    val wrappedDelegateExpression =
        element("WrappedDelegateExpression", Expression, wrappedExpression)

    val namedReference = element("NamedReference", Reference, reference)
    val superReference = element("SuperReference", Reference, reference)
    val thisReference = element("ThisReference", Reference, reference)

    val resolvedNamedReference = element("ResolvedNamedReference", Reference, namedReference)
    val delegateFieldReference =
        element("DelegateFieldReference", Reference, resolvedNamedReference)
    val backingFieldReference = element("BackingFieldReference", Reference, resolvedNamedReference)

    val resolvedCallableReference =
        element("ResolvedCallableReference", Reference, resolvedNamedReference)

    val simpleType =
        element("SimpleType", Type, type)
}
