package com.ivianuu.ast.types.builder

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.types.AstResolvedTypeRef
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.types.impl.AstResolvedTypeRefImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstResolvedTypeRefBuilder : AstAnnotationContainerBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    lateinit var type: ConeKotlinType
    var delegatedTypeRef: AstTypeRef? = null
    var isSuspend: Boolean = false

    @OptIn(AstImplementationDetail::class)
    override fun build(): AstResolvedTypeRef {
        return AstResolvedTypeRefImpl(
            annotations,
            type,
            delegatedTypeRef,
            isSuspend,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedTypeRef(init: AstResolvedTypeRefBuilder.() -> Unit): AstResolvedTypeRef {
    return AstResolvedTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedTypeRefCopy(original: AstResolvedTypeRef, init: AstResolvedTypeRefBuilder.() -> Unit): AstResolvedTypeRef {
    val copyBuilder = AstResolvedTypeRefBuilder()
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.type = original.type
    copyBuilder.delegatedTypeRef = original.delegatedTypeRef
    copyBuilder.isSuspend = original.isSuspend
    return copyBuilder.apply(init).build()
}
