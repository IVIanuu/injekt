package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.PlatformStatus
import com.ivianuu.ast.Visibility
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstRegularClass
import com.ivianuu.ast.declarations.AstTypeParameter
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

internal class AstRegularClassImpl(
    override val context: AstContext,
    override val annotations: MutableList<AstFunctionCall>,
    override val origin: AstDeclarationOrigin,
    override var name: Name,
    override var visibility: Visibility,
    override var modality: Modality,
    override var platformStatus: PlatformStatus,
    override val typeParameters: MutableList<AstTypeParameter>,
    override val declarations: MutableList<AstDeclaration>,
    override var classKind: ClassKind,
    override val delegateInitializers: MutableList<AstDelegateInitializer>,
    override var symbol: AstRegularClassSymbol,
    override val superTypes: MutableList<AstType>,
    override var isInline: Boolean,
    override var isCompanion: Boolean,
    override var isFun: Boolean,
    override var isData: Boolean,
    override var isInner: Boolean,
    override var isExternal: Boolean,
) : AstRegularClass() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()

    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
        delegateInitializers.forEach { it.accept(visitor, data) }
        superTypes.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstRegularClassImpl {
        annotations.transformInplace(transformer, data)
        typeParameters.transformInplace(transformer, data)
        declarations.transformInplace(transformer, data)
        delegateInitializers.transformInplace(transformer, data)
        superTypes.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
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

    override fun replaceDeclarations(newDeclarations: List<AstDeclaration>) {
        declarations.clear()
        declarations.addAll(newDeclarations)
    }

    override fun replaceClassKind(newClassKind: ClassKind) {
        classKind = newClassKind
    }

    override fun replaceDelegateInitializers(newDelegateInitializers: List<AstDelegateInitializer>) {
        delegateInitializers.clear()
        delegateInitializers.addAll(newDelegateInitializers)
    }

    override fun replaceSuperTypes(newSuperTypes: List<AstType>) {
        superTypes.clear()
        superTypes.addAll(newSuperTypes)
    }

    override fun replaceIsInline(newIsInline: Boolean) {
        isInline = newIsInline
    }

    override fun replaceIsCompanion(newIsCompanion: Boolean) {
        isCompanion = newIsCompanion
    }

    override fun replaceIsFun(newIsFun: Boolean) {
        isFun = newIsFun
    }

    override fun replaceIsData(newIsData: Boolean) {
        isData = newIsData
    }

    override fun replaceIsInner(newIsInner: Boolean) {
        isInner = newIsInner
    }

    override fun replaceIsExternal(newIsExternal: Boolean) {
        isExternal = newIsExternal
    }
}
