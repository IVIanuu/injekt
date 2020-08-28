package com.ivianuu.injekt.compiler.ast.tree.visitor

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstConstructor
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclaration
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstModuleFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstPackageFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstProperty
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstSimpleFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeAlias
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeParameter
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstValueParameter
import com.ivianuu.injekt.compiler.ast.tree.expression.AstExpression

interface AstVisitorVoid : AstVisitor<Unit, Nothing?> {

    fun visitElement(element: AstElement) = Unit
    override fun visitElement(element: AstElement, data: Nothing?) = visitElement(element)

    fun visitModuleFragment(declaration: AstModuleFragment) = visitElement(declaration)
    override fun visitModuleFragment(declaration: AstModuleFragment, data: Nothing?) =
        visitModuleFragment(declaration)

    fun visitPackageFragment(declaration: AstPackageFragment) = visitElement(declaration)
    override fun visitPackageFragment(declaration: AstPackageFragment, data: Nothing?) =
        visitPackageFragment(declaration)

    fun visitFile(declaration: AstFile) = visitPackageFragment(declaration)
    override fun visitFile(declaration: AstFile, data: Nothing?) = visitFile(declaration)

    fun visitDeclaration(declaration: AstDeclaration) = visitElement(declaration)
    override fun visitDeclaration(declaration: AstDeclaration, data: Nothing?) =
        visitDeclaration(declaration)

    fun visitClass(declaration: AstClass) = visitElement(declaration)
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

    fun visitProperty(declaration: AstProperty) = visitDeclaration(declaration)
    override fun visitProperty(declaration: AstProperty, data: Nothing?) =
        visitProperty(declaration)

    fun visitTypeParameter(declaration: AstTypeParameter) = visitDeclaration(declaration)
    override fun visitTypeParameter(declaration: AstTypeParameter, data: Nothing?) =
        visitTypeParameter(declaration)

    fun visitValueParameter(declaration: AstValueParameter) = visitDeclaration(declaration)
    override fun visitValueParameter(declaration: AstValueParameter, data: Nothing?) =
        visitValueParameter(declaration)

    fun visitTypeAlias(declaration: AstTypeAlias) = visitDeclaration(declaration)
    override fun visitTypeAlias(declaration: AstTypeAlias, data: Nothing?) =
        visitTypeAlias(declaration)

    fun visitExpression(expression: AstExpression) = visitElement(expression)
    override fun visitExpression(expression: AstExpression, data: Nothing?) =
        visitExpression(expression)

}
