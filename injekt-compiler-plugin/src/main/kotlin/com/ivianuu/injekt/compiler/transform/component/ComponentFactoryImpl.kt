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
import com.ivianuu.injekt.compiler.buildClass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.Name

class ComponentFactoryImpl(
    val irParent: IrDeclarationParent,
    val node: ComponentNode,
    val parent: ComponentFactoryImpl?,
    val pluginContext: IrPluginContext,
    val declarationGraph: DeclarationGraph,
    val symbols: InjektSymbols
) {

    val factoryClass = buildClass {
        name = Name.special("<factory impl>")
        visibility = Visibilities.LOCAL
    }.apply clazz@{
        parent = irParent
        createImplicitParameterDeclarationWithWrappedDescriptor()
        superTypes += node.factory.factory.defaultType

        addConstructor {
            returnType = defaultType
            isPrimary = true
            visibility = Visibilities.PUBLIC
        }.apply {
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
            }
        }
    }

    val componentImpl = ComponentImpl(this)

    fun getClass(): IrClass {
        val superFactoryFunction = node.factory.factory.functions
            .single { !it.isFakeOverride }

        factoryClass.addFunction {
            name = superFactoryFunction.name
            returnType = superFactoryFunction.returnType
        }.apply {
            dispatchReceiverParameter = factoryClass.thisReceiver!!.copyTo(this)

            valueParameters = superFactoryFunction.valueParameters.map { valueParameter ->
                valueParameter.copyTo(this)
            }

            body = DeclarationIrBuilder(
                pluginContext,
                symbol
            ).irExprBody(
                componentImpl.getImplExpression(valueParameters)
            )
        }

        return factoryClass
    }
}
