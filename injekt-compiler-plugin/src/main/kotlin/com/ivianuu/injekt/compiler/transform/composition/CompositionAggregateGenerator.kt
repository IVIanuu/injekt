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

package com.ivianuu.injekt.compiler.transform.composition

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.addClass
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class CompositionAggregateGenerator(
    pluginContext: IrPluginContext,
    private val project: Project
) : AbstractInjektTransformer(pluginContext) {

    val compositionElements = mutableMapOf<IrClassSymbol, MutableList<IrFunctionSymbol>>()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val compositionFactories = mutableListOf<IrFunction>()
        val installInCalls = mutableMapOf<IrFunction, MutableList<IrCall>>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                when {
                    declaration.hasAnnotation(InjektFqNames.CompositionFactory) -> {
                        compositionFactories += declaration
                    }
                    declaration.hasAnnotation(InjektFqNames.Module) -> {
                        declaration.body?.transformChildrenVoid(object :
                            IrElementTransformerVoid() {
                            override fun visitCall(expression: IrCall): IrExpression {
                                if (expression.symbol.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.installIn") {
                                    installInCalls.getOrPut(declaration) { mutableListOf() } += expression
                                }
                                return super.visitCall(expression)
                            }
                        })
                    }
                }
                return super.visitFunction(declaration)
            }
        })

        compositionFactories.forEach { compositionFactory ->
            val compositionType = compositionFactory.returnType.classOrNull!!
            val elementClass = InjektDeclarationIrBuilder(pluginContext, compositionFactory.symbol)
                .emptyClass(
                    InjektNameConventions
                        .getCompositionElementNameForFunction(
                            compositionType.descriptor.fqNameSafe,
                            compositionFactory
                        )
                )
            compositionElements.getOrPut(compositionType) { mutableListOf() } += compositionFactory.symbol
            moduleFragment.addClass(
                pluginContext,
                project,
                InjektFqNames.CompositionsPackage,
                elementClass
            )
        }

        installInCalls.forEach { (function, calls) ->
            calls.forEach { call ->
                val compositionType = call.getTypeArgument(0)!!.classOrNull!!
                val elementClass = InjektDeclarationIrBuilder(pluginContext, function.symbol)
                    .emptyClass(
                        InjektNameConventions
                            .getCompositionElementNameForFunction(
                                compositionType.descriptor.fqNameSafe,
                                function
                            )
                    )
                compositionElements.getOrPut(compositionType) { mutableListOf() } += function.symbol
                moduleFragment.addClass(
                    pluginContext,
                    project,
                    InjektFqNames.CompositionsPackage,
                    elementClass
                )
            }
        }

        return super.visitModuleFragment(declaration)
    }

}
