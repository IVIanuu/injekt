package com.ivianuu.ast.expressions

import com.ivianuu.ast.symbols.impl.AstClassLikeSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstResolvedQualifier : AstExpression() {
    abstract override val type: AstType
    abstract override val annotations: List<AstFunctionCall>
    abstract val packageFqName: FqName
    abstract val relativeClassFqName: FqName?
    abstract val classId: ClassId?
    abstract val symbol: AstClassLikeSymbol<*>?
    abstract val isNullableLHSForCallableReference: Boolean
    abstract val typeArguments: List<AstTypeProjection>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitResolvedQualifier(this, data)

    abstract override fun replaceType(newType: AstType)

    abstract fun replaceIsNullableLHSForCallableReference(newIsNullableLHSForCallableReference: Boolean)

    abstract fun replaceTypeArguments(newTypeArguments: List<AstTypeProjection>)

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstResolvedQualifier

    abstract fun <D> transformTypeArguments(transformer: AstTransformer<D>, data: D): AstResolvedQualifier
}
