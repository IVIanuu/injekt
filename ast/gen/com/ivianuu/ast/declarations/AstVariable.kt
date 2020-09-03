package com.ivianuu.ast.declarations

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

abstract class AstVariable<F : AstVariable<F>> : AstCallableDeclaration<F>, AstNamedDeclaration() {
    abstract override val annotations: List<AstFunctionCall>
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val receiverType: AstType?
    abstract override val returnType: AstType
    abstract override val name: Name
    abstract override val symbol: AstVariableSymbol<F>
    abstract val initializer: AstExpression?
    abstract val delegate: AstExpression?
    abstract val isVar: Boolean
    abstract val getter: AstPropertyAccessor?
    abstract val setter: AstPropertyAccessor?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitVariable(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceReceiverType(newReceiverType: AstType?)

    abstract override fun replaceReturnType(newReturnType: AstType)

    abstract fun replaceInitializer(newInitializer: AstExpression?)

    abstract fun replaceDelegate(newDelegate: AstExpression?)

    abstract fun replaceIsVar(newIsVar: Boolean)

    abstract fun replaceGetter(newGetter: AstPropertyAccessor?)

    abstract fun replaceSetter(newSetter: AstPropertyAccessor?)
}
