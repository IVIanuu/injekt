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
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class KeyTypeParameterTransformer(injektContext: InjektContext) :
    AbstractInjektTransformer(injektContext) {

    private val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()

    override fun lower() {
        injektContext.module.transformChildrenVoid(
            object : IrElementTransformerVoidWithContext() {
                override fun visitFunctionNew(declaration: IrFunction): IrStatement =
                    super.visitFunctionNew(transformFunctionIfNeeded(declaration))
            }
        )
        injektContext.module.transformCallsWithForKey(emptyMap())
    }

    private fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function

        if (function.typeParameters.none { it.descriptor.annotations.hasAnnotation(InjektFqNames.ForKey) })
            return function

        val transformedFunction = function.copyWithKeyParams()
        transformedFunctions[function] = transformedFunction

        val transformedKeyParams = transformedFunction.typeParameters
            .filter { it.descriptor.annotations.hasAnnotation(InjektFqNames.ForKey) }
            .associateWith {
                transformedFunction.addValueParameter(
                    "_${it.name}Key",
                    injektContext.irBuiltIns.stringType
                )
            }

        val keyParams = transformedKeyParams + function.typeParameters
            .filter { it.descriptor.annotations.hasAnnotation(InjektFqNames.ForKey) }
            .associateWith { typeParameter ->
                transformedKeyParams.entries
                    .single { it.key.name == typeParameter.name }
                    .value
            }

        transformedFunction.transformCallsWithForKey(
            keyParams
                .mapValues {
                    {
                        DeclarationIrBuilder(injektContext, transformedFunction.symbol)
                            .irGet(it.value)
                    }
                }
        )

        return transformedFunction
    }

    private fun IrElement.transformCallsWithForKey(
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
        if (expression.symbol.descriptor.fqNameSafe.asString() ==
            "com.ivianuu.injekt.keyOf"
        ) {
            val typeArgument = expression.getTypeArgument(0)!!
            return DeclarationIrBuilder(injektContext, expression.symbol).irCall(
                injektContext.injektSymbols.key.constructors
                    .single()
            ).apply {
                putValueArgument(
                    0,
                    typeArgument.keyStringExpression(expression.symbol, typeParameterKeyExpressions)
                )
            }
        }
        val callee = expression.symbol.owner
        if (callee.typeParameters.none { it.descriptor.annotations.hasAnnotation(InjektFqNames.ForKey) }) return expression
        val transformedCallee = transformFunctionIfNeeded(callee)
        if (expression.symbol == transformedCallee.symbol) return expression
        return IrCallImpl(
            expression.startOffset,
            expression.endOffset,
            transformedCallee.returnType,
            transformedCallee.symbol,
            transformedCallee.typeParameters.size,
            transformedCallee.valueParameters.size,
            expression.origin,
            expression.superQualifierSymbol
        ).apply {
            copyTypeAndValueArgumentsFrom(expression)
            var currentIndex = expression.valueArgumentsCount
            (0 until typeArgumentsCount)
                .map { transformedCallee.typeParameters[it] to getTypeArgument(it)!! }
                .filter { it.first.descriptor.annotations.hasAnnotation(InjektFqNames.ForKey) }
                .forEach { (_, typeArgument) ->
                    putValueArgument(
                        currentIndex++,
                        typeArgument.keyStringExpression(
                            expression.symbol,
                            typeParameterKeyExpressions
                        )
                    )
                }
        }
    }

    private fun IrType.keyStringExpression(
        symbol: IrSymbol,
        typeParameterKeyExpressions: Map<IrTypeParameter, () -> IrExpression>
    ): IrExpression {
        val builder = DeclarationIrBuilder(injektContext, symbol)
        val expressions = mutableListOf<IrExpression>()
        fun IrType.collectExpressions() {
            check(this@collectExpressions is IrSimpleType)
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
        }

        collectExpressions()

        return if (expressions.size == 1) {
            expressions.single()
        } else {
            val stringPlus = injektContext.irBuiltIns.stringClass
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

    private fun IrFunction.copyWithKeyParams(): IrFunction {
        return copy(injektContext).apply {
            val descriptor = descriptor
            if (descriptor is PropertyGetterDescriptor &&
                annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.getterName(descriptor.correspondingProperty.name.identifier)
                annotations += DeclarationIrBuilder(injektContext, symbol)
                    .jvmNameAnnotation(name, injektContext)
                correspondingPropertySymbol?.owner?.getter = this
            }
            if (descriptor is PropertySetterDescriptor &&
                annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.setterName(descriptor.correspondingProperty.name.identifier)
                annotations += DeclarationIrBuilder(injektContext, symbol)
                    .jvmNameAnnotation(name, injektContext)
                correspondingPropertySymbol?.owner?.setter = this
            }

            if (this@copyWithKeyParams is IrOverridableDeclaration<*>) {
                overriddenSymbols = this@copyWithKeyParams.overriddenSymbols.map {
                    transformFunctionIfNeeded(it.owner as IrFunction).symbol as IrSimpleFunctionSymbol
                }
            }
        }
    }

}
