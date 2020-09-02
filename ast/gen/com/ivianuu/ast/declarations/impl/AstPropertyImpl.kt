package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.Visibility
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstPropertyImpl(
    override val origin: AstDeclarationOrigin,
    override var receiverType: AstType?,
    override var returnType: AstType,
    override val name: Name,
    override var initializer: AstExpression?,
    override var delegate: AstExpression?,
    override val isVar: Boolean,
    override var getter: AstPropertyAccessor?,
    override var setter: AstPropertyAccessor?,
    override val annotations: MutableList<AstFunctionCall>,
    override val typeParameters: MutableList<AstTypeParameter>,
    override val symbol: AstPropertySymbol,
    override val hasBackingField: Boolean,
    override val isLocal: Boolean,
    override val visibility: Visibility,
    override val isExpect: Boolean,
    override val isActual: Boolean,
    override val modality: Modality,
    override val isInline: Boolean,
    override val isConst: Boolean,
    override val isLateinit: Boolean,
) : AstProperty() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val isVal: Boolean get() = !isVar

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        receiverType?.accept(visitor, data)
        returnType.accept(visitor, data)
        initializer?.accept(visitor, data)
        delegate?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstPropertyImpl {
        receiverType = receiverType?.transformSingle(transformer, data)
        returnType = returnType.transformSingle(transformer, data)
        initializer = initializer?.transformSingle(transformer, data)
        delegate = delegate?.transformSingle(transformer, data)
        getter = getter?.transformSingle(transformer, data)
        setter = setter?.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
        typeParameters.transformInplace(transformer, data)
        return this
    }
}
