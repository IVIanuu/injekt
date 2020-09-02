package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstAnonymousObject
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstAnonymousObjectSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.ClassKind
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstAnonymousObjectImpl(
    override val origin: AstDeclarationOrigin,
    override val classKind: ClassKind,
    override val superTypes: MutableList<AstType>,
    override val declarations: MutableList<AstDeclaration>,
    override val annotations: MutableList<AstFunctionCall>,
    override var type: AstType,
    override val symbol: AstAnonymousObjectSymbol,
) : AstAnonymousObject() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        superTypes.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
        annotations.forEach { it.accept(visitor, data) }
        type.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstAnonymousObjectImpl {
        superTypes.transformInplace(transformer, data)
        declarations.transformInplace(transformer, data)
        annotations.transformInplace(transformer, data)
        type = type.transformSingle(transformer, data)
        return this
    }
}
