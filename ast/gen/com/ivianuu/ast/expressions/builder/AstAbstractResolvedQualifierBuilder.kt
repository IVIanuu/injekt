package com.ivianuu.ast.expressions.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.expressions.AstResolvedQualifier
import com.ivianuu.ast.symbols.impl.AstClassLikeSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.visitors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
interface AstAbstractResolvedQualifierBuilder {
    abstract var type: AstType
    abstract val annotations: MutableList<AstAnnotationCall>
    abstract var packageFqName: FqName
    abstract var relativeClassFqName: FqName?
    abstract var classId: ClassId?
    abstract var symbol: AstClassLikeSymbol<*>?
    abstract var isNullableLHSForCallableReference: Boolean
    abstract val typeArguments: MutableList<AstTypeProjection>

    fun build(): AstResolvedQualifier
}
