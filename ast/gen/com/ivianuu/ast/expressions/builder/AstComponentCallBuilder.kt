package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.AstImplementationDetail
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstArgumentList
import com.ivianuu.ast.expressions.AstComponentCall
import com.ivianuu.ast.expressions.AstEmptyArgumentList
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.builder.AstCallBuilder
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.impl.AstComponentCallImpl
import com.ivianuu.ast.expressions.impl.AstNoReceiverExpression
import com.ivianuu.ast.references.AstNamedReference
import com.ivianuu.ast.references.AstReference
import com.ivianuu.ast.references.impl.AstSimpleNamedReference
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.impl.AstImplicitTypeImpl
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.name.Name

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstComponentCallBuilder : AstCallBuilder, AstAnnotationContainerBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    val typeArguments: MutableList<AstTypeProjection> = mutableListOf()
    var dispatchReceiver: AstExpression = AstNoReceiverExpression
    var extensionReceiver: AstExpression = AstNoReceiverExpression
    override var argumentList: AstArgumentList = AstEmptyArgumentList
    lateinit var explicitReceiver: AstExpression
    var componentIndex: Int by kotlin.properties.Delegates.notNull<Int>()

    override fun build(): AstComponentCall {
        return AstComponentCallImpl(
            annotations,
            typeArguments,
            dispatchReceiver,
            extensionReceiver,
            argumentList,
            explicitReceiver,
            componentIndex,
        )
    }

    @Deprecated("Modification of 'type' has no impact for AstComponentCallBuilder", level = DeprecationLevel.HIDDEN)
    override var type: AstType
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildComponentCall(init: AstComponentCallBuilder.() -> Unit): AstComponentCall {
    return AstComponentCallBuilder().apply(init).build()
}
