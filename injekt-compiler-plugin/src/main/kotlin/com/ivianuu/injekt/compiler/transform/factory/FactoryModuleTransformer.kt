package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.at
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class FactoryModuleTransformer(
    context: IrPluginContext
) : AbstractInjektTransformer(context) {

    val moduleFunctionsByFactoryFunctions = mutableMapOf<IrFunction, IrFunction>()

    override fun visitFile(declaration: IrFile): IrFile {
        val factoryFunctions = mutableListOf<IrFunction>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Factory) ||
                    declaration.hasAnnotation(InjektFqNames.ChildFactory)
                ) {
                    factoryFunctions += declaration
                }
                return super.visitFunction(declaration)
            }
        })

        factoryFunctions.forEach { factoryFunction ->
            DeclarationIrBuilder(pluginContext, factoryFunction.symbol).run {
                val moduleFunction = moduleFunction(factoryFunction)
                moduleFunctionsByFactoryFunctions[factoryFunction] = moduleFunction
                (factoryFunction.parent as IrDeclarationContainer).addChild(moduleFunction)
                moduleFunction.parent = factoryFunction.parent
                val oldBody = factoryFunction.body!!
                factoryFunction.body = irBlockBody {
                    +irCall(moduleFunction).apply {
                        factoryFunction.valueParameters.forEach {
                            putValueArgument(it.index, irGet(it))
                        }
                    }
                    +oldBody.statements.last()
                }
            }
        }

        return super.visitFile(declaration)
    }

    private fun IrBuilderWithScope.moduleFunction(factoryFunction: IrFunction): IrFunction {
        return buildFun {
            name = InjektNameConventions.getModuleNameForFactoryFunction(factoryFunction.name)
            returnType = irBuiltIns.unitType
        }.apply {
            annotations += InjektDeclarationIrBuilder(pluginContext, symbol)
                .noArgSingleConstructorCall(symbols.module)

            val valueParametersMap = factoryFunction.valueParameters.associateWith {
                it.copyTo(this)
                    .also { valueParameters += it }
            }

            body = irBlockBody {
                factoryFunction.body!!.statements.dropLast(1).forEach {
                    +it
                }
            }
            body!!.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    return valueParametersMap[expression.symbol.owner]
                        ?.let { irGet(it) }
                        ?: super.visitGetValue(expression)
                }

                override fun visitReturn(expression: IrReturn): IrExpression {
                    return if (expression.returnTargetSymbol != factoryFunction.symbol) {
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
                        if (declaration.parent == factoryFunction)
                            declaration.parent = this@apply
                    } catch (e: Exception) {
                    }
                    return super.visitDeclaration(declaration)
                }
            })
        }
    }

}
