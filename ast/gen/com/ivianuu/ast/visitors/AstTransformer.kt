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

    open fun transformLabel(label: AstLabel, data: D): CompositeTransformResult<AstLabel> {
        return transformElement(label, data)
    }

    open fun <E> transformSymbolOwner(symbolOwner: AstSymbolOwner<E>, data: D): CompositeTransformResult<AstSymbolOwner<E>> where E : AstSymbolOwner<E>, E : AstDeclaration {
        return transformElement(symbolOwner, data)
    }

    open fun transformVarargElement(varargElement: AstVarargElement, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(varargElement, data)
    }

    open fun transformTargetElement(targetElement: AstTargetElement, data: D): CompositeTransformResult<AstTargetElement> {
        return transformElement(targetElement, data)
    }

    open fun transformStatement(statement: AstStatement, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(statement, data)
    }

    open fun transformExpression(expression: AstExpression, data: D): CompositeTransformResult<AstVarargElement> {
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

    open fun transformTypeParameter(typeParameter: AstTypeParameter, data: D): CompositeTransformResult<AstDeclaration> {
        return transformElement(typeParameter, data)
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

    open fun transformNamedFunction(namedFunction: AstNamedFunction, data: D): CompositeTransformResult<AstDeclaration> {
        return transformElement(namedFunction, data)
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

    open fun transformAnonymousFunction(anonymousFunction: AstAnonymousFunction, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(anonymousFunction, data)
    }

    open fun transformAnonymousObject(anonymousObject: AstAnonymousObject, data: D): CompositeTransformResult<AstVarargElement> {
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

    open fun transformBlock(block: AstBlock, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(block, data)
    }

    open fun transformBinaryLogicExpression(binaryLogicExpression: AstBinaryLogicExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(binaryLogicExpression, data)
    }

    open fun <E : AstTargetElement> transformJump(jump: AstJump<E>, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(jump, data)
    }

    open fun transformLoopJump(loopJump: AstLoopJump, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(loopJump, data)
    }

    open fun transformBreakExpression(breakExpression: AstBreakExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(breakExpression, data)
    }

    open fun transformContinueExpression(continueExpression: AstContinueExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(continueExpression, data)
    }

    open fun transformCatch(catch: AstCatch, data: D): CompositeTransformResult<AstCatch> {
        return transformElement(catch, data)
    }

    open fun transformTryExpression(tryExpression: AstTryExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(tryExpression, data)
    }

    open fun <T> transformConstExpression(constExpression: AstConstExpression<T>, data: D): CompositeTransformResult<AstVarargElement> {
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

    open fun transformCall(call: AstCall, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(call, data)
    }

    open fun transformComparisonExpression(comparisonExpression: AstComparisonExpression, data: D): CompositeTransformResult<AstVarargElement> {
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

    open fun transformWhenExpression(whenExpression: AstWhenExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(whenExpression, data)
    }

    open fun transformWhenBranch(whenBranch: AstWhenBranch, data: D): CompositeTransformResult<AstWhenBranch> {
        return transformElement(whenBranch, data)
    }

    open fun transformElvisExpression(elvisExpression: AstElvisExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(elvisExpression, data)
    }

    open fun transformClassReference(classReference: AstClassReference, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(classReference, data)
    }

    open fun transformQualifiedAccess(qualifiedAccess: AstQualifiedAccess, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(qualifiedAccess, data)
    }

    open fun transformFunctionCall(functionCall: AstFunctionCall, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(functionCall, data)
    }

    open fun transformDelegatedConstructorCall(delegatedConstructorCall: AstDelegatedConstructorCall, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(delegatedConstructorCall, data)
    }

    open fun transformCallableReferenceAccess(callableReferenceAccess: AstCallableReferenceAccess, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(callableReferenceAccess, data)
    }

    open fun transformThisReceiverExpression(thisReceiverExpression: AstThisReceiverExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(thisReceiverExpression, data)
    }

    open fun transformExpressionWithSmartcast(expressionWithSmartcast: AstExpressionWithSmartcast, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(expressionWithSmartcast, data)
    }

    open fun transformSafeCallExpression(safeCallExpression: AstSafeCallExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(safeCallExpression, data)
    }

    open fun transformCheckedSafeCallSubject(checkedSafeCallSubject: AstCheckedSafeCallSubject, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(checkedSafeCallSubject, data)
    }

    open fun transformGetClassCall(getClassCall: AstGetClassCall, data: D): CompositeTransformResult<AstStatement> {
        return transformElement(getClassCall, data)
    }

    open fun transformVararg(vararg: AstVararg, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(vararg, data)
    }

    open fun transformSpreadElement(spreadElement: AstSpreadElement, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(spreadElement, data)
    }

    open fun transformReturnExpression(returnExpression: AstReturnExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(returnExpression, data)
    }

    open fun transformStringConcatenationCall(stringConcatenationCall: AstStringConcatenationCall, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(stringConcatenationCall, data)
    }

    open fun transformThrowExpression(throwExpression: AstThrowExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(throwExpression, data)
    }

    open fun transformVariableAssignment(variableAssignment: AstVariableAssignment, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(variableAssignment, data)
    }

    open fun transformWhenSubjectExpression(whenSubjectExpression: AstWhenSubjectExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(whenSubjectExpression, data)
    }

    open fun transformSuperReference(superReference: AstSuperReference, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(superReference, data)
    }

    open fun transformThisReference(thisReference: AstThisReference, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(thisReference, data)
    }

    open fun transformBackingFieldReference(backingFieldReference: AstBackingFieldReference, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElement(backingFieldReference, data)
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

    final override fun visitLabel(label: AstLabel, data: D): CompositeTransformResult<AstLabel> {
        return transformLabel(label, data)
    }

    final override fun <E> visitSymbolOwner(symbolOwner: AstSymbolOwner<E>, data: D): CompositeTransformResult<AstSymbolOwner<E>> where E : AstSymbolOwner<E>, E : AstDeclaration {
        return transformSymbolOwner(symbolOwner, data)
    }

    final override fun visitVarargElement(varargElement: AstVarargElement, data: D): CompositeTransformResult<AstVarargElement> {
        return transformVarargElement(varargElement, data)
    }

    final override fun visitTargetElement(targetElement: AstTargetElement, data: D): CompositeTransformResult<AstTargetElement> {
        return transformTargetElement(targetElement, data)
    }

    final override fun visitStatement(statement: AstStatement, data: D): CompositeTransformResult<AstStatement> {
        return transformStatement(statement, data)
    }

    final override fun visitExpression(expression: AstExpression, data: D): CompositeTransformResult<AstVarargElement> {
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

    final override fun visitTypeParameter(typeParameter: AstTypeParameter, data: D): CompositeTransformResult<AstDeclaration> {
        return transformTypeParameter(typeParameter, data)
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

    final override fun visitNamedFunction(namedFunction: AstNamedFunction, data: D): CompositeTransformResult<AstDeclaration> {
        return transformNamedFunction(namedFunction, data)
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

    final override fun visitAnonymousFunction(anonymousFunction: AstAnonymousFunction, data: D): CompositeTransformResult<AstVarargElement> {
        return transformAnonymousFunction(anonymousFunction, data)
    }

    final override fun visitAnonymousObject(anonymousObject: AstAnonymousObject, data: D): CompositeTransformResult<AstVarargElement> {
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

    final override fun visitBlock(block: AstBlock, data: D): CompositeTransformResult<AstVarargElement> {
        return transformBlock(block, data)
    }

    final override fun visitBinaryLogicExpression(binaryLogicExpression: AstBinaryLogicExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformBinaryLogicExpression(binaryLogicExpression, data)
    }

    final override fun <E : AstTargetElement> visitJump(jump: AstJump<E>, data: D): CompositeTransformResult<AstVarargElement> {
        return transformJump(jump, data)
    }

    final override fun visitLoopJump(loopJump: AstLoopJump, data: D): CompositeTransformResult<AstVarargElement> {
        return transformLoopJump(loopJump, data)
    }

    final override fun visitBreakExpression(breakExpression: AstBreakExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformBreakExpression(breakExpression, data)
    }

    final override fun visitContinueExpression(continueExpression: AstContinueExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformContinueExpression(continueExpression, data)
    }

    final override fun visitCatch(catch: AstCatch, data: D): CompositeTransformResult<AstCatch> {
        return transformCatch(catch, data)
    }

    final override fun visitTryExpression(tryExpression: AstTryExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformTryExpression(tryExpression, data)
    }

    final override fun <T> visitConstExpression(constExpression: AstConstExpression<T>, data: D): CompositeTransformResult<AstVarargElement> {
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

    final override fun visitCall(call: AstCall, data: D): CompositeTransformResult<AstStatement> {
        return transformCall(call, data)
    }

    final override fun visitComparisonExpression(comparisonExpression: AstComparisonExpression, data: D): CompositeTransformResult<AstVarargElement> {
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

    final override fun visitWhenExpression(whenExpression: AstWhenExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformWhenExpression(whenExpression, data)
    }

    final override fun visitWhenBranch(whenBranch: AstWhenBranch, data: D): CompositeTransformResult<AstWhenBranch> {
        return transformWhenBranch(whenBranch, data)
    }

    final override fun visitElvisExpression(elvisExpression: AstElvisExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformElvisExpression(elvisExpression, data)
    }

    final override fun visitClassReference(classReference: AstClassReference, data: D): CompositeTransformResult<AstVarargElement> {
        return transformClassReference(classReference, data)
    }

    final override fun visitQualifiedAccess(qualifiedAccess: AstQualifiedAccess, data: D): CompositeTransformResult<AstVarargElement> {
        return transformQualifiedAccess(qualifiedAccess, data)
    }

    final override fun visitFunctionCall(functionCall: AstFunctionCall, data: D): CompositeTransformResult<AstStatement> {
        return transformFunctionCall(functionCall, data)
    }

    final override fun visitDelegatedConstructorCall(delegatedConstructorCall: AstDelegatedConstructorCall, data: D): CompositeTransformResult<AstStatement> {
        return transformDelegatedConstructorCall(delegatedConstructorCall, data)
    }

    final override fun visitCallableReferenceAccess(callableReferenceAccess: AstCallableReferenceAccess, data: D): CompositeTransformResult<AstVarargElement> {
        return transformCallableReferenceAccess(callableReferenceAccess, data)
    }

    final override fun visitThisReceiverExpression(thisReceiverExpression: AstThisReceiverExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformThisReceiverExpression(thisReceiverExpression, data)
    }

    final override fun visitExpressionWithSmartcast(expressionWithSmartcast: AstExpressionWithSmartcast, data: D): CompositeTransformResult<AstVarargElement> {
        return transformExpressionWithSmartcast(expressionWithSmartcast, data)
    }

    final override fun visitSafeCallExpression(safeCallExpression: AstSafeCallExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformSafeCallExpression(safeCallExpression, data)
    }

    final override fun visitCheckedSafeCallSubject(checkedSafeCallSubject: AstCheckedSafeCallSubject, data: D): CompositeTransformResult<AstVarargElement> {
        return transformCheckedSafeCallSubject(checkedSafeCallSubject, data)
    }

    final override fun visitGetClassCall(getClassCall: AstGetClassCall, data: D): CompositeTransformResult<AstStatement> {
        return transformGetClassCall(getClassCall, data)
    }

    final override fun visitVararg(vararg: AstVararg, data: D): CompositeTransformResult<AstVarargElement> {
        return transformVararg(vararg, data)
    }

    final override fun visitSpreadElement(spreadElement: AstSpreadElement, data: D): CompositeTransformResult<AstVarargElement> {
        return transformSpreadElement(spreadElement, data)
    }

    final override fun visitReturnExpression(returnExpression: AstReturnExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformReturnExpression(returnExpression, data)
    }

    final override fun visitStringConcatenationCall(stringConcatenationCall: AstStringConcatenationCall, data: D): CompositeTransformResult<AstVarargElement> {
        return transformStringConcatenationCall(stringConcatenationCall, data)
    }

    final override fun visitThrowExpression(throwExpression: AstThrowExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformThrowExpression(throwExpression, data)
    }

    final override fun visitVariableAssignment(variableAssignment: AstVariableAssignment, data: D): CompositeTransformResult<AstVarargElement> {
        return transformVariableAssignment(variableAssignment, data)
    }

    final override fun visitWhenSubjectExpression(whenSubjectExpression: AstWhenSubjectExpression, data: D): CompositeTransformResult<AstVarargElement> {
        return transformWhenSubjectExpression(whenSubjectExpression, data)
    }

    final override fun visitSuperReference(superReference: AstSuperReference, data: D): CompositeTransformResult<AstVarargElement> {
        return transformSuperReference(superReference, data)
    }

    final override fun visitThisReference(thisReference: AstThisReference, data: D): CompositeTransformResult<AstVarargElement> {
        return transformThisReference(thisReference, data)
    }

    final override fun visitBackingFieldReference(backingFieldReference: AstBackingFieldReference, data: D): CompositeTransformResult<AstVarargElement> {
        return transformBackingFieldReference(backingFieldReference, data)
    }

    final override fun visitSimpleType(simpleType: AstSimpleType, data: D): CompositeTransformResult<AstType> {
        return transformSimpleType(simpleType, data)
    }

}
