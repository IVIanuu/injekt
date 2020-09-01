package com.ivianuu.ast.visitors

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.AstLabel
import com.ivianuu.ast.AstSymbolOwner
import com.ivianuu.ast.expressions.AstResolvable
import com.ivianuu.ast.AstTargetElement
import com.ivianuu.ast.declarations.AstDeclarationStatus
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstAnnotatedDeclaration
import com.ivianuu.ast.declarations.AstAnonymousInitializer
import com.ivianuu.ast.declarations.AstTypedDeclaration
import com.ivianuu.ast.declarations.AstCallableDeclaration
import com.ivianuu.ast.declarations.AstTypeParameterRef
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstTypeParameterRefsOwner
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
import com.ivianuu.ast.declarations.AstSimpleFunction
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstConstructor
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
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstComparisonExpression
import com.ivianuu.ast.expressions.AstTypeOperatorCall
import com.ivianuu.ast.expressions.AstAssignmentOperatorStatement
import com.ivianuu.ast.expressions.AstEqualityOperatorCall
import com.ivianuu.ast.expressions.AstWhenExpression
import com.ivianuu.ast.expressions.AstWhenBranch
import com.ivianuu.ast.expressions.AstQualifiedAccess
import com.ivianuu.ast.expressions.AstElvisExpression
import com.ivianuu.ast.expressions.AstClassReferenceExpression
import com.ivianuu.ast.expressions.AstQualifiedAccessExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstCallableReferenceAccess
import com.ivianuu.ast.expressions.AstThisReceiverExpression
import com.ivianuu.ast.expressions.AstExpressionWithSmartcast
import com.ivianuu.ast.expressions.AstSafeCallExpression
import com.ivianuu.ast.expressions.AstCheckedSafeCallSubject
import com.ivianuu.ast.expressions.AstGetClassCall
import com.ivianuu.ast.expressions.AstWrappedExpression
import com.ivianuu.ast.expressions.AstWrappedArgumentExpression
import com.ivianuu.ast.expressions.AstLambdaArgumentExpression
import com.ivianuu.ast.expressions.AstSpreadArgumentExpression
import com.ivianuu.ast.expressions.AstNamedArgumentExpression
import com.ivianuu.ast.expressions.AstVarargArgumentsExpression
import com.ivianuu.ast.expressions.AstResolvedQualifier
import com.ivianuu.ast.expressions.AstResolvedReifiedParameterReference
import com.ivianuu.ast.expressions.AstReturnExpression
import com.ivianuu.ast.expressions.AstStringConcatenationCall
import com.ivianuu.ast.expressions.AstThrowExpression
import com.ivianuu.ast.expressions.AstVariableAssignment
import com.ivianuu.ast.expressions.AstWhenSubjectExpression
import com.ivianuu.ast.expressions.AstWrappedDelegateExpression
import com.ivianuu.ast.references.AstNamedReference
import com.ivianuu.ast.references.AstSuperReference
import com.ivianuu.ast.references.AstThisReference
import com.ivianuu.ast.references.AstResolvedNamedReference
import com.ivianuu.ast.references.AstDelegateFieldReference
import com.ivianuu.ast.references.AstBackingFieldReference
import com.ivianuu.ast.references.AstResolvedCallableReference
import com.ivianuu.ast.types.AstSimpleType

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstVisitorVoid : AstVisitor<Unit, Nothing?>() {
    abstract fun visitElement(element: AstElement)

    open fun visitAnnotationContainer(annotationContainer: AstAnnotationContainer) {
        visitElement(annotationContainer)
    }

    open fun visitType(type: AstType) {
        visitElement(type)
    }

    open fun visitReference(reference: AstReference) {
        visitElement(reference)
    }

    open fun visitLabel(label: AstLabel) {
        visitElement(label)
    }

    open fun <E> visitSymbolOwner(symbolOwner: AstSymbolOwner<E>) where E : AstSymbolOwner<E>, E : AstDeclaration {
        visitElement(symbolOwner)
    }

    open fun visitResolvable(resolvable: AstResolvable) {
        visitElement(resolvable)
    }

    open fun visitTargetElement(targetElement: AstTargetElement) {
        visitElement(targetElement)
    }

    open fun visitDeclarationStatus(declarationStatus: AstDeclarationStatus) {
        visitElement(declarationStatus)
    }

    open fun visitStatement(statement: AstStatement) {
        visitElement(statement)
    }

    open fun visitExpression(expression: AstExpression) {
        visitElement(expression)
    }

    open fun visitDeclaration(declaration: AstDeclaration) {
        visitElement(declaration)
    }

    open fun visitAnnotatedDeclaration(annotatedDeclaration: AstAnnotatedDeclaration) {
        visitElement(annotatedDeclaration)
    }

    open fun visitAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer) {
        visitElement(anonymousInitializer)
    }

    open fun visitTypedDeclaration(typedDeclaration: AstTypedDeclaration) {
        visitElement(typedDeclaration)
    }

    open fun <F : AstCallableDeclaration<F>> visitCallableDeclaration(callableDeclaration: AstCallableDeclaration<F>) {
        visitElement(callableDeclaration)
    }

    open fun visitTypeParameterRef(typeParameterRef: AstTypeParameterRef) {
        visitElement(typeParameterRef)
    }

    open fun visitTypeParameter(typeParameter: AstTypeParameter) {
        visitElement(typeParameter)
    }

    open fun visitTypeParameterRefsOwner(typeParameterRefsOwner: AstTypeParameterRefsOwner) {
        visitElement(typeParameterRefsOwner)
    }

    open fun visitTypeParametersOwner(typeParametersOwner: AstTypeParametersOwner) {
        visitElement(typeParametersOwner)
    }

    open fun visitMemberDeclaration(memberDeclaration: AstMemberDeclaration) {
        visitElement(memberDeclaration)
    }

    open fun <F : AstCallableMemberDeclaration<F>> visitCallableMemberDeclaration(callableMemberDeclaration: AstCallableMemberDeclaration<F>) {
        visitElement(callableMemberDeclaration)
    }

    open fun <F : AstVariable<F>> visitVariable(variable: AstVariable<F>) {
        visitElement(variable)
    }

    open fun visitValueParameter(valueParameter: AstValueParameter) {
        visitElement(valueParameter)
    }

    open fun visitProperty(property: AstProperty) {
        visitElement(property)
    }

    open fun visitField(field: AstField) {
        visitElement(field)
    }

    open fun visitEnumEntry(enumEntry: AstEnumEntry) {
        visitElement(enumEntry)
    }

    open fun <F : AstClassLikeDeclaration<F>> visitClassLikeDeclaration(classLikeDeclaration: AstClassLikeDeclaration<F>) {
        visitElement(classLikeDeclaration)
    }

    open fun <F : AstClass<F>> visitClass(klass: AstClass<F>) {
        visitElement(klass)
    }

    open fun visitRegularClass(regularClass: AstRegularClass) {
        visitElement(regularClass)
    }

    open fun visitTypeAlias(typeAlias: AstTypeAlias) {
        visitElement(typeAlias)
    }

    open fun <F : AstFunction<F>> visitFunction(function: AstFunction<F>) {
        visitElement(function)
    }

    open fun visitSimpleFunction(simpleFunction: AstSimpleFunction) {
        visitElement(simpleFunction)
    }

    open fun visitPropertyAccessor(propertyAccessor: AstPropertyAccessor) {
        visitElement(propertyAccessor)
    }

    open fun visitConstructor(constructor: AstConstructor) {
        visitElement(constructor)
    }

    open fun visitFile(file: AstFile) {
        visitElement(file)
    }

    open fun visitAnonymousFunction(anonymousFunction: AstAnonymousFunction) {
        visitElement(anonymousFunction)
    }

    open fun visitAnonymousObject(anonymousObject: AstAnonymousObject) {
        visitElement(anonymousObject)
    }

    open fun visitLoop(loop: AstLoop) {
        visitElement(loop)
    }

    open fun visitDoWhileLoop(doWhileLoop: AstDoWhileLoop) {
        visitElement(doWhileLoop)
    }

    open fun visitWhileLoop(whileLoop: AstWhileLoop) {
        visitElement(whileLoop)
    }

    open fun visitBlock(block: AstBlock) {
        visitElement(block)
    }

    open fun visitBinaryLogicExpression(binaryLogicExpression: AstBinaryLogicExpression) {
        visitElement(binaryLogicExpression)
    }

    open fun <E : AstTargetElement> visitJump(jump: AstJump<E>) {
        visitElement(jump)
    }

    open fun visitLoopJump(loopJump: AstLoopJump) {
        visitElement(loopJump)
    }

    open fun visitBreakExpression(breakExpression: AstBreakExpression) {
        visitElement(breakExpression)
    }

    open fun visitContinueExpression(continueExpression: AstContinueExpression) {
        visitElement(continueExpression)
    }

    open fun visitCatch(catch: AstCatch) {
        visitElement(catch)
    }

    open fun visitTryExpression(tryExpression: AstTryExpression) {
        visitElement(tryExpression)
    }

    open fun <T> visitConstExpression(constExpression: AstConstExpression<T>) {
        visitElement(constExpression)
    }

    open fun visitTypeProjection(typeProjection: AstTypeProjection) {
        visitElement(typeProjection)
    }

    open fun visitStarProjection(starProjection: AstStarProjection) {
        visitElement(starProjection)
    }

    open fun visitTypeProjectionWithVariance(typeProjectionWithVariance: AstTypeProjectionWithVariance) {
        visitElement(typeProjectionWithVariance)
    }

    open fun visitArgumentList(argumentList: AstArgumentList) {
        visitElement(argumentList)
    }

    open fun visitCall(call: AstCall) {
        visitElement(call)
    }

    open fun visitAnnotationCall(annotationCall: AstAnnotationCall) {
        visitElement(annotationCall)
    }

    open fun visitComparisonExpression(comparisonExpression: AstComparisonExpression) {
        visitElement(comparisonExpression)
    }

    open fun visitTypeOperatorCall(typeOperatorCall: AstTypeOperatorCall) {
        visitElement(typeOperatorCall)
    }

    open fun visitAssignmentOperatorStatement(assignmentOperatorStatement: AstAssignmentOperatorStatement) {
        visitElement(assignmentOperatorStatement)
    }

    open fun visitEqualityOperatorCall(equalityOperatorCall: AstEqualityOperatorCall) {
        visitElement(equalityOperatorCall)
    }

    open fun visitWhenExpression(whenExpression: AstWhenExpression) {
        visitElement(whenExpression)
    }

    open fun visitWhenBranch(whenBranch: AstWhenBranch) {
        visitElement(whenBranch)
    }

    open fun visitQualifiedAccess(qualifiedAccess: AstQualifiedAccess) {
        visitElement(qualifiedAccess)
    }

    open fun visitElvisExpression(elvisExpression: AstElvisExpression) {
        visitElement(elvisExpression)
    }

    open fun visitClassReferenceExpression(classReferenceExpression: AstClassReferenceExpression) {
        visitElement(classReferenceExpression)
    }

    open fun visitQualifiedAccessExpression(qualifiedAccessExpression: AstQualifiedAccessExpression) {
        visitElement(qualifiedAccessExpression)
    }

    open fun visitFunctionCall(functionCall: AstFunctionCall) {
        visitElement(functionCall)
    }

    open fun visitDelegatedConstructorCall(delegatedConstructorCall: AstDelegatedConstructorCall) {
        visitElement(delegatedConstructorCall)
    }

    open fun visitCallableReferenceAccess(callableReferenceAccess: AstCallableReferenceAccess) {
        visitElement(callableReferenceAccess)
    }

    open fun visitThisReceiverExpression(thisReceiverExpression: AstThisReceiverExpression) {
        visitElement(thisReceiverExpression)
    }

    open fun visitExpressionWithSmartcast(expressionWithSmartcast: AstExpressionWithSmartcast) {
        visitElement(expressionWithSmartcast)
    }

    open fun visitSafeCallExpression(safeCallExpression: AstSafeCallExpression) {
        visitElement(safeCallExpression)
    }

    open fun visitCheckedSafeCallSubject(checkedSafeCallSubject: AstCheckedSafeCallSubject) {
        visitElement(checkedSafeCallSubject)
    }

    open fun visitGetClassCall(getClassCall: AstGetClassCall) {
        visitElement(getClassCall)
    }

    open fun visitWrappedExpression(wrappedExpression: AstWrappedExpression) {
        visitElement(wrappedExpression)
    }

    open fun visitWrappedArgumentExpression(wrappedArgumentExpression: AstWrappedArgumentExpression) {
        visitElement(wrappedArgumentExpression)
    }

    open fun visitLambdaArgumentExpression(lambdaArgumentExpression: AstLambdaArgumentExpression) {
        visitElement(lambdaArgumentExpression)
    }

    open fun visitSpreadArgumentExpression(spreadArgumentExpression: AstSpreadArgumentExpression) {
        visitElement(spreadArgumentExpression)
    }

    open fun visitNamedArgumentExpression(namedArgumentExpression: AstNamedArgumentExpression) {
        visitElement(namedArgumentExpression)
    }

    open fun visitVarargArgumentsExpression(varargArgumentsExpression: AstVarargArgumentsExpression) {
        visitElement(varargArgumentsExpression)
    }

    open fun visitResolvedQualifier(resolvedQualifier: AstResolvedQualifier) {
        visitElement(resolvedQualifier)
    }

    open fun visitResolvedReifiedParameterReference(resolvedReifiedParameterReference: AstResolvedReifiedParameterReference) {
        visitElement(resolvedReifiedParameterReference)
    }

    open fun visitReturnExpression(returnExpression: AstReturnExpression) {
        visitElement(returnExpression)
    }

    open fun visitStringConcatenationCall(stringConcatenationCall: AstStringConcatenationCall) {
        visitElement(stringConcatenationCall)
    }

    open fun visitThrowExpression(throwExpression: AstThrowExpression) {
        visitElement(throwExpression)
    }

    open fun visitVariableAssignment(variableAssignment: AstVariableAssignment) {
        visitElement(variableAssignment)
    }

    open fun visitWhenSubjectExpression(whenSubjectExpression: AstWhenSubjectExpression) {
        visitElement(whenSubjectExpression)
    }

    open fun visitWrappedDelegateExpression(wrappedDelegateExpression: AstWrappedDelegateExpression) {
        visitElement(wrappedDelegateExpression)
    }

    open fun visitNamedReference(namedReference: AstNamedReference) {
        visitElement(namedReference)
    }

    open fun visitSuperReference(superReference: AstSuperReference) {
        visitElement(superReference)
    }

    open fun visitThisReference(thisReference: AstThisReference) {
        visitElement(thisReference)
    }

    open fun visitResolvedNamedReference(resolvedNamedReference: AstResolvedNamedReference) {
        visitElement(resolvedNamedReference)
    }

    open fun visitDelegateFieldReference(delegateFieldReference: AstDelegateFieldReference) {
        visitElement(delegateFieldReference)
    }

    open fun visitBackingFieldReference(backingFieldReference: AstBackingFieldReference) {
        visitElement(backingFieldReference)
    }

    open fun visitResolvedCallableReference(resolvedCallableReference: AstResolvedCallableReference) {
        visitElement(resolvedCallableReference)
    }

    open fun visitSimpleType(simpleType: AstSimpleType) {
        visitElement(simpleType)
    }

    final override fun visitElement(element: AstElement, data: Nothing?) {
        visitElement(element)
    }

    final override fun visitAnnotationContainer(annotationContainer: AstAnnotationContainer, data: Nothing?) {
        visitAnnotationContainer(annotationContainer)
    }

    final override fun visitType(type: AstType, data: Nothing?) {
        visitType(type)
    }

    final override fun visitReference(reference: AstReference, data: Nothing?) {
        visitReference(reference)
    }

    final override fun visitLabel(label: AstLabel, data: Nothing?) {
        visitLabel(label)
    }

    final override fun <E> visitSymbolOwner(symbolOwner: AstSymbolOwner<E>, data: Nothing?) where E : AstSymbolOwner<E>, E : AstDeclaration {
        visitSymbolOwner(symbolOwner)
    }

    final override fun visitResolvable(resolvable: AstResolvable, data: Nothing?) {
        visitResolvable(resolvable)
    }

    final override fun visitTargetElement(targetElement: AstTargetElement, data: Nothing?) {
        visitTargetElement(targetElement)
    }

    final override fun visitDeclarationStatus(declarationStatus: AstDeclarationStatus, data: Nothing?) {
        visitDeclarationStatus(declarationStatus)
    }

    final override fun visitStatement(statement: AstStatement, data: Nothing?) {
        visitStatement(statement)
    }

    final override fun visitExpression(expression: AstExpression, data: Nothing?) {
        visitExpression(expression)
    }

    final override fun visitDeclaration(declaration: AstDeclaration, data: Nothing?) {
        visitDeclaration(declaration)
    }

    final override fun visitAnnotatedDeclaration(annotatedDeclaration: AstAnnotatedDeclaration, data: Nothing?) {
        visitAnnotatedDeclaration(annotatedDeclaration)
    }

    final override fun visitAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer, data: Nothing?) {
        visitAnonymousInitializer(anonymousInitializer)
    }

    final override fun visitTypedDeclaration(typedDeclaration: AstTypedDeclaration, data: Nothing?) {
        visitTypedDeclaration(typedDeclaration)
    }

    final override fun <F : AstCallableDeclaration<F>> visitCallableDeclaration(callableDeclaration: AstCallableDeclaration<F>, data: Nothing?) {
        visitCallableDeclaration(callableDeclaration)
    }

    final override fun visitTypeParameterRef(typeParameterRef: AstTypeParameterRef, data: Nothing?) {
        visitTypeParameterRef(typeParameterRef)
    }

    final override fun visitTypeParameter(typeParameter: AstTypeParameter, data: Nothing?) {
        visitTypeParameter(typeParameter)
    }

    final override fun visitTypeParameterRefsOwner(typeParameterRefsOwner: AstTypeParameterRefsOwner, data: Nothing?) {
        visitTypeParameterRefsOwner(typeParameterRefsOwner)
    }

    final override fun visitTypeParametersOwner(typeParametersOwner: AstTypeParametersOwner, data: Nothing?) {
        visitTypeParametersOwner(typeParametersOwner)
    }

    final override fun visitMemberDeclaration(memberDeclaration: AstMemberDeclaration, data: Nothing?) {
        visitMemberDeclaration(memberDeclaration)
    }

    final override fun <F : AstCallableMemberDeclaration<F>> visitCallableMemberDeclaration(callableMemberDeclaration: AstCallableMemberDeclaration<F>, data: Nothing?) {
        visitCallableMemberDeclaration(callableMemberDeclaration)
    }

    final override fun <F : AstVariable<F>> visitVariable(variable: AstVariable<F>, data: Nothing?) {
        visitVariable(variable)
    }

    final override fun visitValueParameter(valueParameter: AstValueParameter, data: Nothing?) {
        visitValueParameter(valueParameter)
    }

    final override fun visitProperty(property: AstProperty, data: Nothing?) {
        visitProperty(property)
    }

    final override fun visitField(field: AstField, data: Nothing?) {
        visitField(field)
    }

    final override fun visitEnumEntry(enumEntry: AstEnumEntry, data: Nothing?) {
        visitEnumEntry(enumEntry)
    }

    final override fun <F : AstClassLikeDeclaration<F>> visitClassLikeDeclaration(classLikeDeclaration: AstClassLikeDeclaration<F>, data: Nothing?) {
        visitClassLikeDeclaration(classLikeDeclaration)
    }

    final override fun <F : AstClass<F>> visitClass(klass: AstClass<F>, data: Nothing?) {
        visitClass(klass)
    }

    final override fun visitRegularClass(regularClass: AstRegularClass, data: Nothing?) {
        visitRegularClass(regularClass)
    }

    final override fun visitTypeAlias(typeAlias: AstTypeAlias, data: Nothing?) {
        visitTypeAlias(typeAlias)
    }

    final override fun <F : AstFunction<F>> visitFunction(function: AstFunction<F>, data: Nothing?) {
        visitFunction(function)
    }

    final override fun visitSimpleFunction(simpleFunction: AstSimpleFunction, data: Nothing?) {
        visitSimpleFunction(simpleFunction)
    }

    final override fun visitPropertyAccessor(propertyAccessor: AstPropertyAccessor, data: Nothing?) {
        visitPropertyAccessor(propertyAccessor)
    }

    final override fun visitConstructor(constructor: AstConstructor, data: Nothing?) {
        visitConstructor(constructor)
    }

    final override fun visitFile(file: AstFile, data: Nothing?) {
        visitFile(file)
    }

    final override fun visitAnonymousFunction(anonymousFunction: AstAnonymousFunction, data: Nothing?) {
        visitAnonymousFunction(anonymousFunction)
    }

    final override fun visitAnonymousObject(anonymousObject: AstAnonymousObject, data: Nothing?) {
        visitAnonymousObject(anonymousObject)
    }

    final override fun visitLoop(loop: AstLoop, data: Nothing?) {
        visitLoop(loop)
    }

    final override fun visitDoWhileLoop(doWhileLoop: AstDoWhileLoop, data: Nothing?) {
        visitDoWhileLoop(doWhileLoop)
    }

    final override fun visitWhileLoop(whileLoop: AstWhileLoop, data: Nothing?) {
        visitWhileLoop(whileLoop)
    }

    final override fun visitBlock(block: AstBlock, data: Nothing?) {
        visitBlock(block)
    }

    final override fun visitBinaryLogicExpression(binaryLogicExpression: AstBinaryLogicExpression, data: Nothing?) {
        visitBinaryLogicExpression(binaryLogicExpression)
    }

    final override fun <E : AstTargetElement> visitJump(jump: AstJump<E>, data: Nothing?) {
        visitJump(jump)
    }

    final override fun visitLoopJump(loopJump: AstLoopJump, data: Nothing?) {
        visitLoopJump(loopJump)
    }

    final override fun visitBreakExpression(breakExpression: AstBreakExpression, data: Nothing?) {
        visitBreakExpression(breakExpression)
    }

    final override fun visitContinueExpression(continueExpression: AstContinueExpression, data: Nothing?) {
        visitContinueExpression(continueExpression)
    }

    final override fun visitCatch(catch: AstCatch, data: Nothing?) {
        visitCatch(catch)
    }

    final override fun visitTryExpression(tryExpression: AstTryExpression, data: Nothing?) {
        visitTryExpression(tryExpression)
    }

    final override fun <T> visitConstExpression(constExpression: AstConstExpression<T>, data: Nothing?) {
        visitConstExpression(constExpression)
    }

    final override fun visitTypeProjection(typeProjection: AstTypeProjection, data: Nothing?) {
        visitTypeProjection(typeProjection)
    }

    final override fun visitStarProjection(starProjection: AstStarProjection, data: Nothing?) {
        visitStarProjection(starProjection)
    }

    final override fun visitTypeProjectionWithVariance(typeProjectionWithVariance: AstTypeProjectionWithVariance, data: Nothing?) {
        visitTypeProjectionWithVariance(typeProjectionWithVariance)
    }

    final override fun visitArgumentList(argumentList: AstArgumentList, data: Nothing?) {
        visitArgumentList(argumentList)
    }

    final override fun visitCall(call: AstCall, data: Nothing?) {
        visitCall(call)
    }

    final override fun visitAnnotationCall(annotationCall: AstAnnotationCall, data: Nothing?) {
        visitAnnotationCall(annotationCall)
    }

    final override fun visitComparisonExpression(comparisonExpression: AstComparisonExpression, data: Nothing?) {
        visitComparisonExpression(comparisonExpression)
    }

    final override fun visitTypeOperatorCall(typeOperatorCall: AstTypeOperatorCall, data: Nothing?) {
        visitTypeOperatorCall(typeOperatorCall)
    }

    final override fun visitAssignmentOperatorStatement(assignmentOperatorStatement: AstAssignmentOperatorStatement, data: Nothing?) {
        visitAssignmentOperatorStatement(assignmentOperatorStatement)
    }

    final override fun visitEqualityOperatorCall(equalityOperatorCall: AstEqualityOperatorCall, data: Nothing?) {
        visitEqualityOperatorCall(equalityOperatorCall)
    }

    final override fun visitWhenExpression(whenExpression: AstWhenExpression, data: Nothing?) {
        visitWhenExpression(whenExpression)
    }

    final override fun visitWhenBranch(whenBranch: AstWhenBranch, data: Nothing?) {
        visitWhenBranch(whenBranch)
    }

    final override fun visitQualifiedAccess(qualifiedAccess: AstQualifiedAccess, data: Nothing?) {
        visitQualifiedAccess(qualifiedAccess)
    }

    final override fun visitElvisExpression(elvisExpression: AstElvisExpression, data: Nothing?) {
        visitElvisExpression(elvisExpression)
    }

    final override fun visitClassReferenceExpression(classReferenceExpression: AstClassReferenceExpression, data: Nothing?) {
        visitClassReferenceExpression(classReferenceExpression)
    }

    final override fun visitQualifiedAccessExpression(qualifiedAccessExpression: AstQualifiedAccessExpression, data: Nothing?) {
        visitQualifiedAccessExpression(qualifiedAccessExpression)
    }

    final override fun visitFunctionCall(functionCall: AstFunctionCall, data: Nothing?) {
        visitFunctionCall(functionCall)
    }

    final override fun visitDelegatedConstructorCall(delegatedConstructorCall: AstDelegatedConstructorCall, data: Nothing?) {
        visitDelegatedConstructorCall(delegatedConstructorCall)
    }

    final override fun visitCallableReferenceAccess(callableReferenceAccess: AstCallableReferenceAccess, data: Nothing?) {
        visitCallableReferenceAccess(callableReferenceAccess)
    }

    final override fun visitThisReceiverExpression(thisReceiverExpression: AstThisReceiverExpression, data: Nothing?) {
        visitThisReceiverExpression(thisReceiverExpression)
    }

    final override fun visitExpressionWithSmartcast(expressionWithSmartcast: AstExpressionWithSmartcast, data: Nothing?) {
        visitExpressionWithSmartcast(expressionWithSmartcast)
    }

    final override fun visitSafeCallExpression(safeCallExpression: AstSafeCallExpression, data: Nothing?) {
        visitSafeCallExpression(safeCallExpression)
    }

    final override fun visitCheckedSafeCallSubject(checkedSafeCallSubject: AstCheckedSafeCallSubject, data: Nothing?) {
        visitCheckedSafeCallSubject(checkedSafeCallSubject)
    }

    final override fun visitGetClassCall(getClassCall: AstGetClassCall, data: Nothing?) {
        visitGetClassCall(getClassCall)
    }

    final override fun visitWrappedExpression(wrappedExpression: AstWrappedExpression, data: Nothing?) {
        visitWrappedExpression(wrappedExpression)
    }

    final override fun visitWrappedArgumentExpression(wrappedArgumentExpression: AstWrappedArgumentExpression, data: Nothing?) {
        visitWrappedArgumentExpression(wrappedArgumentExpression)
    }

    final override fun visitLambdaArgumentExpression(lambdaArgumentExpression: AstLambdaArgumentExpression, data: Nothing?) {
        visitLambdaArgumentExpression(lambdaArgumentExpression)
    }

    final override fun visitSpreadArgumentExpression(spreadArgumentExpression: AstSpreadArgumentExpression, data: Nothing?) {
        visitSpreadArgumentExpression(spreadArgumentExpression)
    }

    final override fun visitNamedArgumentExpression(namedArgumentExpression: AstNamedArgumentExpression, data: Nothing?) {
        visitNamedArgumentExpression(namedArgumentExpression)
    }

    final override fun visitVarargArgumentsExpression(varargArgumentsExpression: AstVarargArgumentsExpression, data: Nothing?) {
        visitVarargArgumentsExpression(varargArgumentsExpression)
    }

    final override fun visitResolvedQualifier(resolvedQualifier: AstResolvedQualifier, data: Nothing?) {
        visitResolvedQualifier(resolvedQualifier)
    }

    final override fun visitResolvedReifiedParameterReference(resolvedReifiedParameterReference: AstResolvedReifiedParameterReference, data: Nothing?) {
        visitResolvedReifiedParameterReference(resolvedReifiedParameterReference)
    }

    final override fun visitReturnExpression(returnExpression: AstReturnExpression, data: Nothing?) {
        visitReturnExpression(returnExpression)
    }

    final override fun visitStringConcatenationCall(stringConcatenationCall: AstStringConcatenationCall, data: Nothing?) {
        visitStringConcatenationCall(stringConcatenationCall)
    }

    final override fun visitThrowExpression(throwExpression: AstThrowExpression, data: Nothing?) {
        visitThrowExpression(throwExpression)
    }

    final override fun visitVariableAssignment(variableAssignment: AstVariableAssignment, data: Nothing?) {
        visitVariableAssignment(variableAssignment)
    }

    final override fun visitWhenSubjectExpression(whenSubjectExpression: AstWhenSubjectExpression, data: Nothing?) {
        visitWhenSubjectExpression(whenSubjectExpression)
    }

    final override fun visitWrappedDelegateExpression(wrappedDelegateExpression: AstWrappedDelegateExpression, data: Nothing?) {
        visitWrappedDelegateExpression(wrappedDelegateExpression)
    }

    final override fun visitNamedReference(namedReference: AstNamedReference, data: Nothing?) {
        visitNamedReference(namedReference)
    }

    final override fun visitSuperReference(superReference: AstSuperReference, data: Nothing?) {
        visitSuperReference(superReference)
    }

    final override fun visitThisReference(thisReference: AstThisReference, data: Nothing?) {
        visitThisReference(thisReference)
    }

    final override fun visitResolvedNamedReference(resolvedNamedReference: AstResolvedNamedReference, data: Nothing?) {
        visitResolvedNamedReference(resolvedNamedReference)
    }

    final override fun visitDelegateFieldReference(delegateFieldReference: AstDelegateFieldReference, data: Nothing?) {
        visitDelegateFieldReference(delegateFieldReference)
    }

    final override fun visitBackingFieldReference(backingFieldReference: AstBackingFieldReference, data: Nothing?) {
        visitBackingFieldReference(backingFieldReference)
    }

    final override fun visitResolvedCallableReference(resolvedCallableReference: AstResolvedCallableReference, data: Nothing?) {
        visitResolvedCallableReference(resolvedCallableReference)
    }

    final override fun visitSimpleType(simpleType: AstSimpleType, data: Nothing?) {
        visitSimpleType(simpleType)
    }

}
