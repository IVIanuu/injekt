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
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class TypeKeyTransformer(
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {

    private val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        super.visitModuleFragment(declaration)
        declaration.transformCallsWithForTypeKey(emptyMap())
        return declaration
    }

    override fun visitFunction(declaration: IrFunction): IrStatement =
        super.visitFunction(transformFunctionIfNeeded(declaration))

    private fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function

        if (function.descriptor.typeParameters.none { it.hasAnnotation(InjektFqNames.ForTypeKey) })
            return function

        val transformedFunction = function.copyWithTypeKeyParams()
        transformedFunctions[function] = transformedFunction

        val typeKeyParams = transformedFunction.typeParameters
            .filter { it.descriptor.hasAnnotation(InjektFqNames.ForTypeKey) }
            .associateWith {
                transformedFunction.addValueParameter(
                    "_${it.name}Key",
                    pluginContext.irBuiltIns.stringType
                )
            }

        transformedFunction.transformCallsWithForTypeKey(
            typeKeyParams
                .mapValues {
                    {
                        DeclarationIrBuilder(pluginContext, transformedFunction.symbol)
                            .irGet(it.value)
                    }
                }
        )

        return transformedFunction
    }

    private fun IrElement.transformCallsWithForTypeKey(
        typeParameterKeyExpressions: Map<IrTypeParameter, () -> IrExpression>
    ) {
        transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val result = transformCallIfNeeded(expression, typeParameterKeyExpressions)
                return if (result is IrCall) super.visitCall(result) else result
            }
        })
    }

    private fun transformCallIfNeeded(
        expression: IrCall,
        typeParameterKeyExpressions: Map<IrTypeParameter, () -> IrExpression>
    ): IrExpression {
        if (expression.symbol.descriptor.fqNameSafe == InjektFqNames.typeKeyOf) {
            val typeArgument = expression.getTypeArgument(0)!!
            return DeclarationIrBuilder(pluginContext, expression.symbol).irCall(
                pluginContext.referenceClass(InjektFqNames.TypeKey)!!
                    .constructors
                    .single()
            ).apply {
                putTypeArgument(0, typeArgument)
                putValueArgument(
                    0,
                    typeArgument.typeKeyStringExpression(expression.symbol, typeParameterKeyExpressions)
                )
            }
        }
        val callee = expression.symbol.owner
        if (callee.descriptor.typeParameters.none { it.hasAnnotation(InjektFqNames.ForTypeKey) }) return expression
        val transformedCallee = transformFunctionIfNeeded(callee)
        if (expression.symbol == transformedCallee.symbol) return expression
        return IrCallImpl(
            expression.startOffset,
            expression.endOffset,
            transformedCallee.returnType,
            transformedCallee.symbol as IrSimpleFunctionSymbol,
            transformedCallee.typeParameters.size,
            transformedCallee.valueParameters.size,
            expression.origin,
            expression.superQualifierSymbol
        ).apply {
            copyTypeAndValueArgumentsFrom(expression)
            var currentIndex = expression.valueArgumentsCount
            (0 until typeArgumentsCount)
                .map { transformedCallee.typeParameters[it] to getTypeArgument(it)!! }
                .filter { it.first.descriptor.hasAnnotation(InjektFqNames.ForTypeKey) }
                .forEach { (_, typeArgument) ->
                    putValueArgument(
                        currentIndex++,
                        typeArgument.typeKeyStringExpression(
                            expression.symbol,
                            typeParameterKeyExpressions
                        )
                    )
                }
        }
    }

    private fun IrType.typeKeyStringExpression(
        symbol: IrSymbol,
        typeParameterKeyExpressions: Map<IrTypeParameter, () -> IrExpression>
    ): IrExpression {
        val builder = DeclarationIrBuilder(pluginContext, symbol)
        val expressions = mutableListOf<IrExpression>()
        fun IrType.collectExpressions() {
            check(this@collectExpressions is IrSimpleType)

            val typeAnnotations = listOfNotNull(
                if (hasAnnotation(InjektFqNames.Given)) "@Given" else null,
                if (hasAnnotation(InjektFqNames.Composable)) "@Composable" else null,
                *getAnnotatedAnnotations(InjektFqNames.Qualifier)
                    .map { it.type.classifierOrFail.descriptor.fqNameSafe.asString() +
                            (0 until it.valueArgumentsCount)
                                .map { i -> it.getValueArgument(i) as IrConst<*> }
                                .map { it.value }
                                .hashCode()
                                .toString()
                    }
                    .toTypedArray()
            )
            if (typeAnnotations.isNotEmpty()) {
                expressions += builder.irString(
                    buildString {
                        append("[")
                        typeAnnotations.forEachIndexed { index, annotation ->
                            append(annotation)
                            if (index != typeAnnotations.lastIndex) append(", ")
                        }
                        append("]")
                    }
                )
            }

            when {
                abbreviation != null -> {
                    expressions += builder.irString(abbreviation!!.typeAlias.descriptor.fqNameSafe.asString())
                }
                classifierOrFail is IrTypeParameterSymbol -> {
                    expressions += typeParameterKeyExpressions[classifierOrFail.owner]!!()
                }
                else -> {
                    expressions += builder.irString(classifierOrFail.descriptor.fqNameSafe.asString())
                }
            }

            val arguments = abbreviation?.arguments ?: arguments

            if (arguments.isNotEmpty()) {
                expressions += builder.irString("<")
                arguments.forEachIndexed { index, typeArgument ->
                    if (typeArgument.typeOrNull != null)
                        typeArgument.typeOrNull?.collectExpressions()
                    else expressions += builder.irString("*")
                    if (index != arguments.lastIndex) expressions += builder.irString(", ")
                }
                expressions += builder.irString(">")
            }

            if ((abbreviation != null && abbreviation!!.hasQuestionMark) ||
                (abbreviation == null && hasQuestionMark))
                    expressions += builder.irString("?")
        }

        collectExpressions()

        return if (expressions.size == 1) {
            expressions.single()
        } else {
            val stringPlus = pluginContext.irBuiltIns.stringClass
                .functions
                .map { it.owner }
                .first { it.name.asString() == "plus" }
            expressions.reduce { acc, expression ->
                builder.irCall(stringPlus).apply {
                    dispatchReceiver = acc
                    putValueArgument(0, expression)
                }
            }
        }
    }

    private fun IrFunction.copyWithTypeKeyParams(): IrFunction {
        return copy(pluginContext).apply {
            val descriptor = descriptor
            if (descriptor is PropertyGetterDescriptor &&
                annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.getterName(descriptor.correspondingProperty.name.identifier)
                annotations += DeclarationIrBuilder(pluginContext, symbol)
                    .jvmNameAnnotation(name, pluginContext)
                correspondingPropertySymbol?.owner?.getter = this
            }
            if (descriptor is PropertySetterDescriptor &&
                annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.setterName(descriptor.correspondingProperty.name.identifier)
                annotations += DeclarationIrBuilder(pluginContext, symbol)
                    .jvmNameAnnotation(name, pluginContext)
                correspondingPropertySymbol?.owner?.setter = this
            }

            if (this@copyWithTypeKeyParams is IrOverridableDeclaration<*>) {
                overriddenSymbols = this@copyWithTypeKeyParams.overriddenSymbols.map {
                    transformFunctionIfNeeded(it.owner as IrFunction).symbol as IrSimpleFunctionSymbol
                }
            }
        }
    }

}

