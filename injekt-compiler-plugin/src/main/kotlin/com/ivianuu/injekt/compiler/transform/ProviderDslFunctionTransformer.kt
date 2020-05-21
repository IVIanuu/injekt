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
import com.ivianuu.injekt.compiler.transform.factory.Key
import com.ivianuu.injekt.compiler.transform.factory.asKey
import com.ivianuu.injekt.compiler.typeArguments
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
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
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
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
                            InjektDeclarationIrBuilder(pluginContext, decoy.symbol).run {
                                if (transformed.valueParameters.any {
                                        it.name.asString().startsWith("dsl_provider\$")
                                    }) {
                                    decoy.annotations += noArgSingleConstructorCall(symbols.astProviderDsl)
                                }
                                decoy.body = builder.irExprBody(irInjektIntrinsicUnit())
                            }
                        }
                )
            }
        }
    }

    private val callStack = mutableListOf<IrCall>()

    override fun visitCall(expression: IrCall): IrExpression {
        callStack.push(expression)
        return super.visitCall(expression)
            .also { callStack.pop() }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        callStack.lastOrNull()?.let { call ->
            val valueParameter = call.getArgumentsWithIr()
                .singleOrNull {
                    it.second is IrFunctionExpression &&
                            (it.second as IrFunctionExpression).function == declaration
                }
                ?.first
            if (valueParameter != null && valueParameter.type.isFunction() &&
                valueParameter.type.typeArguments.first().classOrNull == symbols.providerDsl
            ) {
                rewriteProviderDefinitionCalls(declaration)
                return declaration
            }
        }
        return transformFunctionIfNeeded(super.visitFunction(declaration) as IrFunction)
    }

    private fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        if (function.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB ||
            function.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
        ) {
            return if (function.hasAnnotation(InjektFqNames.AstProviderDsl)) {
                pluginContext.referenceFunctions(function.descriptor.fqNameSafe)
                    .map { it.owner }
                    .single { other ->
                        other.name == function.name &&
                                other.valueParameters.any {
                                    it.name.asString().startsWith("dsl_provider\$")
                                }
                    }
            } else function
        }
        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function
        decoys[function]?.let { return it }
        if (function in transformingFunctions) return function
        transformingFunctions += function

        val originalGetCalls = mutableListOf<IrCall>()
        val originalProviderDslFunctionCalls = mutableListOf<IrCall>()

        function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            private val functionStack = mutableListOf(function)
            override fun visitFunction(declaration: IrFunction): IrStatement {
                functionStack.push(declaration)
                return super.visitFunction(declaration)
                    .also { functionStack.pop() }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                if (functionStack.lastOrNull() != function) return super.visitCall(expression)
                val callee = transformFunctionIfNeeded(expression.symbol.owner)
                when {
                    callee.name.asString() == "get" &&
                            callee.dispatchReceiverParameter?.type?.classOrNull == symbols.providerDsl -> {
                        originalGetCalls += expression
                    }
                    callee.symbol.owner.hasAnnotation(InjektFqNames.AstProviderDsl) -> {
                        originalProviderDslFunctionCalls += expression
                    }
                }
                return super.visitCall(expression)
            }
        })

        if (originalGetCalls.isEmpty() && originalProviderDslFunctionCalls.isEmpty()) {
            transformedFunctions[function] = function
            transformingFunctions -= function
            return function
        }

        val transformedFunction = function.deepCopyWithPreservingQualifiers()
        transformedFunction.dispatchReceiverParameter = null
        transformedFunction.extensionReceiverParameter = null

        transformedFunctions[function] = transformedFunction
        transformingFunctions -= function

        val getCalls = mutableListOf<IrCall>()
        val providerDslFunctionCalls = mutableListOf<IrCall>()

        transformedFunction.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            private val functionStack = mutableListOf(transformedFunction)
            override fun visitFunction(declaration: IrFunction): IrStatement {
                functionStack.push(declaration)
                return super.visitFunction(declaration)
                    .also { functionStack.pop() }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                if (functionStack.lastOrNull() != transformedFunction) return super.visitCall(
                    expression
                )
                val callee = transformFunctionIfNeeded(expression.symbol.owner)
                when {
                    callee.name.asString() == "get" &&
                            callee.dispatchReceiverParameter?.type?.classOrNull == symbols.providerDsl -> {
                        getCalls += expression
                    }
                    callee.symbol.owner.hasAnnotation(InjektFqNames.AstProviderDsl) -> {
                        providerDslFunctionCalls += expression
                    }
                }
                return super.visitCall(expression)
            }
        })

        transformedFunction.annotations +=
            InjektDeclarationIrBuilder(pluginContext, transformedFunction.symbol)
                .noArgSingleConstructorCall(symbols.astProviderDsl)

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

        getCalls
            .map {
                irBuiltIns.function(0)
                    .typeWith(it.getTypeArgument(0)!!)
                    .asKey()
            }
            .forEach { addProviderValueParameterIfNeeded(it) }

        providerDslFunctionCalls.forEach { providerDslFunctionCall ->
            val callee = transformFunctionIfNeeded(providerDslFunctionCall.symbol.owner)
            callee
                .valueParameters
                .filter { it.name.asString().startsWith("dsl_provider\$") }
                .map {
                    it.type.substitute(
                        callee.typeParameters,
                        providerDslFunctionCall.typeArguments
                    ).asKey()
                }
                .forEach { addProviderValueParameterIfNeeded(it) }
        }

        rewriteProviderDslFunctionCalls(
            transformedFunction,
            providerValueParameters,
            getCalls,
            providerDslFunctionCalls
        )

        return transformedFunction
    }

    private fun rewriteProviderDslFunctionCalls(
        function: IrFunction,
        providerValueParameters: Map<Key, IrValueParameter>,
        getCalls: List<IrCall>,
        providerDslFunctionCalls: List<IrCall>
    ) {
        function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                return when (expression) {
                    in getCalls -> {
                        val valueParameter = providerValueParameters
                            .getValue(
                                irBuiltIns.function(0)
                                    .typeWith(expression.getTypeArgument(0)!!)
                                    .asKey()
                            )
                        DeclarationIrBuilder(pluginContext, expression.symbol).run {
                            irCall(
                                valueParameter.type.classOrNull!!
                                    .functions.first { it.owner.name.asString() == "invoke" }
                            ).apply {
                                dispatchReceiver = irGet(valueParameter)
                            }
                        }
                    }
                    in providerDslFunctionCalls -> {
                        val transformedCallee = transformFunctionIfNeeded(expression.symbol.owner)

                        DeclarationIrBuilder(pluginContext, transformedCallee.symbol).irCall(
                            transformedCallee
                        )
                            .apply {
                                expression.typeArguments.forEachIndexed { index, it ->
                                    putTypeArgument(index, it)
                                }

                                transformedCallee.valueParameters.forEach { valueParameter ->
                                    var valueArgument = try {
                                        expression.getValueArgument(valueParameter.index)
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
                    else -> super.visitCall(expression)
                }
            }
        })
    }

    private fun rewriteProviderDefinitionCalls(
        function: IrFunction
    ) {
        function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val callee = transformFunctionIfNeeded(expression.symbol.owner)
                if (!callee.hasAnnotation(InjektFqNames.AstProviderDsl)) return super.visitCall(
                    expression
                )

                return DeclarationIrBuilder(pluginContext, callee.symbol).irCall(callee)
                    .apply {
                        expression.typeArguments.forEachIndexed { index, it ->
                            putTypeArgument(index, it)
                        }

                        callee.valueParameters.forEach { valueParameter ->
                            putValueArgument(
                                valueParameter.index,
                                try {
                                    expression.getValueArgument(valueParameter.index)
                                } catch (e: Throwable) {
                                    null
                                }
                            )
                        }
                    }
            }
        })
    }

}
