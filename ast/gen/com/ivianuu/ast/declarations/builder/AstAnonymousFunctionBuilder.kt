package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstLabel
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstAnonymousFunction
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.builder.AstFunctionBuilder
import com.ivianuu.ast.declarations.impl.AstAnonymousFunctionImpl
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.symbols.impl.AstAnonymousFunctionSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstAnonymousFunctionBuilder : AstFunctionBuilder, AstAnnotationContainerBuilder, AstExpressionBuilder {
    override lateinit var origin: AstDeclarationOrigin
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var returnType: AstType
    var receiverType: AstType? = null
    override val valueParameters: MutableList<AstValueParameter> = mutableListOf()
    override var body: AstBlock? = null
    override lateinit var type: AstType
    lateinit var symbol: AstAnonymousFunctionSymbol
    var label: AstLabel? = null
    var isLambda: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    val typeParameters: MutableList<AstTypeParameter> = mutableListOf()

    override fun build(): AstAnonymousFunction {
        return AstAnonymousFunctionImpl(
            origin,
            annotations,
            returnType,
            receiverType,
            valueParameters,
            body,
            type,
            symbol,
            label,
            isLambda,
            typeParameters,
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
