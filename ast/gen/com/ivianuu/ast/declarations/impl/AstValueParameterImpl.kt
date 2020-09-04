package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstValueParameter
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

open class AstValueParameterImpl @AstImplementationDetail constructor(
    override val context: AstContext,
    override val annotations: MutableList<AstFunctionCall>,
    override val origin: AstDeclarationOrigin,
    override var returnType: AstType,
    override var name: Name,
    override var symbol: AstValueParameterSymbol,
    override var defaultValue: AstExpression?,
    override var isCrossinline: Boolean,
    override var isNoinline: Boolean,
    override var isVararg: Boolean,
) : AstValueParameter() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val dispatchReceiverType: AstType? get() = null
    override val extensionReceiverType: AstType? get() = null
    override val initializer: AstExpression? get() = null
    override val delegate: AstExpression? get() = null
    override var isVar: Boolean = false
    override val getter: AstPropertyAccessor? get() = null
    override val setter: AstPropertyAccessor? get() = null

    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        returnType.accept(visitor, data)
        defaultValue?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstValueParameterImpl {
        annotations.transformInplace(transformer, data)
        returnType = returnType.transformSingle(transformer, data)
        defaultValue = defaultValue?.transformSingle(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<AstFunctionCall>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceDispatchReceiverType(newDispatchReceiverType: AstType?) {}

    override fun replaceExtensionReceiverType(newExtensionReceiverType: AstType?) {}

    override fun replaceReturnType(newReturnType: AstType) {
        returnType = newReturnType
    }

    override fun replaceInitializer(newInitializer: AstExpression?) {}

    override fun replaceDelegate(newDelegate: AstExpression?) {}

    override fun replaceIsVar(newIsVar: Boolean) {
        isVar = newIsVar
    }

    override fun replaceGetter(newGetter: AstPropertyAccessor?) {}

    override fun replaceSetter(newSetter: AstPropertyAccessor?) {}

    override fun replaceDefaultValue(newDefaultValue: AstExpression?) {
        defaultValue = newDefaultValue
    }

    override fun replaceIsCrossinline(newIsCrossinline: Boolean) {
        isCrossinline = newIsCrossinline
    }

    override fun replaceIsNoinline(newIsNoinline: Boolean) {
        isNoinline = newIsNoinline
    }

    override fun replaceIsVararg(newIsVararg: Boolean) {
        isVararg = newIsVararg
    }
}
