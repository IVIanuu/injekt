package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstPropertyAccessorSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.Modality
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

open class AstPropertyAccessorImpl @AstImplementationDetail constructor(
    override val origin: AstDeclarationOrigin,
    override var returnType: AstType,
    override val annotations: MutableList<AstFunctionCall>,
    override val valueParameters: MutableList<AstValueParameter>,
    override var body: AstBlock?,
    override val typeParameters: MutableList<AstTypeParameter>,
    override val symbol: AstPropertyAccessorSymbol,
    override val isGetter: Boolean,
) : AstPropertyAccessor() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val receiverType: AstType? get() = null
    override val isSetter: Boolean get() = !isGetter

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        returnType.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstPropertyAccessorImpl {
        returnType = returnType.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        valueParameters.transformInplace(transformer, data)
        body = body?.transformSingle(transformer, data)
        typeParameters.transformInplace(transformer, data)
        return this
    }
}
