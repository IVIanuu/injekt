package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstValueParameterSymbol
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstValueParameter : AstPureAbstractElement(), AstVariable<AstValueParameter> {
    abstract override val annotations: List<AstFunctionCall>
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val receiverType: AstType?
    abstract override val returnType: AstType
    abstract override val name: Name
    abstract override val initializer: AstExpression?
    abstract override val delegate: AstExpression?
    abstract override val isVar: Boolean
    abstract override val getter: AstPropertyAccessor?
    abstract override val setter: AstPropertyAccessor?
    abstract override val symbol: AstValueParameterSymbol
    abstract val defaultValue: AstExpression?
    abstract val isCrossinline: Boolean
    abstract val isNoinline: Boolean
    abstract val isVararg: Boolean

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitValueParameter(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceReceiverType(newReceiverType: AstType?)

    abstract override fun replaceReturnType(newReturnType: AstType)

    abstract override fun replaceInitializer(newInitializer: AstExpression?)

    abstract override fun replaceDelegate(newDelegate: AstExpression?)

    abstract override fun replaceIsVar(newIsVar: Boolean)

    abstract override fun replaceGetter(newGetter: AstPropertyAccessor?)

    abstract override fun replaceSetter(newSetter: AstPropertyAccessor?)

    abstract fun replaceDefaultValue(newDefaultValue: AstExpression?)

    abstract fun replaceIsCrossinline(newIsCrossinline: Boolean)

    abstract fun replaceIsNoinline(newIsNoinline: Boolean)

    abstract fun replaceIsVararg(newIsVararg: Boolean)
}
