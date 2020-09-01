package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.expressions.AstStringConcatenationCall
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.impl.AstImplicitStringType
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstStringConcatenationCallImpl(
    override val annotations: MutableList<AstAnnotationCall>,
    override var argumentList: AstArgumentList,
) : AstStringConcatenationCall() {
    override var type: AstType = AstImplicitStringType()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        argumentList.accept(visitor, data)
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstStringConcatenationCallImpl {
        transformAnnotations(transformer, data)
        argumentList = argumentList.transformSingle(transformer, data)
        type = type.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstStringConcatenationCallImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceArgumentList(newArgumentList: AstArgumentList) {
        argumentList = newArgumentList
    }

    override fun replaceType(newType: AstType) {
        type = newType
    }
}
