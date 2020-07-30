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

import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.functions

class ComponentFactoryImpl(
    val irParent: IrDeclarationParent,
    val factory: IrClass,
    val entryPoints: List<IrClass>,
    val parent: ComponentFactoryImpl?,
    val pluginContext: IrPluginContext,
    val declarationGraph: DeclarationGraph,
    val symbols: InjektSymbols
) {

    val component = factory.functions
        .filterNot { it.isFakeOverride }
        .single()
        .returnType
        .classOrNull!!
        .owner

    val clazz: IrClass = buildClass {
        name = "${factory.name.asString()}Impl".asNameId()
        if (parent != null) visibility = Visibilities.PRIVATE else Visibilities.INTERNAL
        if (parent == null) kind = ClassKind.OBJECT
    }.apply clazz@{
        parent = irParent
        createImplicitParameterDeclarationWithWrappedDescriptor()
        superTypes += factory.defaultType

        val parentField = if (this@ComponentFactoryImpl.parent != null) {
            addField(
                "parent",
                this@ComponentFactoryImpl.parent.componentImpl.clazz.defaultType
            )
        } else {
            null
        }

        addConstructor {
            returnType = defaultType
            isPrimary = true
            visibility = Visibilities.PUBLIC
        }.apply {
            val parentValueParameter = if (parentField != null) {
                addValueParameter(parentField.name.asString(), parentField.type)
            } else {
                null
            }

            body = DeclarationIrBuilder(
                pluginContext,
                symbol
            ).irBlockBody {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.constructors.single().owner)
                +IrInstanceInitializerCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    this@clazz.symbol,
                    context.irBuiltIns.unitType
                )
                if (parentValueParameter != null) {
                    +irSetField(
                        irGet(thisReceiver!!),
                        parentField!!,
                        irGet(parentValueParameter)
                    )
                }
            }
        }
    }

    val componentImpl = ComponentImpl(this)

    fun init() {
        val superFactoryFunction = factory.functions
            .single { !it.isFakeOverride }

        clazz.addFunction {
            name = superFactoryFunction.name
            returnType = superFactoryFunction.returnType
        }.apply {
            dispatchReceiverParameter = clazz.thisReceiver!!.copyTo(this)

            valueParameters = superFactoryFunction.valueParameters.map { valueParameter ->
                valueParameter.copyTo(
                    this,
                    type = valueParameter.type
                )
            }

            componentImpl.init()
            clazz.addChild(componentImpl.clazz)

            body = DeclarationIrBuilder(
                pluginContext,
                symbol
            ).run {
                irExprBody(
                    irCall(
                        componentImpl.clazz.constructors.single()
                    ).apply {
                        if (this@ComponentFactoryImpl.parent != null) {
                            putValueArgument(
                                0,
                                irGetField(
                                    irGet(dispatchReceiverParameter!!),
                                    this@ComponentFactoryImpl.clazz.fields.single {
                                        it.name.asString() == "parent"
                                    }
                                )
                            )
                        }

                        valueParameters.forEach { valueParameter ->
                            putValueArgument(
                                valueParameter.index + if (this@ComponentFactoryImpl.parent != null) 1 else 0,
                                irGet(valueParameter)
                            )
                        }
                    }
                )
            }
        }
    }
}
