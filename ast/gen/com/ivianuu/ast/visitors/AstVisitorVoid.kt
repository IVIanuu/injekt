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
import com.ivianuu.ast.declarations.AstPackageFragment
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
import com.ivianuu.ast.expressions.AstBaseQualifiedAccess
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

    open fun <E> visitSymbolOwner(symbolOwner: AstSymbolOwner<E>) where E : AstSymbolOwner<E>, E : AstDeclaration {
        visitElement(symbolOwner)
    }

    open fun visitVarargElement(varargElement: AstVarargElement) {
        visitElement(varargElement)
    }

    open fun visitTargetElement(targetElement: AstTargetElement) {
        visitElement(targetElement)
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

    open fun visitDeclarationContainer(declarationContainer: AstDeclarationContainer) {
        visitElement(declarationContainer)
    }

    open fun visitNamedDeclaration(namedDeclaration: AstNamedDeclaration) {
        visitElement(namedDeclaration)
    }

    open fun visitMemberDeclaration(memberDeclaration: AstMemberDeclaration) {
        visitElement(memberDeclaration)
    }

    open fun visitAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer) {
        visitElement(anonymousInitializer)
    }

    open fun <F : AstCallableDeclaration<F>> visitCallableDeclaration(callableDeclaration: AstCallableDeclaration<F>) {
        visitElement(callableDeclaration)
    }

    open fun visitTypeParameter(typeParameter: AstTypeParameter) {
        visitElement(typeParameter)
    }

    open fun visitTypeParametersOwner(typeParametersOwner: AstTypeParametersOwner) {
        visitElement(typeParametersOwner)
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

    open fun visitNamedFunction(namedFunction: AstNamedFunction) {
        visitElement(namedFunction)
    }

    open fun visitPropertyAccessor(propertyAccessor: AstPropertyAccessor) {
        visitElement(propertyAccessor)
    }

    open fun visitConstructor(constructor: AstConstructor) {
        visitElement(constructor)
    }

    open fun visitModuleFragment(moduleFragment: AstModuleFragment) {
        visitElement(moduleFragment)
    }

    open fun visitPackageFragment(packageFragment: AstPackageFragment) {
        visitElement(packageFragment)
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

    open fun visitLoopJump(loopJump: AstLoopJump) {
        visitElement(loopJump)
    }

    open fun visitBreak(breakExpression: AstBreak) {
        visitElement(breakExpression)
    }

    open fun visitContinue(continueExpression: AstContinue) {
        visitElement(continueExpression)
    }

    open fun visitCatch(catch: AstCatch) {
        visitElement(catch)
    }

    open fun visitTry(tryExpression: AstTry) {
        visitElement(tryExpression)
    }

    open fun <T> visitConst(const: AstConst<T>) {
        visitElement(const)
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

    open fun visitCall(call: AstCall) {
        visitElement(call)
    }

    open fun visitWhen(whenExpression: AstWhen) {
        visitElement(whenExpression)
    }

    open fun visitWhenBranch(whenBranch: AstWhenBranch) {
        visitElement(whenBranch)
    }

    open fun visitClassReference(classReference: AstClassReference) {
        visitElement(classReference)
    }

    open fun visitBaseQualifiedAccess(baseQualifiedAccess: AstBaseQualifiedAccess) {
        visitElement(baseQualifiedAccess)
    }

    open fun visitQualifiedAccess(qualifiedAccess: AstQualifiedAccess) {
        visitElement(qualifiedAccess)
    }

    open fun visitFunctionCall(functionCall: AstFunctionCall) {
        visitElement(functionCall)
    }

    open fun visitDelegatedConstructorCall(delegatedConstructorCall: AstDelegatedConstructorCall) {
        visitElement(delegatedConstructorCall)
    }

    open fun visitCallableReference(callableReference: AstCallableReference) {
        visitElement(callableReference)
    }

    open fun visitVararg(vararg: AstVararg) {
        visitElement(vararg)
    }

    open fun visitSpreadElement(spreadElement: AstSpreadElement) {
        visitElement(spreadElement)
    }

    open fun visitReturn(returnExpression: AstReturn) {
        visitElement(returnExpression)
    }

    open fun visitThrow(throwExpression: AstThrow) {
        visitElement(throwExpression)
    }

    open fun visitVariableAssignment(variableAssignment: AstVariableAssignment) {
        visitElement(variableAssignment)
    }

    open fun visitSuperReference(superReference: AstSuperReference) {
        visitElement(superReference)
    }

    open fun visitThisReference(thisReference: AstThisReference) {
        visitElement(thisReference)
    }

    open fun visitPropertyBackingFieldReference(propertyBackingFieldReference: AstPropertyBackingFieldReference) {
        visitElement(propertyBackingFieldReference)
    }

    open fun visitSimpleType(simpleType: AstSimpleType) {
        visitElement(simpleType)
    }

    open fun visitDelegatedType(delegatedType: AstDelegatedType) {
        visitElement(delegatedType)
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

    final override fun <E> visitSymbolOwner(symbolOwner: AstSymbolOwner<E>, data: Nothing?) where E : AstSymbolOwner<E>, E : AstDeclaration {
        visitSymbolOwner(symbolOwner)
    }

    final override fun visitVarargElement(varargElement: AstVarargElement, data: Nothing?) {
        visitVarargElement(varargElement)
    }

    final override fun visitTargetElement(targetElement: AstTargetElement, data: Nothing?) {
        visitTargetElement(targetElement)
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

    final override fun visitDeclarationContainer(declarationContainer: AstDeclarationContainer, data: Nothing?) {
        visitDeclarationContainer(declarationContainer)
    }

    final override fun visitNamedDeclaration(namedDeclaration: AstNamedDeclaration, data: Nothing?) {
        visitNamedDeclaration(namedDeclaration)
    }

    final override fun visitMemberDeclaration(memberDeclaration: AstMemberDeclaration, data: Nothing?) {
        visitMemberDeclaration(memberDeclaration)
    }

    final override fun visitAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer, data: Nothing?) {
        visitAnonymousInitializer(anonymousInitializer)
    }

    final override fun <F : AstCallableDeclaration<F>> visitCallableDeclaration(callableDeclaration: AstCallableDeclaration<F>, data: Nothing?) {
        visitCallableDeclaration(callableDeclaration)
    }

    final override fun visitTypeParameter(typeParameter: AstTypeParameter, data: Nothing?) {
        visitTypeParameter(typeParameter)
    }

    final override fun visitTypeParametersOwner(typeParametersOwner: AstTypeParametersOwner, data: Nothing?) {
        visitTypeParametersOwner(typeParametersOwner)
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

    final override fun visitNamedFunction(namedFunction: AstNamedFunction, data: Nothing?) {
        visitNamedFunction(namedFunction)
    }

    final override fun visitPropertyAccessor(propertyAccessor: AstPropertyAccessor, data: Nothing?) {
        visitPropertyAccessor(propertyAccessor)
    }

    final override fun visitConstructor(constructor: AstConstructor, data: Nothing?) {
        visitConstructor(constructor)
    }

    final override fun visitModuleFragment(moduleFragment: AstModuleFragment, data: Nothing?) {
        visitModuleFragment(moduleFragment)
    }

    final override fun visitPackageFragment(packageFragment: AstPackageFragment, data: Nothing?) {
        visitPackageFragment(packageFragment)
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

    final override fun visitLoopJump(loopJump: AstLoopJump, data: Nothing?) {
        visitLoopJump(loopJump)
    }

    final override fun visitBreak(breakExpression: AstBreak, data: Nothing?) {
        visitBreak(breakExpression)
    }

    final override fun visitContinue(continueExpression: AstContinue, data: Nothing?) {
        visitContinue(continueExpression)
    }

    final override fun visitCatch(catch: AstCatch, data: Nothing?) {
        visitCatch(catch)
    }

    final override fun visitTry(tryExpression: AstTry, data: Nothing?) {
        visitTry(tryExpression)
    }

    final override fun <T> visitConst(const: AstConst<T>, data: Nothing?) {
        visitConst(const)
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

    final override fun visitCall(call: AstCall, data: Nothing?) {
        visitCall(call)
    }

    final override fun visitWhen(whenExpression: AstWhen, data: Nothing?) {
        visitWhen(whenExpression)
    }

    final override fun visitWhenBranch(whenBranch: AstWhenBranch, data: Nothing?) {
        visitWhenBranch(whenBranch)
    }

    final override fun visitClassReference(classReference: AstClassReference, data: Nothing?) {
        visitClassReference(classReference)
    }

    final override fun visitBaseQualifiedAccess(baseQualifiedAccess: AstBaseQualifiedAccess, data: Nothing?) {
        visitBaseQualifiedAccess(baseQualifiedAccess)
    }

    final override fun visitQualifiedAccess(qualifiedAccess: AstQualifiedAccess, data: Nothing?) {
        visitQualifiedAccess(qualifiedAccess)
    }

    final override fun visitFunctionCall(functionCall: AstFunctionCall, data: Nothing?) {
        visitFunctionCall(functionCall)
    }

    final override fun visitDelegatedConstructorCall(delegatedConstructorCall: AstDelegatedConstructorCall, data: Nothing?) {
        visitDelegatedConstructorCall(delegatedConstructorCall)
    }

    final override fun visitCallableReference(callableReference: AstCallableReference, data: Nothing?) {
        visitCallableReference(callableReference)
    }

    final override fun visitVararg(vararg: AstVararg, data: Nothing?) {
        visitVararg(vararg)
    }

    final override fun visitSpreadElement(spreadElement: AstSpreadElement, data: Nothing?) {
        visitSpreadElement(spreadElement)
    }

    final override fun visitReturn(returnExpression: AstReturn, data: Nothing?) {
        visitReturn(returnExpression)
    }

    final override fun visitThrow(throwExpression: AstThrow, data: Nothing?) {
        visitThrow(throwExpression)
    }

    final override fun visitVariableAssignment(variableAssignment: AstVariableAssignment, data: Nothing?) {
        visitVariableAssignment(variableAssignment)
    }

    final override fun visitSuperReference(superReference: AstSuperReference, data: Nothing?) {
        visitSuperReference(superReference)
    }

    final override fun visitThisReference(thisReference: AstThisReference, data: Nothing?) {
        visitThisReference(thisReference)
    }

    final override fun visitPropertyBackingFieldReference(propertyBackingFieldReference: AstPropertyBackingFieldReference, data: Nothing?) {
        visitPropertyBackingFieldReference(propertyBackingFieldReference)
    }

    final override fun visitSimpleType(simpleType: AstSimpleType, data: Nothing?) {
        visitSimpleType(simpleType)
    }

    final override fun visitDelegatedType(delegatedType: AstDelegatedType, data: Nothing?) {
        visitDelegatedType(delegatedType)
    }

}
