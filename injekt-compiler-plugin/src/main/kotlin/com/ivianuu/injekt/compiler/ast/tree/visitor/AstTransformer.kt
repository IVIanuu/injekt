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
import com.ivianuu.injekt.compiler.ast.tree.expression.AstConst
import com.ivianuu.injekt.compiler.ast.tree.expression.AstContinue
import com.ivianuu.injekt.compiler.ast.tree.expression.AstExpression
import com.ivianuu.injekt.compiler.ast.tree.expression.AstQualifiedAccess
import com.ivianuu.injekt.compiler.ast.tree.expression.AstReturn
import com.ivianuu.injekt.compiler.ast.tree.expression.AstStatement
import com.ivianuu.injekt.compiler.ast.tree.expression.AstStringConcatenation
import com.ivianuu.injekt.compiler.ast.tree.expression.AstThrow
import com.ivianuu.injekt.compiler.ast.tree.expression.AstTry
import com.ivianuu.injekt.compiler.ast.tree.expression.AstWhen
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeArgument
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeProjection

interface AstTransformer<D> : AstVisitor<AstTransformResult<AstElement>, D> {

    override fun visitElement(element: AstElement, data: D): AstTransformResult<AstElement> {
        element.transformChildren(this, data)
        return element.compose()
    }

    override fun visitModuleFragment(
        moduleFragment: AstModuleFragment,
        data: D
    ): AstTransformResult<AstModuleFragment> {
        moduleFragment.transformChildren(this, data)
        return moduleFragment.compose()
    }

    override fun visitPackageFragment(
        packageFragment: AstPackageFragment,
        data: D
    ): AstTransformResult<AstPackageFragment> {
        packageFragment.transformChildren(this, data)
        return packageFragment.compose()
    }

    override fun visitFile(file: AstFile, data: D) =
        visitPackageFragment(file, data)

    override fun visitDeclaration(
        declaration: AstDeclaration,
        data: D
    ): AstTransformResult<AstStatement> =
        visitElement(declaration, data) as AstTransformResult<AstStatement>

    override fun visitClass(klass: AstClass, data: D) =
        visitDeclaration(klass, data)

    override fun visitFunction(function: AstFunction, data: D) =
        visitDeclaration(function, data)

    override fun visitProperty(property: AstProperty, data: D) =
        visitDeclaration(property, data)

    override fun visitAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer, data: D) =
        visitDeclaration(anonymousInitializer, data)

    override fun visitTypeParameter(typeParameter: AstTypeParameter, data: D) =
        visitDeclaration(typeParameter, data)

    override fun visitValueParameter(valueParameter: AstValueParameter, data: D) =
        visitDeclaration(valueParameter, data)

    override fun visitTypeAlias(typeAlias: AstTypeAlias, data: D) =
        visitDeclaration(typeAlias, data)

    override fun visitExpression(
        expression: AstExpression,
        data: D
    ): AstTransformResult<AstStatement> =
        visitElement(expression, data) as AstTransformResult<AstStatement>

    override fun <T> visitConst(const: AstConst<T>, data: D) =
        visitExpression(const, data)

    override fun visitBlock(block: AstBlock, data: D) =
        visitExpression(block, data)

    override fun visitStringConcatenation(stringConcatenation: AstStringConcatenation, data: D) =
        visitExpression(stringConcatenation, data)

    override fun visitQualifiedAccess(qualifiedAccess: AstQualifiedAccess, data: D) =
        visitExpression(qualifiedAccess, data)

    override fun visitWhen(astWhen: AstWhen, data: D) =
        visitExpression(astWhen, data)

    override fun visitBranch(branch: AstBranch, data: D): AstTransformResult<AstBranch> =
        visitElement(branch, data) as AstTransformResult<AstBranch>

    override fun visitTry(astTry: AstTry, data: D) =
        visitExpression(astTry, data)

    override fun visitCatch(astCatch: AstCatch, data: D): AstTransformResult<AstCatch> =
        visitElement(astCatch, data) as AstTransformResult<AstCatch>

    override fun visitBreakContinue(breakContinue: AstBreakContinue, data: D) =
        visitExpression(breakContinue, data)

    override fun visitBreak(astBreak: AstBreak, data: D) = visitBreakContinue(astBreak, data)

    override fun visitContinue(astContinue: AstContinue, data: D) =
        visitBreakContinue(astContinue, data)

    override fun visitReturn(astReturn: AstReturn, data: D) =
        visitExpression(astReturn, data)

    override fun visitThrow(astThrow: AstThrow, data: D) =
        visitElement(astThrow, data)

    override fun visitTypeArgument(
        typeArgument: AstTypeArgument,
        data: D
    ): AstTransformResult<AstTypeArgument> =
        visitElement(typeArgument, data) as AstTransformResult<AstTypeArgument>

    override fun visitType(type: AstType, data: D): AstTransformResult<AstType> =
        visitTypeArgument(type, data) as AstTransformResult<AstType>

    override fun visitTypeProjection(
        typeProjection: AstTypeProjection,
        data: D
    ): AstTransformResult<AstTypeProjection> =
        visitTypeArgument(typeProjection, data) as AstTransformResult<AstTypeProjection>

}

sealed class AstTransformResult<out T : AstElement> {
    data class Single<out T : AstElement>(val element: T) : AstTransformResult<T>()
    data class Multiple<out T : AstElement>(val elements: List<T>) : AstTransformResult<T>()
    companion object {
        fun <T : AstElement> Empty() = Multiple<T>(emptyList())
    }
}

val <T : AstElement> AstTransformResult<T>.elements: List<T>
    get() = when (this) {
        is AstTransformResult.Multiple<*> -> elements as List<T>
        else -> error("Expected multi result but was $this")
    }


val <T : AstElement> AstTransformResult<T>.element: T
    get() = when (this) {
        is AstTransformResult.Single<*> -> element as T
        else -> error("Expected single result but was $this")
    }

fun <T : AstElement> T.compose() =
    AstTransformResult.Single(this)

fun <T : AstElement, D> T.transformSingle(transformer: AstTransformer<D>, data: D): T =
    transform(transformer, data).element as T

fun <T : AstElement?, D> MutableList<T>.transformInplace(transformer: AstTransformer<D>, data: D) {
    val iterator = this.listIterator()
    while (iterator.hasNext()) {
        val next = iterator.next() as? AstElement
        val result = next?.transform(transformer, data)
        if (result != null) {
            if (result is AstTransformResult.Single) {
                iterator.set(result.element as T)
            } else {
                val resultIterator = result.elements.listIterator() as ListIterator<T>
                if (!resultIterator.hasNext()) {
                    iterator.remove()
                } else {
                    iterator.set(resultIterator.next())
                }
                while (resultIterator.hasNext()) {
                    iterator.add(resultIterator.next())
                }
            }
        }
    }
}
