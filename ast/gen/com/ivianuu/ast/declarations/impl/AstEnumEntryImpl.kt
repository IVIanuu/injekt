package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstEnumEntry
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstCallableSymbol
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstEnumEntryImpl(
    override val annotations: MutableList<AstFunctionCall>,
    override val origin: AstDeclarationOrigin,
    override var returnType: AstType,
    override var name: Name,
    override var symbol: AstVariableSymbol<AstEnumEntry>,
    override var initializer: AstExpression?,
) : AstEnumEntry() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val receiverType: AstType? get() = null
    override val delegate: AstExpression? get() = null
    override val isVar: Boolean get() = false
    override val getter: AstPropertyAccessor? get() = null
    override val setter: AstPropertyAccessor? get() = null

    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        returnType.accept(visitor, data)
        initializer?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstEnumEntryImpl {
        annotations.transformInplace(transformer, data)
        returnType = returnType.transformSingle(transformer, data)
        initializer = initializer?.transformSingle(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceReceiverType(newReceiverType: AstType?) {}

    override fun replaceReturnType(newReturnType: AstType) {
        returnType = newReturnType
    }

    override fun replaceInitializer(newInitializer: AstExpression?) {
        initializer = newInitializer
    }

    override fun replaceDelegate(newDelegate: AstExpression?) {}

    override fun replaceIsVar(newIsVar: Boolean) {}

    override fun replaceGetter(newGetter: AstPropertyAccessor?) {}

    override fun replaceSetter(newSetter: AstPropertyAccessor?) {}
}
