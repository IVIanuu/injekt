/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, UnsafeDuringIrConstructionAPI::class)

package com.ivianuu.injekt.compiler.ir

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class ContextualFunctionTransformer(
  compilationDeclarations: CompilationDeclarations,
  irCtx: IrPluginContext,
  ctx: InjektContext
) : AbstractInjektTransformer(compilationDeclarations, irCtx, ctx) {
  override fun visitFunctionNew(declaration: IrFunction): IrStatement {
    ensureContextualParametersAdded(declaration)
    return super.visitFunctionNew(declaration)
  }

  override fun visitCall(expression: IrCall): IrExpression {
    val result = super.visitCall(expression) as IrCall

    val newResult = if (!result.symbol.owner.hasAnnotation(InjektFqNames.Contextual)) result
    else {
      ensureContextualParametersAdded(expression.symbol.owner)
      DeclarationIrBuilder(irCtx, result.symbol, result.startOffset, result.endOffset)
        .irCall(
          result.symbol.cast(),
          result.type,
          result.symbol.owner.valueParameters.size,
          result.symbol.owner.typeParameters.size,
          result.origin
        ).let { newResult ->
          (0 until result.typeArgumentsCount).forEach {
            newResult.putTypeArgument(it, result.getTypeArgument(it))
          }
          newResult.dispatchReceiver = result.dispatchReceiver
          newResult.extensionReceiver = result.extensionReceiver
          (0 until result.valueArgumentsCount).forEach {
            newResult.putValueArgument(it, result.getValueArgument(it))
          }
          newResult
        }
    }

    return newResult
  }

  private fun ensureContextualParametersAdded(declaration: IrFunction) {
    if (declaration.hasAnnotation(InjektFqNames.Contextual)) {
      val callableInfo = declaration.symbol
        .toFirSymbol<FirFunctionSymbol<*>>()!!
        .toInjektCallable(ctx)

      if (declaration.valueParameters.size == callableInfo.parameterTypes.size) {
        callableInfo.contextualParameters.forEach {
          declaration.addValueParameter(
            name = it.contextualParameterName(),
            type = it.toIrType(DeclarationIrBuilder(
              irCtx,
              declaration.symbol
            )).cast()
          )
        }
      }
    }
  }
}
