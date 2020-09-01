/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.visitors

import com.ivianuu.ast.expressions.AstBreakExpression
import com.ivianuu.ast.expressions.AstCallableReferenceAccess
import com.ivianuu.ast.expressions.AstComponentCall
import com.ivianuu.ast.expressions.AstContinueExpression
import com.ivianuu.ast.expressions.AstLambdaArgumentExpression
import com.ivianuu.ast.expressions.AstNamedArgumentExpression
import com.ivianuu.ast.expressions.AstReturnExpression
import com.ivianuu.ast.expressions.AstSpreadArgumentExpression
import com.ivianuu.ast.types.AstDynamicType
import com.ivianuu.ast.types.AstFunctionType
import com.ivianuu.ast.types.AstImplicitType
import com.ivianuu.ast.types.AstResolvedFunctionType
import com.ivianuu.ast.types.AstResolvedType
import com.ivianuu.ast.types.AstTypeWithNullability
import com.ivianuu.ast.types.AstUserType

abstract class AstDefaultVisitor<R, D> : AstVisitor<R, D>() {
    override fun visitImplicitType(implicitType: AstImplicitType, data: D): R {
        return visitType(implicitType, data)
    }

    override fun visitResolvedType(resolvedType: AstResolvedType, data: D): R {
        return visitType(resolvedType, data)
    }

    override fun visitResolvedFunctionType(
        resolvedFunctionType: AstResolvedFunctionType,
        data: D
    ): R {
        return visitResolvedType(resolvedFunctionType, data)
    }

    override fun visitTypeWithNullability(
        TypeWithNullability: AstTypeWithNullability,
        data: D
    ): R {
        return visitType(TypeWithNullability, data)
    }

    override fun visitDynamicType(dynamicType: AstDynamicType, data: D): R {
        return visitTypeWithNullability(dynamicType, data)
    }

    override fun visitFunctionType(functionType: AstFunctionType, data: D): R {
        return visitTypeWithNullability(functionType, data)
    }

    override fun visitUserType(userType: AstUserType, data: D): R {
        return visitTypeWithNullability(userType, data)
    }

    override fun visitCallableReferenceAccess(
        callableReferenceAccess: AstCallableReferenceAccess,
        data: D
    ): R {
        return visitQualifiedAccessExpression(callableReferenceAccess, data)
    }

    override fun visitComponentCall(componentCall: AstComponentCall, data: D): R {
        return visitFunctionCall(componentCall, data)
    }

    override fun visitReturnExpression(returnExpression: AstReturnExpression, data: D): R {
        return visitJump(returnExpression, data)
    }

    override fun visitContinueExpression(continueExpression: AstContinueExpression, data: D): R {
        return visitJump(continueExpression, data)
    }

    override fun visitBreakExpression(breakExpression: AstBreakExpression, data: D): R {
        return visitJump(breakExpression, data)
    }


    override fun visitLambdaArgumentExpression(
        lambdaArgumentExpression: AstLambdaArgumentExpression,
        data: D
    ): R {
        return visitWrappedArgumentExpression(lambdaArgumentExpression, data)
    }

    override fun visitSpreadArgumentExpression(
        spreadArgumentExpression: AstSpreadArgumentExpression,
        data: D
    ): R {
        return visitWrappedArgumentExpression(spreadArgumentExpression, data)
    }

    override fun visitNamedArgumentExpression(
        namedArgumentExpression: AstNamedArgumentExpression,
        data: D
    ): R {
        return visitWrappedArgumentExpression(namedArgumentExpression, data)
    }

}

