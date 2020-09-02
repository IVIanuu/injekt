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
import com.ivianuu.ast.visitors.CompositeTransformResult

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstTransformer<in D> : AstVisitor<CompositeTransformResult<AstElement>, D>() {

    abstract fun <E : AstElement> transformElement(element: E, data: D): CompositeTransformResult<E>

    open fun transformAnnotationContainer(annotationContainer: AstAnnotationContainer, data: D): CompositeTransformResult<AstAnnotationContainer> {
        return transformElement(annotationContainer, data)
    }

    open fun transformType(type: AstType, data: D): CompositeTransformResult<AstType> {
        return transformElement(type, data)
    }

    open fun transformReference(reference: AstReference, data: D): CompositeTransformResult<AstReference> {
        return transformElement(reference, data)
    }

    open fun transformLabel(label: AstLabel, data: D): CompositeTransformResult<AstLabel> {
        return transformElement(label, data)
    }

    open fun <E> transformSymbolOwner(symbolOwner: AstSymbolOwner<E>, data: D): CompositeTransformResult<AstSymbolOwner<E>> where E : AstSymbolOwner<E>, E : AstDeclaration {
        return transformElement(symbolOwner, data)
    }

    open fun transformResolvable(resolvable: AstResolvable, data: D): CompositeTransformResult<AstResolvable> {
        return transformElement(resolvable, data)
    }

    open fun transformTargetElement(targetElement: AstTargetElement, data: D): CompositeTransformResult<AstTargetElement> {
        return transformElement(targetElement, data)
    }

    open fun transformDeclarationStatus(declarationStatus: AstDeclarationStatus, data: D): CompositeTransformResult<AstDeclarationStatus> {
        return transformElement(declarationStatus, data)
    }

    open fun transformStatement(statement: AstStatement, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(statement, data)
    }

    open fun transformExpression(expression: AstExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(expression, data)
    }

    open fun transformDeclaration(declaration: AstDeclaration, data: D): CompositeTransformResult<AstDeclaration> {
        return transformElement(declaration, data)
    }

    open fun transformAnnotatedDeclaration(annotatedDeclaration: AstAnnotatedDeclaration, data: D): CompositeTransformResult<AstDeclaration> {
        return transformElement(annotatedDeclaration, data)
    }

    open fun transformAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer, data: D): CompositeTransformResult<AstDeclaration> {
        return transformElement(anonymousInitializer, data)
    }

    open fun transformTypedDeclaration(typedDeclaration: AstTypedDeclaration, data: D): CompositeTransformResult<AstDeclaration> {
        return transformElement(typedDeclaration, data)
    }

    open fun <F : AstCallableDeclaration<F>> transformCallableDeclaration(callableDeclaration: AstCallableDeclaration<F>, data: D): CompositeTransformResult<AstDeclaration> {
        return transformElement(callableDeclaration, data)
    }

    open fun transformTypeParameterRef(typeParameterRef: AstTypeParameterRef, data: D): CompositeTransformResult<AstTypeParameterRef> {
        return transformElement(typeParameterRef, data)
    }

    open fun transformTypeParameter(typeParameter: AstTypeParameter, data: D): CompositeTransformResult<AstDeclaration> {
        return transformElement(typeParameter, data)
    }

    open fun transformTypeParameterRefsOwner(typeParameterRefsOwner: AstTypeParameterRefsOwner, data: D): CompositeTransformResult<AstTypeParameterRefsOwner> {
        return transformElement(typeParameterRefsOwner, data)
    }

    open fun transformTypeParametersOwner(typeParametersOwner: AstTypeParametersOwner, data: D): CompositeTransformResult<AstTypeParametersOwner> {
        return transformElement(typeParametersOwner, data)
    }

    open fun transformMemberDeclaration(memberDeclaration: AstMemberDeclaration, data: D): CompositeTransformResult<AstDeclaration> {
        return transformElement(memberDeclaration, data)
    }

    open fun <F : AstCallableMemberDeclaration<F>> transformCallableMemberDeclaration(callableMemberDeclaration: AstCallableMemberDeclaration<F>, data: D): CompositeTransformResult<AstDeclaration> {
        return transformElement(callableMemberDeclaration, data)
    }

    open fun <F : AstVariable<F>> transformVariable(variable: AstVariable<F>, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(variable, data)
    }

    open fun transformValueParameter(valueParameter: AstValueParameter, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(valueParameter, data)
    }

    open fun transformProperty(property: AstProperty, data: D): CompositeTransformResult<AstDeclaration> {
        return transformElement(property, data)
    }

    open fun transformField(field: AstField, data: D): CompositeTransformResult<AstDeclaration> {
        return transformElement(field, data)
    }

    open fun transformEnumEntry(enumEntry: AstEnumEntry, data: D): CompositeTransformResult<AstDeclaration> {
        return transformElement(enumEntry, data)
    }

    open fun <F : AstClassLikeDeclaration<F>> transformClassLikeDeclaration(classLikeDeclaration: AstClassLikeDeclaration<F>, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(classLikeDeclaration, data)
    }

    open fun <F : AstClass<F>> transformClass(klass: AstClass<F>, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(klass, data)
    }

    open fun transformRegularClass(regularClass: AstRegularClass, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(regularClass, data)
    }

    open fun transformTypeAlias(typeAlias: AstTypeAlias, data: D): CompositeTransformResult<AstDeclaration> {
        return transformElement(typeAlias, data)
    }

    open fun <F : AstFunction<F>> transformFunction(function: AstFunction<F>, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(function, data)
    }

    open fun transformSimpleFunction(simpleFunction: AstSimpleFunction, data: D): CompositeTransformResult<AstDeclaration> {
        return transformElement(simpleFunction, data)
    }

    open fun transformPropertyAccessor(propertyAccessor: AstPropertyAccessor, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(propertyAccessor, data)
    }

    open fun transformConstructor(constructor: AstConstructor, data: D): CompositeTransformResult<AstDeclaration> {
        return transformElement(constructor, data)
    }

    open fun transformModuleFragment(moduleFragment: AstModuleFragment, data: D): CompositeTransformResult<AstModuleFragment> {
        return transformElement(moduleFragment, data)
    }

    open fun transformFile(file: AstFile, data: D): CompositeTransformResult<AstFile> {
        return transformElement(file, data)
    }

    open fun transformAnonymousFunction(anonymousFunction: AstAnonymousFunction, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(anonymousFunction, data)
    }

    open fun transformAnonymousObject(anonymousObject: AstAnonymousObject, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(anonymousObject, data)
    }

    open fun transformLoop(loop: AstLoop, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(loop, data)
    }

    open fun transformDoWhileLoop(doWhileLoop: AstDoWhileLoop, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(doWhileLoop, data)
    }

    open fun transformWhileLoop(whileLoop: AstWhileLoop, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(whileLoop, data)
    }

    open fun transformBlock(block: AstBlock, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(block, data)
    }

    open fun transformBinaryLogicExpression(binaryLogicExpression: AstBinaryLogicExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(binaryLogicExpression, data)
    }

    open fun <E : AstTargetElement> transformJump(jump: AstJump<E>, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(jump, data)
    }

    open fun transformLoopJump(loopJump: AstLoopJump, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(loopJump, data)
    }

    open fun transformBreakExpression(breakExpression: AstBreakExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(breakExpression, data)
    }

    open fun transformContinueExpression(continueExpression: AstContinueExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(continueExpression, data)
    }

    open fun transformCatch(catch: AstCatch, data: D): CompositeTransformResult<AstCatch> {
        return transformElement(catch, data)
    }

    open fun transformTryExpression(tryExpression: AstTryExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(tryExpression, data)
    }

    open fun <T> transformConstExpression(constExpression: AstConstExpression<T>, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(constExpression, data)
    }

    open fun transformTypeProjection(typeProjection: AstTypeProjection, data: D): CompositeTransformResult<AstTypeProjection> {
        return transformElement(typeProjection, data)
    }

    open fun transformStarProjection(starProjection: AstStarProjection, data: D): CompositeTransformResult<AstTypeProjection> {
        return transformElement(starProjection, data)
    }

    open fun transformTypeProjectionWithVariance(typeProjectionWithVariance: AstTypeProjectionWithVariance, data: D): CompositeTransformResult<AstTypeProjection> {
        return transformElement(typeProjectionWithVariance, data)
    }

    open fun transformArgumentList(argumentList: AstArgumentList, data: D): CompositeTransformResult<AstArgumentList> {
        return transformElement(argumentList, data)
    }

    open fun transformCall(call: AstCall, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(call, data)
    }

    open fun transformAnnotationCall(annotationCall: AstAnnotationCall, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(annotationCall, data)
    }

    open fun transformComparisonExpression(comparisonExpression: AstComparisonExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(comparisonExpression, data)
    }

    open fun transformTypeOperatorCall(typeOperatorCall: AstTypeOperatorCall, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(typeOperatorCall, data)
    }

    open fun transformAssignmentOperatorStatement(assignmentOperatorStatement: AstAssignmentOperatorStatement, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(assignmentOperatorStatement, data)
    }

    open fun transformEqualityOperatorCall(equalityOperatorCall: AstEqualityOperatorCall, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(equalityOperatorCall, data)
    }

    open fun transformWhenExpression(whenExpression: AstWhenExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(whenExpression, data)
    }

    open fun transformWhenBranch(whenBranch: AstWhenBranch, data: D): CompositeTransformResult<AstWhenBranch> {
        return transformElement(whenBranch, data)
    }

    open fun transformQualifiedAccess(qualifiedAccess: AstQualifiedAccess, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(qualifiedAccess, data)
    }

    open fun transformElvisExpression(elvisExpression: AstElvisExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(elvisExpression, data)
    }

    open fun transformClassReferenceExpression(classReferenceExpression: AstClassReferenceExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(classReferenceExpression, data)
    }

    open fun transformQualifiedAccessExpression(qualifiedAccessExpression: AstQualifiedAccessExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(qualifiedAccessExpression, data)
    }

    open fun transformFunctionCall(functionCall: AstFunctionCall, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(functionCall, data)
    }

    open fun transformDelegatedConstructorCall(delegatedConstructorCall: AstDelegatedConstructorCall, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(delegatedConstructorCall, data)
    }

    open fun transformCallableReferenceAccess(callableReferenceAccess: AstCallableReferenceAccess, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(callableReferenceAccess, data)
    }

    open fun transformThisReceiverExpression(thisReceiverExpression: AstThisReceiverExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(thisReceiverExpression, data)
    }

    open fun transformExpressionWithSmartcast(expressionWithSmartcast: AstExpressionWithSmartcast, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(expressionWithSmartcast, data)
    }

    open fun transformSafeCallExpression(safeCallExpression: AstSafeCallExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(safeCallExpression, data)
    }

    open fun transformCheckedSafeCallSubject(checkedSafeCallSubject: AstCheckedSafeCallSubject, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(checkedSafeCallSubject, data)
    }

    open fun transformGetClassCall(getClassCall: AstGetClassCall, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(getClassCall, data)
    }

    open fun transformWrappedExpression(wrappedExpression: AstWrappedExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(wrappedExpression, data)
    }

    open fun transformWrappedArgumentExpression(wrappedArgumentExpression: AstWrappedArgumentExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(wrappedArgumentExpression, data)
    }

    open fun transformLambdaArgumentExpression(lambdaArgumentExpression: AstLambdaArgumentExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(lambdaArgumentExpression, data)
    }

    open fun transformSpreadArgumentExpression(spreadArgumentExpression: AstSpreadArgumentExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(spreadArgumentExpression, data)
    }

    open fun transformNamedArgumentExpression(namedArgumentExpression: AstNamedArgumentExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(namedArgumentExpression, data)
    }

    open fun transformVarargArgumentsExpression(varargArgumentsExpression: AstVarargArgumentsExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(varargArgumentsExpression, data)
    }

    open fun transformResolvedQualifier(resolvedQualifier: AstResolvedQualifier, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(resolvedQualifier, data)
    }

    open fun transformResolvedReifiedParameterReference(resolvedReifiedParameterReference: AstResolvedReifiedParameterReference, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(resolvedReifiedParameterReference, data)
    }

    open fun transformReturnExpression(returnExpression: AstReturnExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(returnExpression, data)
    }

    open fun transformStringConcatenationCall(stringConcatenationCall: AstStringConcatenationCall, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(stringConcatenationCall, data)
    }

    open fun transformThrowExpression(throwExpression: AstThrowExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(throwExpression, data)
    }

    open fun transformVariableAssignment(variableAssignment: AstVariableAssignment, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(variableAssignment, data)
    }

    open fun transformWhenSubjectExpression(whenSubjectExpression: AstWhenSubjectExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(whenSubjectExpression, data)
    }

    open fun transformWrappedDelegateExpression(wrappedDelegateExpression: AstWrappedDelegateExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(wrappedDelegateExpression, data)
    }

    open fun transformNamedReference(namedReference: AstNamedReference, data: D): CompositeTransformResult<AstReference> {
        return transformElement(namedReference, data)
    }

    open fun transformSuperReference(superReference: AstSuperReference, data: D): CompositeTransformResult<AstReference> {
        return transformElement(superReference, data)
    }

    open fun transformThisReference(thisReference: AstThisReference, data: D): CompositeTransformResult<AstReference> {
        return transformElement(thisReference, data)
    }

    open fun transformResolvedNamedReference(resolvedNamedReference: AstResolvedNamedReference, data: D): CompositeTransformResult<AstReference> {
        return transformElement(resolvedNamedReference, data)
    }

    open fun transformDelegateFieldReference(delegateFieldReference: AstDelegateFieldReference, data: D): CompositeTransformResult<AstReference> {
        return transformElement(delegateFieldReference, data)
    }

    open fun transformBackingFieldReference(backingFieldReference: AstBackingFieldReference, data: D): CompositeTransformResult<AstReference> {
        return transformElement(backingFieldReference, data)
    }

    open fun transformResolvedCallableReference(resolvedCallableReference: AstResolvedCallableReference, data: D): CompositeTransformResult<AstReference> {
        return transformElement(resolvedCallableReference, data)
    }

    open fun transformSimpleType(simpleType: AstSimpleType, data: D): CompositeTransformResult<AstType> {
        return transformElement(simpleType, data)
    }

    final override fun visitElement(element: AstElement, data: D): CompositeTransformResult<AstElement> {
        return transformElement(element, data)
    }

    final override fun visitAnnotationContainer(annotationContainer: AstAnnotationContainer, data: D): CompositeTransformResult<AstAnnotationContainer> {
        return transformAnnotationContainer(annotationContainer, data)
    }

    final override fun visitType(type: AstType, data: D): CompositeTransformResult<AstType> {
        return transformType(type, data)
    }

    final override fun visitReference(reference: AstReference, data: D): CompositeTransformResult<AstReference> {
        return transformReference(reference, data)
    }

    final override fun visitLabel(label: AstLabel, data: D): CompositeTransformResult<AstLabel> {
        return transformLabel(label, data)
    }

    final override fun <E> visitSymbolOwner(symbolOwner: AstSymbolOwner<E>, data: D): CompositeTransformResult<AstSymbolOwner<E>> where E : AstSymbolOwner<E>, E : AstDeclaration {
        return transformSymbolOwner(symbolOwner, data)
    }

    final override fun visitResolvable(resolvable: AstResolvable, data: D): CompositeTransformResult<AstResolvable> {
        return transformResolvable(resolvable, data)
    }

    final override fun visitTargetElement(targetElement: AstTargetElement, data: D): CompositeTransformResult<AstTargetElement> {
        return transformTargetElement(targetElement, data)
    }

    final override fun visitDeclarationStatus(declarationStatus: AstDeclarationStatus, data: D): CompositeTransformResult<AstDeclarationStatus> {
        return transformDeclarationStatus(declarationStatus, data)
    }

    final override fun visitStatement(statement: AstStatement, data: D): CompositeTransformResult<AstStatement> {
        return transformStatement(statement, data)
    }

    final override fun visitExpression(expression: AstExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformExpression(expression, data)
    }

    final override fun visitDeclaration(declaration: AstDeclaration, data: D): CompositeTransformResult<AstDeclaration> {
        return transformDeclaration(declaration, data)
    }

    final override fun visitAnnotatedDeclaration(annotatedDeclaration: AstAnnotatedDeclaration, data: D): CompositeTransformResult<AstDeclaration> {
        return transformAnnotatedDeclaration(annotatedDeclaration, data)
    }

    final override fun visitAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer, data: D): CompositeTransformResult<AstDeclaration> {
        return transformAnonymousInitializer(anonymousInitializer, data)
    }

    final override fun visitTypedDeclaration(typedDeclaration: AstTypedDeclaration, data: D): CompositeTransformResult<AstDeclaration> {
        return transformTypedDeclaration(typedDeclaration, data)
    }

    final override fun <F : AstCallableDeclaration<F>> visitCallableDeclaration(callableDeclaration: AstCallableDeclaration<F>, data: D): CompositeTransformResult<AstDeclaration> {
        return transformCallableDeclaration(callableDeclaration, data)
    }

    final override fun visitTypeParameterRef(typeParameterRef: AstTypeParameterRef, data: D): CompositeTransformResult<AstTypeParameterRef> {
        return transformTypeParameterRef(typeParameterRef, data)
    }

    final override fun visitTypeParameter(typeParameter: AstTypeParameter, data: D): CompositeTransformResult<AstDeclaration> {
        return transformTypeParameter(typeParameter, data)
    }

    final override fun visitTypeParameterRefsOwner(typeParameterRefsOwner: AstTypeParameterRefsOwner, data: D): CompositeTransformResult<AstTypeParameterRefsOwner> {
        return transformTypeParameterRefsOwner(typeParameterRefsOwner, data)
    }

    final override fun visitTypeParametersOwner(typeParametersOwner: AstTypeParametersOwner, data: D): CompositeTransformResult<AstTypeParametersOwner> {
        return transformTypeParametersOwner(typeParametersOwner, data)
    }

    final override fun visitMemberDeclaration(memberDeclaration: AstMemberDeclaration, data: D): CompositeTransformResult<AstDeclaration> {
        return transformMemberDeclaration(memberDeclaration, data)
    }

    final override fun <F : AstCallableMemberDeclaration<F>> visitCallableMemberDeclaration(callableMemberDeclaration: AstCallableMemberDeclaration<F>, data: D): CompositeTransformResult<AstDeclaration> {
        return transformCallableMemberDeclaration(callableMemberDeclaration, data)
    }

    final override fun <F : AstVariable<F>> visitVariable(variable: AstVariable<F>, data: D): CompositeTransformResult<AstStatement> {
        return transformVariable(variable, data)
    }

    final override fun visitValueParameter(valueParameter: AstValueParameter, data: D): CompositeTransformResult<AstStatement> {
        return transformValueParameter(valueParameter, data)
    }

    final override fun visitProperty(property: AstProperty, data: D): CompositeTransformResult<AstDeclaration> {
        return transformProperty(property, data)
    }

    final override fun visitField(field: AstField, data: D): CompositeTransformResult<AstDeclaration> {
        return transformField(field, data)
    }

    final override fun visitEnumEntry(enumEntry: AstEnumEntry, data: D): CompositeTransformResult<AstDeclaration> {
        return transformEnumEntry(enumEntry, data)
    }

    final override fun <F : AstClassLikeDeclaration<F>> visitClassLikeDeclaration(classLikeDeclaration: AstClassLikeDeclaration<F>, data: D): CompositeTransformResult<AstStatement> {
        return transformClassLikeDeclaration(classLikeDeclaration, data)
    }

    final override fun <F : AstClass<F>> visitClass(klass: AstClass<F>, data: D): CompositeTransformResult<AstStatement> {
        return transformClass(klass, data)
    }

    final override fun visitRegularClass(regularClass: AstRegularClass, data: D): CompositeTransformResult<AstStatement> {
        return transformRegularClass(regularClass, data)
    }

    final override fun visitTypeAlias(typeAlias: AstTypeAlias, data: D): CompositeTransformResult<AstDeclaration> {
        return transformTypeAlias(typeAlias, data)
    }

    final override fun <F : AstFunction<F>> visitFunction(function: AstFunction<F>, data: D): CompositeTransformResult<AstStatement> {
        return transformFunction(function, data)
    }

    final override fun visitSimpleFunction(simpleFunction: AstSimpleFunction, data: D): CompositeTransformResult<AstDeclaration> {
        return transformSimpleFunction(simpleFunction, data)
    }

    final override fun visitPropertyAccessor(propertyAccessor: AstPropertyAccessor, data: D): CompositeTransformResult<AstStatement> {
        return transformPropertyAccessor(propertyAccessor, data)
    }

    final override fun visitConstructor(constructor: AstConstructor, data: D): CompositeTransformResult<AstDeclaration> {
        return transformConstructor(constructor, data)
    }

    final override fun visitModuleFragment(moduleFragment: AstModuleFragment, data: D): CompositeTransformResult<AstModuleFragment> {
        return transformModuleFragment(moduleFragment, data)
    }

    final override fun visitFile(file: AstFile, data: D): CompositeTransformResult<AstFile> {
        return transformFile(file, data)
    }

    final override fun visitAnonymousFunction(anonymousFunction: AstAnonymousFunction, data: D): CompositeTransformResult<AstStatement> {
        return transformAnonymousFunction(anonymousFunction, data)
    }

    final override fun visitAnonymousObject(anonymousObject: AstAnonymousObject, data: D): CompositeTransformResult<AstStatement> {
        return transformAnonymousObject(anonymousObject, data)
    }

    final override fun visitLoop(loop: AstLoop, data: D): CompositeTransformResult<AstStatement> {
        return transformLoop(loop, data)
    }

    final override fun visitDoWhileLoop(doWhileLoop: AstDoWhileLoop, data: D): CompositeTransformResult<AstStatement> {
        return transformDoWhileLoop(doWhileLoop, data)
    }

    final override fun visitWhileLoop(whileLoop: AstWhileLoop, data: D): CompositeTransformResult<AstStatement> {
        return transformWhileLoop(whileLoop, data)
    }

    final override fun visitBlock(block: AstBlock, data: D): CompositeTransformResult<AstStatement> {
        return transformBlock(block, data)
    }

    final override fun visitBinaryLogicExpression(binaryLogicExpression: AstBinaryLogicExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformBinaryLogicExpression(binaryLogicExpression, data)
    }

    final override fun <E : AstTargetElement> visitJump(jump: AstJump<E>, data: D): CompositeTransformResult<AstStatement> {
        return transformJump(jump, data)
    }

    final override fun visitLoopJump(loopJump: AstLoopJump, data: D): CompositeTransformResult<AstStatement> {
        return transformLoopJump(loopJump, data)
    }

    final override fun visitBreakExpression(breakExpression: AstBreakExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformBreakExpression(breakExpression, data)
    }

    final override fun visitContinueExpression(continueExpression: AstContinueExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformContinueExpression(continueExpression, data)
    }

    final override fun visitCatch(catch: AstCatch, data: D): CompositeTransformResult<AstCatch> {
        return transformCatch(catch, data)
    }

    final override fun visitTryExpression(tryExpression: AstTryExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformTryExpression(tryExpression, data)
    }

    final override fun <T> visitConstExpression(constExpression: AstConstExpression<T>, data: D): CompositeTransformResult<AstStatement> {
        return transformConstExpression(constExpression, data)
    }

    final override fun visitTypeProjection(typeProjection: AstTypeProjection, data: D): CompositeTransformResult<AstTypeProjection> {
        return transformTypeProjection(typeProjection, data)
    }

    final override fun visitStarProjection(starProjection: AstStarProjection, data: D): CompositeTransformResult<AstTypeProjection> {
        return transformStarProjection(starProjection, data)
    }

    final override fun visitTypeProjectionWithVariance(typeProjectionWithVariance: AstTypeProjectionWithVariance, data: D): CompositeTransformResult<AstTypeProjection> {
        return transformTypeProjectionWithVariance(typeProjectionWithVariance, data)
    }

    final override fun visitArgumentList(argumentList: AstArgumentList, data: D): CompositeTransformResult<AstArgumentList> {
        return transformArgumentList(argumentList, data)
    }

    final override fun visitCall(call: AstCall, data: D): CompositeTransformResult<AstStatement> {
        return transformCall(call, data)
    }

    final override fun visitAnnotationCall(annotationCall: AstAnnotationCall, data: D): CompositeTransformResult<AstStatement> {
        return transformAnnotationCall(annotationCall, data)
    }

    final override fun visitComparisonExpression(comparisonExpression: AstComparisonExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformComparisonExpression(comparisonExpression, data)
    }

    final override fun visitTypeOperatorCall(typeOperatorCall: AstTypeOperatorCall, data: D): CompositeTransformResult<AstStatement> {
        return transformTypeOperatorCall(typeOperatorCall, data)
    }

    final override fun visitAssignmentOperatorStatement(assignmentOperatorStatement: AstAssignmentOperatorStatement, data: D): CompositeTransformResult<AstStatement> {
        return transformAssignmentOperatorStatement(assignmentOperatorStatement, data)
    }

    final override fun visitEqualityOperatorCall(equalityOperatorCall: AstEqualityOperatorCall, data: D): CompositeTransformResult<AstStatement> {
        return transformEqualityOperatorCall(equalityOperatorCall, data)
    }

    final override fun visitWhenExpression(whenExpression: AstWhenExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformWhenExpression(whenExpression, data)
    }

    final override fun visitWhenBranch(whenBranch: AstWhenBranch, data: D): CompositeTransformResult<AstWhenBranch> {
        return transformWhenBranch(whenBranch, data)
    }

    final override fun visitQualifiedAccess(qualifiedAccess: AstQualifiedAccess, data: D): CompositeTransformResult<AstStatement> {
        return transformQualifiedAccess(qualifiedAccess, data)
    }

    final override fun visitElvisExpression(elvisExpression: AstElvisExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformElvisExpression(elvisExpression, data)
    }

    final override fun visitClassReferenceExpression(classReferenceExpression: AstClassReferenceExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformClassReferenceExpression(classReferenceExpression, data)
    }

    final override fun visitQualifiedAccessExpression(qualifiedAccessExpression: AstQualifiedAccessExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformQualifiedAccessExpression(qualifiedAccessExpression, data)
    }

    final override fun visitFunctionCall(functionCall: AstFunctionCall, data: D): CompositeTransformResult<AstStatement> {
        return transformFunctionCall(functionCall, data)
    }

    final override fun visitDelegatedConstructorCall(delegatedConstructorCall: AstDelegatedConstructorCall, data: D): CompositeTransformResult<AstStatement> {
        return transformDelegatedConstructorCall(delegatedConstructorCall, data)
    }

    final override fun visitCallableReferenceAccess(callableReferenceAccess: AstCallableReferenceAccess, data: D): CompositeTransformResult<AstStatement> {
        return transformCallableReferenceAccess(callableReferenceAccess, data)
    }

    final override fun visitThisReceiverExpression(thisReceiverExpression: AstThisReceiverExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformThisReceiverExpression(thisReceiverExpression, data)
    }

    final override fun visitExpressionWithSmartcast(expressionWithSmartcast: AstExpressionWithSmartcast, data: D): CompositeTransformResult<AstStatement> {
        return transformExpressionWithSmartcast(expressionWithSmartcast, data)
    }

    final override fun visitSafeCallExpression(safeCallExpression: AstSafeCallExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformSafeCallExpression(safeCallExpression, data)
    }

    final override fun visitCheckedSafeCallSubject(checkedSafeCallSubject: AstCheckedSafeCallSubject, data: D): CompositeTransformResult<AstStatement> {
        return transformCheckedSafeCallSubject(checkedSafeCallSubject, data)
    }

    final override fun visitGetClassCall(getClassCall: AstGetClassCall, data: D): CompositeTransformResult<AstStatement> {
        return transformGetClassCall(getClassCall, data)
    }

    final override fun visitWrappedExpression(wrappedExpression: AstWrappedExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformWrappedExpression(wrappedExpression, data)
    }

    final override fun visitWrappedArgumentExpression(wrappedArgumentExpression: AstWrappedArgumentExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformWrappedArgumentExpression(wrappedArgumentExpression, data)
    }

    final override fun visitLambdaArgumentExpression(lambdaArgumentExpression: AstLambdaArgumentExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformLambdaArgumentExpression(lambdaArgumentExpression, data)
    }

    final override fun visitSpreadArgumentExpression(spreadArgumentExpression: AstSpreadArgumentExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformSpreadArgumentExpression(spreadArgumentExpression, data)
    }

    final override fun visitNamedArgumentExpression(namedArgumentExpression: AstNamedArgumentExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformNamedArgumentExpression(namedArgumentExpression, data)
    }

    final override fun visitVarargArgumentsExpression(varargArgumentsExpression: AstVarargArgumentsExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformVarargArgumentsExpression(varargArgumentsExpression, data)
    }

    final override fun visitResolvedQualifier(resolvedQualifier: AstResolvedQualifier, data: D): CompositeTransformResult<AstStatement> {
        return transformResolvedQualifier(resolvedQualifier, data)
    }

    final override fun visitResolvedReifiedParameterReference(resolvedReifiedParameterReference: AstResolvedReifiedParameterReference, data: D): CompositeTransformResult<AstStatement> {
        return transformResolvedReifiedParameterReference(resolvedReifiedParameterReference, data)
    }

    final override fun visitReturnExpression(returnExpression: AstReturnExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformReturnExpression(returnExpression, data)
    }

    final override fun visitStringConcatenationCall(stringConcatenationCall: AstStringConcatenationCall, data: D): CompositeTransformResult<AstStatement> {
        return transformStringConcatenationCall(stringConcatenationCall, data)
    }

    final override fun visitThrowExpression(throwExpression: AstThrowExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformThrowExpression(throwExpression, data)
    }

    final override fun visitVariableAssignment(variableAssignment: AstVariableAssignment, data: D): CompositeTransformResult<AstStatement> {
        return transformVariableAssignment(variableAssignment, data)
    }

    final override fun visitWhenSubjectExpression(whenSubjectExpression: AstWhenSubjectExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformWhenSubjectExpression(whenSubjectExpression, data)
    }

    final override fun visitWrappedDelegateExpression(wrappedDelegateExpression: AstWrappedDelegateExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformWrappedDelegateExpression(wrappedDelegateExpression, data)
    }

    final override fun visitNamedReference(namedReference: AstNamedReference, data: D): CompositeTransformResult<AstReference> {
        return transformNamedReference(namedReference, data)
    }

    final override fun visitSuperReference(superReference: AstSuperReference, data: D): CompositeTransformResult<AstReference> {
        return transformSuperReference(superReference, data)
    }

    final override fun visitThisReference(thisReference: AstThisReference, data: D): CompositeTransformResult<AstReference> {
        return transformThisReference(thisReference, data)
    }

    final override fun visitResolvedNamedReference(resolvedNamedReference: AstResolvedNamedReference, data: D): CompositeTransformResult<AstReference> {
        return transformResolvedNamedReference(resolvedNamedReference, data)
    }

    final override fun visitDelegateFieldReference(delegateFieldReference: AstDelegateFieldReference, data: D): CompositeTransformResult<AstReference> {
        return transformDelegateFieldReference(delegateFieldReference, data)
    }

    final override fun visitBackingFieldReference(backingFieldReference: AstBackingFieldReference, data: D): CompositeTransformResult<AstReference> {
        return transformBackingFieldReference(backingFieldReference, data)
    }

    final override fun visitResolvedCallableReference(resolvedCallableReference: AstResolvedCallableReference, data: D): CompositeTransformResult<AstReference> {
        return transformResolvedCallableReference(resolvedCallableReference, data)
    }

    final override fun visitSimpleType(simpleType: AstSimpleType, data: D): CompositeTransformResult<AstType> {
        return transformSimpleType(simpleType, data)
    }

}
