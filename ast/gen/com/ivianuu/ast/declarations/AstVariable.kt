package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstStatement
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

abstract class AstVariable<F : AstVariable<F>> : AstPureAbstractElement(),
    AstCallableDeclaration<F>, AstAnnotatedDeclaration, AstStatement {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val returnTypeRef: AstTypeRef
    abstract override val receiverTypeRef: AstTypeRef?
    abstract val name: Name
    abstract override val symbol: AstVariableSymbol<F>
    abstract val initializer: AstExpression?
    abstract val delegate: AstExpression?
    abstract val delegateFieldSymbol: AstDelegateFieldSymbol<F>?
    abstract val isVar: Boolean
    abstract val isVal: Boolean
    abstract val getter: AstPropertyAccessor?
    abstract val setter: AstPropertyAccessor?
    abstract override val annotations: List<AstAnnotationCall>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitVariable(this, data)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: AstTypeRef)

    abstract override fun replaceReceiverTypeRef(newReceiverTypeRef: AstTypeRef?)

    abstract override fun <D> transformReturnTypeRef(
        transformer: AstTransformer<D>,
        data: D
    ): AstVariable<F>

    abstract override fun <D> transformReceiverTypeRef(
        transformer: AstTransformer<D>,
        data: D
    ): AstVariable<F>

    abstract fun <D> transformInitializer(transformer: AstTransformer<D>, data: D): AstVariable<F>

    abstract fun <D> transformDelegate(transformer: AstTransformer<D>, data: D): AstVariable<F>

    abstract fun <D> transformGetter(transformer: AstTransformer<D>, data: D): AstVariable<F>

    abstract fun <D> transformSetter(transformer: AstTransformer<D>, data: D): AstVariable<F>

    abstract override fun <D> transformAnnotations(
        transformer: AstTransformer<D>,
        data: D
    ): AstVariable<F>

    abstract fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstVariable<F>
}
