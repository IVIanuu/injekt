package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
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
    abstract override val context: AstContext
    abstract override val annotations: List<AstFunctionCall>
    abstract override val origin: AstDeclarationOrigin
    abstract override val attributes: AstDeclarationAttributes
    abstract override val dispatchReceiverType: AstType?
    abstract override val extensionReceiverType: AstType?
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
    abstract val correspondingProperty: AstPropertySymbol?
    abstract val varargElementType: AstType?

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitValueParameter(this, data)

    abstract override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>)

    abstract override fun replaceOrigin(newOrigin: AstDeclarationOrigin)

    abstract override fun replaceAttributes(newAttributes: AstDeclarationAttributes)

    abstract override fun replaceDispatchReceiverType(newDispatchReceiverType: AstType?)

    abstract override fun replaceExtensionReceiverType(newExtensionReceiverType: AstType?)

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

    abstract fun replaceCorrespondingProperty(newCorrespondingProperty: AstPropertySymbol?)

    abstract fun replaceVarargElementType(newVarargElementType: AstType?)
}
