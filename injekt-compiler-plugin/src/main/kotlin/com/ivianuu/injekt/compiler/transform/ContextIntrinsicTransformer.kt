package com.ivianuu.injekt.compiler.transform

import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ContextIntrinsicTransformer(injektContext: InjektContext) :
    AbstractInjektTransformer(injektContext) {

    override fun lower() {
        injektContext.module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.<get-readerContext>") {
                    return expression.getValueArgument(0) ?: error("Expected non-null context argument")
                } else if (expression.symbol.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.runReader") {
                    return DeclarationIrBuilder(injektContext, expression.symbol)
                        .irCall(
                            injektContext.referenceFunctions(
                                FqName("com.ivianuu.injekt.runReaderDummy")
                            ).single()
                        ).apply {
                            copyTypeArgumentsFrom(expression)
                            putValueArgument(0, expression.extensionReceiver)
                            putValueArgument(1, expression.getValueArgument(0))
                            transformChildrenVoid()
                        }
                }
                return super.visitCall(expression)
            }
        })
    }

}
