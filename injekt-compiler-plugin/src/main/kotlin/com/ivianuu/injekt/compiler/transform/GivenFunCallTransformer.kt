package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.analysis.GivenFunFunctionDescriptor
import com.ivianuu.injekt.compiler.asNameId
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

class GivenFunCallTransformer(private val pluginContext: IrPluginContext) :
    IrElementTransformerVoid() {
    override fun visitCall(expression: IrCall): IrExpression {
        val result = super.visitCall(expression) as IrCall
        val descriptor = expression.symbol.descriptor
        if (descriptor is GivenFunFunctionDescriptor) {
            val givenFunInvoke = pluginContext.referenceFunctions(
                descriptor.givenFunDescriptor
                    .fqNameUnsafe
                    .parent()
                    .child("invoke${
                        descriptor.givenFunDescriptor.name.asString().capitalize()
                    }".asNameId())
                    .toSafe()
            ).single()
            return DeclarationIrBuilder(pluginContext, result.symbol)
                .irCall(givenFunInvoke)
                .apply {
                    extensionReceiver = result.dispatchReceiver
                    (0 until valueArgumentsCount)
                        .forEach { putValueArgument(it, result.getValueArgument(it)) }
                }
        }
        return result
    }
}