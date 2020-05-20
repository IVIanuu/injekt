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
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.withNoArgQualifiers
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class ObjectGraphCallTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {
    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val objectGraphCalls = mutableListOf<Pair<IrCall, IrFile>>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.owner.isObjectGraphGet || expression.symbol.owner.isObjectGraphInject) {
                    objectGraphCalls += expression to currentFile
                }
                return super.visitCall(expression)
            }
        })

        val newExpressionsByCall = mutableMapOf<IrCall, IrExpression>()

        objectGraphCalls.forEach { (call, file) ->
            when {
                call.symbol.owner.isObjectGraphGet -> {
                    val entryPoint = entryPointForGet(
                        InjektNameConventions.getObjectGraphGetNameForCall(file, call),
                        call.getTypeArgument(0)!!
                    )

                    file.addChild(entryPoint)

                    newExpressionsByCall[call] =
                        DeclarationIrBuilder(pluginContext, call.symbol).run {
                            irCall(entryPoint.functions.single()).apply {
                                dispatchReceiver = IrCallImpl(
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
                            }
                        }
                }
                call.symbol.owner.isObjectGraphInject -> {
                    val entryPoint = entryPointForInject(
                        InjektNameConventions.getObjectGraphInjectNameForCall(file, call),
                        call.getValueArgument(0)!!.type
                    )

                    file.addChild(entryPoint)

                    newExpressionsByCall[call] =
                        DeclarationIrBuilder(pluginContext, call.symbol).run {
                            irCall(
                                irBuiltIns.function(1).functions
                                    .first { it.owner.name.asString() == "invoke" }
                                    .owner
                            ).apply {
                                dispatchReceiver = irCall(entryPoint.functions.single()).apply {
                                    dispatchReceiver = IrCallImpl(
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
                                }

                                putValueArgument(0, call.getValueArgument(0)!!)
                            }
                        }
                }
                else -> error("Unexpected call ${call.dump()}")
            }
        }

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression =
                newExpressionsByCall[expression] ?: super.visitCall(expression)
        })

        return super.visitModuleFragment(declaration)
    }

    private fun entryPointForGet(
        name: Name,
        requestedType: IrType
    ) = buildClass {
        this.name = name
        kind = ClassKind.INTERFACE
    }.apply {
        createImplicitParameterDeclarationWithWrappedDescriptor()
        (this as IrClassImpl).metadata = MetadataSource.Class(descriptor)
        addFunction {
            this.name = InjektNameConventions.nameWithoutIllegalChars(
                "get\$${requestedType.render()}"
            )
            returnType = requestedType
            modality = Modality.ABSTRACT
        }.apply {
            dispatchReceiverParameter = thisReceiver?.copyTo(this)
        }
    }

    private fun entryPointForInject(
        name: Name,
        injectedType: IrType
    ) = buildClass {
        this.name = name
        kind = ClassKind.INTERFACE
    }.apply {
        createImplicitParameterDeclarationWithWrappedDescriptor()
        (this as IrClassImpl).metadata = MetadataSource.Class(descriptor)
        addFunction {
            this.name = InjektNameConventions.nameWithoutIllegalChars(
                "inject\$${injectedType.render()}"
            )
            returnType = irBuiltIns.function(1)
                .typeWith(injectedType, irBuiltIns.unitType)
                .withNoArgQualifiers(pluginContext, listOf(InjektFqNames.MembersInjector))

            modality = Modality.ABSTRACT
        }.apply {
            dispatchReceiverParameter = thisReceiver?.copyTo(this)
        }
    }
}
