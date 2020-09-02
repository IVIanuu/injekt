package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstDefaultSetterValueParameter(
    override val origin: AstDeclarationOrigin,
    override var returnType: AstType,
    override var receiverType: AstType?,
    override val symbol: AstVariableSymbol<AstValueParameter>,
    override var initializer: AstExpression?,
    override var delegate: AstExpression?,
    override val isVar: Boolean,
    override val isVal: Boolean,
    override var getter: AstPropertyAccessor?,
    override var setter: AstPropertyAccessor?,
    override val annotations: MutableList<AstFunctionCall>,
    override var defaultValue: AstExpression?,
    override val isCrossinline: Boolean,
    override val isNoinline: Boolean,
    override val isVararg: Boolean,
) : AstValueParameter() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val name: Name = Name.identifier("value")

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        returnType.accept(visitor, data)
        receiverType?.accept(visitor, data)
        initializer?.accept(visitor, data)
        delegate?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        defaultValue?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstDefaultSetterValueParameter {
        transformReturnType(transformer, data)
        transformReceiverType(transformer, data)
        transformInitializer(transformer, data)
        transformDelegate(transformer, data)
        transformGetter(transformer, data)
        transformSetter(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformReturnType(transformer: AstTransformer<D>, data: D): AstDefaultSetterValueParameter {
        returnType = returnType.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverType(transformer: AstTransformer<D>, data: D): AstDefaultSetterValueParameter {
        receiverType = receiverType?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformInitializer(transformer: AstTransformer<D>, data: D): AstDefaultSetterValueParameter {
        initializer = initializer?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformDelegate(transformer: AstTransformer<D>, data: D): AstDefaultSetterValueParameter {
        delegate = delegate?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformGetter(transformer: AstTransformer<D>, data: D): AstDefaultSetterValueParameter {
        getter = getter?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformSetter(transformer: AstTransformer<D>, data: D): AstDefaultSetterValueParameter {
        setter = setter?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstDefaultSetterValueParameter {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstDefaultSetterValueParameter {
        transformAnnotations(transformer, data)
        defaultValue = defaultValue?.transformSingle(transformer, data)
        return this
    }

    override fun replaceReturnType(newReturnType: AstType) {
        returnType = newReturnType
    }

    override fun replaceReceiverType(newReceiverType: AstType?) {
        receiverType = newReceiverType
    }
}
