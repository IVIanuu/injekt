package com.ivianuu.injekt.compiler.ast

import org.jetbrains.kotlin.ir.types.IrTypeArgument

interface AstElement {

    fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitElement(this, data)

    fun <D> acceptChildren(visitor: AstVisitor<*, D>, data: D)

    fun <E : AstElement, D> transform(visitor: AstTransformer<D>, data: D): AstTransformResult<E> =
        accept(visitor, data) as AstTransformResult<E>

    fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstElement

}

fun AstElement.accept(visitor: AstVisitorVoid) = accept(visitor, null)

fun AstElement.acceptChildren(visitor: AstVisitorVoid) = acceptChildren(visitor, null)

fun <E : AstElement> E.transform(transformer: AstTransformerVoid): AstTransformResult<E> =
    transform(transformer, null)

fun AstElement.transformChildren(visitor: AstTransformerVoid) = transformChildren(visitor, null)

interface AstAnnotationContainer {
    val annotations: MutableList<AstCall>
}

interface AstModifierContainer {
    val modifiers: List<AstModifier>
}

operator fun AstModifierContainer.contains(modifier: AstModifier) = modifier in modifiers

enum class AstModifier {
    ABSTRACT, FINAL, OPEN, ANNOTATION, COMPANION, OBJECT, SEALED, DATA, OVERRIDE, LATEINIT, INNER,
    PRIVATE, PROTECTED, PUBLIC, INTERNAL,
    IN, OUT, NOINLINE, CROSSINLINE, VARARG, REIFIED,
    TAILREC, OPERATOR, INFIX, INLINE, EXTERNAL, SUSPEND, CONST,
    ACTUAL, EXPECT
}

interface AstTypeParameterContainer {
    val typeParameters: MutableList<AstTypeParameter>
}

interface AstTypeParameter

interface AstType : AstAnnotationContainer {
    val classifier: AstClassifier
    val hasQuestionMark: Boolean
    val arguments: List<IrTypeArgument>
    val abbreviation: AstTypeAbbreviation?
}

interface AstTypeAbbreviation

interface AstDeclarationContainer {
    val declarations: MutableList<AstDeclaration>
}

interface AstStatement : AstElement

interface AstDeclaration : AstStatement {
    val name: String
}

interface AstExpression : AstStatement {

}

interface AstCall : AstExpression {
    val callee: AstFunction
}

interface AstClassifier

interface AstClass : AstDeclaration, AstAnnotationContainer, AstModifierContainer,
    AstTypeParameterContainer, AstDeclarationContainer

class AstClassImpl(
    override val name: String,
    override val annotations: MutableList<AstCall> = mutableListOf(),
    override val modifiers: List<AstModifier> = mutableListOf(),
    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf(),
    override val declarations: MutableList<AstDeclaration> = mutableListOf()
) : AstClass {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitClass(this, data)

    override fun <D> acceptChildren(visitor: AstVisitor<*, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstElement {
        annotations.transformInplace(transformer, data)
        declarations.transformInplace(transformer, data)
        return this
    }

}

interface AstFunction : AstDeclaration, AstAnnotationContainer, AstModifierContainer,
    AstTypeParameterContainer

interface AstFile : AstDeclaration, AstAnnotationContainer, AstDeclarationContainer {
    var packageFqName: String
}

class AstFileImpl(
    override var packageFqName: String,
    override var name: String,
    override val annotations: MutableList<AstCall> = mutableListOf(),
    override val declarations: MutableList<AstDeclaration> = mutableListOf()
) : AstFile {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitFile(this, data)

    override fun <D> acceptChildren(visitor: AstVisitor<*, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstElement {
        annotations.transformInplace(transformer, data)
        declarations.transformInplace(transformer, data)
        return this
    }

}

