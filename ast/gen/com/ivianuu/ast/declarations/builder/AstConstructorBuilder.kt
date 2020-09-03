package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.builder.AstAbstractConstructorBuilder
import com.ivianuu.ast.declarations.impl.AstConstructorImpl
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstConstructorBuilder : AstAbstractConstructorBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    override var dispatchReceiverType: AstType? = null
    override var extensionReceiverType: AstType? = null
    override lateinit var returnType: AstType
    override val valueParameters: MutableList<AstValueParameter> = mutableListOf()
    override lateinit var symbol: AstConstructorSymbol
    override var delegatedConstructor: AstDelegatedConstructorCall? = null
    override var body: AstBlock? = null
    var isPrimary: Boolean = false

    override fun build(): AstConstructor {
        return AstConstructorImpl(
            annotations,
            origin,
            dispatchReceiverType,
            extensionReceiverType,
            returnType,
            valueParameters,
            symbol,
            delegatedConstructor,
            body,
            isPrimary,
        )
    }


    @Deprecated("Modification of 'attributes' has no impact for AstConstructorBuilder", level = DeprecationLevel.HIDDEN)
    override var attributes: AstDeclarationAttributes
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildConstructor(init: AstConstructorBuilder.() -> Unit): AstConstructor {
    return AstConstructorBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstConstructor.copy(init: AstConstructorBuilder.() -> Unit = {}): AstConstructor {
    val copyBuilder = AstConstructorBuilder()
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.origin = origin
    copyBuilder.dispatchReceiverType = dispatchReceiverType
    copyBuilder.extensionReceiverType = extensionReceiverType
    copyBuilder.returnType = returnType
    copyBuilder.valueParameters.addAll(valueParameters)
    copyBuilder.symbol = symbol
    copyBuilder.delegatedConstructor = delegatedConstructor
    copyBuilder.body = body
    copyBuilder.isPrimary = isPrimary
    return copyBuilder.apply(init).build()
}
