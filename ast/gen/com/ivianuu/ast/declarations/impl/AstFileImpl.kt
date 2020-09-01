package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstFile
import com.ivianuu.ast.expressions.AstAnnotationCall
import org.jetbrains.kotlin.name.FqName
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstFileImpl(
    override val origin: AstDeclarationOrigin,
    override val annotations: MutableList<AstAnnotationCall>,
    override val declarations: MutableList<AstDeclaration>,
    override val name: String,
    override val packageFqName: FqName,
) : AstFile() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstFileImpl {
        transformAnnotations(transformer, data)
        transformDeclarations(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstFileImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformDeclarations(transformer: AstTransformer<D>, data: D): AstFileImpl {
        declarations.transformInplace(transformer, data)
        return this
    }
}
