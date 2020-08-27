package com.ivianuu.injekt.compiler.ast.tree.visitor

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclaration
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstModuleFragment
import com.ivianuu.injekt.compiler.ast.tree.expression.AstExpression

interface AstVisitorVoid : AstVisitor<Unit, Nothing?> {

    fun visitElement(element: AstElement) = Unit
    override fun visitElement(element: AstElement, data: Nothing?) = visitElement(element)

    fun visitModuleFragment(declaration: AstModuleFragment) = visitElement(declaration)
    override fun visitModuleFragment(declaration: AstModuleFragment, data: Nothing?) =
        visitModuleFragment(declaration)

    fun visitFile(declaration: AstFile) = visitElement(declaration)
    override fun visitFile(declaration: AstFile, data: Nothing?) = visitFile(declaration)

    fun visitDeclaration(declaration: AstDeclaration) = visitElement(declaration)
    override fun visitDeclaration(declaration: AstDeclaration, data: Nothing?) =
        visitDeclaration(declaration)

    fun visitClass(declaration: AstClass) = visitElement(declaration)
    override fun visitClass(declaration: AstClass, data: Nothing?) = visitClass(declaration)

    fun visitExpression(expression: AstExpression) = visitElement(expression)
    override fun visitExpression(expression: AstExpression, data: Nothing?) =
        visitExpression(expression)

}
