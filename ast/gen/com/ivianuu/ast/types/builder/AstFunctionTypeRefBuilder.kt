package com.ivianuu.ast.types.builder

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.types.AstFunctionTypeRef
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.types.impl.AstFunctionTypeRefImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstFunctionTypeRefBuilder : AstAnnotationContainerBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    var isMarkedNullable: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var receiverTypeRef: AstTypeRef? = null
    val valueParameters: MutableList<AstValueParameter> = mutableListOf()
    lateinit var returnTypeRef: AstTypeRef
    var isSuspend: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    override fun build(): AstFunctionTypeRef {
        return AstFunctionTypeRefImpl(
            annotations,
            isMarkedNullable,
            receiverTypeRef,
            valueParameters,
            returnTypeRef,
            isSuspend,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionTypeRef(init: AstFunctionTypeRefBuilder.() -> Unit): AstFunctionTypeRef {
    return AstFunctionTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionTypeRefCopy(original: AstFunctionTypeRef, init: AstFunctionTypeRefBuilder.() -> Unit): AstFunctionTypeRef {
    val copyBuilder = AstFunctionTypeRefBuilder()
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.isMarkedNullable = original.isMarkedNullable
    copyBuilder.receiverTypeRef = original.receiverTypeRef
    copyBuilder.valueParameters.addAll(original.valueParameters)
    copyBuilder.returnTypeRef = original.returnTypeRef
    copyBuilder.isSuspend = original.isSuspend
    return copyBuilder.apply(init).build()
}
