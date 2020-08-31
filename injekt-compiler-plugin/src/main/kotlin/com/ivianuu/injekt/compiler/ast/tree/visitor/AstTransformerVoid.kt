package com.ivianuu.injekt.compiler.ast.tree.visitor

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstAnonymousInitializer
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclaration
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstModuleFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstPackageFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstProperty
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeAlias
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeParameter
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstValueParameter
import com.ivianuu.injekt.compiler.ast.tree.expression.AstBlock
import com.ivianuu.injekt.compiler.ast.tree.expression.AstBranch
import com.ivianuu.injekt.compiler.ast.tree.expression.AstBreak
import com.ivianuu.injekt.compiler.ast.tree.expression.AstBreakContinue
import com.ivianuu.injekt.compiler.ast.tree.expression.AstCatch
import com.ivianuu.injekt.compiler.ast.tree.expression.AstConditionBranch
import com.ivianuu.injekt.compiler.ast.tree.expression.AstConst
import com.ivianuu.injekt.compiler.ast.tree.expression.AstContinue
import com.ivianuu.injekt.compiler.ast.tree.expression.AstDoWhileLoop
import com.ivianuu.injekt.compiler.ast.tree.expression.AstElseBranch
import com.ivianuu.injekt.compiler.ast.tree.expression.AstExpression
import com.ivianuu.injekt.compiler.ast.tree.expression.AstLoop
import com.ivianuu.injekt.compiler.ast.tree.expression.AstQualifiedAccess
import com.ivianuu.injekt.compiler.ast.tree.expression.AstReturn
import com.ivianuu.injekt.compiler.ast.tree.expression.AstStatement
import com.ivianuu.injekt.compiler.ast.tree.expression.AstStringConcatenation
import com.ivianuu.injekt.compiler.ast.tree.expression.AstThrow
import com.ivianuu.injekt.compiler.ast.tree.expression.AstTry
import com.ivianuu.injekt.compiler.ast.tree.expression.AstWhen
import com.ivianuu.injekt.compiler.ast.tree.expression.AstWhileLoop
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeArgument
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeProjection

interface AstTransformerVoid : AstTransformer<Nothing?> {

    fun <T : AstElement> T.transformChildrenAndCompose(): AstTransformResult<T> {
        transformChildren(this@AstTransformerVoid)
        return compose()
    }

    fun visitElement(element: AstElement) = element.transformChildrenAndCompose()
    override fun visitElement(element: AstElement, data: Nothing?) = visitElement(element)

    fun visitModuleFragment(moduleFragment: AstModuleFragment) =
        moduleFragment.transformChildrenAndCompose()

    override fun visitModuleFragment(moduleFragment: AstModuleFragment, data: Nothing?) =
        visitModuleFragment(moduleFragment)

    fun visitPackageFragment(packageFragment: AstPackageFragment) =
        packageFragment.transformChildrenAndCompose()

    override fun visitPackageFragment(packageFragment: AstPackageFragment, data: Nothing?) =
        visitPackageFragment(packageFragment)

    fun visitFile(file: AstFile) = visitPackageFragment(file)
    override fun visitFile(file: AstFile, data: Nothing?) = visitFile(file)

    fun visitDeclaration(declaration: AstDeclaration): AstTransformResult<AstStatement> =
        declaration.transformChildrenAndCompose() as AstTransformResult<AstStatement>

    override fun visitDeclaration(declaration: AstDeclaration, data: Nothing?) =
        visitDeclaration(declaration)

    fun visitClass(klass: AstClass) = visitDeclaration(klass)
    override fun visitClass(klass: AstClass, data: Nothing?) = visitClass(klass)

    fun visitFunction(function: AstFunction) = visitDeclaration(function)
    override fun visitFunction(function: AstFunction, data: Nothing?) =
        visitFunction(function)

    fun visitProperty(property: AstProperty) = visitDeclaration(property)
    override fun visitProperty(property: AstProperty, data: Nothing?) =
        visitProperty(property)

