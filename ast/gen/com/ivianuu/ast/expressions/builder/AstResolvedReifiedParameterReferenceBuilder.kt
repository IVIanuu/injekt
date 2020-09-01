package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstResolvedReifiedParameterReference
import com.ivianuu.ast.expressions.impl.AstResolvedReifiedParameterReferenceImpl
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.types.impl.AstImplicitTypeRefImpl
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstResolvedReifiedParameterReferenceBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override var typeRef: AstTypeRef = AstImplicitTypeRefImpl()
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    lateinit var symbol: AstTypeParameterSymbol

    override fun build(): AstResolvedReifiedParameterReference {
        return AstResolvedReifiedParameterReferenceImpl(
            typeRef,
            annotations,
            symbol,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedReifiedParameterReference(init: AstResolvedReifiedParameterReferenceBuilder.() -> Unit): AstResolvedReifiedParameterReference {
    return AstResolvedReifiedParameterReferenceBuilder().apply(init).build()
}
