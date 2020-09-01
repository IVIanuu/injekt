package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstDeclarationStatus
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.symbols.impl.AstBackingFieldSymbol
import com.ivianuu.ast.symbols.impl.AstDelegateFieldSymbol
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformInplace
import com.ivianuu.ast.visitors.transformSingle
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstPropertyImpl(
    override val origin: AstDeclarationOrigin,
    override var returnTypeRef: AstTypeRef,
    override var receiverTypeRef: AstTypeRef?,
    override val name: Name,
    override var initializer: AstExpression?,
    override var delegate: AstExpression?,
    override val delegateFieldSymbol: AstDelegateFieldSymbol<AstProperty>?,
    override val isVar: Boolean,
    override var getter: AstPropertyAccessor?,
    override var setter: AstPropertyAccessor?,
    override val annotations: MutableList<AstAnnotationCall>,
    override val typeParameters: MutableList<AstTypeParameter>,
    override val symbol: AstPropertySymbol,
    override val isLocal: Boolean,
    override var status: AstDeclarationStatus,
) : AstProperty() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val isVal: Boolean get() = !isVar
    override val backingFieldSymbol: AstBackingFieldSymbol =
        AstBackingFieldSymbol(symbol.callableId)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        receiverTypeRef?.accept(visitor, data)
        initializer?.accept(visitor, data)
        delegate?.accept(visitor, data)
        getter?.accept(visitor, data)
        setter?.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstPropertyImpl {
        transformReturnTypeRef(transformer, data)
        transformReceiverTypeRef(transformer, data)
        transformInitializer(transformer, data)
        transformDelegate(transformer, data)
        transformGetter(transformer, data)
        transformSetter(transformer, data)
        transformTypeParameters(transformer, data)
        transformStatus(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(
        transformer: AstTransformer<D>,
        data: D
    ): AstPropertyImpl {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(
        transformer: AstTransformer<D>,
        data: D
    ): AstPropertyImpl {
        receiverTypeRef = receiverTypeRef?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformInitializer(
        transformer: AstTransformer<D>,
        data: D
    ): AstPropertyImpl {
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

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstPropertyImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(
        transformer: AstTransformer<D>,
        data: D
    ): AstPropertyImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: AstTransformer<D>, data: D): AstPropertyImpl {
        status = status.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstPropertyImpl {
        transformAnnotations(transformer, data)
        return this
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: AstTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceReceiverTypeRef(newReceiverTypeRef: AstTypeRef?) {
        receiverTypeRef = newReceiverTypeRef
    }
}
