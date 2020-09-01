package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstResolvedQualifier
import com.ivianuu.ast.expressions.impl.AstResolvedQualifierImpl
import com.ivianuu.ast.symbols.impl.AstClassLikeSymbol
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.types.impl.AstImplicitTypeRefImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstResolvedQualifierBuilder : AstAbstractResolvedQualifierBuilder, AstAnnotationContainerBuilder, AstExpressionBuilder {
    override var typeRef: AstTypeRef = AstImplicitTypeRefImpl()
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    override lateinit var packageFqName: FqName
    override var relativeClassFqName: FqName? = null
    override var symbol: AstClassLikeSymbol<*>? = null
    override var isNullableLHSForCallableReference: Boolean = false
    override val typeArguments: MutableList<AstTypeProjection> = mutableListOf()

    override fun build(): AstResolvedQualifier {
        return AstResolvedQualifierImpl(
            typeRef,
            annotations,
            packageFqName,
            relativeClassFqName,
            symbol,
            isNullableLHSForCallableReference,
            typeArguments,
        )
    }


    @Deprecated("Modification of 'classId' has no impact for AstResolvedQualifierBuilder", level = DeprecationLevel.HIDDEN)
    override var classId: ClassId?
        get() = throw IllegalStateException()
        set(value) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedQualifier(init: AstResolvedQualifierBuilder.() -> Unit): AstResolvedQualifier {
    return AstResolvedQualifierBuilder().apply(init).build()
}
