package com.ivianuu.ast.declarations.impl

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

internal class AstDefaultSetterValueParameter(
    override val origin: AstDeclarationOrigin,
    override var receiverType: AstType?,
    override var returnType: AstType,
    override var initializer: AstExpression?,
    override var delegate: AstExpression?,
    override val isVar: Boolean,
    override val isVal: Boolean,
    override var getter: AstPropertyAccessor?,
    override var setter: AstPropertyAccessor?,
    override val annotations: MutableList<AstFunctionCall>,
    override val symbol: AstValueParameterSymbol,
    override var defaultValue: AstExpression?,
    override val isCrossinline: Boolean,
    override val isNoinline: Boolean,
    override val isVararg: Boolean,
) : AstValueParameter() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val name: Name = Name.identifier("value")

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        receiverType?.accept(visitor, data)
        returnType.accept(visitor, data)
        initializer?.accept(visitor, data)
        delegate?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        defaultValue?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstDefaultSetterValueParameter {
        receiverType = receiverType?.transformSingle(transformer, data)
        returnType = returnType.transformSingle(transformer, data)
        initializer = initializer?.transformSingle(transformer, data)
        delegate = delegate?.transformSingle(transformer, data)
        getter = getter?.transformSingle(transformer, data)
        setter = setter?.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        defaultValue = defaultValue?.transformSingle(transformer, data)
        return this
    }
}
