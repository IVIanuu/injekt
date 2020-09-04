package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstContext
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

interface AstVariable<F : AstVariable<F>> : AstCallableDeclaration<F>, AstNamedDeclaration {
    override val context: AstContext
    override val annotations: List<AstFunctionCall>
    override val origin: AstDeclarationOrigin
    override val attributes: AstDeclarationAttributes
    override val dispatchReceiverType: AstType?
    override val extensionReceiverType: AstType?
    override val returnType: AstType
    override val name: Name
    override val symbol: AstVariableSymbol<F>
    val initializer: AstExpression?
    val delegate: AstExpression?
    val isVar: Boolean
    val getter: AstPropertyAccessor?
    val setter: AstPropertyAccessor?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitVariable(this, data)

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    override fun replaceDispatchReceiverType(newDispatchReceiverType: AstType?)

    override fun replaceExtensionReceiverType(newExtensionReceiverType: AstType?)

    override fun replaceReturnType(newReturnType: AstType)

    fun replaceInitializer(newInitializer: AstExpression?)

    fun replaceDelegate(newDelegate: AstExpression?)

    fun replaceIsVar(newIsVar: Boolean)

    fun replaceGetter(newGetter: AstPropertyAccessor?)

    fun replaceSetter(newSetter: AstPropertyAccessor?)
}
