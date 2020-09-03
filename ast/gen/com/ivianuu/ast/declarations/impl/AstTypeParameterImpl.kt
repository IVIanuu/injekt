package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstTypeParameterImpl(
    override val annotations: MutableList<AstFunctionCall>,
    override val origin: AstDeclarationOrigin,
    override var name: Name,
    override var symbol: AstTypeParameterSymbol,
    override var variance: Variance,
    override var isReified: Boolean,
    override val bounds: MutableList<AstType>,
) : AstTypeParameter() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()

    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        bounds.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstTypeParameterImpl {
        annotations.transformInplace(transformer, data)
        bounds.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceVariance(newVariance: Variance) {
        variance = newVariance
    }

    override fun replaceIsReified(newIsReified: Boolean) {
        isReified = newIsReified
    }

    override fun replaceBounds(newBounds: List<AstType>) {
        bounds.clear()
        bounds.addAll(newBounds)
    }
}
