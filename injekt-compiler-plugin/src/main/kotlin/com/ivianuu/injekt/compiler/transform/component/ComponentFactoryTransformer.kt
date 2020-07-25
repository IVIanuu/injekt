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

package com.ivianuu.injekt.compiler.transform.component

import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ComponentFactoryTransformer(
    pluginContext: IrPluginContext
) : AbstractInjektTransformer(pluginContext) {

    override fun lower() {
        val nameProvider = NameProvider()

        val componentFactories = mutableListOf<IrClass>()

        module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                val result = super.visitCall(expression) as IrCall
                if (expression.symbol.descriptor.fqNameSafe.asString() !=
                    "com.ivianuu.injekt.rootComponent" &&
                    expression.symbol.descriptor.fqNameSafe.asString() !=
                    "com.ivianuu.injekt.childComponent"
                ) return result

                val isChild = result.symbol.descriptor.fqNameSafe.asString() ==
                        "com.ivianuu.injekt.childComponent"

                val componentType = result.getTypeArgument(0)!!

                val componentInputs = (result.getValueArgument(0) as? IrVarargImpl)
                    ?.elements
                    ?.map { it as IrExpression } ?: emptyList()

                val componentFactory = buildClass {
                    // todo naming
                    name = nameProvider.allocateForGroup(
                        "${currentScope!!.scope.scopeOwner.name.asString()}\$Factory".asNameId()
                    )
                    kind = ClassKind.INTERFACE
                }.apply {
                    parent = currentFile
                    createImplicitParameterDeclarationWithWrappedDescriptor()
                    addMetadataIfNotLocal()

                    annotations += DeclarationIrBuilder(pluginContext, symbol)
                        .irCall(
                            (if (isChild) symbols.childComponentFactory else symbols.rootComponentFactory)
                                .constructors.single()
                        )

                    addFunction {
                        this.name = "create".asNameId()
                        returnType = componentType
                        modality = Modality.ABSTRACT
                    }.apply {
                        dispatchReceiverParameter = thisReceiver?.copyTo(this)
                        addMetadataIfNotLocal()

                        componentInputs.forEachIndexed { index, inputExpression ->
                            addValueParameter(
                                "p$index",
                                inputExpression.type
                            )
                        }
                    }

                    componentFactories += this
                }

                return DeclarationIrBuilder(pluginContext, componentFactory.symbol).run {
                    val componentFactoryExpression = if (isChild) {
                        irCall(
                            pluginContext.referenceFunctions(FqName("com.ivianuu.injekt.given"))
                                .single(),
                            componentFactory.defaultType
                        ).apply {
                            putTypeArgument(0, componentFactory.defaultType)
                        }
                    } else {
                        irCall(
                            symbols.rootComponentFactories
                                .functions
                                .single { it.owner.name.asString() == "get" }
                        ).apply {
                            dispatchReceiver = irGetObject(symbols.rootComponentFactories)
                            putValueArgument(
                                0,
                                IrClassReferenceImpl(
                                    startOffset, endOffset,
                                    context.irBuiltIns.kClassClass.typeWith(componentFactory.defaultType),
                                    componentFactory.symbol,
                                    componentFactory.defaultType
                                )
                            )
                        }
                    }

                    irCall(componentFactory.functions.single { it.name.asString() == "create" }).apply {
                        dispatchReceiver = componentFactoryExpression
                        componentInputs.forEachIndexed { index, input ->
                            putValueArgument(index, input)
                        }
                    }
                }
            }
        })

        componentFactories.forEach {
            (it.parent as IrFile).addChild(it)
        }
    }

}
