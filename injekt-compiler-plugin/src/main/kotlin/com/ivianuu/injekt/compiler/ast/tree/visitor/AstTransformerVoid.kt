package com.ivianuu.injekt.compiler.ast.tree.visitor

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclaration
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstModuleFragment
import com.ivianuu.injekt.compiler.ast.tree.expression.AstExpression

interface AstTransformerVoid : AstTransformer<Nothing?> {

    fun <T : AstElement> T.transformChildrenAndCompose(): AstTransformResult<T> {
        transformChildren(this@AstTransformerVoid)
        return compose()
    }

    fun visitElement(element: AstElement) = element.transformChildrenAndCompose()
    override fun visitElement(element: AstElement, data: Nothing?) = visitElement(element)

    fun visitModuleFragment(declaration: AstModuleFragment) =
        declaration.transformChildrenAndCompose()

    override fun visitModuleFragment(declaration: AstModuleFragment, data: Nothing?) =
        visitModuleFragment(declaration)

    fun visitFile(declaration: AstFile) = declaration.transformChildrenAndCompose()
    override fun visitFile(declaration: AstFile, data: Nothing?) = visitFile(declaration)

    fun visitDeclaration(declaration: AstDeclaration) = declaration.transformChildrenAndCompose()
    override fun visitDeclaration(declaration: AstDeclaration, data: Nothing?) =
        visitDeclaration(declaration)

    fun visitClass(declaration: AstClass) = visitDeclaration(declaration)
    override fun visitClass(declaration: AstClass, data: Nothing?) = visitClass(declaration)

    fun visitExpression(expression: AstExpression) = expression.transformChildrenAndCompose()
    override fun visitExpression(expression: AstExpression, data: Nothing?) =
        visitExpression(expression)

}
