package com.ivianuu.injekt.compiler.ast

interface AstVisitor<R, D> {

    fun visitElement(element: AstElement, data: D): R

    fun visitFile(declaration: AstFile, data: D): R = visitElement(declaration, data)

    fun visitClass(declaration: AstClass, data: D): R = visitClass(declaration, data)

}

interface AstVisitorVoid : AstVisitor<Unit, Nothing?> {

    override fun visitElement(element: AstElement, data: Nothing?) = visitElement(element)
    fun visitElement(element: AstElement) {
    }

    override fun visitFile(declaration: AstFile, data: Nothing?) = visitFile(declaration)
    fun visitFile(declaration: AstFile) = visitElement(declaration)

    override fun visitClass(declaration: AstClass, data: Nothing?) = visitClass(declaration)
    fun visitClass(declaration: AstClass) = visitElement(declaration)

}
