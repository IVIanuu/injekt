package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.getConstant
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.at
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ComponentBlockTransformer(context: IrPluginContext) :
    AbstractInjektTransformer(context) {

    override fun visitFile(declaration: IrFile): IrFile {
        val componentCalls = mutableListOf<IrCall>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.Component") {
                    componentCalls += expression
                }

                return super.visitCall(expression)
            }
        })

        componentCalls.forEach { componentCall ->
            DeclarationIrBuilder(context, componentCall.symbol).run {
                val key = componentCall.getValueArgument(0)!!.getConstant<String>()
                val expr = componentCall.getValueArgument(1) as? IrFunctionExpression

                val result = moduleFunction(key, expr)
                declaration.addChild(result.function)

                expr?.function?.body = irExprBody(
                    irCall(result.function).apply {
                        extensionReceiver = componentCall.extensionReceiver
                        result.valueParametersByCaptures.forEach { (capture, parameter) ->
                            putValueArgument(parameter.index, capture)
                        }
                    }
                )
            }
        }

        return super.visitFile(declaration)
    }

    private data class ModuleFunctionResult(
        val function: IrFunction,
        val valueParametersByCaptures: Map<IrGetValue, IrValueParameter>
    )

    private fun IrBuilderWithScope.moduleFunction(
        key: String,
        componentDefinition: IrFunctionExpression?
    ): ModuleFunctionResult {
        val definitionFunction = componentDefinition?.function

        val capturedValueParameters = mutableListOf<IrGetValue>()
        componentDefinition?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                if (expression.symbol != componentDefinition.function.extensionReceiverParameter?.symbol &&
                    expression.type.classifierOrNull != symbols.providerDsl
                ) {
                    capturedValueParameters += expression
                }
                return super.visitGetValue(expression)
            }
        })

        val valueParametersByCapture = mutableMapOf<IrGetValue, IrValueParameter>()

        val function = buildFun {
            name = Name.identifier("$key\$ComponentModule")
            returnType = this@ComponentBlockTransformer.context.irBuiltIns.unitType
            visibility = Visibilities.PUBLIC
        }.apply {
            annotations += irCall(symbols.module.constructors.single())

            capturedValueParameters.forEachIndexed { index, capture ->
                valueParametersByCapture[capture] = addValueParameter(
                    "p$index",
                    capture.type
                )
            }

            definitionFunction?.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    return valueParametersByCapture[expression]
                        ?.let { irGet(it) }
                        ?: super.visitGetValue(expression)
                }

                override fun visitReturn(expression: IrReturn): IrExpression {
                    return if (expression.returnTargetSymbol != definitionFunction.symbol) {
                        super.visitReturn(expression)
                    } else {
                        at(expression.startOffset, expression.endOffset)
                        DeclarationIrBuilder(
                            this@ComponentBlockTransformer.context,
                            symbol
                        ).irReturn(expression.value.transform(this@ComponentBlockTransformer, null))
                            .apply {
                                this.returnTargetSymbol
                            }
                    }
                }

                override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                    try {
                        if (declaration.parent == definitionFunction)
                            declaration.parent = this@apply
                    } catch (e: Exception) {
                    }
                    return super.visitDeclaration(declaration)
                }
            })

            body = definitionFunction?.body?.deepCopyWithVariables()
        }

        return ModuleFunctionResult(function, valueParametersByCapture)
    }

}