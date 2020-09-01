package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.expressions.AstEqualityOperatorCall
import com.ivianuu.ast.expressions.AstOperation
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.impl.AstImplicitBooleanType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstEqualityOperatorCallImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override var argumentList: AstArgumentList,
    override val operation: AstOperation,
) : AstEqualityOperatorCall() {
    override var type: AstType = AstImplicitBooleanType()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        argumentList.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstEqualityOperatorCallImpl {
        type = type.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        argumentList = argumentList.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstEqualityOperatorCallImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }

    override fun replaceArgumentList(newArgumentList: AstArgumentList) {
        argumentList = newArgumentList
    }
}
