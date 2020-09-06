package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstEnumEntry
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstClassSymbol
import com.ivianuu.ast.symbols.impl.AstEnumEntrySymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstEnumEntryImpl(
    override val context: AstContext,
    override val annotations: MutableList<AstFunctionCall>,
    override val origin: AstDeclarationOrigin,
    override val declarations: MutableList<AstDeclaration>,
    override var name: Name,
    override var symbol: AstEnumEntrySymbol,
) : AstEnumEntry() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val classKind: ClassKind get() = ClassKind.ENUM_ENTRY
    override val superTypes: List<AstType> get() = emptyList()

    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstEnumEntryImpl {
        annotations.transformInplace(transformer, data)
        declarations.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceDeclarations(newDeclarations: List<AstDeclaration>) {
        declarations.clear()
        declarations.addAll(newDeclarations)
    }

    override fun replaceClassKind(newClassKind: ClassKind) {}

    override fun replaceSuperTypes(newSuperTypes: List<AstType>) {}
}
