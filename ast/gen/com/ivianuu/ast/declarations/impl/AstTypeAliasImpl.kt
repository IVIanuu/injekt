package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.Visibility
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstTypeAlias
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstTypeAliasSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstTypeAliasImpl(
    override val origin: AstDeclarationOrigin,
    override val typeParameters: MutableList<AstTypeParameter>,
    override val name: Name,
    override val visibility: Visibility,
    override val isExpect: Boolean,
    override val isActual: Boolean,
    override val modality: Modality,
    override val symbol: AstTypeAliasSymbol,
    override var expandedType: AstType,
    override val annotations: MutableList<AstFunctionCall>,
) : AstTypeAlias() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }
        expandedType.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstTypeAliasImpl {
        transformTypeParameters(transformer, data)
        expandedType = expandedType.transformSingle(transformer, data)
        transformAnnotations(transformer, data)
        return this
    }

    override fun <D> transformTypeParameters(transformer: AstTransformer<D>, data: D): AstTypeAliasImpl {
        typeParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstTypeAliasImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceExpandedType(newExpandedType: AstType) {
        expandedType = newExpandedType
    }
}
