package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.at
import org.jetbrains.kotlin.ir.builders.declarations.addExtensionReceiver
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ComponentBlockTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val componentDsl = getTopLevelClass(InjektClassNames.ComponentDsl)
    private val module = getTopLevelClass(InjektClassNames.Module)

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
            DeclarationIrBuilder(pluginContext, componentCall.symbol).run {
                val expr = componentCall.getValueArgument(1) as IrFunctionExpression
                val function = moduleFunction(expr)
                declaration.addChild(function)
                expr.function.body = irExprBody(
                    irCall(function).apply {
                        extensionReceiver = componentCall.extensionReceiver
                    }
                )
            }
        }

        return super.visitFile(declaration)
    }

    private fun IrBuilderWithScope.moduleFunction(componentDefinition: IrFunctionExpression): IrFunction {
        val definitionFunction = componentDefinition.function
        return buildFun {
            name = Name.identifier("ComponentModule${definitionFunction.startOffset}")
            returnType = pluginContext.irBuiltIns.unitType
            visibility = Visibilities.PRIVATE
        }.apply {
            annotations += irCall(
                symbolTable.referenceConstructor(
                    module.unsubstitutedPrimaryConstructor!!
                ),
                module.defaultType.toIrType()
            )
            addExtensionReceiver(componentDsl.defaultType.toIrType())
            body = definitionFunction.body!!.deepCopyWithVariables()
            body!!.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    return if (expression.symbol == definitionFunction.extensionReceiverParameter!!.symbol) {
                        irGet(extensionReceiverParameter!!)
                    } else super.visitGetValue(expression)
                }

                override fun visitReturn(expression: IrReturn): IrExpression {
                    return if (expression.returnTargetSymbol != definitionFunction.symbol) {
                        super.visitReturn(expression)
                    } else {
                        at(expression.startOffset, expression.endOffset)
                        DeclarationIrBuilder(
                            pluginContext,
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
        }
    }

}