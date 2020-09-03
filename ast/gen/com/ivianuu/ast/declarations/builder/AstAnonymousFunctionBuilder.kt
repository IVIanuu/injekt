package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstAnonymousFunction
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.builder.AstFunctionBuilder
import com.ivianuu.ast.declarations.impl.AstAnonymousFunctionImpl
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.symbols.impl.AstAnonymousFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstAnonymousFunctionBuilder : AstFunctionBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    var receiverType: AstType? = null
    override lateinit var returnType: AstType
    override val valueParameters: MutableList<AstValueParameter> = mutableListOf()
    override var body: AstBlock? = null
    override lateinit var type: AstType
    lateinit var symbol: AstAnonymousFunctionSymbol
    var label: String? = null

    override fun build(): AstAnonymousFunction {
        return AstAnonymousFunctionImpl(
            annotations,
            origin,
            receiverType,
            returnType,
            valueParameters,
            body,
            type,
            symbol,
            label,
        )
    }


    @Deprecated("Modification of 'attributes' has no impact for AstAnonymousFunctionBuilder", level = DeprecationLevel.HIDDEN)
    override var attributes: AstDeclarationAttributes
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildAnonymousFunction(init: AstAnonymousFunctionBuilder.() -> Unit): AstAnonymousFunction {
    return AstAnonymousFunctionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstAnonymousFunction.copy(init: AstAnonymousFunctionBuilder.() -> Unit = {}): AstAnonymousFunction {
    val copyBuilder = AstAnonymousFunctionBuilder()
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.origin = origin
    copyBuilder.receiverType = receiverType
    copyBuilder.returnType = returnType
    copyBuilder.valueParameters.addAll(valueParameters)
    copyBuilder.body = body
    copyBuilder.type = type
    copyBuilder.symbol = symbol
    copyBuilder.label = label
    return copyBuilder.apply(init).build()
}
