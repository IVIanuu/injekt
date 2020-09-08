package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstEnumEntry
import com.ivianuu.ast.expressions.AstDelegateInitializer
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
    override var origin: AstDeclarationOrigin,
    override var attributes: AstDeclarationAttributes,
    override val declarations: MutableList<AstDeclaration>,
    override val delegateInitializers: MutableList<AstDelegateInitializer>,
    override var name: Name,
    override var symbol: AstEnumEntrySymbol,
    override var initializer: AstFunctionCall,
) : AstEnumEntry() {
    override val classKind: ClassKind get() = ClassKind.ENUM_ENTRY
    override val superTypes: List<AstType> get() = emptyList()

    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
        delegateInitializers.forEach { it.accept(visitor, data) }
        initializer.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstEnumEntryImpl {
        annotations.transformInplace(transformer, data)
        declarations.transformInplace(transformer, data)
        delegateInitializers.transformInplace(transformer, data)
        initializer = initializer.transformSingle(transformer, data)
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

    override fun replaceDeclarations(newDeclarations: List<AstDeclaration>) {
        declarations.clear()
        declarations.addAll(newDeclarations)
    }

    override fun replaceClassKind(newClassKind: ClassKind) {}

    override fun replaceSuperTypes(newSuperTypes: List<AstType>) {}

    override fun replaceDelegateInitializers(newDelegateInitializers: List<AstDelegateInitializer>) {
        delegateInitializers.clear()
        delegateInitializers.addAll(newDelegateInitializers)
    }

    override fun replaceInitializer(newInitializer: AstFunctionCall) {
        initializer = newInitializer
    }
}
