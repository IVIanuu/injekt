package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.at
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irReturnUnit
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class FactoryBlockTransformer(
    context: IrPluginContext
) : AbstractInjektTransformer(context) {

    override fun visitFile(declaration: IrFile): IrFile {
        val factoryFunctions = mutableListOf<IrFunction>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.annotations.hasAnnotation(InjektFqNames.Factory) ||
                    declaration.annotations.hasAnnotation(InjektFqNames.ChildFactory)
                ) {
                    factoryFunctions += declaration
                }
                return super.visitFunction(declaration)
            }
        })

        factoryFunctions.forEach { factoryFunction ->
            DeclarationIrBuilder(context, factoryFunction.symbol).run {
                val createImplementation = factoryFunction.body!!.statements.single()
                    .let { it as IrReturn }.value as IrCall
                val moduleBlock =
                    createImplementation.getValueArgument(0) as? IrFunctionExpression ?: return@run

                val moduleFunction = moduleFunction(factoryFunction, moduleBlock)
                (factoryFunction.parent as IrDeclarationContainer).addChild(moduleFunction)
                moduleFunction.parent = factoryFunction.parent

                moduleBlock.function.body = irExprBody(
                    irCall(moduleFunction).apply {
                        factoryFunction.valueParameters.forEach {
                            putValueArgument(it.index, irGet(it))
                        }
                    }
                )
            }
        }

        return super.visitFile(declaration)
    }

    private fun IrBuilderWithScope.moduleFunction(
        factoryFunction: IrFunction,
        moduleBlock: IrFunctionExpression?
    ): IrFunction {
        val moduleBlockFunction = moduleBlock?.function

        return buildFun {
            name = InjektNameConventions.getModuleNameForFactoryBlock(factoryFunction.name)
            returnType = irBuiltIns.unitType
        }.apply {
            annotations += noArgSingleConstructorCall(symbols.module)

            val valueParametersMap = factoryFunction.valueParameters.associateWith {
                it.copyTo(this)
                    .also { valueParameters += it }
            }

            moduleBlockFunction?.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    return valueParametersMap[expression.symbol.owner]
                        ?.let { irGet(it) }
                        ?: super.visitGetValue(expression)
                }

                override fun visitReturn(expression: IrReturn): IrExpression {
                    return if (expression.returnTargetSymbol != moduleBlockFunction.symbol) {
                        super.visitReturn(expression)
                    } else {
                        at(expression.startOffset, expression.endOffset)
                        DeclarationIrBuilder(context, symbol).irReturn(
                            expression.value.transform(this, null)
                        )
                    }
                }

                override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                    try {
                        if (declaration.parent == moduleBlockFunction)
                            declaration.parent = this@apply
                    } catch (e: Exception) {
                    }
                    return super.visitDeclaration(declaration)
                }
            })

            body = moduleBlockFunction?.body?.deepCopyWithVariables() ?: irExprBody(irReturnUnit())
        }
    }

}
