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

import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class RunReadingTransformer(
    pluginContext: IrPluginContext
) : AbstractInjektTransformer(pluginContext) {

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val runReadingCalls = mutableListOf<Pair<IrCall, IrFile>>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.descriptor.fqNameSafe.asString() ==
                    "com.ivianuu.injekt.composition.runReading"
                ) {
                    runReadingCalls += expression to currentFile
                }
                return super.visitCall(expression)
            }
        })

        val newExpressionsByCall = mutableMapOf<IrCall, IrExpression>()

        runReadingCalls.forEach { (call, file) ->
            val entryPoint = try {
                entryPointForReader(
                    InjektNameConventions.getObjectGraphGetNameForCall(file, call),
                    call.getValueArgument(0)!!.type.typeArguments.first().typeOrFail
                )
            } catch (e: Exception) {
                error("Not working ${call.dump()}")
            }

            file.addChild(entryPoint)

            newExpressionsByCall[call] =
                DeclarationIrBuilder(pluginContext, call.symbol).run {
                    irCall(
                        pluginContext.tmpFunction(1)
                            .owner
                            .functions
                            .first { it.name.asString() == "invoke" }
                    ).apply {
                        dispatchReceiver = call.getValueArgument(0)!!
                        putValueArgument(
                            0,
                            IrCallImpl(
                                call.startOffset,
                                call.endOffset,
                                entryPoint.defaultType,
                                pluginContext.referenceFunctions(
                                    FqName("com.ivianuu.injekt.composition.entryPointOf")
                                ).single()
                            ).apply {
                                putTypeArgument(0, entryPoint.defaultType)
                                putValueArgument(0, call.extensionReceiver)
                            }
                        )
                    }
                }
        }

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression =
                newExpressionsByCall[expression]
                    ?.also { println("lololo ${expression.dump()} to ${it.dump()}") }
                    ?: super.visitCall(expression)
        })

        return super.visitModuleFragment(declaration)
    }

    private fun entryPointForReader(
        name: Name,
        superType: IrType
    ) = buildClass {
        this.name = name
        kind = ClassKind.INTERFACE
    }.apply {
        createImplicitParameterDeclarationWithWrappedDescriptor()
        addMetadataIfNotLocal()
        superTypes += superType
    }

}
