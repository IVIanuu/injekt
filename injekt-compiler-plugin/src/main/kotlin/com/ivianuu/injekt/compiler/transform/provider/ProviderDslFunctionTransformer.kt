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

package com.ivianuu.injekt.compiler.transform.provider

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.dumpSrc
import com.ivianuu.injekt.compiler.hasTypeAnnotation
import com.ivianuu.injekt.compiler.irTrace
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.deepCopyWithPreservingQualifiers
import com.ivianuu.injekt.compiler.transform.factory.Key
import com.ivianuu.injekt.compiler.transform.factory.asKey
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.withAnnotations
import com.ivianuu.injekt.compiler.withNoArgQualifiers
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
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

class ProviderDslFunctionTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()
    private val decoys = mutableMapOf<IrFunction, IrFunction>()
    private val transformingFunctions = mutableSetOf<IrFunction>()

    override fun visitFile(declaration: IrFile): IrFile {
        val originalFunctions = declaration.declarations.filterIsInstance<IrFunction>()
        val result = super.visitFile(declaration)
        result.patchWithDecoys(originalFunctions)
        return result
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        val originalFunctions = declaration.declarations.filterIsInstance<IrFunction>()
        val result = super.visitClass(declaration) as IrClass
        result.patchWithDecoys(originalFunctions)
        return result
    }

    private fun IrDeclarationContainer.patchWithDecoys(originalFunctions: List<IrFunction>) {
        for (original in originalFunctions) {
            val transformed = transformedFunctions[original]
            if (transformed != null && transformed != original) {
                declarations.add(
                    original.deepCopyWithPreservingQualifiers()
                        .also { decoy ->
                            decoys[original] = decoy
                            InjektDeclarationIrBuilder(
                                pluginContext,
                                decoy.symbol
                            ).run {
                                decoy.body = builder.irExprBody(irInjektIntrinsicUnit())
                            }
                        }
                )
            }
        }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement =
        super.visitFunction(transformFunctionIfNeeded(declaration)) as IrFunction

    private fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        if (function.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.get" &&
            function.hasTypeAnnotation(InjektFqNames.ProviderDsl, pluginContext.bindingContext)
        ) return function

        if (function.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB ||
            function.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
        ) {
            return if (function.hasTypeAnnotation(
                    InjektFqNames.ProviderDsl,
                    pluginContext.bindingContext
                )
            ) {
                pluginContext.referenceFunctions(function.descriptor.fqNameSafe)
                    .map { it.owner }
                    .singleOrNull { other ->
                        other.name == function.name &&
                                other.valueParameters.any {
                                    it.name.asString().startsWith("dsl_provider\$")
                                }
                    } ?: function
            } else function
        }
        if (!function.hasTypeAnnotation(
                InjektFqNames.ProviderDsl,
                pluginContext.bindingContext
            )
        ) return function
        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function
        decoys[function]?.let { return it }
        if (function in transformingFunctions) return function
        transformingFunctions += function

        val transformedFunction = function.deepCopyWithPreservingQualifiers()

        val providerDslCalls = mutableListOf<IrCall>()

        transformedFunction.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val callee = transformFunctionIfNeeded(expression.symbol.owner)
                if (callee.hasTypeAnnotation(
                        InjektFqNames.ProviderDsl,
                        pluginContext.bindingContext
                    )
                ) {
                    providerDslCalls += expression
                }

                return super.visitCall(expression)
            }
        })

        if (providerDslCalls.isEmpty()) {
            transformedFunctions[function] = function
            transformingFunctions -= function
            return function
        }

        transformedFunctions[function] = transformedFunction
        transformingFunctions -= function

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

        providerDslCalls.forEach { providerDslCall ->
            val callee = transformFunctionIfNeeded(providerDslCall.symbol.owner)
            if (callee.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.get") {
                val exprQualifiers =
                    pluginContext.irTrace[InjektWritableSlices.QUALIFIERS, providerDslCall]
                        ?: emptyList()
                addProviderValueParameterIfNeeded(
                    irBuiltIns.function(0)
                        .typeWith(
                            providerDslCall.getTypeArgument(0)!!
                                .withAnnotations(exprQualifiers)
                        )
                        .withNoArgQualifiers(pluginContext, listOf(InjektFqNames.Provider))
                        .asKey()
                )
            } else {
                callee
                    .valueParameters
                    .filter { it.name.asString().startsWith("dsl_provider\$") }
                    .map {
                        it.type.substitute(
                            callee.typeParameters,
                            providerDslCall.typeArguments
                        ).asKey()
                    }
                    .forEach { addProviderValueParameterIfNeeded(it) }
            }
        }

        transformFunctionBody(
            transformedFunction,
            providerValueParameters,
            providerDslCalls
        )

        return transformedFunction
    }

    private fun transformFunctionBody(
        function: IrFunction,
        providerValueParameters: Map<Key, IrValueParameter>,
        providerDslFunctionCalls: List<IrCall>
    ) {
        function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)
                return if (expression !in providerDslFunctionCalls) expression
                else {
                    transformProviderDslCall(
                        function, expression,
                        transformFunctionIfNeeded(expression.symbol.owner), providerValueParameters
                    )
                }
            }
        })
    }

    private fun transformProviderDslCall(
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
                        .withNoArgQualifiers(pluginContext, listOf(InjektFqNames.Provider))
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
