package com.ivianuu.ast.declarations

import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.symbols.impl.AstDelegateFieldSymbol
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstValueParameter : AstVariable<AstValueParameter>() {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val returnTypeRef: AstTypeRef
    abstract override val receiverTypeRef: AstTypeRef?
    abstract override val name: Name
    abstract override val symbol: AstVariableSymbol<AstValueParameter>
    abstract override val initializer: AstExpression?
    abstract override val delegate: AstExpression?
    abstract override val delegateFieldSymbol: AstDelegateFieldSymbol<AstValueParameter>?
    abstract override val isVar: Boolean
    abstract override val isVal: Boolean
    abstract override val getter: AstPropertyAccessor?
    abstract override val setter: AstPropertyAccessor?
    abstract override val annotations: List<AstAnnotationCall>
    abstract val defaultValue: AstExpression?
    abstract val isCrossinline: Boolean
    abstract val isNoinline: Boolean
    abstract val isVararg: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitValueParameter(this, data)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: AstTypeRef)

    abstract override fun replaceReceiverTypeRef(newReceiverTypeRef: AstTypeRef?)

    abstract override fun <D> transformReturnTypeRef(transformer: AstTransformer<D>, data: D): AstValueParameter

    abstract override fun <D> transformReceiverTypeRef(transformer: AstTransformer<D>, data: D): AstValueParameter

    abstract override fun <D> transformInitializer(transformer: AstTransformer<D>, data: D): AstValueParameter

    abstract override fun <D> transformDelegate(transformer: AstTransformer<D>, data: D): AstValueParameter

    abstract override fun <D> transformGetter(transformer: AstTransformer<D>, data: D): AstValueParameter

    abstract override fun <D> transformSetter(transformer: AstTransformer<D>, data: D): AstValueParameter

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstValueParameter

    abstract override fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstValueParameter
}
