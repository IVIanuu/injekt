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
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.isTypeParameter
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.deepCopyWithPreservingQualifiers
import com.ivianuu.injekt.compiler.typeArguments
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
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
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
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
    private val decoys = mutableSetOf<IrFunction>()

    fun getTransformedModule(function: IrFunction): IrFunction {
        return transformedFunctions[function] ?: function
    }

    fun isDecoy(function: IrFunction): Boolean = function in decoys

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

    override fun visitFunction(declaration: IrFunction): IrStatement =
        transformFunctionIfNeeded(super.visitFunction(declaration) as IrFunction)

    private fun IrDeclarationContainer.patchWithDecoys(originalFunctions: List<IrFunction>) {
        for (original in originalFunctions) {
            val transformed = transformedFunctions[original]
            if (transformed != null && transformed != original) {
                declarations.add(
                    original.deepCopyWithPreservingQualifiers()
                        .also { decoy ->
                            decoys += decoy
                            InjektDeclarationIrBuilder(pluginContext, decoy.symbol).run {
                                if (transformed.valueParameters
                                        .any { it.name.asString().startsWith("class\$") }
                                ) {
                                    decoy.annotations += noArgSingleConstructorCall(symbols.astTyped)
                                }

                                decoy.body = builder.irExprBody(irInjektIntrinsicUnit())
                            }
                        }
                )
            }
        }
    }

    private fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        if (function.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB ||
            function.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
        ) {
            return if (function.hasAnnotation(InjektFqNames.AstTyped)) {
                pluginContext.referenceFunctions(function.descriptor.fqNameSafe)
                    .map { it.owner }
                    .single { other ->
                        other.name == function.name &&
                                other.returnType == function.returnType &&
                                other.valueParameters.any {
                                    "class\$" in it.name.asString()
                                }
                    }
            } else function
        }
        if (!function.isModule(pluginContext.bindingContext)) return function
        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function
        if (function in decoys) return function

        val originalCaptures = mutableListOf<IrGetValue>()
        val originalClassOfCalls = mutableListOf<IrCall>()
        val originalTypedModuleCalls = mutableListOf<IrCall>()
        var hasUnresolvedClassOfCalls = false

        function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            private val moduleStack = mutableListOf(function)
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.isModule(pluginContext.bindingContext))
                    moduleStack.push(declaration)
                return super.visitFunction(declaration)
                    .also {
                        if (declaration.isModule(pluginContext.bindingContext))
                            moduleStack.pop()
                    }
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                if (function.isLocal &&
                    moduleStack.last() == function &&
                    expression.symbol.owner !in function.valueParameters &&
                    expression.type.classOrNull != symbols.providerDsl
                ) {
                    originalCaptures += expression
                }
                return super.visitGetValue(expression)
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val callee = transformFunctionIfNeeded(expression.symbol.owner)
                if (callee.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.classOf") {
                    originalClassOfCalls += expression
                    if (expression.getTypeArgument(0)!!.isTypeParameter()) {
                        hasUnresolvedClassOfCalls = true
                    }
                } else if (callee.hasAnnotation(InjektFqNames.AstTyped)) {
                    originalTypedModuleCalls += expression
                    if (expression.typeArguments.any { it.isTypeParameter() }) {
                        hasUnresolvedClassOfCalls = true
                    }
                }
                return super.visitCall(expression)
            }
        })

        if (!hasUnresolvedClassOfCalls && originalCaptures.isEmpty()) {
            transformedFunctions[function] = function
            rewriteTypedFunctionCalls(
                function,
                originalClassOfCalls,
                originalTypedModuleCalls,
                emptyMap()
            )
            return function
        }

        val transformedFunction = function.deepCopyWithPreservingQualifiers()
        transformedFunctions[function] = transformedFunction

        val classOfCalls = mutableListOf<IrCall>()
        val typedModuleCalls = mutableListOf<IrCall>()
        val captures = mutableListOf<IrGetValue>()

        transformedFunction.body?.transformChildrenVoid(object :
            IrElementTransformerVoid() {
            private val moduleStack = mutableListOf(transformedFunction)
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.isModule(pluginContext.bindingContext))
                    moduleStack.push(declaration)
                return super.visitFunction(declaration)
                    .also {
                        if (declaration.isModule(pluginContext.bindingContext))
                            moduleStack.pop()
                    }
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                if (transformedFunction.isLocal &&
                    moduleStack.last() == transformedFunction &&
                    expression.symbol.owner !in transformedFunction.valueParameters &&
                    expression.type.classOrNull != symbols.providerDsl
                ) {
                    captures += expression
                }
                return super.visitGetValue(expression)
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val callee = transformFunctionIfNeeded(expression.symbol.owner)
                try {
                    if (callee.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.classOf") {
                        classOfCalls += expression
                    } else if (callee.hasAnnotation(InjektFqNames.AstTyped)) {
                        typedModuleCalls += expression
                    }
                } catch (e: Exception) {
                }
                return super.visitCall(expression)
            }
        })

        if (classOfCalls.isNotEmpty() || typedModuleCalls.isNotEmpty()) {
            transformedFunction.annotations +=
                InjektDeclarationIrBuilder(pluginContext, transformedFunction.symbol)
                    .noArgSingleConstructorCall(symbols.astTyped)

            val valueParametersByUnresolvedType =
                mutableMapOf<IrTypeParameterSymbol, IrValueParameter>()

            (classOfCalls
                .map { it.getTypeArgument(0)!! } + typedModuleCalls.flatMap { it.typeArguments })
                .filter { it.isTypeParameter() }
                .map { it.classifierOrFail as IrTypeParameterSymbol }
                .distinct()
                .forEach { typeParameter ->
                    valueParametersByUnresolvedType[typeParameter] =
                        transformedFunction.addValueParameter(
                            InjektNameConventions.classParameterNameForTypeParameter(
                                    typeParameter.owner
                                )
                                .asString(),
                            irBuiltIns.kClassClass.typeWith(typeParameter.defaultType)
                        )
                }

            rewriteTypedFunctionCalls(
                transformedFunction,
                classOfCalls,
                typedModuleCalls,
                valueParametersByUnresolvedType
            )
        } else {
            rewriteTypedFunctionCalls(
                transformedFunction,
                classOfCalls,
                typedModuleCalls,
                emptyMap()
            )
        }

        if (captures.isNotEmpty()) {
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
        }

        return transformedFunction
    }

    private fun rewriteTypedFunctionCalls(
        function: IrFunction,
        classOfCalls: List<IrCall>,
        typedModuleCalls: List<IrCall>,
        valueParametersByUnresolvedType: Map<IrTypeParameterSymbol, IrValueParameter>
    ) {
        function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                return when (expression) {
                    in classOfCalls -> {
                        val typeArgument = expression.getTypeArgument(0)!!
                        if (typeArgument.isTypeParameter()) {
                            val symbol = typeArgument.classifierOrFail as IrTypeParameterSymbol
                            DeclarationIrBuilder(pluginContext, expression.symbol)
                                .irGet(valueParametersByUnresolvedType.getValue(symbol))
                        } else {
                            IrClassReferenceImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                irBuiltIns.kClassClass.typeWith(typeArgument),
                                typeArgument.classifierOrFail,
                                typeArgument
                            )
                        }
                    }
                    in typedModuleCalls -> {
                        val transformedFunction = transformFunctionIfNeeded(expression.symbol.owner)
                        DeclarationIrBuilder(pluginContext, expression.symbol)
                            .irCall(transformedFunction).apply {
                                expression.typeArguments.forEachIndexed { index, typeArgument ->
                                    putTypeArgument(index, typeArgument)
                                }

                                dispatchReceiver = expression.dispatchReceiver
                                extensionReceiver = expression.extensionReceiver

                                transformedFunction.valueParameters.forEach { valueParameter ->
                                    var valueArgument = try {
                                        expression.getValueArgument(valueParameter.index)
                                    } catch (e: Throwable) {
                                        null
                                    }

                                    if (valueArgument == null) {
                                        val typeParameterName = InjektNameConventions
                                            .typeParameterNameForClassParameterName(valueParameter.name)
                                        val typeParameter = transformedFunction.typeParameters
                                            .single { it.name == typeParameterName }
                                        val typeArgument = getTypeArgument(typeParameter.index)!!
                                        valueArgument = if (typeArgument.isTypeParameter()) {
                                            val symbol =
                                                typeArgument.classifierOrFail as IrTypeParameterSymbol
                                            DeclarationIrBuilder(pluginContext, expression.symbol)
                                                .irGet(
                                                    valueParametersByUnresolvedType.getValue(
                                                        symbol
                                                    )
                                                )
                                        } else {
                                            IrClassReferenceImpl(
                                                UNDEFINED_OFFSET,
                                                UNDEFINED_OFFSET,
                                                irBuiltIns.kClassClass.typeWith(typeArgument),
                                                typeArgument.classifierOrFail,
                                                typeArgument
                                            )
                                        }
                                    }

                                    putValueArgument(
                                        valueParameter.index,
                                        valueArgument
                                    )
                                }
                            }
                    }
                    else -> super.visitCall(expression)
                }
            }
        })
    }

}
