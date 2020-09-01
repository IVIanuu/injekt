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
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.types.AstDynamicTypeRef
import com.ivianuu.ast.types.AstFunctionTypeRef
import com.ivianuu.ast.types.AstImplicitTypeRef
import com.ivianuu.ast.types.AstResolvedFunctionTypeRef
import com.ivianuu.ast.types.AstResolvedTypeRef
import com.ivianuu.ast.types.AstTypeRef
import com.ivianuu.ast.types.AstTypeRefWithNullability
import com.ivianuu.ast.types.AstUserTypeRef

abstract class AstDefaultTransformer<D> : AstTransformer<D>() {
    override fun transformImplicitTypeRef(
        implicitTypeRef: AstImplicitTypeRef,
        data: D
    ): CompositeTransformResult<AstTypeRef> {
        return transformTypeRef(implicitTypeRef, data)
    }

    override fun transformResolvedTypeRef(
        resolvedTypeRef: AstResolvedTypeRef,
        data: D
    ): CompositeTransformResult<AstTypeRef> {
        return transformTypeRef(resolvedTypeRef, data)
    }

    override fun transformResolvedFunctionTypeRef(
        resolvedFunctionTypeRef: AstResolvedFunctionTypeRef,
        data: D
    ): CompositeTransformResult<AstTypeRef> {
        return transformResolvedTypeRef(resolvedFunctionTypeRef, data)
    }

    override fun transformTypeRefWithNullability(
        typeRefWithNullability: AstTypeRefWithNullability,
        data: D
    ): CompositeTransformResult<AstTypeRef> {
        return transformTypeRef(typeRefWithNullability, data)
    }

    override fun transformDynamicTypeRef(
        dynamicTypeRef: AstDynamicTypeRef,
        data: D
    ): CompositeTransformResult<AstTypeRef> {
        return transformTypeRefWithNullability(dynamicTypeRef, data)
    }

    override fun transformFunctionTypeRef(
        functionTypeRef: AstFunctionTypeRef,
        data: D
    ): CompositeTransformResult<AstTypeRef> {
        return transformTypeRefWithNullability(functionTypeRef, data)
    }

    override fun transformUserTypeRef(
        userTypeRef: AstUserTypeRef,
        data: D
    ): CompositeTransformResult<AstTypeRef> {
        return transformTypeRefWithNullability(userTypeRef, data)
    }

    override fun transformCallableReferenceAccess(
        callableReferenceAccess: AstCallableReferenceAccess,
        data: D
    ): CompositeTransformResult<AstStatement> {
        return transformQualifiedAccessExpression(callableReferenceAccess, data)
    }

    override fun transformComponentCall(
        componentCall: AstComponentCall,
        data: D
    ): CompositeTransformResult<AstStatement> {
        return transformFunctionCall(componentCall, data)
    }

    override fun transformReturnExpression(
        returnExpression: AstReturnExpression,
        data: D
    ): CompositeTransformResult<AstStatement> {
        return transformJump(returnExpression, data)
    }

    override fun transformContinueExpression(
        continueExpression: AstContinueExpression,
        data: D
    ): CompositeTransformResult<AstStatement> {
        return transformJump(continueExpression, data)
    }

    override fun transformBreakExpression(
        breakExpression: AstBreakExpression,
        data: D
    ): CompositeTransformResult<AstStatement> {
        return transformJump(breakExpression, data)
    }

    override fun transformLambdaArgumentExpression(
        lambdaArgumentExpression: AstLambdaArgumentExpression,
        data: D
    ): CompositeTransformResult<AstStatement> {
        return transformWrappedArgumentExpression(lambdaArgumentExpression, data)
    }

    override fun transformSpreadArgumentExpression(
        spreadArgumentExpression: AstSpreadArgumentExpression,
        data: D
    ): CompositeTransformResult<AstStatement> {
        return transformWrappedArgumentExpression(spreadArgumentExpression, data)
    }

    override fun transformNamedArgumentExpression(
        namedArgumentExpression: AstNamedArgumentExpression,
        data: D
    ): CompositeTransformResult<AstStatement> {
        return transformWrappedArgumentExpression(namedArgumentExpression, data)
    }

}

