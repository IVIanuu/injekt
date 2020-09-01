package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstDeclarationStatus
import com.ivianuu.ast.declarations.AstField
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.symbols.impl.AstDelegateFieldSymbol
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
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

internal class AstFieldImpl(
    override val origin: AstDeclarationOrigin,
    override var returnTypeRef: AstTypeRef,
    override val name: Name,
    override val symbol: AstVariableSymbol<AstField>,
    override val isVar: Boolean,
    override val annotations: MutableList<AstAnnotationCall>,
    override val typeParameters: MutableList<AstTypeParameter>,
    override var status: AstDeclarationStatus,
) : AstField() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val receiverTypeRef: AstTypeRef? get() = null
    override val initializer: AstExpression? get() = null
    override val delegate: AstExpression? get() = null
    override val delegateFieldSymbol: AstDelegateFieldSymbol<AstField>? get() = null
    override val isVal: Boolean get() = !isVar
    override val getter: AstPropertyAccessor? get() = null
    override val setter: AstPropertyAccessor? get() = null

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstFieldImpl {
        transformReturnTypeRef(transformer, data)
        transformTypeParameters(transformer, data)
        transformStatus(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: AstTransformer<D>, data: D): AstFieldImpl {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(
        transformer: AstTransformer<D>,
        data: D
    ): AstFieldImpl {
        return this
    }

    override fun <D> transformInitializer(transformer: AstTransformer<D>, data: D): AstFieldImpl {
        return this
    }

    override fun <D> transformDelegate(transformer: AstTransformer<D>, data: D): AstFieldImpl {
        return this
    }

    override fun <D> transformGetter(transformer: AstTransformer<D>, data: D): AstFieldImpl {
        return this
    }

    override fun <D> transformSetter(transformer: AstTransformer<D>, data: D): AstFieldImpl {
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstFieldImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(
        transformer: AstTransformer<D>,
        data: D
    ): AstFieldImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformStatus(transformer: AstTransformer<D>, data: D): AstFieldImpl {
        status = status.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstFieldImpl {
        transformAnnotations(transformer, data)
        return this
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: AstTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceReceiverTypeRef(newReceiverTypeRef: AstTypeRef?) {}
}
