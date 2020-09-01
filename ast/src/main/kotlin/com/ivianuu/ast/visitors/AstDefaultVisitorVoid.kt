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

abstract class AstDefaultVisitorVoid : AstVisitorVoid() {
    override fun visitImplicitTypeRef(implicitTypeRef: AstImplicitTypeRef) {
        return visitTypeRef(implicitTypeRef)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: AstResolvedTypeRef) {
        return visitTypeRef(resolvedTypeRef)
    }

    override fun visitResolvedFunctionTypeRef(resolvedFunctionTypeRef: AstResolvedFunctionTypeRef) {
        return visitResolvedTypeRef(resolvedFunctionTypeRef)
    }

    override fun visitTypeRefWithNullability(typeRefWithNullability: AstTypeRefWithNullability) {
        return visitTypeRef(typeRefWithNullability)
    }

    override fun visitDynamicTypeRef(dynamicTypeRef: AstDynamicTypeRef) {
        return visitTypeRefWithNullability(dynamicTypeRef)
    }

    override fun visitFunctionTypeRef(functionTypeRef: AstFunctionTypeRef) {
        return visitTypeRefWithNullability(functionTypeRef)
    }

    override fun visitUserTypeRef(userTypeRef: AstUserTypeRef) {
        return visitTypeRefWithNullability(userTypeRef)
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: AstCallableReferenceAccess) {
        return visitQualifiedAccessExpression(callableReferenceAccess)
    }

    override fun visitComponentCall(componentCall: AstComponentCall) {
        return visitFunctionCall(componentCall)
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
