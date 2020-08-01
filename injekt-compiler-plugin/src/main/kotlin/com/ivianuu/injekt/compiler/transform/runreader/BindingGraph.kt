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

package com.ivianuu.injekt.compiler.transform.runreader

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.canUseImplicits
import com.ivianuu.injekt.compiler.getClassFromAnnotation
import com.ivianuu.injekt.compiler.getContext
import com.ivianuu.injekt.compiler.getContextValueParameter
import com.ivianuu.injekt.compiler.irLambda
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.implicit.ImplicitContextTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class BindingGraph(
    pluginContext: IrPluginContext,
    declarationGraph: DeclarationGraph,
    val symbols: InjektSymbols,
    inputs: List<IrField>,
    private val implicitTransformer: ImplicitContextTransformer
) {

    private val allBindings = buildList<BindingNode> {
        this += declarationGraph.bindings
            .map { function ->
                val scoping = function.getClassFromAnnotation(
                    InjektFqNames.Given, 0, pluginContext
                )
                    ?: if (function is IrConstructor) function.constructedClass.getClassFromAnnotation(
                        InjektFqNames.Given, 0, pluginContext
                    ) else null

                val scopingFunction = scoping
                    ?.functions
                    ?.single { it.canUseImplicits(pluginContext) }
                    ?.let { implicitTransformer.getTransformedFunction(it) }

                val explicitParameters = function.valueParameters
                    .filter { it != function.getContextValueParameter() }

                val key = if (explicitParameters.isEmpty()) function.returnType.asKey()
                else pluginContext.tmpFunction(explicitParameters.size)
                    .typeWith(explicitParameters.map { it.type } + function.returnType)
                    .asKey()

                GivenBindingNode(
                    key = key,
                    contexts = listOfNotNull(
                        function.getContext()!!,
                        scopingFunction?.getContext()
                    ),
                    createExpression = { parametersMap, context ->
                        val call = if (function is IrConstructor) {
                            IrConstructorCallImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                function.returnType,
                                function.symbol,
                                function.constructedClass.typeParameters.size,
                                function.typeParameters.size,
                                function.valueParameters.size
                            )
                        } else {
                            IrCallImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                function.returnType,
                                function.symbol,
                                function.typeParameters.size,
                                function.valueParameters.size
                            )
                        }
                        call.apply {
                            if (function.dispatchReceiverParameter != null) {
                                dispatchReceiver = irGetObject(
                                    function.dispatchReceiverParameter!!.type.classOrNull!!
                                )
                            }

                            parametersMap.values.forEachIndexed { index, expression ->
                                putValueArgument(
                                    index,
                                    expression()
                                )
                            }

                            putValueArgument(valueArgumentsCount - 1, context())
                        }

                        if (scopingFunction != null) {
                            irCall(scopingFunction).apply {
                                dispatchReceiver = irGetObject(scoping.symbol)
                                putValueArgument(
                                    0,
                                    irInt(key.hashCode())
                                )
                                putValueArgument(
                                    1,
                                    irLambda(
                                        pluginContext.tmpFunction(0)
                                            .typeWith(key.type)
                                    ) { call }
                                )
                                putValueArgument(
                                    2,
                                    context()
                                )
                            }
                        } else {
                            call
                        }
                    },
                    explicitParameters = explicitParameters,
                    origin = function.descriptor.fqNameSafe,
                    function = function
                )
            }

        this += declarationGraph.mapEntries
            .groupBy { it.returnType.asKey() }
            .map { (key, entries) ->
                MapBindingNode(
                    key,
                    entries.map { it.getContext()!! },
                    entries
                )
            }

        this += declarationGraph.setElements
            .groupBy { it.returnType.asKey() }
            .map { (key, elements) ->
                SetBindingNode(
                    key,
                    elements.map { it.getContext()!! },
                    elements
                )
            }

        this += inputs.map { InputBindingNode(it) }
    }

    val resolvedBindings = mutableMapOf<Key, BindingNode>()

    fun getBinding(request: BindingRequest): BindingNode {
        var binding = resolvedBindings[request.key]
        if (binding != null) return binding

        val bindings = allBindings
            .filter { it.key == request.key }

        if (bindings.size > 1) {
            error(
                "Multiple bindings found for '${request.key}' at:\n${
                bindings
                    .joinToString("\n") { "'${it.origin.orUnknown()}'" }
                }"
            )
        }

        binding = bindings.singleOrNull()

        binding?.let {
            resolvedBindings[request.key] = it
            return it
        }

        if (request.key.type.isMarkedNullable()) {
            binding = NullBindingNode(request.key)
            resolvedBindings[request.key] = binding
            return binding
        }

        error(
            "No binding found for '${request.key}'\n" +
                    "required at '${request.requestingKey}' '${request.requestOrigin.orUnknown()}'\n"
        )
    }

}
