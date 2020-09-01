package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.symbols.impl.AstDelegateFieldSymbol
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstVariable<F : AstVariable<F>> : AstPureAbstractElement(), AstCallableDeclaration<F>, AstAnnotatedDeclaration, AstStatement {
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val returnType: AstType
    abstract override val receiverType: AstType?
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

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitVariable(this, data)

    abstract override fun replaceReturnType(newReturnType: AstType)

    abstract override fun replaceReceiverType(newReceiverType: AstType?)

    abstract override fun <D> transformReturnType(transformer: AstTransformer<D>, data: D): AstVariable<F>

    abstract override fun <D> transformReceiverType(transformer: AstTransformer<D>, data: D): AstVariable<F>

    abstract fun <D> transformInitializer(transformer: AstTransformer<D>, data: D): AstVariable<F>

    abstract fun <D> transformDelegate(transformer: AstTransformer<D>, data: D): AstVariable<F>

    abstract fun <D> transformGetter(transformer: AstTransformer<D>, data: D): AstVariable<F>

    abstract fun <D> transformSetter(transformer: AstTransformer<D>, data: D): AstVariable<F>

    abstract override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstVariable<F>

    abstract fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstVariable<F>
}
