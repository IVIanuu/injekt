package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.interpreter.hasAnnotation
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class TypeClassCallableTransformer(
  private val context: InjektContext,
  private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {
  // add a empty body to each synthetic extension callable
  override fun visitFunction(declaration: IrFunction): IrStatement {
    if (declaration.hasAnnotation(InjektFqNames.SyntheticExtensionCallable)) {
      declaration.body = DeclarationIrBuilder(pluginContext, declaration.symbol)
        .irBlockBody {
        }
    }
    return super.visitFunction(declaration)
  }

  // transform synthetic extension callable calls
  override fun visitCall(expression: IrCall): IrExpression {
    val result = super.visitCall(expression) as IrCall
    val callee = result.symbol.owner
    val actualCalleeKey = callee
      .annotations
      .findAnnotation(InjektFqNames.SyntheticExtensionCallable)
      ?.getValueArgument(0)
      ?.cast<IrConst<String>>()
      ?.value
      ?: return result

    val actualCallee = callee.parent
      .cast<IrClass>()
      .parent
      .cast<IrClass>()
      .functions
      .single { it.descriptor.uniqueKey(context) == actualCalleeKey }

    return DeclarationIrBuilder(pluginContext, actualCallee.symbol)
      .irCall(actualCallee).apply {
        dispatchReceiver = result.getValueArgument(result.valueArgumentsCount - 1)
        extensionReceiver = result.extensionReceiver
        (0 until result.valueArgumentsCount - 1)
          .forEach { putValueArgument(it, result.getValueArgument(it)) }
      }
  }
}
