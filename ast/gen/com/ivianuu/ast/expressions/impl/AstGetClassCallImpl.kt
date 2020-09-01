package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstGetClassCall
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.impl.AstImplicitTypeImpl
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstGetClassCallImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override var argumentList: AstArgumentList,
) : AstGetClassCall() {
    override var type: AstType = AstImplicitTypeImpl()
    override val argument: AstExpression get() = argumentList.arguments.first()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        argumentList.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstGetClassCallImpl {
        type = type.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        argumentList = argumentList.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstGetClassCallImpl {
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
