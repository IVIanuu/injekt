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
import com.ivianuu.ast.types.AstDynamicType
import com.ivianuu.ast.types.AstFunctionType
import com.ivianuu.ast.types.AstImplicitType
import com.ivianuu.ast.types.AstResolvedFunctionType
import com.ivianuu.ast.types.AstResolvedType
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeWithNullability
import com.ivianuu.ast.types.AstUserType

abstract class AstDefaultTransformer<D> : AstTransformer<D>() {
    override fun transformImplicitType(
        implicitType: AstImplicitType,
        data: D
    ): CompositeTransformResult<AstType> {
        return transformType(implicitType, data)
    }

    override fun transformResolvedType(
        resolvedType: AstResolvedType,
        data: D
    ): CompositeTransformResult<AstType> {
        return transformType(resolvedType, data)
    }

    override fun transformResolvedFunctionType(
        resolvedFunctionType: AstResolvedFunctionType,
        data: D
    ): CompositeTransformResult<AstType> {
        return transformResolvedType(resolvedFunctionType, data)
    }

    override fun transformTypeWithNullability(
        TypeWithNullability: AstTypeWithNullability,
        data: D
    ): CompositeTransformResult<AstType> {
        return transformType(TypeWithNullability, data)
    }

    override fun transformDynamicType(
        dynamicType: AstDynamicType,
        data: D
    ): CompositeTransformResult<AstType> {
        return transformTypeWithNullability(dynamicType, data)
    }

    override fun transformFunctionType(
        functionType: AstFunctionType,
        data: D
    ): CompositeTransformResult<AstType> {
        return transformTypeWithNullability(functionType, data)
    }

    override fun transformUserType(
        userType: AstUserType,
        data: D
    ): CompositeTransformResult<AstType> {
        return transformTypeWithNullability(userType, data)
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

