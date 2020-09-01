package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.symbols.impl.AstDelegateFieldSymbol
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

open class AstValueParameterImpl @AstImplementationDetail constructor(
    override val origin: AstDeclarationOrigin,
    override var returnType: AstType,
    override val name: Name,
    override val symbol: AstVariableSymbol<AstValueParameter>,
    override val annotations: MutableList<AstAnnotationCall>,
    override var defaultValue: AstExpression?,
    override val isCrossinline: Boolean,
    override val isNoinline: Boolean,
    override val isVararg: Boolean,
) : AstValueParameter() {
    override val attributes: AstDeclarationAttributes = AstDeclarationAttributes()
    override val receiverType: AstType? get() = null
    override val initializer: AstExpression? get() = null
    override val delegate: AstExpression? get() = null
    override val delegateFieldSymbol: AstDelegateFieldSymbol<AstValueParameter>? get() = null
    override val isVar: Boolean get() = false
    override val isVal: Boolean get() = true
    override val getter: AstPropertyAccessor? get() = null
    override val setter: AstPropertyAccessor? get() = null

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        returnType.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        defaultValue?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D): AstValueParameterImpl {
        transformReturnType(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun <D> transformReturnType(transformer: AstTransformer<D>, data: D): AstValueParameterImpl {
        returnType = returnType.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReceiverType(transformer: AstTransformer<D>, data: D): AstValueParameterImpl {
        return this
    }

    override fun <D> transformInitializer(transformer: AstTransformer<D>, data: D): AstValueParameterImpl {
        return this
    }

    override fun <D> transformDelegate(transformer: AstTransformer<D>, data: D): AstValueParameterImpl {
        return this
    }

    override fun <D> transformGetter(transformer: AstTransformer<D>, data: D): AstValueParameterImpl {
        return this
    }

    override fun <D> transformSetter(transformer: AstTransformer<D>, data: D): AstValueParameterImpl {
        return this
    }

    override fun <D> transformAnnotations(transformer: AstTransformer<D>, data: D): AstValueParameterImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: AstTransformer<D>, data: D): AstValueParameterImpl {
        transformAnnotations(transformer, data)
        defaultValue = defaultValue?.transformSingle(transformer, data)
        return this
    }

    override fun replaceReturnType(newReturnType: AstType) {
        returnType = newReturnType
    }

    override fun replaceReceiverType(newReceiverType: AstType?) {}
}
