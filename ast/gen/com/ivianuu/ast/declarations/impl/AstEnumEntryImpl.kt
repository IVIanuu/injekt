package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstEnumEntry
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class AstEnumEntryImpl(
    override val origin: AstDeclarationOrigin,
    override var returnType: AstType,
    override val name: Name,
    override val symbol: AstVariableSymbol<AstEnumEntry>,
    override var initializer: AstExpression?,
    override val annotations: MutableList<AstFunctionCall>,
) : AstEnumEntry() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val receiverType: AstType? get() = null
    override val delegate: AstExpression? get() = null
    override val isVar: Boolean get() = false
    override val isVal: Boolean get() = true
    override val getter: AstPropertyAccessor? get() = null
    override val setter: AstPropertyAccessor? get() = null

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        returnType.accept(visitor, data)
        initializer?.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstEnumEntryImpl {
        transformReturnType(transformer, data)
        transformInitializer(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformReturnType(transformer: AstTransformer<D>, data: D): AstEnumEntryImpl {
        returnType = returnType.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverType(transformer: AstTransformer<D>, data: D): AstEnumEntryImpl {
        return this
    }

    override fun <D> transformInitializer(transformer: AstTransformer<D>, data: D): AstEnumEntryImpl {
        initializer = initializer?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformDelegate(transformer: AstTransformer<D>, data: D): AstEnumEntryImpl {
        return this
    }

    override fun <D> transformGetter(transformer: AstTransformer<D>, data: D): AstEnumEntryImpl {
        return this
    }

    override fun <D> transformSetter(transformer: AstTransformer<D>, data: D): AstEnumEntryImpl {
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstEnumEntryImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstEnumEntryImpl {
        transformAnnotations(transformer, data)
        return this
    }

    override fun replaceReturnType(newReturnType: AstType) {
        returnType = newReturnType
    }

    override fun replaceReceiverType(newReceiverType: AstType?) {}
}
