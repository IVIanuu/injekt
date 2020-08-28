package com.ivianuu.injekt.compiler.ast.tree.visitor

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstConstructor
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclaration
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstModuleFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstSimpleFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstValueParameter
import com.ivianuu.injekt.compiler.ast.tree.expression.AstExpression

interface AstVisitor<R, D> {

    fun visitElement(element: AstElement, data: D): R
    fun visitModuleFragment(declaration: AstModuleFragment, data: D) =
        visitElement(declaration, data)
    fun visitFile(declaration: AstFile, data: D) = visitElement(declaration, data)

    fun visitDeclaration(declaration: AstDeclaration, data: D) = visitElement(declaration, data)
    fun visitClass(declaration: AstClass, data: D) = visitElement(declaration, data)
    fun visitFunction(declaration: AstFunction, data: D) = visitDeclaration(declaration, data)
    fun visitSimpleFunction(declaration: AstSimpleFunction, data: D) =
        visitFunction(declaration, data)

    fun visitConstructor(declaration: AstConstructor, data: D) = visitFunction(declaration, data)
    fun visitValueParameter(declaration: AstValueParameter, data: D) =
        visitDeclaration(declaration, data)

    fun visitExpression(expression: AstExpression, data: D) = visitElement(expression, data)

}
