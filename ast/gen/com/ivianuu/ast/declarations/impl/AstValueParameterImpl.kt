package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstValueParameterSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

open class AstValueParameterImpl @AstImplementationDetail constructor(
    override val origin: AstDeclarationOrigin,
    override var returnType: AstType,
    override val name: Name,
    override val annotations: MutableList<AstFunctionCall>,
    override val symbol: AstValueParameterSymbol,
    override var defaultValue: AstExpression?,
    override val isCrossinline: Boolean,
    override val isNoinline: Boolean,
    override val isVararg: Boolean,
) : AstValueParameter() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val receiverType: AstType? get() = null
    override val initializer: AstExpression? get() = null
    override val delegate: AstExpression? get() = null
    override val isVar: Boolean get() = false
    override val isVal: Boolean get() = true
    override val getter: AstPropertyAccessor? get() = null
    override val setter: AstPropertyAccessor? get() = null

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        returnType.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        defaultValue?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstValueParameterImpl {
        returnType = returnType.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        defaultValue = defaultValue?.transformSingle(transformer, data)
        return this
    }
}
