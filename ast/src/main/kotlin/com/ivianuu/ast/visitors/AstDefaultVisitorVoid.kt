/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.visitors

import com.ivianuu.ast.expressions.AstBreakExpression
import com.ivianuu.ast.expressions.AstCallableReferenceAccess
import com.ivianuu.ast.expressions.AstContinueExpression
import com.ivianuu.ast.expressions.AstLambdaArgumentExpression
import com.ivianuu.ast.expressions.AstNamedArgumentExpression
import com.ivianuu.ast.expressions.AstReturnExpression
import com.ivianuu.ast.expressions.AstSpreadArgumentExpression

abstract class AstDefaultVisitorVoid : AstVisitorVoid() {

    override fun visitCallableReferenceAccess(callableReferenceAccess: AstCallableReferenceAccess) {
        return visitQualifiedAccessExpression(callableReferenceAccess)
    }

    override fun visitReturnExpression(returnExpression: AstReturnExpression) {
        return visitJump(returnExpression)
    }

    override fun visitContinueExpression(continueExpression: AstContinueExpression) {
        return visitJump(continueExpression)
    }

    override fun visitBreakExpression(breakExpression: AstBreakExpression) {
        return visitJump(breakExpression)
    }


    override fun visitLambdaArgumentExpression(lambdaArgumentExpression: AstLambdaArgumentExpression) {
        return visitWrappedArgumentExpression(lambdaArgumentExpression)
    }

    override fun visitSpreadArgumentExpression(spreadArgumentExpression: AstSpreadArgumentExpression) {
        return visitWrappedArgumentExpression(spreadArgumentExpression)
    }

    override fun visitNamedArgumentExpression(namedArgumentExpression: AstNamedArgumentExpression) {
        return visitWrappedArgumentExpression(namedArgumentExpression)
    }

}
