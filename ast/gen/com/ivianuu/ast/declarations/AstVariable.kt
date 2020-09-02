package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstStatement
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
    abstract val isVar: Boolean
    abstract val isVal: Boolean
    abstract val getter: AstPropertyAccessor?
    abstract val setter: AstPropertyAccessor?
    abstract override val annotations: List<AstFunctionCall>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitVariable(this, data)

    abstract fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstVariable<F>
}
