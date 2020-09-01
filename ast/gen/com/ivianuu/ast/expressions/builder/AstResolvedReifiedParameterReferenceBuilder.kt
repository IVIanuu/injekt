package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstResolvedReifiedParameterReference
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstResolvedReifiedParameterReferenceImpl
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.impl.AstImplicitTypeImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstResolvedReifiedParameterReferenceBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override var type: AstType = AstImplicitTypeImpl()
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    lateinit var symbol: AstTypeParameterSymbol

    override fun build(): AstResolvedReifiedParameterReference {
        return AstResolvedReifiedParameterReferenceImpl(
            type,
            annotations,
            symbol,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedReifiedParameterReference(init: AstResolvedReifiedParameterReferenceBuilder.() -> Unit): AstResolvedReifiedParameterReference {
    return AstResolvedReifiedParameterReferenceBuilder().apply(init).build()
}
