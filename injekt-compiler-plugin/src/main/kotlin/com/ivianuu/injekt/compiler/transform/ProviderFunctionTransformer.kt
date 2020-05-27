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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.deepCopyWithPreservingQualifiers
import com.ivianuu.injekt.compiler.dumpSrc
import com.ivianuu.injekt.compiler.hasTypeAnnotation
import com.ivianuu.injekt.compiler.irTrace
import com.ivianuu.injekt.compiler.transform.factory.Key
import com.ivianuu.injekt.compiler.transform.factory.asKey
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.withAnnotations
import com.ivianuu.injekt.compiler.withNoArgAnnotations
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ProviderFunctionTransformer(pluginContext: IrPluginContext) :
    AbstractFunctionTransformer(pluginContext, TransformOrder.TopDown) {

    override fun needsTransform(function: IrFunction): Boolean {
        if (!function.hasTypeAnnotation(
                InjektFqNames.Provider,
                pluginContext.bindingContext
            )
        ) return false
        if (function.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.get")
            return false
        return true
    }

    override fun transform(function: IrFunction): IrFunction {
        val originalProviderCalls = mutableListOf<IrCall>()

        function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val callee = transformFunctionIfNeeded(expression.symbol.owner)
                if (callee.hasTypeAnnotation(
                        InjektFqNames.Provider,
                        pluginContext.bindingContext
                    )
                ) {
                    originalProviderCalls += expression
                }

                return super.visitCall(expression)
            }
        })

        if (originalProviderCalls.isEmpty()) return function

        val transformedFunction = function.deepCopyWithPreservingQualifiers(
            wrapDescriptor = true
        )

        val oldParameters = transformedFunction.valueParameters.toList()
        transformedFunction.valueParameters = oldParameters
            .map {
                it.copyTo(
                    transformedFunction,
                    type = it.type.withNoArgAnnotations(
                        pluginContext,
                        listOf(InjektFqNames.AstAssisted)
                    )
                )
            }

        val parametersMap = oldParameters
            .map { it.symbol }
            .zip(transformedFunction.valueParameters)
            .toMap()

        transformedFunction.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                return parametersMap[expression.symbol]?.let {
                    DeclarationIrBuilder(pluginContext, it.symbol)
                        .irGet(it)
                } ?: super.visitGetValue(expression)
            }
        })

        val providerCalls = mutableListOf<IrCall>()

        transformedFunction.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val callee = transformFunctionIfNeeded(expression.symbol.owner)
                if (callee.hasTypeAnnotation(
                        InjektFqNames.Provider,
                        pluginContext.bindingContext
                    )
                ) {
                    providerCalls += expression
                }

                return super.visitCall(expression)
            }
        })

        transformedFunction.addMetadataIfNotLocal()

        // todo add a provider per request not by key

        val providerValueParameters = mutableMapOf<Key, IrValueParameter>()
        fun addProviderValueParameterIfNeeded(providerKey: Key) {
            if (providerKey !in providerValueParameters) {
                providerValueParameters[providerKey] =
                    transformedFunction.addValueParameter(
                        "dsl_provider\$${providerValueParameters.size}",
                        providerKey.type
                    )
            }
        }

        providerCalls.forEach { providerCall ->
            val callee = transformFunctionIfNeeded(providerCall.symbol.owner)
            if (callee.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.get") {
                val exprQualifiers =
                    pluginContext.irTrace[InjektWritableSlices.QUALIFIERS, providerCall]
                        ?: emptyList()
                addProviderValueParameterIfNeeded(
                    irBuiltIns.function(0)
                        .typeWith(
                            providerCall.getTypeArgument(0)!!
                                .withAnnotations(exprQualifiers)
                        )
                        .withNoArgAnnotations(pluginContext, listOf(InjektFqNames.Provider))
                        .asKey()
                )
            } else {
                callee
                    .valueParameters
                    .filter { it.name.asString().startsWith("dsl_provider\$") }
                    .map {
                        it.type.substitute(
                            callee.typeParameters,
                            providerCall.typeArguments
                        ).asKey()
                    }
                    .forEach { addProviderValueParameterIfNeeded(it) }
            }
        }

        transformFunctionBody(
            transformedFunction,
            providerValueParameters,
            providerCalls
        )

        return transformedFunction
    }

    override fun transformExternal(function: IrFunction): IrFunction {
        return pluginContext.referenceFunctions(function.descriptor.fqNameSafe)
            .map { it.owner }
            .singleOrNull { other ->
                other.name == function.name &&
                        other.valueParameters.any {
                            it.name.asString().startsWith("dsl_provider\$")
                        }
            } ?: function
    }

    override fun createDecoy(original: IrFunction, transformed: IrFunction): IrFunction {
        return original.deepCopyWithPreservingQualifiers(wrapDescriptor = false)
            .also { decoy ->
                decoys[original] = decoy
                InjektDeclarationIrBuilder(
                    pluginContext,
                    decoy.symbol
                ).run {
                    decoy.body = builder.irExprBody(irInjektIntrinsicUnit())
                }
            }
    }

    private fun transformFunctionBody(
        function: IrFunction,
        providerValueParameters: Map<Key, IrValueParameter>,
        providerFunctionCalls: List<IrCall>
    ) {
        function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)
                return if (expression !in providerFunctionCalls) expression
                else {
                    transformProviderCall(
                        function, expression,
                        transformFunctionIfNeeded(expression.symbol.owner), providerValueParameters
                    )
                }
            }
        })
    }

    private fun transformProviderCall(
        caller: IrFunction,
        originalCall: IrCall,
        transformedCallee: IrFunction,
        providerValueParameters: Map<Key, IrValueParameter>
    ): IrExpression {
        if (transformedCallee.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.get") {
            val exprQualifiers =
                pluginContext.irTrace[InjektWritableSlices.QUALIFIERS, originalCall] ?: emptyList()
            val typeArgument = originalCall.getTypeArgument(0)!!
            val valueParameter = providerValueParameters
                .get(
                    irBuiltIns.function(0)
                        .typeWith(
                            typeArgument
                                .withAnnotations(exprQualifiers)
                        )
                        .withNoArgAnnotations(pluginContext, listOf(InjektFqNames.Provider))
                        .asKey()
                ) ?: error(
                "Couldn't find param for ${originalCall.render()} " +
                        "in ${caller.dumpSrc()}"
            )

            return DeclarationIrBuilder(pluginContext, originalCall.symbol).run {
                if (typeArgument.isFunction() &&
                    typeArgument.hasAnnotation(InjektFqNames.Provider)
                ) {
                    irGet(valueParameter)
                } else {
                    irCall(
                        valueParameter.type.classOrNull!!
                            .functions.first { it.owner.name.asString() == "invoke" }
                    ).apply {
                        dispatchReceiver = irGet(valueParameter)
                    }
                }
            }
        }

        return DeclarationIrBuilder(pluginContext, transformedCallee.symbol).irCall(
            transformedCallee
        )
            .apply {
                originalCall.typeArguments.forEachIndexed { index, it ->
                    putTypeArgument(index, it)
                }

                dispatchReceiver = originalCall.dispatchReceiver
                extensionReceiver = originalCall.extensionReceiver

                transformedCallee.valueParameters.forEach { valueParameter ->
                    var valueArgument = try {
                        originalCall.getValueArgument(valueParameter.index)
                    } catch (e: Throwable) {
                        null
                    }

                    if (valueArgument == null) {
                        valueArgument = when {
                            valueParameter.name.asString()
                                .startsWith("dsl_provider\$") -> {
                                val substitutedType = valueParameter.type
                                    .substitute(
                                        transformedCallee.typeParameters
                                            .map { it.symbol }
                                            .zip(typeArguments)
                                            .toMap()
                                    )

                                DeclarationIrBuilder(pluginContext, symbol)
                                    .irGet(
                                        providerValueParameters
                                            .get(substitutedType.asKey()) ?: error(
                                            "Couldn't find ${substitutedType.asKey()} in ${providerValueParameters.map {
                                                it.key to it.value.render()
                                            }}"
                                        )
                                    )
                            }
                            else -> null
                        }
                    }

                    putValueArgument(valueParameter.index, valueArgument)
                }
            }
    }

}
