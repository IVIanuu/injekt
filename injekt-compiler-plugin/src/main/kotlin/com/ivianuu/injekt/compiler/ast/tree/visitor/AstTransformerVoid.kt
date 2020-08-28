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
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeAlias
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstValueParameter
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

    fun visitPackageFragment(declaration: AstPackageFragment) =
        declaration.transformChildrenAndCompose()
    override fun visitPackageFragment(declaration: AstPackageFragment, data: Nothing?) =
        visitPackageFragment(declaration)

    fun visitFile(declaration: AstFile) = declaration.transformChildrenAndCompose()
    override fun visitFile(declaration: AstFile, data: Nothing?) = visitFile(declaration)

    fun visitDeclaration(declaration: AstDeclaration) = declaration.transformChildrenAndCompose()
    override fun visitDeclaration(declaration: AstDeclaration, data: Nothing?) =
        visitDeclaration(declaration)

    fun visitClass(declaration: AstClass) = visitDeclaration(declaration)
    override fun visitClass(declaration: AstClass, data: Nothing?) = visitClass(declaration)

    fun visitFunction(declaration: AstFunction) = visitDeclaration(declaration)
    override fun visitFunction(declaration: AstFunction, data: Nothing?) =
        visitFunction(declaration)

    fun visitSimpleFunction(declaration: AstSimpleFunction) = visitFunction(declaration)
    override fun visitSimpleFunction(declaration: AstSimpleFunction, data: Nothing?) =
        visitSimpleFunction(declaration)

    fun visitConstructor(declaration: AstConstructor) = visitFunction(declaration)
    override fun visitConstructor(declaration: AstConstructor, data: Nothing?) =
        visitConstructor(declaration)

    fun visitValueParameter(declaration: AstValueParameter) = visitDeclaration(declaration)
    override fun visitValueParameter(declaration: AstValueParameter, data: Nothing?) =
        visitValueParameter(declaration)

    fun visitTypeAlias(declaration: AstTypeAlias) = visitDeclaration(declaration)
    override fun visitTypeAlias(declaration: AstTypeAlias, data: Nothing?) =
        visitTypeAlias(declaration)

    fun visitExpression(expression: AstExpression) = expression.transformChildrenAndCompose()
    override fun visitExpression(expression: AstExpression, data: Nothing?) =
        visitExpression(expression)

}
