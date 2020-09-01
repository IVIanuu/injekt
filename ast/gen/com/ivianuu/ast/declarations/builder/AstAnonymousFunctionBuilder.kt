package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstLabel
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstAnonymousFunction
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.impl.AstAnonymousFunctionImpl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.symbols.impl.AstAnonymousFunctionSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.types.impl.AstImplicitTypeRefImpl
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstAnonymousFunctionBuilder : AstFunctionBuilder, AstAnnotationContainerBuilder, AstExpressionBuilder {
    override lateinit var origin: AstDeclarationOrigin
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    override lateinit var returnTypeRef: AstTypeRef
    var receiverTypeRef: AstTypeRef? = null
    override val valueParameters: MutableList<AstValueParameter> = mutableListOf()
    override var body: AstBlock? = null
    override var typeRef: AstTypeRef = AstImplicitTypeRefImpl()
    lateinit var symbol: AstAnonymousFunctionSymbol
    var label: AstLabel? = null
    var isLambda: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    val typeParameters: MutableList<AstTypeParameter> = mutableListOf()

    override fun build(): AstAnonymousFunction {
        return AstAnonymousFunctionImpl(
            origin,
            annotations,
            returnTypeRef,
            receiverTypeRef,
            valueParameters,
            body,
            typeRef,
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
