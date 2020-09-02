package com.ivianuu.ast.declarations

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

abstract class AstEnumEntry : AstVariable<AstEnumEntry>(), AstCallableMemberDeclaration<AstEnumEntry> {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val returnType: AstType
    abstract override val receiverType: AstType?
    abstract override val name: Name
    abstract override val symbol: AstVariableSymbol<AstEnumEntry>
    abstract override val initializer: AstExpression?
    abstract override val delegate: AstExpression?
    abstract override val isVar: Boolean
    abstract override val isVal: Boolean
    abstract override val getter: AstPropertyAccessor?
    abstract override val setter: AstPropertyAccessor?
    abstract override val annotations: List<AstFunctionCall>
    abstract override val status: AstDeclarationStatus

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitEnumEntry(this, data)

    abstract override fun replaceReturnType(newReturnType: AstType)

    abstract override fun replaceReceiverType(newReceiverType: AstType?)

    abstract override fun <D> transformReturnType(transformer: AstTransformer<D>, data: D): AstEnumEntry

    abstract override fun <D> transformReceiverType(transformer: AstTransformer<D>, data: D): AstEnumEntry

    abstract override fun <D> transformInitializer(transformer: AstTransformer<D>, data: D): AstEnumEntry

    abstract override fun <D> transformDelegate(transformer: AstTransformer<D>, data: D): AstEnumEntry

    abstract override fun <D> transformGetter(transformer: AstTransformer<D>, data: D): AstEnumEntry

    abstract override fun <D> transformSetter(transformer: AstTransformer<D>, data: D): AstEnumEntry

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstEnumEntry

    abstract override fun <D> transformStatus(transformer: AstTransformer<D>, data: D): AstEnumEntry

    abstract override fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstEnumEntry
}
