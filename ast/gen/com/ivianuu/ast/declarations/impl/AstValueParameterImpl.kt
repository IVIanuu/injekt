package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstValueParameter
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

open class AstValueParameterImpl @AstImplementationDetail constructor(
    override val origin: AstDeclarationOrigin,
    override var returnTypeRef: AstTypeRef,
    override val name: Name,
    override val symbol: AstVariableSymbol<AstValueParameter>,
    override val annotations: MutableList<AstAnnotationCall>,
    override var defaultValue: AstExpression?,
    override val isCrossinline: Boolean,
    override val isNoinline: Boolean,
    override val isVararg: Boolean,
) : AstValueParameter() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val receiverTypeRef: AstTypeRef? get() = null
    override val initializer: AstExpression? get() = null
    override val delegate: AstExpression? get() = null
    override val delegateFieldSymbol: AstDelegateFieldSymbol<AstValueParameter>? get() = null
    override val isVar: Boolean get() = false
    override val isVal: Boolean get() = true
    override val getter: AstPropertyAccessor? get() = null
    override val setter: AstPropertyAccessor? get() = null

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        defaultValue?.accept(visitor, data)
    }

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstValueParameterImpl {
        transformReturnTypeRef(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(
        transformer: AstTransformer<D>,
        data: D
    ): AstValueParameterImpl {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(
        transformer: AstTransformer<D>,
        data: D
    ): AstValueParameterImpl {
        return this
    }

    override fun <D> transformInitializer(
        transformer: AstTransformer<D>,
        data: D
    ): AstValueParameterImpl {
        return this
    }

    override fun <D> transformDelegate(
        transformer: AstTransformer<D>,
        data: D
    ): AstValueParameterImpl {
        return this
    }

    override fun <D> transformGetter(
        transformer: AstTransformer<D>,
        data: D
    ): AstValueParameterImpl {
        return this
    }

    override fun <D> transformSetter(
        transformer: AstTransformer<D>,
        data: D
    ): AstValueParameterImpl {
        return this
    }

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstValueParameterImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstValueParameterImpl {
        transformAnnotations(transformer, data)
        defaultValue = defaultValue?.transformSingle(transformer, data)
        return this
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: AstTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceReceiverTypeRef(newReceiverTypeRef: AstTypeRef?) {}
}
