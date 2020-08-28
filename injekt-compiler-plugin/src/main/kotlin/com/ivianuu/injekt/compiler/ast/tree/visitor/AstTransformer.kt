package com.ivianuu.injekt.compiler.ast.tree.visitor

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstConstructor
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclaration
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstModuleFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstPackageFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstSimpleFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstValueParameter
import com.ivianuu.injekt.compiler.ast.tree.expression.AstExpression

interface AstTransformer<D> : AstVisitor<AstTransformResult<AstElement>, D> {

    override fun visitElement(element: AstElement, data: D): AstTransformResult<AstElement> {
        element.transformChildren(this, data)
        return element.compose()
    }

    override fun visitModuleFragment(
        declaration: AstModuleFragment,
        data: D
    ): AstTransformResult<AstModuleFragment> {
        declaration.transformChildren(this, data)
        return declaration.compose()
    }

    override fun visitPackageFragment(
        declaration: AstPackageFragment,
        data: D
    ): AstTransformResult<AstPackageFragment> {
        declaration.transformChildren(this, data)
        return declaration.compose()
    }

    override fun visitFile(declaration: AstFile, data: D): AstTransformResult<AstFile> =
        visitPackageFragment(declaration, data) as AstTransformResult<AstFile>

    override fun visitDeclaration(
        declaration: AstDeclaration,
        data: D
    ): AstTransformResult<AstDeclaration> =
        visitElement(declaration, data) as AstTransformResult<AstDeclaration>

    override fun visitClass(declaration: AstClass, data: D) =
        visitDeclaration(declaration, data)

    override fun visitFunction(declaration: AstFunction, data: D) =
        visitDeclaration(declaration, data)

    override fun visitSimpleFunction(declaration: AstSimpleFunction, data: D) =
        visitFunction(declaration, data)

    override fun visitConstructor(declaration: AstConstructor, data: D) =
        visitFunction(declaration, data)

    override fun visitValueParameter(declaration: AstValueParameter, data: D) =
        visitDeclaration(declaration, data)

    override fun visitExpression(
        expression: AstExpression,
        data: D
    ): AstTransformResult<AstExpression> =
        visitElement(expression, data) as AstTransformResult<AstExpression>

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

fun <T : AstElement, D> MutableList<T>.transformInplace(transformer: AstTransformer<D>, data: D) {
    val iterator = this.listIterator()
    while (iterator.hasNext()) {
        val next = iterator.next() as AstElement
        val result = next.transform(transformer, data)
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
