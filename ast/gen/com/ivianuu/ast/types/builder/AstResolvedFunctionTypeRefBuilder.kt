package com.ivianuu.ast.types.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.types.AstResolvedFunctionTypeRef
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.types.impl.AstResolvedFunctionTypeRefImpl
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstResolvedFunctionTypeRefBuilder : AstAnnotationContainerBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    lateinit var type: ConeKotlinType
    var isSuspend: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isMarkedNullable: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var receiverTypeRef: AstTypeRef? = null
    val valueParameters: MutableList<AstValueParameter> = mutableListOf()
    lateinit var returnTypeRef: AstTypeRef

    override fun build(): AstResolvedFunctionTypeRef {
        return AstResolvedFunctionTypeRefImpl(
            annotations,
            type,
            isSuspend,
            isMarkedNullable,
            receiverTypeRef,
            valueParameters,
            returnTypeRef,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedFunctionTypeRef(init: AstResolvedFunctionTypeRefBuilder.() -> Unit): AstResolvedFunctionTypeRef {
    return AstResolvedFunctionTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedFunctionTypeRefCopy(
    original: AstResolvedFunctionTypeRef,
    init: AstResolvedFunctionTypeRefBuilder.() -> Unit
): AstResolvedFunctionTypeRef {
    val copyBuilder = AstResolvedFunctionTypeRefBuilder()
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.type = original.type
    copyBuilder.isSuspend = original.isSuspend
    copyBuilder.isMarkedNullable = original.isMarkedNullable
    copyBuilder.receiverTypeRef = original.receiverTypeRef
    copyBuilder.valueParameters.addAll(original.valueParameters)
    copyBuilder.returnTypeRef = original.returnTypeRef
    return copyBuilder.apply(init).build()
}
