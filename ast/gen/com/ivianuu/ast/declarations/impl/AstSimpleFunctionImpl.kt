package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstDeclarationStatus
import com.ivianuu.ast.declarations.AstSimpleFunction
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
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

open class AstSimpleFunctionImpl @AstImplementationDetail constructor(
    override val origin: AstDeclarationOrigin,
    override var returnTypeRef: AstTypeRef,
    override var receiverTypeRef: AstTypeRef?,
    override val valueParameters: MutableList<AstValueParameter>,
    override var body: AstBlock?,
    override var status: AstDeclarationStatus,
    override val name: Name,
    override val symbol: AstFunctionSymbol<AstSimpleFunction>,
    override val annotations: MutableList<AstAnnotationCall>,
    override val typeParameters: MutableList<AstTypeParameter>,
) : AstSimpleFunction() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        returnTypeRef.accept(visitor, data)
        receiverTypeRef?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
        status.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstSimpleFunctionImpl {
        transformReturnTypeRef(transformer, data)
        transformReceiverTypeRef(transformer, data)
        transformValueParameters(transformer, data)
        transformBody(transformer, data)
        transformStatus(transformer, data)
        transformAnnotations(transformer, data)
        transformTypeParameters(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(
        transformer: AstTransformer<D>,
        data: D
    ): AstSimpleFunctionImpl {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverTypeRef(
        transformer: AstTransformer<D>,
        data: D
    ): AstSimpleFunctionImpl {
        receiverTypeRef = receiverTypeRef?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformValueParameters(
        transformer: AstTransformer<D>,
        data: D
    ): AstSimpleFunctionImpl {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformBody(transformer: AstTransformer<D>, data: D): AstSimpleFunctionImpl {
        body = body?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformStatus(
        transformer: AstTransformer<D>,
        data: D
    ): AstSimpleFunctionImpl {
        status = status.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstSimpleFunctionImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(
        transformer: AstTransformer<D>,
        data: D
    ): AstSimpleFunctionImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun replaceReturnTypeRef(newReturnTypeRef: AstTypeRef) {
        returnTypeRef = newReturnTypeRef
    }

    override fun replaceReceiverTypeRef(newReceiverTypeRef: AstTypeRef?) {
        receiverTypeRef = newReceiverTypeRef
    }

    override fun replaceValueParameters(newValueParameters: List<AstValueParameter>) {
        valueParameters.clear()
        valueParameters.addAll(newValueParameters)
    }
}
