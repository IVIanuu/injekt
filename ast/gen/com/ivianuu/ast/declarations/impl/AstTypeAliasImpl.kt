package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstTypeAlias
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstClassLikeSymbol
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
    override val context: AstContext,
    override val annotations: MutableList<AstFunctionCall>,
    override var origin: AstDeclarationOrigin,
    override var attributes: AstDeclarationAttributes,
    override var name: Name,
    override var visibility: Visibility,
    override var modality: Modality,
    override var platformStatus: PlatformStatus,
    override val typeParameters: MutableList<AstTypeParameter>,
    override var symbol: AstTypeAliasSymbol,
    override var expandedType: AstType,
) : AstTypeAlias() {
    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        expandedType.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstTypeAliasImpl {
        annotations.transformInplace(transformer, data)
        typeParameters.transformInplace(transformer, data)
        expandedType = expandedType.transformSingle(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceOrigin(newOrigin: AstDeclarationOrigin) {
        origin = newOrigin
    }

    override fun replaceAttributes(newAttributes: AstDeclarationAttributes) {
        attributes = newAttributes
    }

    override fun replaceVisibility(newVisibility: Visibility) {
        visibility = newVisibility
    }

    override fun replaceModality(newModality: Modality) {
        modality = newModality
    }

    override fun replacePlatformStatus(newPlatformStatus: PlatformStatus) {
        platformStatus = newPlatformStatus
    }

    override fun replaceTypeParameters(newTypeParameters: List<AstTypeParameter>) {
        typeParameters.clear()
        typeParameters.addAll(newTypeParameters)
    }

    override fun replaceExpandedType(newExpandedType: AstType) {
        expandedType = newExpandedType
    }
}
