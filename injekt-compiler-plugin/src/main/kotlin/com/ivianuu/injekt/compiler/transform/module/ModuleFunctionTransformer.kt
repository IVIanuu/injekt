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

package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasTypeAnnotation
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.deepCopyWithPreservingQualifiers
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ModuleFunctionTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()

    fun getTransformedModule(function: IrFunction): IrFunction {
        return transformedFunctions[function] ?: function
    }

    override fun visitFunction(declaration: IrFunction): IrStatement =
        transformFunctionIfNeeded(super.visitFunction(declaration) as IrFunction)

    private fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        if (function.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB ||
            function.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
        ) {
            return if (function.hasAnnotation(InjektFqNames.AstTyped)) {
                pluginContext.referenceFunctions(function.descriptor.fqNameSafe)
                    .map { it.owner }
                    .single { other ->
                        other.name == function.name &&
                                other.valueParameters.any {
                                    "class\$" in it.name.asString()
                                }
                    }
            } else function
        }
        if (!function.hasTypeAnnotation(
                InjektFqNames.Module,
                pluginContext.bindingContext
            )
        ) return function
        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function

        val originalCaptures = mutableListOf<IrGetValue>()
        function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            private val moduleStack = mutableListOf(function)
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.hasTypeAnnotation(
                        InjektFqNames.Module,
                        pluginContext.bindingContext
                    )
                )
                    moduleStack.push(declaration)
                return super.visitFunction(declaration)
                    .also {
                        if (declaration.hasTypeAnnotation(
                                InjektFqNames.Module,
                                pluginContext.bindingContext
                            )
                        )
                            moduleStack.pop()
                    }
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                if (function.isLocal &&
                    moduleStack.last() == function &&
                    expression.symbol.owner !in function.valueParameters
                ) {
                    originalCaptures += expression
                }
                return super.visitGetValue(expression)
            }
        })

        if (originalCaptures.isEmpty()) {
            transformedFunctions[function] = function
            return function
        }

        val transformedFunction = function.deepCopyWithPreservingQualifiers()
        transformedFunctions[function] = transformedFunction

        val captures = mutableListOf<IrGetValue>()

        transformedFunction.body?.transformChildrenVoid(object :
            IrElementTransformerVoid() {
            private val moduleStack = mutableListOf(transformedFunction)
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.hasTypeAnnotation(
                        InjektFqNames.Module,
                        pluginContext.bindingContext
                    )
                )
                    moduleStack.push(declaration)
                return super.visitFunction(declaration)
                    .also {
                        if (declaration.hasTypeAnnotation(
                                InjektFqNames.Module,
                                pluginContext.bindingContext
                            )
                        )
                            moduleStack.pop()
                    }
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                if (transformedFunction.isLocal &&
                    moduleStack.last() == transformedFunction &&
                    expression.symbol.owner !in transformedFunction.valueParameters
                ) {
                    captures += expression
                }
                return super.visitGetValue(expression)
            }
        })

        val valueParameterByCapture = captures.associateWith { capture ->
            transformedFunction.addValueParameter(
                "capture_${captures.indexOf(capture)}",
                capture.type
            )
        }

        transformedFunction.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                return valueParameterByCapture[expression]?.let {
                    DeclarationIrBuilder(pluginContext, expression.symbol)
                        .irGet(it)
                } ?: super.visitGetValue(expression)
            }
        })

        function.file.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol != function.symbol) {
                    return super.visitCall(expression)
                }
                return DeclarationIrBuilder(pluginContext, expression.symbol).run {
                    irCall(transformedFunction).apply {
                        copyTypeAndValueArgumentsFrom(expression)
                        captures.forEach { capture ->
                            val valueParameter = valueParameterByCapture.getValue(capture)
                            putValueArgument(valueParameter.index, capture)
                        }
                    }
                }
            }
        })

        return transformedFunction
    }

}
