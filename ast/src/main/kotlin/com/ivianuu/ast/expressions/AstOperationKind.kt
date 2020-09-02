/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.expressions

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.EnumMap
import java.util.EnumSet

enum class AstOperation(val operator: String = "???") {
    // Binary
    EQ("=="),
    NOT_EQ("!="),
    IDENTITY("==="),
    NOT_IDENTITY("!=="),
    LT("<"),
    GT(">"),
    LT_EQ("<="),
    GT_EQ(">="),

    ASSIGN("="),
    PLUS_ASSIGN("+="),
    MINUS_ASSIGN("-="),
    TIMES_ASSIGN("*="),
    DIV_ASSIGN("/="),
    REM_ASSIGN("%="),

    // Unary
    EXCL("!"),

    // Type
    IS("is"),
    NOT_IS("!is"),
    AS("as"),
    SAFE_AS("as?"),

    // All non-standard operations (infix calls)
    OTHER;

    companion object {
        val ASSIGNMENTS: Set<AstOperation> =
            EnumSet.of(ASSIGN, PLUS_ASSIGN, MINUS_ASSIGN, TIMES_ASSIGN, DIV_ASSIGN, REM_ASSIGN)

        val BOOLEANS: Set<AstOperation> = EnumSet.of(
            EQ, NOT_EQ, IDENTITY, NOT_IDENTITY, LT, GT, LT_EQ, GT_EQ, IS, NOT_IS
        )

        val COMPARISONS: Set<AstOperation> = EnumSet.of(LT, GT, LT_EQ, GT_EQ)

        val TYPES: Set<AstOperation> = EnumSet.of(IS, NOT_IS, AS, SAFE_AS)
    }
}

object AstOperationNameConventions {
    val ASSIGNMENTS: Map<AstOperation, Name> = EnumMap(
        mapOf(
            AstOperation.PLUS_ASSIGN to OperatorNameConventions.PLUS_ASSIGN,
            AstOperation.MINUS_ASSIGN to OperatorNameConventions.MINUS_ASSIGN,
            AstOperation.TIMES_ASSIGN to OperatorNameConventions.TIMES_ASSIGN,
            AstOperation.DIV_ASSIGN to OperatorNameConventions.DIV_ASSIGN,
            AstOperation.REM_ASSIGN to OperatorNameConventions.REM_ASSIGN
        )
    )

    val ASSIGNMENTS_TO_SIMPLE_OPERATOR: Map<AstOperation, Name> = EnumMap(
        mapOf(
            AstOperation.PLUS_ASSIGN to OperatorNameConventions.PLUS,
            AstOperation.MINUS_ASSIGN to OperatorNameConventions.MINUS,
            AstOperation.TIMES_ASSIGN to OperatorNameConventions.TIMES,
            AstOperation.DIV_ASSIGN to OperatorNameConventions.DIV,
            AstOperation.REM_ASSIGN to OperatorNameConventions.REM
        )
    )
}

/*
sealed class AstOperationKind(val operator: String)

sealed class AstComparisonOperationKind(operator: String) : AstOperationKind(operator) {
    object LessThan : AstComparisonOperationKind("<")
    object GreaterThan : AstComparisonOperationKind(">")
    object LessThanEqual : AstComparisonOperationKind("<=")
    object GreaterThanEqual : AstComparisonOperationKind(">=")
}

sealed class AstEqualityOperationKind(operator: String) : AstOperationKind(operator) {
    object Equal : AstEqualityOperationKind("==")
    object NotEqual : AstEqualityOperationKind("!=")
    object Identity : AstEqualityOperationKind("===")
    object NotIdentity : AstEqualityOperationKind("!==")
}

sealed class AstLogicOperationKind(operator: String) : AstOperationKind(operator) {
    object And : AstLogicOperationKind("&&")
    object Or : AstLogicOperationKind("||")
}

sealed class AstTypeOperationKind(operator: String) : AstOperationKind(operator) {
    object Is : AstTypeOperationKind("is")
    object NotIs : AstTypeOperationKind("!is")
    object As : AstTypeOperationKind("as")
    object SafeAs : AstTypeOperationKind("as?")
}

sealed class AstAssignmentOperationKind(operator: String) : AstOperationKind(operator) {
    object PlusAssign = ""
}

enum class AstOperation2(val operator: String = "???") {
    PLUS_ASSIGN("+="),
    MINUS_ASSIGN("-="),
    TIMES_ASSIGN("*="),
    DIV_ASSIGN("/="),
    REM_ASSIGN("%="),

    // Unary
    EXCL("!"),
}
*/
