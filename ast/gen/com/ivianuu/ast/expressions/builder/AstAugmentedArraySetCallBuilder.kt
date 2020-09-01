package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstAugmentedArraySetCall
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstOperation
import com.ivianuu.ast.expressions.impl.AstAugmentedArraySetCallImpl
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.references.impl.AstStubReference
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstAugmentedArraySetCallBuilder : AstAnnotationContainerBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    lateinit var assignCall: AstFunctionCall
    lateinit var setGetBlock: AstBlock
    lateinit var operation: AstOperation
    var calleeReference: AstReference = AstStubReference

    override fun build(): AstAugmentedArraySetCall {
        return AstAugmentedArraySetCallImpl(
            annotations,
            assignCall,
            setGetBlock,
            operation,
            calleeReference,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildAugmentedArraySetCall(init: AstAugmentedArraySetCallBuilder.() -> Unit): AstAugmentedArraySetCall {
    return AstAugmentedArraySetCallBuilder().apply(init).build()
}
