package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class DeclarationNameIntrinsicTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    override fun visitFunction(declaration: IrFunction): IrStatement {
        declaration.transformChildrenVoid(DeclarationNameVisitor(declaration))
        return super.visitFunction(declaration)
    }

    override fun visitProperty(declaration: IrProperty): IrStatement {
        declaration.transformChildrenVoid(DeclarationNameVisitor(declaration))
        return super.visitProperty(declaration)
    }

    private inner class DeclarationNameVisitor(
        private val declaration: IrDeclaration
    ) : IrElementTransformerVoid() {
        override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
            val callee = expression.symbol.owner
            callee.valueParameters.forEach { valueParameter ->
                // todo ir valueParameter has no annotations
                if (valueParameter.descriptor.annotations.hasAnnotation(InjektClassNames.DeclarationName) &&
                    expression.getValueArgument(valueParameter.index) == null
                ) {
                    expression.putValueArgument(
                        valueParameter.index,
                        DeclarationIrBuilder(pluginContext, expression.symbol)
                            .irString(declaration.descriptor.fqNameSafe.asString())
                    )
                }
            }
            return super.visitConstructorCall(expression)
        }

        override fun visitCall(expression: IrCall): IrExpression {
            val callee = expression.symbol.owner
            callee.valueParameters.forEach { valueParameter ->
                // todo ir valueParameter has no annotations
                if (valueParameter.descriptor.annotations.hasAnnotation(InjektClassNames.DeclarationName) &&
                    expression.getValueArgument(valueParameter.index) == null
                ) {
                    expression.putValueArgument(
                        valueParameter.index,
                        DeclarationIrBuilder(pluginContext, expression.symbol)
                            .irString(declaration.descriptor.fqNameSafe.asString())
                    )
                }
            }
            return super.visitCall(expression)
        }
    }

}
