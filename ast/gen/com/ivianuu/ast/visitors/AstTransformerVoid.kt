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
import com.ivianuu.ast.declarations.AstDeclarationContainer
import com.ivianuu.ast.declarations.AstNamedDeclaration
import com.ivianuu.ast.declarations.AstMemberDeclaration
import com.ivianuu.ast.declarations.AstAnonymousInitializer
import com.ivianuu.ast.declarations.AstCallableDeclaration
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstTypeParametersOwner
import com.ivianuu.ast.declarations.AstVariable
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.AstClassLikeDeclaration
import com.ivianuu.ast.declarations.AstClass
import com.ivianuu.ast.declarations.AstRegularClass
import com.ivianuu.ast.declarations.AstTypeAlias
import com.ivianuu.ast.declarations.AstEnumEntry
import com.ivianuu.ast.declarations.AstFunction
import com.ivianuu.ast.declarations.AstNamedFunction
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstModuleFragment
import com.ivianuu.ast.declarations.AstPackageFragment
import com.ivianuu.ast.declarations.AstFile
import com.ivianuu.ast.declarations.AstAnonymousFunction
import com.ivianuu.ast.declarations.AstAnonymousObject
import com.ivianuu.ast.expressions.AstJump
import com.ivianuu.ast.expressions.AstLoop
import com.ivianuu.ast.expressions.AstDoWhileLoop
import com.ivianuu.ast.expressions.AstWhileLoop
import com.ivianuu.ast.expressions.AstForLoop
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
import com.ivianuu.ast.expressions.AstCalleeReference
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstWhen
import com.ivianuu.ast.expressions.AstWhenBranch
import com.ivianuu.ast.expressions.AstClassReference
import com.ivianuu.ast.expressions.AstBaseQualifiedAccess
import com.ivianuu.ast.expressions.AstQualifiedAccess
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstDelegateInitializer
import com.ivianuu.ast.expressions.AstCallableReference
import com.ivianuu.ast.expressions.AstVararg
import com.ivianuu.ast.AstSpreadElement
import com.ivianuu.ast.expressions.AstReturn
import com.ivianuu.ast.expressions.AstThrow
import com.ivianuu.ast.expressions.AstVariableAssignment
import com.ivianuu.ast.expressions.AstSuperReference
import com.ivianuu.ast.expressions.AstThisReference
import com.ivianuu.ast.expressions.AstPropertyBackingFieldReference
import com.ivianuu.ast.expressions.AstTypeOperation

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstTransformerVoid : AstTransformer<Nothing?>() {
    open fun <E : AstElement> transformElement(element: E): CompositeTransformResult<E> {
        element.transformChildren(this, null)
        return element.compose()
        }

    open fun transformAnnotationContainer(annotationContainer: AstAnnotationContainer): CompositeTransformResult<AstElement> {
        return transformElement(annotationContainer)
    }

    open fun transformType(type: AstType): CompositeTransformResult<AstType> {
        return transformElement(type)
    }

    open fun <E> transformSymbolOwner(symbolOwner: AstSymbolOwner<E>): CompositeTransformResult<AstElement> where E : AstSymbolOwner<E>, E : AstDeclaration {
        return transformElement(symbolOwner)
    }

    open fun transformVarargElement(varargElement: AstVarargElement): CompositeTransformResult<AstVarargElement> {
        return transformElement(varargElement)
    }

    open fun transformTargetElement(targetElement: AstTargetElement): CompositeTransformResult<AstTargetElement> {
        return transformElement(targetElement)
    }

    open fun transformStatement(statement: AstStatement): CompositeTransformResult<AstStatement> {
        return transformElement(statement)
    }

    open fun transformExpression(expression: AstExpression): CompositeTransformResult<AstStatement> {
        return transformElement(expression)
    }

    open fun transformDeclaration(declaration: AstDeclaration): CompositeTransformResult<AstStatement> {
        return transformElement(declaration)
    }

    open fun transformDeclarationContainer(declarationContainer: AstDeclarationContainer): CompositeTransformResult<AstElement> {
        return transformElement(declarationContainer)
    }

    open fun transformNamedDeclaration(namedDeclaration: AstNamedDeclaration): CompositeTransformResult<AstStatement> {
        return transformElement(namedDeclaration)
    }

    open fun transformMemberDeclaration(memberDeclaration: AstMemberDeclaration): CompositeTransformResult<AstStatement> {
        return transformElement(memberDeclaration)
    }

    open fun transformAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer): CompositeTransformResult<AstStatement> {
        return transformElement(anonymousInitializer)
    }

    open fun <F : AstCallableDeclaration<F>> transformCallableDeclaration(callableDeclaration: AstCallableDeclaration<F>): CompositeTransformResult<AstStatement> {
        return transformElement(callableDeclaration)
    }

    open fun transformTypeParameter(typeParameter: AstTypeParameter): CompositeTransformResult<AstStatement> {
        return transformElement(typeParameter)
    }

    open fun transformTypeParametersOwner(typeParametersOwner: AstTypeParametersOwner): CompositeTransformResult<AstElement> {
        return transformElement(typeParametersOwner)
    }

    open fun <F : AstVariable<F>> transformVariable(variable: AstVariable<F>): CompositeTransformResult<AstStatement> {
        return transformElement(variable)
    }

    open fun transformValueParameter(valueParameter: AstValueParameter): CompositeTransformResult<AstStatement> {
        return transformElement(valueParameter)
    }

    open fun transformProperty(property: AstProperty): CompositeTransformResult<AstStatement> {
        return transformElement(property)
    }

    open fun <F : AstClassLikeDeclaration<F>> transformClassLikeDeclaration(classLikeDeclaration: AstClassLikeDeclaration<F>): CompositeTransformResult<AstStatement> {
        return transformElement(classLikeDeclaration)
    }

    open fun <F : AstClass<F>> transformClass(klass: AstClass<F>): CompositeTransformResult<AstStatement> {
        return transformElement(klass)
    }

    open fun transformRegularClass(regularClass: AstRegularClass): CompositeTransformResult<AstStatement> {
        return transformElement(regularClass)
    }

    open fun transformTypeAlias(typeAlias: AstTypeAlias): CompositeTransformResult<AstStatement> {
        return transformElement(typeAlias)
    }

    open fun transformEnumEntry(enumEntry: AstEnumEntry): CompositeTransformResult<AstStatement> {
        return transformElement(enumEntry)
    }

    open fun <F : AstFunction<F>> transformFunction(function: AstFunction<F>): CompositeTransformResult<AstStatement> {
        return transformElement(function)
    }

    open fun transformNamedFunction(namedFunction: AstNamedFunction): CompositeTransformResult<AstStatement> {
        return transformElement(namedFunction)
    }

    open fun transformPropertyAccessor(propertyAccessor: AstPropertyAccessor): CompositeTransformResult<AstStatement> {
        return transformElement(propertyAccessor)
    }

    open fun transformConstructor(constructor: AstConstructor): CompositeTransformResult<AstStatement> {
        return transformElement(constructor)
    }

    open fun transformModuleFragment(moduleFragment: AstModuleFragment): CompositeTransformResult<AstModuleFragment> {
        return transformElement(moduleFragment)
    }

    open fun transformPackageFragment(packageFragment: AstPackageFragment): CompositeTransformResult<AstElement> {
        return transformElement(packageFragment)
    }

    open fun transformFile(file: AstFile): CompositeTransformResult<AstElement> {
        return transformElement(file)
    }

    open fun transformAnonymousFunction(anonymousFunction: AstAnonymousFunction): CompositeTransformResult<AstStatement> {
        return transformElement(anonymousFunction)
    }

    open fun transformAnonymousObject(anonymousObject: AstAnonymousObject): CompositeTransformResult<AstStatement> {
        return transformElement(anonymousObject)
    }

    open fun <E : AstTargetElement> transformJump(jump: AstJump<E>): CompositeTransformResult<AstStatement> {
        return transformElement(jump)
    }

    open fun transformLoop(loop: AstLoop): CompositeTransformResult<AstStatement> {
        return transformElement(loop)
    }

    open fun transformDoWhileLoop(doWhileLoop: AstDoWhileLoop): CompositeTransformResult<AstStatement> {
        return transformElement(doWhileLoop)
    }

    open fun transformWhileLoop(whileLoop: AstWhileLoop): CompositeTransformResult<AstStatement> {
        return transformElement(whileLoop)
    }

    open fun transformForLoop(forLoop: AstForLoop): CompositeTransformResult<AstStatement> {
        return transformElement(forLoop)
    }

    open fun transformBlock(block: AstBlock): CompositeTransformResult<AstStatement> {
        return transformElement(block)
    }

    open fun transformLoopJump(loopJump: AstLoopJump): CompositeTransformResult<AstStatement> {
        return transformElement(loopJump)
    }

    open fun transformBreak(breakExpression: AstBreak): CompositeTransformResult<AstStatement> {
        return transformElement(breakExpression)
    }

    open fun transformContinue(continueExpression: AstContinue): CompositeTransformResult<AstStatement> {
        return transformElement(continueExpression)
    }

    open fun transformCatch(catch: AstCatch): CompositeTransformResult<AstCatch> {
        return transformElement(catch)
    }

    open fun transformTry(tryExpression: AstTry): CompositeTransformResult<AstStatement> {
        return transformElement(tryExpression)
    }

    open fun <T> transformConst(const: AstConst<T>): CompositeTransformResult<AstStatement> {
        return transformElement(const)
    }

    open fun transformTypeProjection(typeProjection: AstTypeProjection): CompositeTransformResult<AstTypeProjection> {
        return transformElement(typeProjection)
    }

    open fun transformStarProjection(starProjection: AstStarProjection): CompositeTransformResult<AstTypeProjection> {
        return transformElement(starProjection)
    }

    open fun transformTypeProjectionWithVariance(typeProjectionWithVariance: AstTypeProjectionWithVariance): CompositeTransformResult<AstTypeProjection> {
        return transformElement(typeProjectionWithVariance)
    }

    open fun transformCalleeReference(calleeReference: AstCalleeReference): CompositeTransformResult<AstStatement> {
        return transformElement(calleeReference)
    }

    open fun transformCall(call: AstCall): CompositeTransformResult<AstStatement> {
        return transformElement(call)
    }

    open fun transformWhen(whenExpression: AstWhen): CompositeTransformResult<AstStatement> {
        return transformElement(whenExpression)
    }

    open fun transformWhenBranch(whenBranch: AstWhenBranch): CompositeTransformResult<AstWhenBranch> {
        return transformElement(whenBranch)
    }

    open fun transformClassReference(classReference: AstClassReference): CompositeTransformResult<AstStatement> {
        return transformElement(classReference)
    }

    open fun transformBaseQualifiedAccess(baseQualifiedAccess: AstBaseQualifiedAccess): CompositeTransformResult<AstStatement> {
        return transformElement(baseQualifiedAccess)
    }

    open fun transformQualifiedAccess(qualifiedAccess: AstQualifiedAccess): CompositeTransformResult<AstStatement> {
        return transformElement(qualifiedAccess)
    }

    open fun transformFunctionCall(functionCall: AstFunctionCall): CompositeTransformResult<AstStatement> {
        return transformElement(functionCall)
    }

    open fun transformDelegatedConstructorCall(delegatedConstructorCall: AstDelegatedConstructorCall): CompositeTransformResult<AstStatement> {
        return transformElement(delegatedConstructorCall)
    }

    open fun transformDelegateInitializer(delegateInitializer: AstDelegateInitializer): CompositeTransformResult<AstElement> {
        return transformElement(delegateInitializer)
    }

    open fun transformCallableReference(callableReference: AstCallableReference): CompositeTransformResult<AstStatement> {
        return transformElement(callableReference)
    }

    open fun transformVararg(vararg: AstVararg): CompositeTransformResult<AstStatement> {
        return transformElement(vararg)
    }

    open fun transformSpreadElement(spreadElement: AstSpreadElement): CompositeTransformResult<AstVarargElement> {
        return transformElement(spreadElement)
    }

    open fun transformReturn(returnExpression: AstReturn): CompositeTransformResult<AstStatement> {
        return transformElement(returnExpression)
    }

    open fun transformThrow(throwExpression: AstThrow): CompositeTransformResult<AstStatement> {
        return transformElement(throwExpression)
    }

    open fun transformVariableAssignment(variableAssignment: AstVariableAssignment): CompositeTransformResult<AstStatement> {
        return transformElement(variableAssignment)
    }

    open fun transformSuperReference(superReference: AstSuperReference): CompositeTransformResult<AstStatement> {
        return transformElement(superReference)
    }

    open fun transformThisReference(thisReference: AstThisReference): CompositeTransformResult<AstStatement> {
        return transformElement(thisReference)
    }

    open fun transformPropertyBackingFieldReference(propertyBackingFieldReference: AstPropertyBackingFieldReference): CompositeTransformResult<AstStatement> {
        return transformElement(propertyBackingFieldReference)
    }

    open fun transformTypeOperation(typeOperation: AstTypeOperation): CompositeTransformResult<AstStatement> {
        return transformElement(typeOperation)
    }

    final override fun <E : AstElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return transformElement(element)
    }

    final override fun transformAnnotationContainer(annotationContainer: AstAnnotationContainer, data: Nothing?): CompositeTransformResult<AstElement> {
        return transformAnnotationContainer(annotationContainer)
    }

    final override fun transformType(type: AstType, data: Nothing?): CompositeTransformResult<AstType> {
        return transformType(type)
    }

    final override fun <E> transformSymbolOwner(symbolOwner: AstSymbolOwner<E>, data: Nothing?): CompositeTransformResult<AstElement> where E : AstSymbolOwner<E>, E : AstDeclaration {
        return transformSymbolOwner(symbolOwner)
    }

    final override fun transformVarargElement(varargElement: AstVarargElement, data: Nothing?): CompositeTransformResult<AstVarargElement> {
        return transformVarargElement(varargElement)
    }

    final override fun transformTargetElement(targetElement: AstTargetElement, data: Nothing?): CompositeTransformResult<AstTargetElement> {
        return transformTargetElement(targetElement)
    }

    final override fun transformStatement(statement: AstStatement, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformStatement(statement)
    }

    final override fun transformExpression(expression: AstExpression, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformExpression(expression)
    }

    final override fun transformDeclaration(declaration: AstDeclaration, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformDeclaration(declaration)
    }

    final override fun transformDeclarationContainer(declarationContainer: AstDeclarationContainer, data: Nothing?): CompositeTransformResult<AstElement> {
        return transformDeclarationContainer(declarationContainer)
    }

    final override fun transformNamedDeclaration(namedDeclaration: AstNamedDeclaration, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformNamedDeclaration(namedDeclaration)
    }

    final override fun transformMemberDeclaration(memberDeclaration: AstMemberDeclaration, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformMemberDeclaration(memberDeclaration)
    }

    final override fun transformAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformAnonymousInitializer(anonymousInitializer)
    }

    final override fun <F : AstCallableDeclaration<F>> transformCallableDeclaration(callableDeclaration: AstCallableDeclaration<F>, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformCallableDeclaration(callableDeclaration)
    }

    final override fun transformTypeParameter(typeParameter: AstTypeParameter, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformTypeParameter(typeParameter)
    }

    final override fun transformTypeParametersOwner(typeParametersOwner: AstTypeParametersOwner, data: Nothing?): CompositeTransformResult<AstElement> {
        return transformTypeParametersOwner(typeParametersOwner)
    }

    final override fun <F : AstVariable<F>> transformVariable(variable: AstVariable<F>, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformVariable(variable)
    }

    final override fun transformValueParameter(valueParameter: AstValueParameter, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformValueParameter(valueParameter)
    }

    final override fun transformProperty(property: AstProperty, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformProperty(property)
    }

    final override fun <F : AstClassLikeDeclaration<F>> transformClassLikeDeclaration(classLikeDeclaration: AstClassLikeDeclaration<F>, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformClassLikeDeclaration(classLikeDeclaration)
    }

    final override fun <F : AstClass<F>> transformClass(klass: AstClass<F>, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformClass(klass)
    }

    final override fun transformRegularClass(regularClass: AstRegularClass, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformRegularClass(regularClass)
    }

    final override fun transformTypeAlias(typeAlias: AstTypeAlias, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformTypeAlias(typeAlias)
    }

    final override fun transformEnumEntry(enumEntry: AstEnumEntry, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformEnumEntry(enumEntry)
    }

    final override fun <F : AstFunction<F>> transformFunction(function: AstFunction<F>, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformFunction(function)
    }

    final override fun transformNamedFunction(namedFunction: AstNamedFunction, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformNamedFunction(namedFunction)
    }

    final override fun transformPropertyAccessor(propertyAccessor: AstPropertyAccessor, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformPropertyAccessor(propertyAccessor)
    }

    final override fun transformConstructor(constructor: AstConstructor, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformConstructor(constructor)
    }

    final override fun transformModuleFragment(moduleFragment: AstModuleFragment, data: Nothing?): CompositeTransformResult<AstModuleFragment> {
        return transformModuleFragment(moduleFragment)
    }

    final override fun transformPackageFragment(packageFragment: AstPackageFragment, data: Nothing?): CompositeTransformResult<AstElement> {
        return transformPackageFragment(packageFragment)
    }

    final override fun transformFile(file: AstFile, data: Nothing?): CompositeTransformResult<AstElement> {
        return transformFile(file)
    }

    final override fun transformAnonymousFunction(anonymousFunction: AstAnonymousFunction, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformAnonymousFunction(anonymousFunction)
    }

    final override fun transformAnonymousObject(anonymousObject: AstAnonymousObject, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformAnonymousObject(anonymousObject)
    }

    final override fun <E : AstTargetElement> transformJump(jump: AstJump<E>, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformJump(jump)
    }

    final override fun transformLoop(loop: AstLoop, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformLoop(loop)
    }

    final override fun transformDoWhileLoop(doWhileLoop: AstDoWhileLoop, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformDoWhileLoop(doWhileLoop)
    }

    final override fun transformWhileLoop(whileLoop: AstWhileLoop, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformWhileLoop(whileLoop)
    }

    final override fun transformForLoop(forLoop: AstForLoop, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformForLoop(forLoop)
    }

    final override fun transformBlock(block: AstBlock, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformBlock(block)
    }

    final override fun transformLoopJump(loopJump: AstLoopJump, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformLoopJump(loopJump)
    }

    final override fun transformBreak(breakExpression: AstBreak, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformBreak(breakExpression)
    }

    final override fun transformContinue(continueExpression: AstContinue, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformContinue(continueExpression)
    }

    final override fun transformCatch(catch: AstCatch, data: Nothing?): CompositeTransformResult<AstCatch> {
        return transformCatch(catch)
    }

    final override fun transformTry(tryExpression: AstTry, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformTry(tryExpression)
    }

    final override fun <T> transformConst(const: AstConst<T>, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformConst(const)
    }

    final override fun transformTypeProjection(typeProjection: AstTypeProjection, data: Nothing?): CompositeTransformResult<AstTypeProjection> {
        return transformTypeProjection(typeProjection)
    }

    final override fun transformStarProjection(starProjection: AstStarProjection, data: Nothing?): CompositeTransformResult<AstTypeProjection> {
        return transformStarProjection(starProjection)
    }

    final override fun transformTypeProjectionWithVariance(typeProjectionWithVariance: AstTypeProjectionWithVariance, data: Nothing?): CompositeTransformResult<AstTypeProjection> {
        return transformTypeProjectionWithVariance(typeProjectionWithVariance)
    }

    final override fun transformCalleeReference(calleeReference: AstCalleeReference, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformCalleeReference(calleeReference)
    }

    final override fun transformCall(call: AstCall, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformCall(call)
    }

    final override fun transformWhen(whenExpression: AstWhen, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformWhen(whenExpression)
    }

    final override fun transformWhenBranch(whenBranch: AstWhenBranch, data: Nothing?): CompositeTransformResult<AstWhenBranch> {
        return transformWhenBranch(whenBranch)
    }

    final override fun transformClassReference(classReference: AstClassReference, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformClassReference(classReference)
    }

    final override fun transformBaseQualifiedAccess(baseQualifiedAccess: AstBaseQualifiedAccess, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformBaseQualifiedAccess(baseQualifiedAccess)
    }

    final override fun transformQualifiedAccess(qualifiedAccess: AstQualifiedAccess, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformQualifiedAccess(qualifiedAccess)
    }

    final override fun transformFunctionCall(functionCall: AstFunctionCall, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformFunctionCall(functionCall)
    }

    final override fun transformDelegatedConstructorCall(delegatedConstructorCall: AstDelegatedConstructorCall, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformDelegatedConstructorCall(delegatedConstructorCall)
    }

    final override fun transformDelegateInitializer(delegateInitializer: AstDelegateInitializer, data: Nothing?): CompositeTransformResult<AstElement> {
        return transformDelegateInitializer(delegateInitializer)
    }

    final override fun transformCallableReference(callableReference: AstCallableReference, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformCallableReference(callableReference)
    }

    final override fun transformVararg(vararg: AstVararg, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformVararg(vararg)
    }

    final override fun transformSpreadElement(spreadElement: AstSpreadElement, data: Nothing?): CompositeTransformResult<AstVarargElement> {
        return transformSpreadElement(spreadElement)
    }

    final override fun transformReturn(returnExpression: AstReturn, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformReturn(returnExpression)
    }

    final override fun transformThrow(throwExpression: AstThrow, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformThrow(throwExpression)
    }

    final override fun transformVariableAssignment(variableAssignment: AstVariableAssignment, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformVariableAssignment(variableAssignment)
    }

    final override fun transformSuperReference(superReference: AstSuperReference, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformSuperReference(superReference)
    }

    final override fun transformThisReference(thisReference: AstThisReference, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformThisReference(thisReference)
    }

    final override fun transformPropertyBackingFieldReference(propertyBackingFieldReference: AstPropertyBackingFieldReference, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformPropertyBackingFieldReference(propertyBackingFieldReference)
    }

    final override fun transformTypeOperation(typeOperation: AstTypeOperation, data: Nothing?): CompositeTransformResult<AstStatement> {
        return transformTypeOperation(typeOperation)
    }

}
