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
    override var name: String,
    override var packageFqName: FqName,
) : AstFile() {
    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstFileImpl {
        annotations.transformInplace(transformer, data)
        declarations.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceDeclarations(newDeclarations: List<AstDeclaration>) {
        declarations.clear()
        declarations.addAll(newDeclarations)
    }

    override fun replaceName(newName: String) {
        name = newName
    }

    override fun replacePackageFqName(newPackageFqName: FqName) {
        packageFqName = newPackageFqName
    }
}
