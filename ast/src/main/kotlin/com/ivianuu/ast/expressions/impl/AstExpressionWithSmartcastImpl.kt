/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstExpressionWithSmartcast
import com.ivianuu.ast.expressions.AstQualifiedAccessExpression
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformSingle

internal class AstExpressionWithSmartcastImpl(
    override var originalExpression: AstQualifiedAccessExpression,
    override val type: AstType,
    override val typesFromSmartCast: Collection<AstType>
) : AstExpressionWithSmartcast() {

    override val annotations: List<AstAnnotationCall> get() = originalExpression.annotations
    override val typeArguments: List<AstTypeProjection> get() = originalExpression.typeArguments
    override val explicitReceiver: AstExpression? get() = originalExpression.explicitReceiver
    override val dispatchReceiver: AstExpression get() = originalExpression.dispatchReceiver
    override val extensionReceiver: AstExpression get() = originalExpression.extensionReceiver
    override val calleeReference: AstReference get() = originalExpression.calleeReference
    override val originalType: AstType get() = originalExpression.type

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstExpressionWithSmartcast {
        originalExpression = originalExpression.transformSingle(transformer, data)
        return this
    }

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        originalExpression.acceptChildren(visitor, data)
    }

    override fun <D> transformCalleeReference(
        transformer: AstTransformer<D>,
        data: D
    ): AstExpressionWithSmartcast {
        throw IllegalStateException()
    }

    override fun <D> transformExplicitReceiver(
        transformer: AstTransformer<D>,
        data: D
    ): AstExpressionWithSmartcast {
        throw IllegalStateException()
    }

    override fun <D> transformDispatchReceiver(
        transformer: AstTransformer<D>,
        data: D
    ): AstExpressionWithSmartcast {
        throw IllegalStateException()
    }

    override fun <D> transformExtensionReceiver(
        transformer: AstTransformer<D>,
        data: D
    ): AstExpressionWithSmartcast {
        throw IllegalStateException()
    }

    override fun <D> transformTypeArguments(
        transformer: AstTransformer<D>,
        data: D
    ): AstExpressionWithSmartcast {
        throw IllegalStateException()
    }

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstExpressionWithSmartcast {
        throw IllegalStateException()
    }

    override fun replaceTypeArguments(newTypeArguments: List<AstTypeProjection>) {
        throw IllegalStateException()
    }

    override fun replaceCalleeReference(newCalleeReference: AstReference) {
        throw IllegalStateException()
    }

    override fun replaceExplicitReceiver(newExplicitReceiver: AstExpression?) {
        throw IllegalStateException()
    }

    override fun replaceType(newType: AstType) {}
}
