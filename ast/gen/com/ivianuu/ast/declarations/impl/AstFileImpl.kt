package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstFile
import com.ivianuu.ast.expressions.AstFunctionCall
import org.jetbrains.kotlin.name.FqName
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstFileImpl(
    override val annotations: MutableList<AstFunctionCall>,
    override val declarations: MutableList<AstDeclaration>,
    override val name: String,
    override val packageFqName: FqName,
) : AstFile() {
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
