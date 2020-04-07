package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class PropertyNameIntrinsicTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val propertyStack = mutableListOf<IrProperty>()

    override fun visitProperty(declaration: IrProperty): IrStatement {
        return try {
            propertyStack.push(declaration)
            super.visitProperty(declaration)
        } finally {
            propertyStack.pop()
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        super.visitCall(expression)

        if (propertyStack.isNotEmpty()) {
            expression.symbol.descriptor.valueParameters.forEach { valueParameterDescriptor ->
                val valueArgument = expression.getValueArgument(valueParameterDescriptor.index)
                if (valueArgument == null &&
                    valueParameterDescriptor.annotations.hasAnnotation(InjektClassNames.PropertyName)
                ) {
                    expression.putValueArgument(
                        valueParameterDescriptor.index,
                        DeclarationIrBuilder(pluginContext, expression.symbol)
                            .irString(propertyStack.last().descriptor.fqNameSafe.asString())
                    )
                }
            }
        }

        return expression
    }

}