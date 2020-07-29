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

import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.flatMapFix
import com.ivianuu.injekt.compiler.getAllClasses
import com.ivianuu.injekt.compiler.getContext
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ComponentImpl(
    val factoryImpl: ComponentFactoryImpl
) {
    val origin: FqName? = factoryImpl.component.descriptor.fqNameSafe

    val clazz = buildClass {
        name = Name.identifier("ComponentImpl")
        visibility = Visibilities.PRIVATE
    }.apply clazz@{
        parent = factoryImpl.clazz
        createImplicitParameterDeclarationWithWrappedDescriptor()
        superTypes += factoryImpl.component.defaultType
    }

    val dependencyRequests = mutableListOf<Pair<IrFunction, BindingRequest>>()
    val implementedRequests = mutableListOf<Key>()

    private val componentMembers = ComponentMembers(this, factoryImpl.pluginContext)

    private lateinit var graph: ComponentGraph
    private lateinit var componentExpressions: ComponentExpressions

    fun init() {
        val parentField = if (factoryImpl.parent != null) {
            clazz.addField(
                "parent",
                factoryImpl.parent.componentImpl.clazz.defaultType
            )
        } else {
            null
        }

        val inputs = factoryImpl.clazz.functions.single { it.name.asString() == "create" }
            .valueParameters

        val inputFields = inputs.map {
            clazz.addField(
                it.name.asString(),
                it.type
            )
        }

        graph = ComponentGraph(
            parent = factoryImpl.parent?.componentImpl?.graph,
            component = this@ComponentImpl,
            context = factoryImpl.pluginContext,
            declarationGraph = factoryImpl.declarationGraph,
            symbols = factoryImpl.symbols,
            inputs = inputFields
        )

        componentExpressions = ComponentExpressions(
            graph = graph,
            pluginContext = factoryImpl.pluginContext,
            members = componentMembers,
            parent = factoryImpl.parent?.componentImpl?.componentExpressions,
            component = this@ComponentImpl
        )

        clazz.addConstructor {
            returnType = clazz.defaultType
            isPrimary = true
            visibility = Visibilities.PUBLIC
        }.apply {
            val parentValueParameter = if (parentField != null) {
                addValueParameter(parentField.name.asString(), parentField.type)
            } else {
                null
            }

            val inputValueParametersByField = inputFields.associateWith {
                addValueParameter(
                    it.name.asString(),
                    it.type
                )
            }

            body = DeclarationIrBuilder(factoryImpl.pluginContext, symbol).irBlockBody {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.constructors.single().owner)
                +IrInstanceInitializerCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    clazz.symbol,
                    context.irBuiltIns.unitType
                )

                if (parentValueParameter != null) {
                    +irSetField(
                        irGet(clazz.thisReceiver!!),
                        parentField!!,
                        irGet(parentValueParameter)
                    )
                }

                inputValueParametersByField.forEach { (field, valueParameter) ->
                    +irSetField(
                        irGet(clazz.thisReceiver!!),
                        field,
                        irGet(valueParameter)
                    )
                }

                componentMembers.constructorBlockBuilder = this
            }
        }

        implementRequests()
    }

    private fun implementRequests() {
        val processedSuperTypes = mutableSetOf<IrType>()
        val declarationNames = mutableSetOf<Name>()
        var firstRound = true

        while (true) {
            val entryPoints =
                (if (firstRound) factoryImpl.entryPoints
                else graph.resolvedBindings.values
                    .flatMapFix { it.contexts.map { it.getContext()!! } })
                    .flatMapFix { it.getAllClasses() }
                    .flatMapFix {
                        factoryImpl.implicitTransformer
                            .getFunctionForContext(it)
                            ?.let { factoryImpl.declarationGraph.getAllContextsForFunction(it) }
                            ?: listOf(it)
                    }
                    .distinct()
                    .filter { it.defaultType !in processedSuperTypes }

            if (entryPoints.isEmpty()) break

            fun collect(superClass: IrClass) {
                if (superClass.defaultType in processedSuperTypes) return
                processedSuperTypes += superClass.defaultType

                for (declaration in superClass.declarations.toList()) {
                    if (declaration !is IrFunction) continue
                    if (declaration is IrConstructor) continue
                    if (declaration.name in declarationNames) continue
                    if (declaration.dispatchReceiverParameter?.type ==
                        factoryImpl.pluginContext.irBuiltIns.anyType
                    ) break
                    declarationNames += declaration.name
                    val request = BindingRequest(
                        declaration.returnType.asKey(),
                        null,
                        declaration.descriptor.fqNameSafe
                    )
                    dependencyRequests += declaration to request
                }

                superClass.superTypes
                    .map { it.classOrNull!!.owner }
                    .forEach { collect(it) }
            }

            entryPoints.forEach { entryPoint ->
                clazz.superTypes += entryPoint.defaultType
                collect(entryPoint)
            }

            dependencyRequests.forEach { (_, request) ->
                if (request.key !in implementedRequests) {
                    componentExpressions.getBindingExpression(request)
                }
            }

            firstRound = false
        }
    }

}
