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
import com.ivianuu.ast.types.AstDynamicTypeRef
import com.ivianuu.ast.types.AstFunctionTypeRef
import com.ivianuu.ast.types.AstImplicitTypeRef
import com.ivianuu.ast.types.AstResolvedFunctionTypeRef
import com.ivianuu.ast.types.AstResolvedTypeRef
import com.ivianuu.ast.types.AstTypeRefWithNullability
import com.ivianuu.ast.types.AstUserTypeRef

abstract class AstDefaultVisitor<R, D> : AstVisitor<R, D>() {
    override fun visitImplicitTypeRef(implicitTypeRef: AstImplicitTypeRef, data: D): R {
        return visitTypeRef(implicitTypeRef, data)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: AstResolvedTypeRef, data: D): R {
        return visitTypeRef(resolvedTypeRef, data)
    }

    override fun visitResolvedFunctionTypeRef(
        resolvedFunctionTypeRef: AstResolvedFunctionTypeRef,
        data: D
    ): R {
        return visitResolvedTypeRef(resolvedFunctionTypeRef, data)
    }

    override fun visitTypeRefWithNullability(
        typeRefWithNullability: AstTypeRefWithNullability,
        data: D
    ): R {
        return visitTypeRef(typeRefWithNullability, data)
    }

    override fun visitDynamicTypeRef(dynamicTypeRef: AstDynamicTypeRef, data: D): R {
        return visitTypeRefWithNullability(dynamicTypeRef, data)
    }

    override fun visitFunctionTypeRef(functionTypeRef: AstFunctionTypeRef, data: D): R {
        return visitTypeRefWithNullability(functionTypeRef, data)
    }

    override fun visitUserTypeRef(userTypeRef: AstUserTypeRef, data: D): R {
        return visitTypeRefWithNullability(userTypeRef, data)
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

