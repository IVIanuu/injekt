package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstContext
import com.ivianuu.ast.builder.AstBuilder
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
class AstAnonymousFunctionBuilder(override val context: AstContext) : AstFunctionBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override var origin: AstDeclarationOrigin = AstDeclarationOrigin.Source
    var extensionReceiverType: AstType? = null
    override var returnType: AstType = context.builtIns.unitType
    override val valueParameters: MutableList<AstValueParameter> = mutableListOf()
    override var body: AstBlock? = null
    override lateinit var type: AstType
    var label: String? = null
    lateinit var symbol: AstAnonymousFunctionSymbol

    override fun build(): AstAnonymousFunction {
        return AstAnonymousFunctionImpl(
            context,
            annotations,
            origin,
            extensionReceiverType,
            returnType,
            valueParameters,
            body,
            type,
            label,
            symbol,
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
inline fun AstBuilder.buildAnonymousFunction(init: AstAnonymousFunctionBuilder.() -> Unit): AstAnonymousFunction {
    return AstAnonymousFunctionBuilder(context).apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun AstAnonymousFunction.copy(init: AstAnonymousFunctionBuilder.() -> Unit = {}): AstAnonymousFunction {
    val copyBuilder = AstAnonymousFunctionBuilder(context)
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.origin = origin
    copyBuilder.extensionReceiverType = extensionReceiverType
    copyBuilder.returnType = returnType
    copyBuilder.valueParameters.addAll(valueParameters)
    copyBuilder.body = body
    copyBuilder.type = type
    copyBuilder.label = label
    copyBuilder.symbol = symbol
    return copyBuilder.apply(init).build()
}
