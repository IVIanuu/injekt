/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.getNearestDeclarationContainer
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.module.ModuleFunctionTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.at
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class FactoryModuleTransformer(
    context: IrPluginContext,
    private val moduleFunctionTransformer: ModuleFunctionTransformer
) : AbstractInjektTransformer(context) {

    private val moduleFunctionsByFactoryFunctions =
        mutableMapOf<IrFunction, IrFunction>()

    override fun visitFile(declaration: IrFile): IrFile {
        val factoryFunctions = mutableListOf<IrFunction>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Factory) ||
                    declaration.hasAnnotation(InjektFqNames.ChildFactory) ||
                    declaration.hasAnnotation(InjektFqNames.CompositionFactory)
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
        moduleFunctionsByFactoryFunctions[factoryFunction]?.let {
            return moduleFunctionTransformer.getTransformedModule(it)
        }

        val moduleFunction = DeclarationIrBuilder(pluginContext, factoryFunction.symbol).run {
            val moduleFunction = moduleFunction(factoryFunction)
            moduleFunction.parent = factoryFunction.parent
            var block: IrBlock? = null
            if (factoryFunction.isLocal) {
                factoryFunction.parent.accept(object : IrElementTransformerVoid() {
                    override fun visitBlock(expression: IrBlock): IrExpression {
                        if (expression.statements.any { it === factoryFunction }) {
                            block = expression
                        }
                        return super.visitBlock(expression)
                    }
                }, null)
            }
            if (block != null) {
                val index = block!!.statements.indexOf(factoryFunction)
                block!!.statements.add(index, moduleFunction)
            } else {
                factoryFunction.getNearestDeclarationContainer().addChild(moduleFunction)
            }
            factoryFunction.body = irBlockBody {
                +irCall(moduleFunction).apply {
                    factoryFunction.typeParameters.forEach {
                        putTypeArgument(it.index, it.defaultType)
                    }
                    factoryFunction.allParameters.forEachIndexed { index, valueParameter ->
                        putValueArgument(index, irGet(valueParameter))
                    }
                }
                +factoryFunction.body!!.statements.last()
            }
            moduleFunction
        }

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

            metadata = MetadataSource.Function(descriptor)

            copyTypeParametersFrom(factoryFunction)

            val valueParametersMap = factoryFunction.allParameters
                .associateWith {
                    addValueParameter(
                        "p${valueParameters.size}",
                        it.type
                    )
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
