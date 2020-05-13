package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFunction

class InlineModuleLambdaTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val functionStack = mutableListOf<IrFunction>()

    override fun visitFunction(declaration: IrFunction): IrStatement {
        functionStack.push(declaration)
        return super.visitFunction(declaration)
            .also { functionStack.pop() }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val callee = expression.symbol.owner
        if (!callee.isModule(pluginContext.bindingContext)) return super.visitCall(expression)

        val moduleLambdasByParameter = expression.getArgumentsWithIr()
            .filter { it.first.type.isFunction() && it.first.type.hasAnnotation(InjektFqNames.Module) }
            .filter { it.second is IrFunctionExpression }
            .map { it.second as IrFunctionExpression }

        if (moduleLambdasByParameter.isEmpty()) return super.visitCall(expression)

        return DeclarationIrBuilder(pluginContext, expression.symbol).run {
            irBlock(origin = InlineModuleLambdaOrigin) {
                +expression
                moduleLambdasByParameter.forEach { moduleLambda ->
                    +irCall(moduleLambda.function).apply {
                        moduleLambda.function.valueParameters.indices.forEach {
                            putValueArgument(it, null)
                        }
                    }
                }
            }
        }
    }

}

object InlineModuleLambdaOrigin : IrStatementOrigin
