package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.Visibility
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstField
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstBackingFieldSymbol
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
    override var returnType: AstType,
    override var receiverType: AstType?,
    override val name: Name,
    override var initializer: AstExpression?,
    override var delegate: AstExpression?,
    override val isVar: Boolean,
    override var getter: AstPropertyAccessor?,
    override var setter: AstPropertyAccessor?,
    override val annotations: MutableList<AstFunctionCall>,
    override val typeParameters: MutableList<AstTypeParameter>,
    override val symbol: AstPropertySymbol,
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
    override val backingFieldSymbol: AstBackingFieldSymbol = AstBackingFieldSymbol(symbol.callableId)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        returnType.accept(visitor, data)
        receiverType?.accept(visitor, data)
        initializer?.accept(visitor, data)
        delegate?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstPropertyImpl {
        transformReturnType(transformer, data)
        transformReceiverType(transformer, data)
        transformInitializer(transformer, data)
        transformDelegate(transformer, data)
        transformGetter(transformer, data)
        transformSetter(transformer, data)
        transformTypeParameters(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformReturnType(transformer: AstTransformer<D>, data: D): AstPropertyImpl {
        returnType = returnType.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverType(transformer: AstTransformer<D>, data: D): AstPropertyImpl {
        receiverType = receiverType?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformInitializer(transformer: AstTransformer<D>, data: D): AstPropertyImpl {
        initializer = initializer?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformDelegate(transformer: AstTransformer<D>, data: D): AstPropertyImpl {
        delegate = delegate?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformGetter(transformer: AstTransformer<D>, data: D): AstPropertyImpl {
        getter = getter?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformSetter(transformer: AstTransformer<D>, data: D): AstPropertyImpl {
        setter = setter?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstPropertyImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstPropertyImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstPropertyImpl {
        transformAnnotations(transformer, data)
        return this
    }

    override fun replaceReturnType(newReturnType: AstType) {
        returnType = newReturnType
    }

    override fun replaceReceiverType(newReceiverType: AstType?) {
        receiverType = newReceiverType
    }
}
