package com.ivianuu.ast

import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstExpression
import org.jetbrains.kotlin.analyzer.ModuleInfo

fun ModuleInfo.dependenciesWithoutSelf(): Sequence<ModuleInfo> =
    dependencies().asSequence().filter { it != this }

// TODO: rewrite
fun AstBlock.returnExpressions(): List<AstExpression> =
    listOfNotNull(statements.lastOrNull() as? AstExpression)

private val PUBLIC_METHOD_NAMES_IN_OBJECT =
    setOf("equals", "hashCode", "getClass", "wait", "notify", "notifyAll", "toString")
