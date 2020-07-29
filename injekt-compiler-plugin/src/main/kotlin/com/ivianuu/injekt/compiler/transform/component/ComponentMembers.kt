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
import com.ivianuu.injekt.compiler.readableName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrFunction

class ComponentMembers(
    private val component: ComponentImpl,
    private val pluginContext: IrPluginContext
) {

    lateinit var constructorBlockBuilder: IrBlockBodyBuilder

    private val getFunctions = mutableListOf<IrFunction>()
    private val getFunctionsNameProvider = NameProvider()

    private val membersNameProvider = NameProvider()

    fun cachedValue(
        key: Key,
        initializer: ComponentExpression
    ): ComponentExpression {
        val field = component.clazz.addField(
            fieldName = membersNameProvider.allocateForType(key.type).asString()
                .decapitalize(),
            fieldType = key.type,
            fieldVisibility = Visibilities.PRIVATE
        )

        with(constructorBlockBuilder) {
            val context = ComponentExpressionContext(component) {
                irGet(component.clazz.thisReceiver!!)
            }
            +irSetField(
                context[component],
                field,
                initializer(this, context)
            )
        }

        return { c -> irGetField(c[component], field) }
    }

    fun getFunction(
        key: Key,
        body: ComponentExpression
    ): IrFunction {
        val dependencyRequest = component.dependencyRequests
            .singleOrNull { it.second.key == key }

        if (dependencyRequest != null) {
            component.implementedRequests += dependencyRequest.second.key
        }

        return buildFun {
            this.name = dependencyRequest?.first?.name
                ?: key.type.readableName()
            returnType = key.type
        }.apply {
            dispatchReceiverParameter = component.clazz.thisReceiver!!.copyTo(this)
            this.parent = component.clazz
            component.clazz.addChild(this)
            this.body = DeclarationIrBuilder(pluginContext, symbol).run {
                irExprBody(
                    body(
                        this,
                        ComponentExpressionContext(component) { irGet(dispatchReceiverParameter!!) }
                    )
                )
            }
            getFunctions += this
        }
    }
}
