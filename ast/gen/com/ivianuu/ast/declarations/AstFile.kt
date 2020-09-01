package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.name.FqName

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstFile : AstPureAbstractElement(), AstAnnotatedDeclaration {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val annotations: List<AstAnnotationCall>
    abstract val imports: List<AstImport>
    abstract val declarations: List<AstDeclaration>
    abstract val name: String
    abstract val packageFqName: FqName

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitFile(this, data)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstFile

    abstract fun <D> transformImports(transformer: AstTransformer<D>, data: D): AstFile

    abstract fun <D> transformDeclarations(transformer: AstTransformer<D>, data: D): AstFile
}
