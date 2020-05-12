package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.getNearestDeclarationContainer
import com.ivianuu.injekt.compiler.transform.module.TypedModuleTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.at
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class FactoryModuleTransformer(
    context: IrPluginContext,
    private val typedModuleTransformer: TypedModuleTransformer
) : AbstractInjektTransformer(context) {

    private val moduleFunctionsByFactoryFunctions =
        mutableMapOf<IrFunction, IrFunction>()

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
            getModuleFunctionForFactoryFunction(factoryFunction)
        }

        return super.visitFile(declaration)
    }

    fun getModuleFunctionForFactoryFunction(factoryFunction: IrFunction): IrFunction {
        moduleFunctionsByFactoryFunctions[factoryFunction]?.let { return it }

        val moduleFunction = DeclarationIrBuilder(pluginContext, factoryFunction.symbol).run {
            val moduleFunction = moduleFunction(factoryFunction)
            moduleFunction.parent = factoryFunction.parent
            if (factoryFunction.parent is IrFunction) {
                var block: IrBlockBody? = null
                factoryFunction.parent.accept(object : IrElementTransformerVoid() {
                    private val blockStack = mutableListOf<IrBlockBody>()
                    override fun visitBlockBody(body: IrBlockBody): IrBody {
                        blockStack.push(body)
                        return super.visitBlockBody(body)
                            .also { blockStack.pop() }
                    }

                    override fun visitFunction(declaration: IrFunction): IrStatement {
                        if (declaration == factoryFunction) {
                            block = blockStack.last()
                        }
                        return super.visitFunction(declaration)
                    }
                }, null)
                checkNotNull(block)

                val index = block!!.statements.indexOf(factoryFunction)
                block!!.statements.add(index + 1, moduleFunction)
            } else {
                factoryFunction.getNearestDeclarationContainer().addChild(moduleFunction)
            }
            factoryFunction.body = irBlockBody {
                +InjektDeclarationIrBuilder(pluginContext, factoryFunction.symbol)
                    .irInjektIntrinsicUnit()
            }
            moduleFunction
        }.let { typedModuleTransformer.getTransformedModule(it) }

        moduleFunctionsByFactoryFunctions[factoryFunction] = moduleFunction
        return moduleFunction
    }

    private fun IrBuilderWithScope.moduleFunction(factoryFunction: IrFunction): IrFunction {
        return buildFun {
            name = InjektNameConventions.getModuleNameForFactoryFunction(factoryFunction)
            returnType = irBuiltIns.unitType
            visibility = factoryFunction.visibility
            isInline = factoryFunction.isInline
        }.apply {
            annotations += InjektDeclarationIrBuilder(pluginContext, symbol)
                .noArgSingleConstructorCall(symbols.module)

            copyTypeParametersFrom(factoryFunction)

            val valueParametersMap = factoryFunction.allParameters
                .associateWith {
                    it.copyTo(
                            this,
                            index = valueParameters.size,
                            name = Name.identifier("p${valueParameters.size}")
                        )
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
