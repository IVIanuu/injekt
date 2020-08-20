package com.ivianuu.injekt.compiler.transform.readercontext

import com.ivianuu.injekt.compiler.copy
import com.ivianuu.injekt.compiler.getFunctionType
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektContext
import com.ivianuu.injekt.compiler.uniqueKey
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class MockReaderTransformer(
    injektContext: InjektContext
) : AbstractInjektTransformer(injektContext) {

    override fun lower() {
        injektContext.module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.descriptor.fqNameSafe.asString() != "com.ivianuu.injekt.mockReader")
                    return super.visitCall(expression)
                val uniqueKey = (expression.getValueArgument(0) as IrFunctionReference)
                    .symbol.owner.uniqueKey()
                val mockExpression = (expression.getValueArgument(1) as IrFunctionExpression)

                val functionType = mockExpression.function.getFunctionType(
                    injektContext
                ).let {
                    it.copy(
                        annotations = it.annotations + DeclarationIrBuilder(
                            injektContext,
                            expression.symbol
                        ).run {
                            irCall(injektContext.injektSymbols.qualifier.constructors.single()).apply {
                                putValueArgument(
                                    0,
                                    irString(uniqueKey)
                                )
                            }
                        }
                    )
                }

                return IrFunctionExpressionImpl(
                    mockExpression.startOffset,
                    mockExpression.endOffset,
                    functionType,
                    mockExpression.function,
                    mockExpression.origin
                )
            }
        })
    }

}