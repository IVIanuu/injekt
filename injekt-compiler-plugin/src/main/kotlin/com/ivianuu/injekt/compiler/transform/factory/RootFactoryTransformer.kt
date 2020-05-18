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
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class RootFactoryTransformer(
    context: IrPluginContext,
    private val declarationStore: InjektDeclarationStore
) : AbstractInjektTransformer(context) {

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val factoryFunctions = mutableListOf<IrFunction>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Factory) && !declaration.isInline) {
                    factoryFunctions += declaration
                }
                return super.visitFunctionNew(declaration)
            }
        })

        factoryFunctions.forEach { function ->
            DeclarationIrBuilder(pluginContext, function.symbol).run {
                val moduleValueArguments = function.body!!.statements
                    .first()
                    .let { it as IrCall }
                    .getArgumentsWithIr()
                    .map { it.second }
                val moduleClass = declarationStore.getModuleClassForFunction(
                    declarationStore.getModuleFunctionForFactory(function)
                )
                function.body = irExprBody(
                    when {
                        function.hasAnnotation(InjektFqNames.AstImplFactory) -> {
                            val implFactory = ImplFactory(
                                origin = function.descriptor.fqNameSafe,
                                parent = null,
                                irDeclarationParent = function.parent,
                                name = InjektNameConventions.getClassImplNameForFactoryFunction(
                                    function
                                ),
                                superType = function.returnType,
                                moduleClass = moduleClass,
                                typeParameterMap = emptyMap(),
                                pluginContext = pluginContext,
                                symbols = symbols,
                                declarationStore = declarationStore
                            )

                            function.file.addChild(implFactory.clazz)

                            implFactory.getInitExpression(moduleValueArguments)
                        }
                        function.hasAnnotation(InjektFqNames.AstInstanceFactory) -> {
                            val instanceFactory = InstanceFactory(
                                factoryFunction = function,
                                typeParameterMap = emptyMap(),
                                moduleClass = moduleClass,
                                pluginContext = pluginContext,
                                symbols = symbols,
                                declarationStore = declarationStore
                            )

                            instanceFactory.getInstanceExpression(moduleValueArguments)
                        }
                        else -> error("Unexpected factory ${function.dump()}")
                    }
                )
            }
        }

        return super.visitModuleFragment(declaration)
    }

}
