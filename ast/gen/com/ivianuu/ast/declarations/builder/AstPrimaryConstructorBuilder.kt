package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstDeclarationStatus
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.builder.AstAbstractConstructorBuilder
import com.ivianuu.ast.declarations.impl.AstPrimaryConstructor
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstPrimaryConstructorBuilder : AstAbstractConstructorBuilder, AstAnnotationContainerBuilder {
    override lateinit var origin: AstDeclarationOrigin
    override lateinit var returnType: AstType
    override var receiverType: AstType? = null
    override val valueParameters: MutableList<AstValueParameter> = mutableListOf()
    override lateinit var status: AstDeclarationStatus
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var symbol: AstConstructorSymbol
    override var delegatedConstructor: AstDelegatedConstructorCall? = null
    override var body: AstBlock? = null

    override fun build(): AstConstructor {
        return AstPrimaryConstructor(
            origin,
            returnType,
            receiverType,
            valueParameters,
            status,
            annotations,
            symbol,
            delegatedConstructor,
            body,
        )
    }


    @Deprecated("Modification of 'attributes' has no impact for AstPrimaryConstructorBuilder", level = DeprecationLevel.HIDDEN)
    override var attributes: AstDeclarationAttributes
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildPrimaryConstructor(init: AstPrimaryConstructorBuilder.() -> Unit): AstConstructor {
    return AstPrimaryConstructorBuilder().apply(init).build()
}
