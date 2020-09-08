package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.expressions.AstDelegateInitializer
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstClassSymbol
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstRegularClass : AstMemberDeclaration(), AstTypeParametersOwner, AstClass<AstRegularClass> {
    abstract override val context: AstContext
    abstract override val annotations: List<AstFunctionCall>
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val name: Name
    abstract override val visibility: Visibility
    abstract override val modality: Modality
    abstract override val platformStatus: PlatformStatus
    abstract override val typeParameters: List<AstTypeParameter>
    abstract override val declarations: List<AstDeclaration>
    abstract override val classKind: ClassKind
    abstract override val delegateInitializers: List<AstDelegateInitializer>
    abstract override val symbol: AstRegularClassSymbol
    abstract override val superTypes: List<AstType>
    abstract val isInline: Boolean
    abstract val isCompanion: Boolean
    abstract val isFun: Boolean
    abstract val isData: Boolean
    abstract val isInner: Boolean
    abstract val isExternal: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitRegularClass(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceOrigin(newOrigin: AstDeclarationOrigin)

    abstract override fun replaceAttributes(newAttributes: AstDeclarationAttributes)

    abstract override fun replaceVisibility(newVisibility: Visibility)

    abstract override fun replaceModality(newModality: Modality)

    abstract override fun replacePlatformStatus(newPlatformStatus: PlatformStatus)

    abstract override fun replaceTypeParameters(newTypeParameters: List<AstTypeParameter>)

    abstract override fun replaceDeclarations(newDeclarations: List<AstDeclaration>)

    abstract override fun replaceClassKind(newClassKind: ClassKind)

    abstract override fun replaceDelegateInitializers(newDelegateInitializers: List<AstDelegateInitializer>)

    abstract override fun replaceSuperTypes(newSuperTypes: List<AstType>)

    abstract fun replaceIsInline(newIsInline: Boolean)

    abstract fun replaceIsCompanion(newIsCompanion: Boolean)

    abstract fun replaceIsFun(newIsFun: Boolean)

    abstract fun replaceIsData(newIsData: Boolean)

    abstract fun replaceIsInner(newIsInner: Boolean)

    abstract fun replaceIsExternal(newIsExternal: Boolean)
}
