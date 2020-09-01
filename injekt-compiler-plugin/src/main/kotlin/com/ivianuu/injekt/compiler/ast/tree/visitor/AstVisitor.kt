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
import com.ivianuu.injekt.compiler.ast.tree.expression.AstAnonymousObjectExpression
import com.ivianuu.injekt.compiler.ast.tree.expression.AstBinaryOperation
import com.ivianuu.injekt.compiler.ast.tree.expression.AstBlock
import com.ivianuu.injekt.compiler.ast.tree.expression.AstBranch
import com.ivianuu.injekt.compiler.ast.tree.expression.AstBreak
import com.ivianuu.injekt.compiler.ast.tree.expression.AstBreakContinue
import com.ivianuu.injekt.compiler.ast.tree.expression.AstCatch
import com.ivianuu.injekt.compiler.ast.tree.expression.AstComparisonOperation
import com.ivianuu.injekt.compiler.ast.tree.expression.AstConditionBranch
import com.ivianuu.injekt.compiler.ast.tree.expression.AstConst
import com.ivianuu.injekt.compiler.ast.tree.expression.AstContinue
import com.ivianuu.injekt.compiler.ast.tree.expression.AstElseBranch
import com.ivianuu.injekt.compiler.ast.tree.expression.AstEqualityOperation
import com.ivianuu.injekt.compiler.ast.tree.expression.AstExpression
import com.ivianuu.injekt.compiler.ast.tree.expression.AstForLoop
import com.ivianuu.injekt.compiler.ast.tree.expression.AstLogicOperation
import com.ivianuu.injekt.compiler.ast.tree.expression.AstLoop
import com.ivianuu.injekt.compiler.ast.tree.expression.AstQualifiedAccess
import com.ivianuu.injekt.compiler.ast.tree.expression.AstReturn
import com.ivianuu.injekt.compiler.ast.tree.expression.AstSpreadElement
import com.ivianuu.injekt.compiler.ast.tree.expression.AstStatement
import com.ivianuu.injekt.compiler.ast.tree.expression.AstThrow
import com.ivianuu.injekt.compiler.ast.tree.expression.AstTry
import com.ivianuu.injekt.compiler.ast.tree.expression.AstVararg
import com.ivianuu.injekt.compiler.ast.tree.expression.AstWhen
import com.ivianuu.injekt.compiler.ast.tree.expression.AstWhileLoop
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeArgument
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeProjection

interface AstVisitor<R, D> {

    fun visitElement(element: AstElement, data: D): R
    fun visitModuleFragment(moduleFragment: AstModuleFragment, data: D) =
        visitElement(moduleFragment, data)

    fun visitPackageFragment(packageFragment: AstPackageFragment, data: D) =
        visitElement(packageFragment, data)

    fun visitFile(file: AstFile, data: D) = visitPackageFragment(file, data)

    fun visitStatement(statement: AstStatement, data: D) = visitElement(statement, data)

    fun visitDeclaration(declaration: AstDeclaration, data: D) = visitStatement(declaration, data)
    fun visitClass(klass: AstClass, data: D) = visitDeclaration(klass, data)
    fun visitFunction(function: AstFunction, data: D) = visitDeclaration(function, data)
    fun visitProperty(property: AstProperty, data: D) = visitDeclaration(property, data)
    fun visitAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer, data: D) =
        visitDeclaration(anonymousInitializer, data)

    fun visitTypeParameter(typeParameter: AstTypeParameter, data: D) =
        visitDeclaration(typeParameter, data)

    fun visitValueParameter(valueParameter: AstValueParameter, data: D) =
        visitDeclaration(valueParameter, data)

    fun visitTypeAlias(typeAlias: AstTypeAlias, data: D) =
        visitDeclaration(typeAlias, data)

    fun visitExpression(expression: AstExpression, data: D) = visitStatement(expression, data)
    fun <T> visitConst(const: AstConst<T>, data: D) = visitExpression(const, data)
    fun visitVararg(vararg: AstVararg, data: D) = visitExpression(vararg, data)
    fun visitSpreadElement(spreadElement: AstSpreadElement, data: D) =
        visitElement(spreadElement, data)

    fun visitBlock(block: AstBlock, data: D) = visitExpression(block, data)

    fun visitQualifiedAccess(qualifiedAccess: AstQualifiedAccess, data: D) =
        visitExpression(qualifiedAccess, data)

    fun visitAnonymousObjectExpression(expression: AstAnonymousObjectExpression, data: D) =
        visitExpression(expression, data)

    fun visitBinaryOperation(binaryOperation: AstBinaryOperation, data: D) =
        visitExpression(binaryOperation, data)

    fun visitComparisonOperation(comparisonOperation: AstComparisonOperation, data: D) =
        visitBinaryOperation(comparisonOperation, data)

    fun visitEqualityOperation(equalityOperation: AstEqualityOperation, data: D) =
        visitBinaryOperation(equalityOperation, data)

    fun visitLogicOperation(logicOperation: AstLogicOperation, data: D) =
        visitBinaryOperation(logicOperation, data)

    fun visitWhen(astWhen: AstWhen, data: D) = visitExpression(astWhen, data)
    fun visitBranch(branch: AstBranch, data: D) = visitElement(branch, data)
    fun visitConditionBranch(conditionBranch: AstConditionBranch, data: D) =
        visitBranch(conditionBranch, data)

    fun visitElseBranch(elseBranch: AstElseBranch, data: D) = visitBranch(elseBranch, data)

    fun visitLoop(loop: AstLoop, data: D) = visitExpression(loop, data)
    fun visitWhileLoop(whileLoop: AstWhileLoop, data: D) = visitLoop(whileLoop, data)
    fun visitForLoop(forLoop: AstForLoop, data: D) = visitLoop(forLoop, data)
    fun visitTry(astTry: AstTry, data: D) = visitExpression(astTry, data)
    fun visitCatch(astCatch: AstCatch, data: D) = visitElement(astCatch, data)
    fun visitBreakContinue(breakContinue: AstBreakContinue, data: D) =
        visitExpression(breakContinue, data)

    fun visitBreak(astBreak: AstBreak, data: D) = visitBreakContinue(astBreak, data)
    fun visitContinue(astContinue: AstContinue, data: D) = visitBreakContinue(astContinue, data)
    fun visitReturn(astReturn: AstReturn, data: D) = visitExpression(astReturn, data)
    fun visitThrow(astThrow: AstThrow, data: D) = visitExpression(astThrow, data)

    fun visitTypeArgument(typeArgument: AstTypeArgument, data: D) = visitElement(typeArgument, data)
    fun visitType(type: AstType, data: D) = visitTypeArgument(type, data)
    fun visitTypeProjection(typeProjection: AstTypeProjection, data: D) =
        visitTypeArgument(typeProjection, data)

}

typealias AstVisitorVoid = AstVisitor<Unit, Nothing?>
