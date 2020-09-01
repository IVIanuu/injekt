package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstAnnotationResolveStatus
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.expressions.AstEmptyArgumentList
import com.ivianuu.ast.expressions.impl.AstAnnotationCallImpl
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.types.AstTypeRef
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstAnnotationCallBuilder : AstCallBuilder, AstAnnotationContainerBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    override var argumentList: AstArgumentList = AstEmptyArgumentList
    lateinit var calleeReference: AstReference
    var useSiteTarget: AnnotationUseSiteTarget? = null
    lateinit var annotationTypeRef: AstTypeRef
    var resolveStatus: AstAnnotationResolveStatus = AstAnnotationResolveStatus.Unresolved

    override fun build(): AstAnnotationCall {
        return AstAnnotationCallImpl(
            annotations,
            argumentList,
            calleeReference,
            useSiteTarget,
            annotationTypeRef,
            resolveStatus,
        )
    }


    @Deprecated("Modification of 'typeRef' has no impact for AstAnnotationCallBuilder", level = DeprecationLevel.HIDDEN)
    override var typeRef: AstTypeRef
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildAnnotationCall(init: AstAnnotationCallBuilder.() -> Unit): AstAnnotationCall {
    return AstAnnotationCallBuilder().apply(init).build()
}
