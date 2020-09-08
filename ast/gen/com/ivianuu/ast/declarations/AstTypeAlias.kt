package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibility
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

abstract class AstTypeAlias : AstClassLikeDeclaration<AstTypeAlias>, AstMemberDeclaration(), AstTypeParametersOwner {
    abstract override val context: AstContext
    abstract override val annotations: List<AstFunctionCall>
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val name: Name
    abstract override val visibility: Visibility
    abstract override val modality: Modality
    abstract override val platformStatus: PlatformStatus
    abstract override val typeParameters: List<AstTypeParameter>
    abstract override val symbol: AstTypeAliasSymbol
    abstract val expandedType: AstType

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitTypeAlias(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceOrigin(newOrigin: AstDeclarationOrigin)

    abstract override fun replaceAttributes(newAttributes: AstDeclarationAttributes)

    abstract override fun replaceVisibility(newVisibility: Visibility)

    abstract override fun replaceModality(newModality: Modality)

    abstract override fun replacePlatformStatus(newPlatformStatus: PlatformStatus)

    abstract override fun replaceTypeParameters(newTypeParameters: List<AstTypeParameter>)

    abstract fun replaceExpandedType(newExpandedType: AstType)
}
