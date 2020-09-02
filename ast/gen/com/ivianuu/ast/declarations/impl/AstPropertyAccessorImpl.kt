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
    override val annotations: MutableList<AstFunctionCall>,
    override var returnType: AstType,
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
        annotations.forEach { it.accept(visitor, data) }
        returnType.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstPropertyAccessorImpl {
        transformAnnotations(transformer, data)
        transformReturnType(transformer, data)
        transformValueParameters(transformer, data)
        transformBody(transformer, data)
        transformTypeParameters(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstPropertyAccessorImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformReturnType(transformer: AstTransformer<D>, data: D): AstPropertyAccessorImpl {
        returnType = returnType.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverType(transformer: AstTransformer<D>, data: D): AstPropertyAccessorImpl {
        return this
    }

    override fun <D> transformValueParameters(transformer: AstTransformer<D>, data: D): AstPropertyAccessorImpl {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformBody(transformer: AstTransformer<D>, data: D): AstPropertyAccessorImpl {
        body = body?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstPropertyAccessorImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun replaceReturnType(newReturnType: AstType) {
        returnType = newReturnType
    }

    override fun replaceReceiverType(newReceiverType: AstType?) {}

    override fun replaceValueParameters(newValueParameters: List<AstValueParameter>) {
        valueParameters.clear()
        valueParameters.addAll(newValueParameters)
    }
}
