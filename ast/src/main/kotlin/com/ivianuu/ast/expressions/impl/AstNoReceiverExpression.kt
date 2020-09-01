/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.impl.AstImplicitTypeImpl
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor

object AstNoReceiverExpression : AstExpression() {
    override val type: AstType = AstImplicitTypeImpl()
    override val annotations: List<AstAnnotationCall> get() = emptyList()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstNoReceiverExpression {
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstExpression {
        return this
    }

    override fun replaceType(newType: AstType) {}
}