package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.expressions.AstOperation
import com.ivianuu.ast.expressions.AstTypeOperatorCall
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.impl.AstImplicitTypeImpl
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstTypeOperatorCallImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override var argumentList: AstArgumentList,
    override val operation: AstOperation,
    override var conversionType: AstType,
) : AstTypeOperatorCall() {
    override var type: AstType = AstImplicitTypeImpl()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        argumentList.accept(visitor, data)
        conversionType.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstTypeOperatorCallImpl {
        transformConversionType(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstTypeOperatorCallImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformConversionType(transformer: AstTransformer<D>, data: D): AstTypeOperatorCallImpl {
        conversionType = conversionType.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstTypeOperatorCallImpl {
        type = type.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        argumentList = argumentList.transformSingle(transformer, data)
        return this
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }

    override fun replaceArgumentList(newArgumentList: AstArgumentList) {
        argumentList = newArgumentList
    }
}
