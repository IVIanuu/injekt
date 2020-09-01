package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstNamedArgumentExpression
import com.ivianuu.ast.expressions.impl.AstNamedArgumentExpressionImpl
import com.ivianuu.ast.types.AstTypeRef
import org.jetbrains.kotlin.name.Name
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstNamedArgumentExpressionBuilder : AstAnnotationContainerBuilder, AstExpressionBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    lateinit var expression: AstExpression
    var isSpread: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var name: Name

    override fun build(): AstNamedArgumentExpression {
        return AstNamedArgumentExpressionImpl(
            annotations,
            expression,
            isSpread,
            name,
        )
    }

    @Deprecated("Modification of 'typeRef' has no impact for AstNamedArgumentExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var typeRef: AstTypeRef
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildNamedArgumentExpression(init: AstNamedArgumentExpressionBuilder.() -> Unit): AstNamedArgumentExpression {
    return AstNamedArgumentExpressionBuilder().apply(init).build()
}
