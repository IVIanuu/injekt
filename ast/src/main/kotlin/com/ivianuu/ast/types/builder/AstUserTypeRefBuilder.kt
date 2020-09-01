package com.ivianuu.ast.types.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.types.AstQualifierPart
import com.ivianuu.ast.types.AstUserTypeRef
import com.ivianuu.ast.types.impl.AstUserTypeRefImpl


@AstBuilderDsl
open class AstUserTypeRefBuilder : AstAnnotationContainerBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    open var isMarkedNullable: Boolean by kotlin.properties.Delegates.notNull()
    val qualifier: MutableList<AstQualifierPart> = mutableListOf()

    override fun build(): AstUserTypeRef {
        return AstUserTypeRefImpl(isMarkedNullable, qualifier, annotations)
    }
}

inline fun buildUserTypeRef(init: AstUserTypeRefBuilder.() -> Unit): AstUserTypeRef {
    return AstUserTypeRefBuilder().apply(init).build()
}
