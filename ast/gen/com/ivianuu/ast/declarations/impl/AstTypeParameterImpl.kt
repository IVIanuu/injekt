package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformInplace
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstTypeParameterImpl(
    override val origin: AstDeclarationOrigin,
    override val name: Name,
    override val symbol: AstTypeParameterSymbol,
    override val variance: Variance,
    override val isReified: Boolean,
    override val bounds: MutableList<AstTypeRef>,
    override val annotations: MutableList<AstAnnotationCall>,
) : AstTypeParameter() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        bounds.forEach { it.accept(visitor, data) }
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstTypeParameterImpl {
        bounds.transformInplace(transformer, data)
        transformAnnotations(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstTypeParameterImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceBounds(newBounds: List<AstTypeRef>) {
        bounds.clear()
        bounds.addAll(newBounds)
    }
}
