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
import com.ivianuu.ast.expressions.AstPropertyBackingFieldReference
import com.ivianuu.ast.types.AstSimpleType
import com.ivianuu.ast.types.AstDelegatedType
import com.ivianuu.ast.visitors.CompositeTransformResult

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstTransformer<in D> : AstVisitor<CompositeTransformResult<AstElement>, D>() {

    abstract fun <E : AstElement> transformElement(element: E, data: D): CompositeTransformResult<E>

    open fun transformAnnotationContainer(annotationContainer: AstAnnotationContainer, data: D): CompositeTransformResult<AstElement> {
        return transformElement(annotationContainer, data)
    }

    open fun transformType(type: AstType, data: D): CompositeTransformResult<AstType> {
        return transformElement(type, data)
    }

    open fun <E> transformSymbolOwner(symbolOwner: AstSymbolOwner<E>, data: D): CompositeTransformResult<AstElement> where E : AstSymbolOwner<E>, E : AstDeclaration {
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

    open fun transformExpression(expression: AstExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformStatement(expression, data)
    }

    open fun transformDeclaration(declaration: AstDeclaration, data: D): CompositeTransformResult<AstStatement> {
        return transformStatement(declaration, data)
    }

    open fun transformAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer, data: D): CompositeTransformResult<AstStatement> {
        return transformDeclaration(anonymousInitializer, data)
    }

    open fun <F : AstCallableDeclaration<F>> transformCallableDeclaration(callableDeclaration: AstCallableDeclaration<F>, data: D): CompositeTransformResult<AstStatement> {
        return transformDeclaration(callableDeclaration, data)
    }

    open fun transformTypeParameter(typeParameter: AstTypeParameter, data: D): CompositeTransformResult<AstStatement> {
        return transformDeclaration(typeParameter, data)
    }

    open fun transformTypeParametersOwner(typeParametersOwner: AstTypeParametersOwner, data: D): CompositeTransformResult<AstElement> {
        return transformElement(typeParametersOwner, data)
    }

    open fun <F : AstVariable<F>> transformVariable(variable: AstVariable<F>, data: D): CompositeTransformResult<AstStatement> {
        return transformDeclaration(variable, data)
    }

    open fun transformValueParameter(valueParameter: AstValueParameter, data: D): CompositeTransformResult<AstStatement> {
        return transformVariable(valueParameter, data)
    }

    open fun transformProperty(property: AstProperty, data: D): CompositeTransformResult<AstStatement> {
        return transformVariable(property, data)
    }

    open fun transformEnumEntry(enumEntry: AstEnumEntry, data: D): CompositeTransformResult<AstStatement> {
        return transformVariable(enumEntry, data)
    }

    open fun <F : AstClassLikeDeclaration<F>> transformClassLikeDeclaration(classLikeDeclaration: AstClassLikeDeclaration<F>, data: D): CompositeTransformResult<AstStatement> {
        return transformDeclaration(classLikeDeclaration, data)
    }

    open fun <F : AstClass<F>> transformClass(klass: AstClass<F>, data: D): CompositeTransformResult<AstStatement> {
        return transformClassLikeDeclaration(klass, data)
    }

    open fun transformRegularClass(regularClass: AstRegularClass, data: D): CompositeTransformResult<AstStatement> {
        return transformClass(regularClass, data)
    }

    open fun transformTypeAlias(typeAlias: AstTypeAlias, data: D): CompositeTransformResult<AstStatement> {
        return transformClassLikeDeclaration(typeAlias, data)
    }

    open fun <F : AstFunction<F>> transformFunction(function: AstFunction<F>, data: D): CompositeTransformResult<AstStatement> {
        return transformDeclaration(function, data)
    }

    open fun transformNamedFunction(namedFunction: AstNamedFunction, data: D): CompositeTransformResult<AstStatement> {
        return transformFunction(namedFunction, data)
    }

    open fun transformPropertyAccessor(propertyAccessor: AstPropertyAccessor, data: D): CompositeTransformResult<AstStatement> {
        return transformFunction(propertyAccessor, data)
    }

    open fun transformConstructor(constructor: AstConstructor, data: D): CompositeTransformResult<AstStatement> {
        return transformFunction(constructor, data)
    }

    open fun transformModuleFragment(moduleFragment: AstModuleFragment, data: D): CompositeTransformResult<AstModuleFragment> {
        return transformElement(moduleFragment, data)
    }

    open fun transformFile(file: AstFile, data: D): CompositeTransformResult<AstFile> {
        return transformElement(file, data)
    }

    open fun transformAnonymousFunction(anonymousFunction: AstAnonymousFunction, data: D): CompositeTransformResult<AstStatement> {
        return transformFunction(anonymousFunction, data)
    }

    open fun transformAnonymousObject(anonymousObject: AstAnonymousObject, data: D): CompositeTransformResult<AstStatement> {
        return transformClass(anonymousObject, data)
    }

    open fun transformLoop(loop: AstLoop, data: D): CompositeTransformResult<AstStatement> {
        return transformExpression(loop, data)
    }

    open fun transformDoWhileLoop(doWhileLoop: AstDoWhileLoop, data: D): CompositeTransformResult<AstStatement> {
        return transformLoop(doWhileLoop, data)
    }

    open fun transformWhileLoop(whileLoop: AstWhileLoop, data: D): CompositeTransformResult<AstStatement> {
        return transformLoop(whileLoop, data)
    }

    open fun transformBlock(block: AstBlock, data: D): CompositeTransformResult<AstStatement> {
        return transformExpression(block, data)
    }

    open fun transformLoopJump(loopJump: AstLoopJump, data: D): CompositeTransformResult<AstStatement> {
        return transformExpression(loopJump, data)
    }

    open fun transformBreak(breakExpression: AstBreak, data: D): CompositeTransformResult<AstStatement> {
        return transformLoopJump(breakExpression, data)
    }

    open fun transformContinue(continueExpression: AstContinue, data: D): CompositeTransformResult<AstStatement> {
        return transformLoopJump(continueExpression, data)
    }

    open fun transformCatch(catch: AstCatch, data: D): CompositeTransformResult<AstCatch> {
        return transformElement(catch, data)
    }

    open fun transformTry(tryExpression: AstTry, data: D): CompositeTransformResult<AstStatement> {
        return transformExpression(tryExpression, data)
    }

    open fun <T> transformConst(const: AstConst<T>, data: D): CompositeTransformResult<AstStatement> {
        return transformExpression(const, data)
    }

    open fun transformTypeProjection(typeProjection: AstTypeProjection, data: D): CompositeTransformResult<AstTypeProjection> {
        return transformElement(typeProjection, data)
    }

    open fun transformStarProjection(starProjection: AstStarProjection, data: D): CompositeTransformResult<AstTypeProjection> {
        return transformTypeProjection(starProjection, data)
    }

    open fun transformTypeProjectionWithVariance(typeProjectionWithVariance: AstTypeProjectionWithVariance, data: D): CompositeTransformResult<AstTypeProjection> {
        return transformTypeProjection(typeProjectionWithVariance, data)
    }

    open fun transformCall(call: AstCall, data: D): CompositeTransformResult<AstStatement> {
        return transformExpression(call, data)
    }

    open fun transformWhen(whenExpression: AstWhen, data: D): CompositeTransformResult<AstStatement> {
        return transformExpression(whenExpression, data)
    }

    open fun transformWhenBranch(whenBranch: AstWhenBranch, data: D): CompositeTransformResult<AstWhenBranch> {
        return transformElement(whenBranch, data)
    }

    open fun transformClassReference(classReference: AstClassReference, data: D): CompositeTransformResult<AstStatement> {
        return transformExpression(classReference, data)
    }

    open fun transformQualifiedAccess(qualifiedAccess: AstQualifiedAccess, data: D): CompositeTransformResult<AstStatement> {
        return transformExpression(qualifiedAccess, data)
    }

    open fun transformFunctionCall(functionCall: AstFunctionCall, data: D): CompositeTransformResult<AstStatement> {
        return transformQualifiedAccess(functionCall, data)
    }

    open fun transformDelegatedConstructorCall(delegatedConstructorCall: AstDelegatedConstructorCall, data: D): CompositeTransformResult<AstStatement> {
        return transformCall(delegatedConstructorCall, data)
    }

    open fun transformCallableReference(callableReference: AstCallableReference, data: D): CompositeTransformResult<AstStatement> {
        return transformQualifiedAccess(callableReference, data)
    }

    open fun transformVararg(vararg: AstVararg, data: D): CompositeTransformResult<AstStatement> {
        return transformExpression(vararg, data)
    }

    open fun transformSpreadElement(spreadElement: AstSpreadElement, data: D): CompositeTransformResult<AstVarargElement> {
        return transformVarargElement(spreadElement, data)
    }

    open fun transformReturn(returnExpression: AstReturn, data: D): CompositeTransformResult<AstStatement> {
        return transformExpression(returnExpression, data)
    }

    open fun transformThrow(throwExpression: AstThrow, data: D): CompositeTransformResult<AstStatement> {
        return transformExpression(throwExpression, data)
    }

    open fun transformVariableAssignment(variableAssignment: AstVariableAssignment, data: D): CompositeTransformResult<AstStatement> {
        return transformQualifiedAccess(variableAssignment, data)
    }

    open fun transformSuperReference(superReference: AstSuperReference, data: D): CompositeTransformResult<AstStatement> {
        return transformExpression(superReference, data)
    }

    open fun transformThisReference(thisReference: AstThisReference, data: D): CompositeTransformResult<AstStatement> {
        return transformExpression(thisReference, data)
    }

    open fun transformPropertyBackingFieldReference(propertyBackingFieldReference: AstPropertyBackingFieldReference, data: D): CompositeTransformResult<AstStatement> {
        return transformExpression(propertyBackingFieldReference, data)
    }

    open fun transformSimpleType(simpleType: AstSimpleType, data: D): CompositeTransformResult<AstType> {
        return transformType(simpleType, data)
    }

    open fun transformDelegatedType(delegatedType: AstDelegatedType, data: D): CompositeTransformResult<AstType> {
        return transformType(delegatedType, data)
    }

    final override fun visitElement(element: AstElement, data: D): CompositeTransformResult<AstElement> {
        return transformElement(element, data)
    }

    final override fun visitAnnotationContainer(annotationContainer: AstAnnotationContainer, data: D): CompositeTransformResult<AstElement> {
        return transformAnnotationContainer(annotationContainer, data)
    }

    final override fun visitType(type: AstType, data: D): CompositeTransformResult<AstType> {
        return transformType(type, data)
    }

    final override fun <E> visitSymbolOwner(symbolOwner: AstSymbolOwner<E>, data: D): CompositeTransformResult<AstElement> where E : AstSymbolOwner<E>, E : AstDeclaration {
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

    final override fun visitExpression(expression: AstExpression, data: D): CompositeTransformResult<AstStatement> {
        return transformExpression(expression, data)
    }

    final override fun visitDeclaration(declaration: AstDeclaration, data: D): CompositeTransformResult<AstStatement> {
        return transformDeclaration(declaration, data)
    }

    final override fun visitAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer, data: D): CompositeTransformResult<AstStatement> {
        return transformAnonymousInitializer(anonymousInitializer, data)
    }

    final override fun <F : AstCallableDeclaration<F>> visitCallableDeclaration(callableDeclaration: AstCallableDeclaration<F>, data: D): CompositeTransformResult<AstStatement> {
        return transformCallableDeclaration(callableDeclaration, data)
    }

    final override fun visitTypeParameter(typeParameter: AstTypeParameter, data: D): CompositeTransformResult<AstStatement> {
        return transformTypeParameter(typeParameter, data)
    }

    final override fun visitTypeParametersOwner(typeParametersOwner: AstTypeParametersOwner, data: D): CompositeTransformResult<AstElement> {
        return transformTypeParametersOwner(typeParametersOwner, data)
    }

    final override fun <F : AstVariable<F>> visitVariable(variable: AstVariable<F>, data: D): CompositeTransformResult<AstStatement> {
        return transformVariable(variable, data)
    }

    final override fun visitValueParameter(valueParameter: AstValueParameter, data: D): CompositeTransformResult<AstStatement> {
        return transformValueParameter(valueParameter, data)
    }

    final override fun visitProperty(property: AstProperty, data: D): CompositeTransformResult<AstStatement> {
        return transformProperty(property, data)
    }

    final override fun visitEnumEntry(enumEntry: AstEnumEntry, data: D): CompositeTransformResult<AstStatement> {
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

    final override fun visitTypeAlias(typeAlias: AstTypeAlias, data: D): CompositeTransformResult<AstStatement> {
        return transformTypeAlias(typeAlias, data)
    }

    final override fun <F : AstFunction<F>> visitFunction(function: AstFunction<F>, data: D): CompositeTransformResult<AstStatement> {
        return transformFunction(function, data)
    }

    final override fun visitNamedFunction(namedFunction: AstNamedFunction, data: D): CompositeTransformResult<AstStatement> {
        return transformNamedFunction(namedFunction, data)
    }

    final override fun visitPropertyAccessor(propertyAccessor: AstPropertyAccessor, data: D): CompositeTransformResult<AstStatement> {
        return transformPropertyAccessor(propertyAccessor, data)
    }

    final override fun visitConstructor(constructor: AstConstructor, data: D): CompositeTransformResult<AstStatement> {
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

    final override fun visitLoopJump(loopJump: AstLoopJump, data: D): CompositeTransformResult<AstStatement> {
        return transformLoopJump(loopJump, data)
    }

    final override fun visitBreak(breakExpression: AstBreak, data: D): CompositeTransformResult<AstStatement> {
        return transformBreak(breakExpression, data)
    }

    final override fun visitContinue(continueExpression: AstContinue, data: D): CompositeTransformResult<AstStatement> {
        return transformContinue(continueExpression, data)
    }

    final override fun visitCatch(catch: AstCatch, data: D): CompositeTransformResult<AstCatch> {
        return transformCatch(catch, data)
    }

    final override fun visitTry(tryExpression: AstTry, data: D): CompositeTransformResult<AstStatement> {
        return transformTry(tryExpression, data)
    }

    final override fun <T> visitConst(const: AstConst<T>, data: D): CompositeTransformResult<AstStatement> {
        return transformConst(const, data)
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

    final override fun visitWhen(whenExpression: AstWhen, data: D): CompositeTransformResult<AstStatement> {
        return transformWhen(whenExpression, data)
    }

    final override fun visitWhenBranch(whenBranch: AstWhenBranch, data: D): CompositeTransformResult<AstWhenBranch> {
        return transformWhenBranch(whenBranch, data)
    }

    final override fun visitClassReference(classReference: AstClassReference, data: D): CompositeTransformResult<AstStatement> {
        return transformClassReference(classReference, data)
    }

    final override fun visitQualifiedAccess(qualifiedAccess: AstQualifiedAccess, data: D): CompositeTransformResult<AstStatement> {
        return transformQualifiedAccess(qualifiedAccess, data)
    }

    final override fun visitFunctionCall(functionCall: AstFunctionCall, data: D): CompositeTransformResult<AstStatement> {
        return transformFunctionCall(functionCall, data)
    }

    final override fun visitDelegatedConstructorCall(delegatedConstructorCall: AstDelegatedConstructorCall, data: D): CompositeTransformResult<AstStatement> {
        return transformDelegatedConstructorCall(delegatedConstructorCall, data)
    }

    final override fun visitCallableReference(callableReference: AstCallableReference, data: D): CompositeTransformResult<AstStatement> {
        return transformCallableReference(callableReference, data)
    }

    final override fun visitVararg(vararg: AstVararg, data: D): CompositeTransformResult<AstStatement> {
        return transformVararg(vararg, data)
    }

    final override fun visitSpreadElement(spreadElement: AstSpreadElement, data: D): CompositeTransformResult<AstVarargElement> {
        return transformSpreadElement(spreadElement, data)
    }

    final override fun visitReturn(returnExpression: AstReturn, data: D): CompositeTransformResult<AstStatement> {
        return transformReturn(returnExpression, data)
    }

    final override fun visitThrow(throwExpression: AstThrow, data: D): CompositeTransformResult<AstStatement> {
        return transformThrow(throwExpression, data)
    }

    final override fun visitVariableAssignment(variableAssignment: AstVariableAssignment, data: D): CompositeTransformResult<AstStatement> {
        return transformVariableAssignment(variableAssignment, data)
    }

    final override fun visitSuperReference(superReference: AstSuperReference, data: D): CompositeTransformResult<AstStatement> {
        return transformSuperReference(superReference, data)
    }

    final override fun visitThisReference(thisReference: AstThisReference, data: D): CompositeTransformResult<AstStatement> {
        return transformThisReference(thisReference, data)
    }

    final override fun visitPropertyBackingFieldReference(propertyBackingFieldReference: AstPropertyBackingFieldReference, data: D): CompositeTransformResult<AstStatement> {
        return transformPropertyBackingFieldReference(propertyBackingFieldReference, data)
    }

    final override fun visitSimpleType(simpleType: AstSimpleType, data: D): CompositeTransformResult<AstType> {
        return transformSimpleType(simpleType, data)
    }

    final override fun visitDelegatedType(delegatedType: AstDelegatedType, data: D): CompositeTransformResult<AstType> {
        return transformDelegatedType(delegatedType, data)
    }

}
