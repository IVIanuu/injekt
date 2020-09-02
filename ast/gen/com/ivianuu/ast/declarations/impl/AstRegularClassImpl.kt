package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.Visibility
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstRegularClass
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.expressions.AstFunctionCall
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
    override val origin: AstDeclarationOrigin,
    override val typeParameters: MutableList<AstTypeParameter>,
    override val classKind: ClassKind,
    override val declarations: MutableList<AstDeclaration>,
    override val annotations: MutableList<AstFunctionCall>,
    override val name: Name,
    override val visibility: Visibility,
    override val isExpect: Boolean,
    override val isActual: Boolean,
    override val modality: Modality,
    override val symbol: AstRegularClassSymbol,
    override val superTypes: MutableList<AstType>,
    override val isInline: Boolean,
    override val isCompanion: Boolean,
    override val isFun: Boolean,
    override val isData: Boolean,
    override val isInner: Boolean,
    override val isExternal: Boolean,
) : AstRegularClass() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
        annotations.forEach { it.accept(visitor, data) }
        superTypes.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstRegularClassImpl {
        typeParameters.transformInplace(transformer, data)
        declarations.transformInplace(transformer, data)
        annotations.transformInplace(transformer, data)
        superTypes.transformInplace(transformer, data)
        return this
    }
}
