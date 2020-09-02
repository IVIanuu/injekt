package com.ivianuu.ast.visitors

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.AstSymbolOwner
import com.ivianuu.ast.AstVarargElement
import com.ivianuu.ast.AstTargetElement
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstAnonymousInitializer
import com.ivianuu.ast.declarations.AstCallableDeclaration
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstTypeParametersOwner
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
import com.ivianuu.ast.expressions.AstBinaryLogicOperation
import com.ivianuu.ast.expressions.AstLoopJump
import com.ivianuu.ast.expressions.AstBreak
import com.ivianuu.ast.expressions.AstContinue
import com.ivianuu.ast.expressions.AstCatch
import com.ivianuu.ast.expressions.AstTry
import com.ivianuu.ast.expressions.AstConst
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.AstStarProjection
import com.ivianuu.ast.types.AstTypeProjectionWithVariance
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstComparisonOperation
import com.ivianuu.ast.expressions.AstTypeOperation
import com.ivianuu.ast.expressions.AstAssignmentOperatorStatement
import com.ivianuu.ast.expressions.AstEqualityOperation
import com.ivianuu.ast.expressions.AstWhen
import com.ivianuu.ast.expressions.AstWhenBranch
import com.ivianuu.ast.expressions.AstClassReference
import com.ivianuu.ast.expressions.AstQualifiedAccess
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstCallableReference
import com.ivianuu.ast.expressions.AstVararg
import com.ivianuu.ast.AstSpreadElement
import com.ivianuu.ast.expressions.AstReturn
import com.ivianuu.ast.expressions.AstThrow
import com.ivianuu.ast.expressions.AstVariableAssignment
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

    open fun <E> visitSymbolOwner(symbolOwner: AstSymbolOwner<E>, data: D): R where E : AstSymbolOwner<E>, E : AstDeclaration  = visitElement(symbolOwner, data)

    open fun visitVarargElement(varargElement: AstVarargElement, data: D): R  = visitElement(varargElement, data)

    open fun visitTargetElement(targetElement: AstTargetElement, data: D): R  = visitElement(targetElement, data)

    open fun visitStatement(statement: AstStatement, data: D): R  = visitElement(statement, data)

    open fun visitExpression(expression: AstExpression, data: D): R  = visitStatement(expression, data)

    open fun visitDeclaration(declaration: AstDeclaration, data: D): R  = visitStatement(declaration, data)

    open fun visitAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer, data: D): R  = visitDeclaration(anonymousInitializer, data)

    open fun <F : AstCallableDeclaration<F>> visitCallableDeclaration(callableDeclaration: AstCallableDeclaration<F>, data: D): R  = visitDeclaration(callableDeclaration, data)

    open fun visitTypeParameter(typeParameter: AstTypeParameter, data: D): R  = visitDeclaration(typeParameter, data)

    open fun visitTypeParametersOwner(typeParametersOwner: AstTypeParametersOwner, data: D): R  = visitElement(typeParametersOwner, data)

    open fun <F : AstVariable<F>> visitVariable(variable: AstVariable<F>, data: D): R  = visitDeclaration(variable, data)

    open fun visitValueParameter(valueParameter: AstValueParameter, data: D): R  = visitVariable(valueParameter, data)

    open fun visitProperty(property: AstProperty, data: D): R  = visitVariable(property, data)

    open fun visitField(field: AstField, data: D): R  = visitVariable(field, data)

    open fun visitEnumEntry(enumEntry: AstEnumEntry, data: D): R  = visitVariable(enumEntry, data)

    open fun <F : AstClassLikeDeclaration<F>> visitClassLikeDeclaration(classLikeDeclaration: AstClassLikeDeclaration<F>, data: D): R  = visitDeclaration(classLikeDeclaration, data)

    open fun <F : AstClass<F>> visitClass(klass: AstClass<F>, data: D): R  = visitClassLikeDeclaration(klass, data)

    open fun visitRegularClass(regularClass: AstRegularClass, data: D): R  = visitClass(regularClass, data)

    open fun visitTypeAlias(typeAlias: AstTypeAlias, data: D): R  = visitClassLikeDeclaration(typeAlias, data)

    open fun <F : AstFunction<F>> visitFunction(function: AstFunction<F>, data: D): R  = visitDeclaration(function, data)

    open fun visitNamedFunction(namedFunction: AstNamedFunction, data: D): R  = visitFunction(namedFunction, data)

    open fun visitPropertyAccessor(propertyAccessor: AstPropertyAccessor, data: D): R  = visitFunction(propertyAccessor, data)

    open fun visitConstructor(constructor: AstConstructor, data: D): R  = visitFunction(constructor, data)

    open fun visitModuleFragment(moduleFragment: AstModuleFragment, data: D): R  = visitElement(moduleFragment, data)

    open fun visitFile(file: AstFile, data: D): R  = visitElement(file, data)

    open fun visitAnonymousFunction(anonymousFunction: AstAnonymousFunction, data: D): R  = visitFunction(anonymousFunction, data)

    open fun visitAnonymousObject(anonymousObject: AstAnonymousObject, data: D): R  = visitClass(anonymousObject, data)

    open fun visitLoop(loop: AstLoop, data: D): R  = visitExpression(loop, data)

    open fun visitDoWhileLoop(doWhileLoop: AstDoWhileLoop, data: D): R  = visitLoop(doWhileLoop, data)

    open fun visitWhileLoop(whileLoop: AstWhileLoop, data: D): R  = visitLoop(whileLoop, data)

    open fun visitBlock(block: AstBlock, data: D): R  = visitExpression(block, data)

    open fun visitBinaryLogicOperation(binaryLogicOperation: AstBinaryLogicOperation, data: D): R  = visitExpression(binaryLogicOperation, data)

    open fun visitLoopJump(loopJump: AstLoopJump, data: D): R  = visitExpression(loopJump, data)

    open fun visitBreak(breakExpression: AstBreak, data: D): R  = visitLoopJump(breakExpression, data)

    open fun visitContinue(continueExpression: AstContinue, data: D): R  = visitLoopJump(continueExpression, data)

    open fun visitCatch(catch: AstCatch, data: D): R  = visitElement(catch, data)

    open fun visitTry(tryExpression: AstTry, data: D): R  = visitExpression(tryExpression, data)

    open fun <T> visitConst(const: AstConst<T>, data: D): R  = visitExpression(const, data)

    open fun visitTypeProjection(typeProjection: AstTypeProjection, data: D): R  = visitElement(typeProjection, data)

    open fun visitStarProjection(starProjection: AstStarProjection, data: D): R  = visitTypeProjection(starProjection, data)

    open fun visitTypeProjectionWithVariance(typeProjectionWithVariance: AstTypeProjectionWithVariance, data: D): R  = visitTypeProjection(typeProjectionWithVariance, data)

    open fun visitCall(call: AstCall, data: D): R  = visitExpression(call, data)

    open fun visitComparisonOperation(comparisonOperation: AstComparisonOperation, data: D): R  = visitExpression(comparisonOperation, data)

    open fun visitTypeOperation(typeOperation: AstTypeOperation, data: D): R  = visitExpression(typeOperation, data)

    open fun visitAssignmentOperatorStatement(assignmentOperatorStatement: AstAssignmentOperatorStatement, data: D): R  = visitStatement(assignmentOperatorStatement, data)

    open fun visitEqualityOperation(equalityOperation: AstEqualityOperation, data: D): R  = visitExpression(equalityOperation, data)

    open fun visitWhen(whenExpression: AstWhen, data: D): R  = visitExpression(whenExpression, data)

    open fun visitWhenBranch(whenBranch: AstWhenBranch, data: D): R  = visitElement(whenBranch, data)

    open fun visitClassReference(classReference: AstClassReference, data: D): R  = visitExpression(classReference, data)

    open fun visitQualifiedAccess(qualifiedAccess: AstQualifiedAccess, data: D): R  = visitExpression(qualifiedAccess, data)

    open fun visitFunctionCall(functionCall: AstFunctionCall, data: D): R  = visitQualifiedAccess(functionCall, data)

    open fun visitDelegatedConstructorCall(delegatedConstructorCall: AstDelegatedConstructorCall, data: D): R  = visitCall(delegatedConstructorCall, data)

    open fun visitCallableReference(callableReference: AstCallableReference, data: D): R  = visitQualifiedAccess(callableReference, data)

    open fun visitVararg(vararg: AstVararg, data: D): R  = visitExpression(vararg, data)

    open fun visitSpreadElement(spreadElement: AstSpreadElement, data: D): R  = visitVarargElement(spreadElement, data)

    open fun visitReturn(returnExpression: AstReturn, data: D): R  = visitExpression(returnExpression, data)

    open fun visitThrow(throwExpression: AstThrow, data: D): R  = visitExpression(throwExpression, data)

    open fun visitVariableAssignment(variableAssignment: AstVariableAssignment, data: D): R  = visitQualifiedAccess(variableAssignment, data)

    open fun visitSuperReference(superReference: AstSuperReference, data: D): R  = visitExpression(superReference, data)

    open fun visitThisReference(thisReference: AstThisReference, data: D): R  = visitExpression(thisReference, data)

    open fun visitBackingFieldReference(backingFieldReference: AstBackingFieldReference, data: D): R  = visitExpression(backingFieldReference, data)

    open fun visitSimpleType(simpleType: AstSimpleType, data: D): R  = visitType(simpleType, data)

}
