package com.ivianuu.ast.expressions.impl

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstResolvedQualifier
import com.ivianuu.ast.symbols.impl.AstClassLikeSymbol
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import com.ivianuu.ast.visitors.transformInplace
import com.ivianuu.ast.visitors.transformSingle
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstResolvedQualifierImpl(
    override var typeRef: AstTypeRef,
    override val annotations: MutableList<AstAnnotationCall>,
    override var packageFqName: FqName,
    override var relativeClassFqName: FqName?,
    override val symbol: AstClassLikeSymbol<*>?,
    override var isNullableLHSForCallableReference: Boolean,
    override val typeArguments: MutableList<AstTypeProjection>,
) : AstResolvedQualifier() {
    override val classId: ClassId?
        get() = relativeClassFqName?.let {
            ClassId(packageFqName, it, false)
        }

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        typeRef.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        typeArguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstResolvedQualifierImpl {
        typeRef = typeRef.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        transformTypeArguments(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstResolvedQualifierImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformTypeArguments(
        transformer: AstTransformer<D>,
        data: D
    ): AstResolvedQualifierImpl {
        typeArguments.transformInplace(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: AstTypeRef) {
        typeRef = newTypeRef
    }

    override fun replaceIsNullableLHSForCallableReference(newIsNullableLHSForCallableReference: Boolean) {
        isNullableLHSForCallableReference = newIsNullableLHSForCallableReference
    }

    override fun replaceTypeArguments(newTypeArguments: List<AstTypeProjection>) {
        typeArguments.clear()
        typeArguments.addAll(newTypeArguments)
    }
}
