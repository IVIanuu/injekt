package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.builder.AstFunctionBuilder
import com.ivianuu.ast.declarations.impl.AstPropertyAccessorImpl
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.symbols.impl.AstPropertyAccessorSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.descriptors.Modality

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstPropertyAccessorBuilder : AstFunctionBuilder {
    override lateinit var origin: AstDeclarationOrigin
    override val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override lateinit var returnType: AstType
    override val valueParameters: MutableList<AstValueParameter> = mutableListOf()
    override var body: AstBlock? = null
    val typeParameters: MutableList<AstTypeParameter> = mutableListOf()
    lateinit var symbol: AstPropertyAccessorSymbol
    var isGetter: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    @OptIn(AstImplementationDetail::class)
    override fun build(): AstPropertyAccessor {
        return AstPropertyAccessorImpl(
            origin,
            annotations,
            returnType,
            valueParameters,
            body,
            typeParameters,
            symbol,
            isGetter,
        )
    }


    @Deprecated("Modification of 'attributes' has no impact for AstPropertyAccessorBuilder", level = DeprecationLevel.HIDDEN)
    override var attributes: AstDeclarationAttributes
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildPropertyAccessor(init: AstPropertyAccessorBuilder.() -> Unit): AstPropertyAccessor {
    return AstPropertyAccessorBuilder().apply(init).build()
}
