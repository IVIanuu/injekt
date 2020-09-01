/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.types.impl

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.types.AstQualifierPart
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.AstUserTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformInplace

open class AstUserTypeRefImpl(
    override val isMarkedNullable: Boolean,
    override val qualifier: MutableList<AstQualifierPart>,
    override val annotations: MutableList<AstAnnotationCall>
) : AstUserTypeRef(), AstAnnotationContainer {

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        for (part in qualifier) {
            part.typeArgumentList.typeArguments.forEach { it.accept(visitor, data) }
        }
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstUserTypeRefImpl {
        for (part in qualifier) {
            (part.typeArgumentList.typeArguments as MutableList<AstTypeProjection>).transformInplace(
                transformer,
                data
            )
        }
        transformAnnotations(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstUserTypeRef {
        annotations.transformInplace(transformer, data)
        return this
    }
}