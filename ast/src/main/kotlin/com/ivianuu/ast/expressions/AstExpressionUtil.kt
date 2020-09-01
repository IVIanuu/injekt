/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.expressions

import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.expressions.impl.AstBlockImpl
import com.ivianuu.ast.expressions.impl.AstResolvedArgumentList
import com.ivianuu.ast.references.AstResolvedNamedReference
import com.ivianuu.ast.symbols.impl.AstCallableSymbol
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.TransformData
import com.ivianuu.ast.visitors.transformInplace
import org.jetbrains.kotlin.name.ClassId

inline val AstAnnotationCall.classId: ClassId?
    get() = coneClassLikeType?.lookupTag?.classId

inline val AstCall.arguments: List<AstExpression> get() = argumentList.arguments

inline val AstCall.argument: AstExpression get() = argumentList.arguments.first()

inline val AstCall.argumentMapping: Map<AstExpression, AstValueParameter>?
    get() = (argumentList as? AstResolvedArgumentList)?.mapping

fun AstExpression.toResolvedCallableReference(): AstResolvedNamedReference? {
    return (this as? AstResolvable)?.calleeReference as? AstResolvedNamedReference
}

fun AstExpression.toResolvedCallableSymbol(): AstCallableSymbol<*>? {
    return toResolvedCallableReference()?.resolvedSymbol as AstCallableSymbol<*>?
}

fun <D> AstBlock.transformStatementsIndexed(
    transformer: AstTransformer<D>,
    dataProducer: (Int) -> TransformData<D>
): AstBlock {
    when (this) {
        is AstBlockImpl -> statements.transformInplace(transformer, dataProducer)
        is AstSingleExpressionBlock -> {
            (dataProducer(0) as? TransformData.Data<D>)?.value?.let {
                transformStatements(
                    transformer,
                    it
                )
            }
        }
    }
    return this
}

fun <D> AstBlock.transformAllStatementsExceptLast(
    transformer: AstTransformer<D>,
    data: D
): AstBlock {
    val threshold = statements.size - 1
    return transformStatementsIndexed(transformer) { index ->
        if (index < threshold) {
            TransformData.Data(data)
        } else {
            TransformData.Nothing
        }
    }
}

fun AstBlock.replaceFirstStatement(statement: AstStatement): AstStatement {
    require(this is AstBlockImpl) {
        "replaceFirstStatement should not be called for ${this::class.simpleName}"
    }
    val existed = statements[0]
    statements[0] = statement
    return existed
}
