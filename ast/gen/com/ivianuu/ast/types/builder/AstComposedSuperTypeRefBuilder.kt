package com.ivianuu.ast.types.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.types.AstComposedSuperTypeRef
import com.ivianuu.ast.types.AstResolvedTypeRef
import com.ivianuu.ast.types.impl.AstComposedSuperTypeRefImpl
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstComposedSuperTypeRefBuilder : AstAnnotationContainerBuilder {
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    val superTypeRefs: MutableList<AstResolvedTypeRef> = mutableListOf()

    override fun build(): AstComposedSuperTypeRef {
        return AstComposedSuperTypeRefImpl(
            annotations,
            superTypeRefs,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildComposedSuperTypeRef(init: AstComposedSuperTypeRefBuilder.() -> Unit = {}): AstComposedSuperTypeRef {
    return AstComposedSuperTypeRefBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildComposedSuperTypeRefCopy(
    original: AstComposedSuperTypeRef,
    init: AstComposedSuperTypeRefBuilder.() -> Unit = {}
): AstComposedSuperTypeRef {
    val copyBuilder = AstComposedSuperTypeRefBuilder()
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.superTypeRefs.addAll(original.superTypeRefs)
    return copyBuilder.apply(init).build()
}
