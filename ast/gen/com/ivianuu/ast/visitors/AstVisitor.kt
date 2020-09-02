package com.ivianuu.ast.visitors

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.AstLabel
import com.ivianuu.ast.AstSymbolOwner
import com.ivianuu.ast.AstVarargElement
import com.ivianuu.ast.AstTargetElement
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstAnnotatedDeclaration
import com.ivianuu.ast.declarations.AstAnonymousInitializer
import com.ivianuu.ast.declarations.AstTypedDeclaration
import com.ivianuu.ast.declarations.AstCallableDeclaration
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstTypeParametersOwner
import com.ivianuu.ast.declarations.AstMemberDeclaration
import com.ivianuu.ast.declarations.AstCallableMemberDeclaration
import com.ivianuu.ast.declarations.AstVariable
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.AstField
import com.ivianuu.ast.declarations.AstEnumEntry
import com.ivianuu.ast.declarations.AstClassLikeDeclaration
import com.ivianuu.ast.declarations.AstClass
import com.ivianuu.ast.declarations.AstRegularClass
import com.ivianuu.ast.declarations.AstTypeAlias
import com.ivianuu.ast.declarations.AstFunction
import com.ivianuu.ast.declarations.AstNamedFunction
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstModuleFragment
import com.ivianuu.ast.declarations.AstFile
import com.ivianuu.ast.declarations.AstAnonymousFunction
import com.ivianuu.ast.declarations.AstAnonymousObject
import com.ivianuu.ast.expressions.AstLoop
import com.ivianuu.ast.expressions.AstDoWhileLoop
import com.ivianuu.ast.expressions.AstWhileLoop
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstBinaryLogicExpression
import com.ivianuu.ast.expressions.AstJump
import com.ivianuu.ast.expressions.AstLoopJump
import com.ivianuu.ast.expressions.AstBreakExpression
import com.ivianuu.ast.expressions.AstContinueExpression
import com.ivianuu.ast.expressions.AstCatch
import com.ivianuu.ast.expressions.AstTryExpression
import com.ivianuu.ast.expressions.AstConstExpression
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.AstStarProjection
import com.ivianuu.ast.types.AstTypeProjectionWithVariance
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstComparisonExpression
import com.ivianuu.ast.expressions.AstTypeOperatorCall
import com.ivianuu.ast.expressions.AstAssignmentOperatorStatement
import com.ivianuu.ast.expressions.AstEqualityOperatorCall
import com.ivianuu.ast.expressions.AstWhenExpression
import com.ivianuu.ast.expressions.AstWhenBranch
import com.ivianuu.ast.expressions.AstElvisExpression
import com.ivianuu.ast.expressions.AstClassReference
import com.ivianuu.ast.expressions.AstQualifiedAccess
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstCallableReferenceAccess
import com.ivianuu.ast.expressions.AstThisReceiverExpression
import com.ivianuu.ast.expressions.AstExpressionWithSmartcast
import com.ivianuu.ast.expressions.AstSafeCallExpression
import com.ivianuu.ast.expressions.AstCheckedSafeCallSubject
import com.ivianuu.ast.expressions.AstGetClassCall
import com.ivianuu.ast.expressions.AstVararg
import com.ivianuu.ast.AstSpreadElement
import com.ivianuu.ast.expressions.AstReturnExpression
import com.ivianuu.ast.expressions.AstStringConcatenationCall
import com.ivianuu.ast.expressions.AstThrowExpression
import com.ivianuu.ast.expressions.AstVariableAssignment
import com.ivianuu.ast.expressions.AstWhenSubjectExpression
import com.ivianuu.ast.expressions.AstSuperReference
import com.ivianuu.ast.expressions.AstThisReference
import com.ivianuu.ast.expressions.AstBackingFieldReference
import com.ivianuu.ast.types.AstSimpleType

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstVisitor<out R, in D> {
    abstract fun visitElement(element: AstElement, data: D): R

    open fun visitAnnotationContainer(annotationContainer: AstAnnotationContainer, data: D): R  = visitElement(annotationContainer, data)

    open fun visitType(type: AstType, data: D): R  = visitElement(type, data)

    open fun visitLabel(label: AstLabel, data: D): R  = visitElement(label, data)

    open fun <E> visitSymbolOwner(symbolOwner: AstSymbolOwner<E>, data: D): R where E : AstSymbolOwner<E>, E : AstDeclaration  = visitElement(symbolOwner, data)

    open fun visitVarargElement(varargElement: AstVarargElement, data: D): R  = visitElement(varargElement, data)

    open fun visitTargetElement(targetElement: AstTargetElement, data: D): R  = visitElement(targetElement, data)

    open fun visitStatement(statement: AstStatement, data: D): R  = visitElement(statement, data)

    open fun visitExpression(expression: AstExpression, data: D): R  = visitElement(expression, data)

    open fun visitDeclaration(declaration: AstDeclaration, data: D): R  = visitElement(declaration, data)

    open fun visitAnnotatedDeclaration(annotatedDeclaration: AstAnnotatedDeclaration, data: D): R  = visitElement(annotatedDeclaration, data)

    open fun visitAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer, data: D): R  = visitElement(anonymousInitializer, data)

    open fun visitTypedDeclaration(typedDeclaration: AstTypedDeclaration, data: D): R  = visitElement(typedDeclaration, data)

    open fun <F : AstCallableDeclaration<F>> visitCallableDeclaration(callableDeclaration: AstCallableDeclaration<F>, data: D): R  = visitElement(callableDeclaration, data)

    open fun visitTypeParameter(typeParameter: AstTypeParameter, data: D): R  = visitElement(typeParameter, data)

    open fun visitTypeParametersOwner(typeParametersOwner: AstTypeParametersOwner, data: D): R  = visitElement(typeParametersOwner, data)

    open fun visitMemberDeclaration(memberDeclaration: AstMemberDeclaration, data: D): R  = visitElement(memberDeclaration, data)

    open fun <F : AstCallableMemberDeclaration<F>> visitCallableMemberDeclaration(callableMemberDeclaration: AstCallableMemberDeclaration<F>, data: D): R  = visitElement(callableMemberDeclaration, data)

    open fun <F : AstVariable<F>> visitVariable(variable: AstVariable<F>, data: D): R  = visitElement(variable, data)

    open fun visitValueParameter(valueParameter: AstValueParameter, data: D): R  = visitElement(valueParameter, data)

    open fun visitProperty(property: AstProperty, data: D): R  = visitElement(property, data)

    open fun visitField(field: AstField, data: D): R  = visitElement(field, data)

    open fun visitEnumEntry(enumEntry: AstEnumEntry, data: D): R  = visitElement(enumEntry, data)

    open fun <F : AstClassLikeDeclaration<F>> visitClassLikeDeclaration(classLikeDeclaration: AstClassLikeDeclaration<F>, data: D): R  = visitElement(classLikeDeclaration, data)

    open fun <F : AstClass<F>> visitClass(klass: AstClass<F>, data: D): R  = visitElement(klass, data)

    open fun visitRegularClass(regularClass: AstRegularClass, data: D): R  = visitElement(regularClass, data)

    open fun visitTypeAlias(typeAlias: AstTypeAlias, data: D): R  = visitElement(typeAlias, data)

    open fun <F : AstFunction<F>> visitFunction(function: AstFunction<F>, data: D): R  = visitElement(function, data)

    open fun visitNamedFunction(namedFunction: AstNamedFunction, data: D): R  = visitElement(namedFunction, data)

    open fun visitPropertyAccessor(propertyAccessor: AstPropertyAccessor, data: D): R  = visitElement(propertyAccessor, data)

    open fun visitConstructor(constructor: AstConstructor, data: D): R  = visitElement(constructor, data)

    open fun visitModuleFragment(moduleFragment: AstModuleFragment, data: D): R  = visitElement(moduleFragment, data)

    open fun visitFile(file: AstFile, data: D): R  = visitElement(file, data)

    open fun visitAnonymousFunction(anonymousFunction: AstAnonymousFunction, data: D): R  = visitElement(anonymousFunction, data)

    open fun visitAnonymousObject(anonymousObject: AstAnonymousObject, data: D): R  = visitElement(anonymousObject, data)

    open fun visitLoop(loop: AstLoop, data: D): R  = visitElement(loop, data)

    open fun visitDoWhileLoop(doWhileLoop: AstDoWhileLoop, data: D): R  = visitElement(doWhileLoop, data)

    open fun visitWhileLoop(whileLoop: AstWhileLoop, data: D): R  = visitElement(whileLoop, data)

    open fun visitBlock(block: AstBlock, data: D): R  = visitElement(block, data)

    open fun visitBinaryLogicExpression(binaryLogicExpression: AstBinaryLogicExpression, data: D): R  = visitElement(binaryLogicExpression, data)

    open fun <E : AstTargetElement> visitJump(jump: AstJump<E>, data: D): R  = visitElement(jump, data)

    open fun visitLoopJump(loopJump: AstLoopJump, data: D): R  = visitElement(loopJump, data)

    open fun visitBreakExpression(breakExpression: AstBreakExpression, data: D): R  = visitElement(breakExpression, data)

    open fun visitContinueExpression(continueExpression: AstContinueExpression, data: D): R  = visitElement(continueExpression, data)

    open fun visitCatch(catch: AstCatch, data: D): R  = visitElement(catch, data)

    open fun visitTryExpression(tryExpression: AstTryExpression, data: D): R  = visitElement(tryExpression, data)

    open fun <T> visitConstExpression(constExpression: AstConstExpression<T>, data: D): R  = visitElement(constExpression, data)

    open fun visitTypeProjection(typeProjection: AstTypeProjection, data: D): R  = visitElement(typeProjection, data)

    open fun visitStarProjection(starProjection: AstStarProjection, data: D): R  = visitElement(starProjection, data)

    open fun visitTypeProjectionWithVariance(typeProjectionWithVariance: AstTypeProjectionWithVariance, data: D): R  = visitElement(typeProjectionWithVariance, data)

    open fun visitCall(call: AstCall, data: D): R  = visitElement(call, data)

    open fun visitComparisonExpression(comparisonExpression: AstComparisonExpression, data: D): R  = visitElement(comparisonExpression, data)

    open fun visitTypeOperatorCall(typeOperatorCall: AstTypeOperatorCall, data: D): R  = visitElement(typeOperatorCall, data)

    open fun visitAssignmentOperatorStatement(assignmentOperatorStatement: AstAssignmentOperatorStatement, data: D): R  = visitElement(assignmentOperatorStatement, data)

    open fun visitEqualityOperatorCall(equalityOperatorCall: AstEqualityOperatorCall, data: D): R  = visitElement(equalityOperatorCall, data)

    open fun visitWhenExpression(whenExpression: AstWhenExpression, data: D): R  = visitElement(whenExpression, data)

    open fun visitWhenBranch(whenBranch: AstWhenBranch, data: D): R  = visitElement(whenBranch, data)

    open fun visitElvisExpression(elvisExpression: AstElvisExpression, data: D): R  = visitElement(elvisExpression, data)

    open fun visitClassReference(classReference: AstClassReference, data: D): R  = visitElement(classReference, data)

    open fun visitQualifiedAccess(qualifiedAccess: AstQualifiedAccess, data: D): R  = visitElement(qualifiedAccess, data)

    open fun visitFunctionCall(functionCall: AstFunctionCall, data: D): R  = visitElement(functionCall, data)

    open fun visitDelegatedConstructorCall(delegatedConstructorCall: AstDelegatedConstructorCall, data: D): R  = visitElement(delegatedConstructorCall, data)

    open fun visitCallableReferenceAccess(callableReferenceAccess: AstCallableReferenceAccess, data: D): R  = visitElement(callableReferenceAccess, data)

    open fun visitThisReceiverExpression(thisReceiverExpression: AstThisReceiverExpression, data: D): R  = visitElement(thisReceiverExpression, data)

    open fun visitExpressionWithSmartcast(expressionWithSmartcast: AstExpressionWithSmartcast, data: D): R  = visitElement(expressionWithSmartcast, data)

    open fun visitSafeCallExpression(safeCallExpression: AstSafeCallExpression, data: D): R  = visitElement(safeCallExpression, data)

    open fun visitCheckedSafeCallSubject(checkedSafeCallSubject: AstCheckedSafeCallSubject, data: D): R  = visitElement(checkedSafeCallSubject, data)

    open fun visitGetClassCall(getClassCall: AstGetClassCall, data: D): R  = visitElement(getClassCall, data)

    open fun visitVararg(vararg: AstVararg, data: D): R  = visitElement(vararg, data)

    open fun visitSpreadElement(spreadElement: AstSpreadElement, data: D): R  = visitElement(spreadElement, data)

    open fun visitReturnExpression(returnExpression: AstReturnExpression, data: D): R  = visitElement(returnExpression, data)

    open fun visitStringConcatenationCall(stringConcatenationCall: AstStringConcatenationCall, data: D): R  = visitElement(stringConcatenationCall, data)

    open fun visitThrowExpression(throwExpression: AstThrowExpression, data: D): R  = visitElement(throwExpression, data)

    open fun visitVariableAssignment(variableAssignment: AstVariableAssignment, data: D): R  = visitElement(variableAssignment, data)

    open fun visitWhenSubjectExpression(whenSubjectExpression: AstWhenSubjectExpression, data: D): R  = visitElement(whenSubjectExpression, data)

    open fun visitSuperReference(superReference: AstSuperReference, data: D): R  = visitElement(superReference, data)

    open fun visitThisReference(thisReference: AstThisReference, data: D): R  = visitElement(thisReference, data)

    open fun visitBackingFieldReference(backingFieldReference: AstBackingFieldReference, data: D): R  = visitElement(backingFieldReference, data)

    open fun visitSimpleType(simpleType: AstSimpleType, data: D): R  = visitElement(simpleType, data)

}