    fun visitAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer) =
        visitDeclaration(anonymousInitializer)

    override fun visitAnonymousInitializer(
        anonymousInitializer: AstAnonymousInitializer,
        data: Nothing?
    ) = visitAnonymousInitializer(anonymousInitializer)

    fun visitTypeParameter(typeParameter: AstTypeParameter) = visitDeclaration(typeParameter)
    override fun visitTypeParameter(typeParameter: AstTypeParameter, data: Nothing?) =
        visitTypeParameter(typeParameter)

    fun visitValueParameter(valueParameter: AstValueParameter) = visitDeclaration(valueParameter)
    override fun visitValueParameter(valueParameter: AstValueParameter, data: Nothing?) =
        visitValueParameter(valueParameter)

    fun visitTypeAlias(typeAlias: AstTypeAlias) = visitDeclaration(typeAlias)
    override fun visitTypeAlias(typeAlias: AstTypeAlias, data: Nothing?) =
        visitTypeAlias(typeAlias)

    fun visitExpression(expression: AstExpression): AstTransformResult<AstStatement> =
        expression.transformChildrenAndCompose() as AstTransformResult<AstStatement>

    override fun visitExpression(expression: AstExpression, data: Nothing?) =
        visitExpression(expression)

    fun <T> visitConst(const: AstConst<T>) = visitExpression(const)
    override fun <T> visitConst(const: AstConst<T>, data: Nothing?) = visitConst(const)

    fun visitBlock(block: AstBlock) = visitExpression(block)
    override fun visitBlock(block: AstBlock, data: Nothing?) = visitBlock(block)

    fun visitStringConcatenation(stringConcatenation: AstStringConcatenation) =
        visitExpression(stringConcatenation)

    override fun visitStringConcatenation(
        stringConcatenation: AstStringConcatenation,
        data: Nothing?
    ) =
        visitStringConcatenation(stringConcatenation)

    fun visitQualifiedAccess(qualifiedAccess: AstQualifiedAccess) =
        visitExpression(qualifiedAccess)

    override fun visitQualifiedAccess(qualifiedAccess: AstQualifiedAccess, data: Nothing?) =
        visitQualifiedAccess(qualifiedAccess)

    fun visitWhen(astWhen: AstWhen) = visitExpression(astWhen)
    override fun visitWhen(astWhen: AstWhen, data: Nothing?) = visitWhen(astWhen)

    fun visitBranch(branch: AstBranch): AstTransformResult<AstBranch> =
        visitElement(branch) as AstTransformResult<AstBranch>

    override fun visitBranch(branch: AstBranch, data: Nothing?) =
        visitBranch(branch)

    fun visitConditionBranch(conditionBranch: AstConditionBranch) =
        visitBranch(conditionBranch)

    override fun visitConditionBranch(conditionBranch: AstConditionBranch, data: Nothing?) =
        visitConditionBranch(conditionBranch)

    fun visitElseBranch(elseBranch: AstElseBranch) =
        visitBranch(elseBranch)

    override fun visitElseBranch(elseBranch: AstElseBranch, data: Nothing?) =
        visitElseBranch(elseBranch)

    fun visitLoop(loop: AstLoop) = visitExpression(loop)
    override fun visitLoop(loop: AstLoop, data: Nothing?) = visitLoop(loop)

    fun visitWhileLoop(whileLoop: AstWhileLoop) = visitLoop(whileLoop)
    override fun visitWhileLoop(whileLoop: AstWhileLoop, data: Nothing?) = visitWhileLoop(whileLoop)

    fun visitDoWhileLoop(doWhileLoop: AstDoWhileLoop) = visitLoop(doWhileLoop)
    override fun visitDoWhileLoop(doWhileLoop: AstDoWhileLoop, data: Nothing?) =
        visitDoWhileLoop(doWhileLoop)

    fun visitTry(astThrow: AstTry) = visitExpression(astThrow)
    override fun visitTry(astTry: AstTry, data: Nothing?) = visitTry(astTry)

    fun visitCatch(astCatch: AstCatch): AstTransformResult<AstCatch> =
        visitElement(astCatch) as AstTransformResult<AstCatch>

    override fun visitCatch(astCatch: AstCatch, data: Nothing?) =
        visitCatch(astCatch)

    fun visitBreakContinue(breakContinue: AstBreakContinue) = visitExpression(breakContinue)
    override fun visitBreakContinue(breakContinue: AstBreakContinue, data: Nothing?) =
        visitBreakContinue(breakContinue)

    fun visitBreak(astBreak: AstBreak) = visitExpression(astBreak)
    override fun visitBreak(astBreak: AstBreak, data: Nothing?) = visitBreak(astBreak)

    fun visitContinue(astContinue: AstContinue) = visitExpression(astContinue)
    override fun visitContinue(astContinue: AstContinue, data: Nothing?) =
        visitContinue(astContinue)

    fun visitReturn(astReturn: AstReturn) = visitExpression(astReturn)
    override fun visitReturn(astReturn: AstReturn, data: Nothing?) = visitReturn(astReturn)

    fun visitThrow(astThrow: AstThrow) = visitExpression(astThrow)
    override fun visitThrow(astThrow: AstThrow, data: Nothing?) = visitThrow(astThrow)

    fun visitTypeArgument(typeArgument: AstTypeArgument) =
        visitElement(typeArgument) as AstTransformResult<AstTypeArgument>

    override fun visitTypeArgument(typeArgument: AstTypeArgument, data: Nothing?) =
        visitTypeArgument(typeArgument)

    fun visitType(type: AstType): AstTransformResult<AstType> =
        visitElement(type) as AstTransformResult<AstType>

    override fun visitType(type: AstType, data: Nothing?) = visitType(type)

    fun visitTypeProjection(typeProjection: AstTypeProjection): AstTransformResult<AstTypeProjection> =
        visitElement(typeProjection) as AstTransformResult<AstTypeProjection>

    override fun visitTypeProjection(typeProjection: AstTypeProjection, data: Nothing?) =
        visitTypeProjection(typeProjection)

}
