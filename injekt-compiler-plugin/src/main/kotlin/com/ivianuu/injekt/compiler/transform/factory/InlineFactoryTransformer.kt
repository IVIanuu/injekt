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
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.typeArguments
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class InlineFactoryTransformer(
    pluginContext: IrPluginContext,
    private val declarationStore: InjektDeclarationStore
) : AbstractInjektTransformer(pluginContext) {

    override fun visitFile(declaration: IrFile): IrFile {
        super.visitFile(declaration)
        val inlineFactoryCalls = mutableListOf<IrCall>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.owner.hasAnnotation(InjektFqNames.Factory) &&
                    expression.symbol.owner.isInline
                ) {
                    inlineFactoryCalls += expression
                }
                return super.visitCall(expression)
            }
        })

        val newCallByOldCall = inlineFactoryCalls.associateWith { inlineFactoryCall ->
            val factoryFunction = inlineFactoryCall.symbol.owner
            DeclarationIrBuilder(pluginContext, inlineFactoryCall.symbol).run {
                irBlock {
                    val inlinedFactory = buildFun {
                        name = InjektNameConventions.getFunctionImplNameForFactoryCall(
                            declaration, inlineFactoryCall
                        )
                        returnType = inlineFactoryCall.type
                        visibility = Visibilities.LOCAL
                    }.apply {
                        parent = scope.getLocalDeclarationParent()

                        metadata = MetadataSource.Function(descriptor)

                        annotations += InjektDeclarationIrBuilder(pluginContext, symbol)
                            .noArgSingleConstructorCall(symbols.factory)

                        // todo this should be transformed by the FactoryFunctionAnnotationTransformer
                        if (factoryFunction.hasAnnotation(InjektFqNames.AstInstanceFactory)) {
                            annotations += InjektDeclarationIrBuilder(pluginContext, symbol)
                                .noArgSingleConstructorCall(symbols.astInstanceFactory)
                        } else if (factoryFunction.hasAnnotation(InjektFqNames.AstImplFactory)) {
                            annotations += InjektDeclarationIrBuilder(pluginContext, symbol)
                                .noArgSingleConstructorCall(symbols.astImplFactory)
                        }

                        body = irBlockBody {
                            val factoryModule = declarationStore
                                .getModuleFunctionForFactory(inlineFactoryCall.symbol.owner)
                            +irCall(factoryModule).apply {
                                inlineFactoryCall.typeArguments.forEachIndexed { index, typeArgument ->
                                    putTypeArgument(index, typeArgument)
                                }
                                inlineFactoryCall.getArgumentsWithIr()
                                    .forEachIndexed { index, (_, valueArgument) ->
                                        putValueArgument(index, valueArgument)
                                    }
                            }
                            val createFunctionName = when {
                                factoryFunction.hasAnnotation(InjektFqNames.AstInstanceFactory) -> "createInstance"
                                factoryFunction.hasAnnotation(InjektFqNames.AstImplFactory) -> "createImpl"
                                else -> error("Unexpected factory function ${factoryFunction.dump()}")
                            }

                            +irCall(
                                pluginContext.referenceFunctions(
                                    InjektFqNames.InjektPackage
                                        .child(Name.identifier(createFunctionName))
                                ).single(),
                                inlineFactoryCall.type
                            )
                        }
                    }

                    +inlinedFactory
                    +irCall(inlinedFactory)
                }
            }
        }

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                return newCallByOldCall[expression] ?: super.visitCall(expression)
            }
        })

        return declaration
    }

}