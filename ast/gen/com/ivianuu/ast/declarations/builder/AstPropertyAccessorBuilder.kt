package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstDeclarationStatus
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.impl.AstPropertyAccessorImpl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.symbols.impl.AstPropertyAccessorSymbol
import com.ivianuu.ast.types.AstTypeRef
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstPropertyAccessorBuilder : AstFunctionBuilder, AstAnnotationContainerBuilder {
    override lateinit var origin: AstDeclarationOrigin
    override lateinit var returnTypeRef: AstTypeRef
    override val valueParameters: MutableList<AstValueParameter> = mutableListOf()
    override var body: AstBlock? = null
    lateinit var symbol: AstPropertyAccessorSymbol
    var isGetter: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var status: AstDeclarationStatus
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    val typeParameters: MutableList<AstTypeParameter> = mutableListOf()

    @OptIn(AstImplementationDetail::class)
    override fun build(): AstPropertyAccessor {
        return AstPropertyAccessorImpl(
            origin,
            returnTypeRef,
            valueParameters,
            body,
            symbol,
            isGetter,
            status,
            annotations,
            typeParameters,
        )
    }


    @Deprecated(
        "Modification of 'attributes' has no impact for AstPropertyAccessorBuilder",
        level = DeprecationLevel.HIDDEN
    )
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
