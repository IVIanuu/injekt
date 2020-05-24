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

package com.ivianuu.injekt.compiler.transform.composition

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.deepCopyWithPreservingQualifiers
import com.ivianuu.injekt.compiler.isTypeParameter
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.factory.Key
import com.ivianuu.injekt.compiler.transform.factory.asKey
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.withAnnotations
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ObjectGraphFunctionTransformer(pluginContext: IrPluginContext) :
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
                    original.deepCopyWithPreservingQualifiers(
                        wrapDescriptor = false
                    )
                        .also { decoy ->
                            decoys[original] = decoy
                            InjektDeclarationIrBuilder(pluginContext, decoy.symbol).run {
                                if (transformed.valueParameters
                                        .any {
                                            it.name.asString().startsWith("og_provider\$") ||
                                                    it.name.asString().startsWith("og_injector\$")
                                        }
                                ) {
                                    decoy.annotations += noArgSingleConstructorCall(symbols.astObjectGraph)
                                }

                                decoy.body = builder.irExprBody(irInjektIntrinsicUnit())
                            }
                        }
                )
            }
        }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement =
        transformFunctionIfNeeded(super.visitFunction(declaration) as IrFunction)

    private fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        if (function.visibility == Visibilities.LOCAL &&
            function.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        ) return function
        if (function.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB ||
            function.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
        ) {
            return if (function.hasAnnotation(InjektFqNames.AstObjectGraph)) {
                pluginContext.referenceFunctions(function.descriptor.fqNameSafe)
                    .map { it.owner }
                    .single { other ->
                        other.name == function.name &&
                                other.valueParameters.any {
                                    it.name.asString().startsWith("og_provider\$") ||
                                            it.name.asString().startsWith("og_injector\$")
                                }
                    }
            } else function
        }
        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function
        decoys[function]?.let { return it }
        if (function in transformingFunctions) return function
        transformingFunctions += function

        val originalUnresolvedGetCalls = mutableListOf<IrCall>()
        val originalUnresolvedInjectCalls = mutableListOf<IrCall>()
        val originalObjectGraphFunctionCalls = mutableListOf<IrCall>()
        var hasUnresolvedCalls = false

        function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val callee = transformFunctionIfNeeded(expression.symbol.owner)
                when {
                    callee.isObjectGraphGet -> {
                        if (expression.extensionReceiver?.type?.isTypeParameter() == true ||
                            expression.getTypeArgument(0)!!.isTypeParameter()
                        ) {
                            originalUnresolvedGetCalls += expression
                            hasUnresolvedCalls = true
                        }
                    }
                    callee.isObjectGraphInject -> {
                        if (expression.extensionReceiver?.type?.isTypeParameter() == true ||
                            expression.getTypeArgument(0)!!.isTypeParameter()
                        ) {
                            originalUnresolvedInjectCalls += expression
                            hasUnresolvedCalls = true
                        }
                    }
                    callee.symbol.owner.hasAnnotation(InjektFqNames.AstObjectGraph) -> {
                        originalObjectGraphFunctionCalls += expression
                        if (expression.typeArguments.any { it.isTypeParameter() }) {
                            hasUnresolvedCalls = true
                        }
                    }
                }
                return super.visitCall(expression)
            }
        })

        if (!hasUnresolvedCalls) {
            transformedFunctions[function] = function
            transformingFunctions -= function
            transformObjectGraphCalls(
                function,
                emptyList(),
                emptyMap(),
                emptyList(),
                emptyMap(),
                originalObjectGraphFunctionCalls
            )
            return function
        }

        val transformedFunction = function.deepCopyWithPreservingQualifiers(
            wrapDescriptor = true
        )
        transformedFunctions[function] = transformedFunction
        transformingFunctions -= function

        val unresolvedGetCalls = mutableListOf<IrCall>()
        val unresolvedInjectCalls = mutableListOf<IrCall>()
        val objectGraphFunctionCalls = mutableListOf<IrCall>()

        transformedFunction.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val callee = transformFunctionIfNeeded(expression.symbol.owner)
                when {
                    callee.isObjectGraphGet -> {
                        if (expression.extensionReceiver?.type?.isTypeParameter() == true ||
                            expression.getTypeArgument(0)!!.isTypeParameter()
                        ) {
                            unresolvedGetCalls += expression
                        }
                    }
                    callee.isObjectGraphInject -> {
                        if (expression.extensionReceiver?.type?.isTypeParameter() == true ||
                            expression.getTypeArgument(0)!!.isTypeParameter()
                        ) {
                            unresolvedInjectCalls += expression
                        }
                    }
                    callee.symbol.owner.hasAnnotation(InjektFqNames.AstObjectGraph) -> {
                        objectGraphFunctionCalls += expression
                    }
                }
                return super.visitCall(expression)
            }
        })

        if (unresolvedGetCalls.isNotEmpty() ||
            unresolvedInjectCalls.isNotEmpty() ||
            objectGraphFunctionCalls.isNotEmpty()
        ) {
            transformedFunction.annotations +=
                InjektDeclarationIrBuilder(pluginContext, transformedFunction.symbol)
                    .noArgSingleConstructorCall(symbols.astObjectGraph)

            val valueParametersByUnresolvedGetCalls =
                mutableMapOf<Key, IrValueParameter>()

            fun addProviderValueParameterIfNeeded(providerKey: Key) {
                if (providerKey !in valueParametersByUnresolvedGetCalls) {
                    valueParametersByUnresolvedGetCalls[providerKey] =
                        transformedFunction.addValueParameter(
                            "og_provider\$${valueParametersByUnresolvedGetCalls.size}",
                            providerKey.type
                        )
                }
            }

            unresolvedGetCalls
                .map {
                    irBuiltIns.function(1)
                        .typeWith(
                            it.extensionReceiver!!.type,
                            it.getTypeArgument(0)!!
                        )
                        .asKey()
                }
                .forEach { addProviderValueParameterIfNeeded(it) }

            objectGraphFunctionCalls.forEach { objectGraphFunctionCall ->
                val callee = transformFunctionIfNeeded(objectGraphFunctionCall.symbol.owner)
                callee
                    .valueParameters
                    .filter { it.name.asString().startsWith("og_provider\$") }
                    .map {
                        it to it.type.substitute(
                            callee.typeParameters,
                            objectGraphFunctionCall.typeArguments
                        )
                    }
                    .filter { it.second.typeArguments.any { it.isTypeParameter() } }
                    .map { it.second.asKey() }
                    .forEach { addProviderValueParameterIfNeeded(it) }
            }

            val valueParametersByUnresolvedInjectCalls =
                mutableMapOf<Key, IrValueParameter>()

            fun addInjectorValueParameterIfNeeded(injectorKey: Key) {
                if (injectorKey !in valueParametersByUnresolvedInjectCalls) {
                    valueParametersByUnresolvedInjectCalls[injectorKey] =
                        transformedFunction.addValueParameter(
                            "og_injector\$${valueParametersByUnresolvedInjectCalls.size}",
                            injectorKey.type
                        )
                }
            }

            unresolvedInjectCalls
                .map {
                    irBuiltIns.function(2)
                        .typeWith(
                            it.extensionReceiver!!.type,
                            it.getTypeArgument(0)!!,
                            irBuiltIns.unitType
                        )
                        .asKey()
                }
                .forEach { addInjectorValueParameterIfNeeded(it) }

            objectGraphFunctionCalls.forEach { objectGraphFunctionCall ->
                val callee = transformFunctionIfNeeded(objectGraphFunctionCall.symbol.owner)
                callee
                    .valueParameters
                    .filter { it.name.asString().startsWith("og_injector\$") }
                    .map {
                        it to it.type.substitute(
                            callee.typeParameters,
                            objectGraphFunctionCall.typeArguments
                        )
                    }
                    .filter { it.second.typeArguments.any { it.isTypeParameter() } }
                    .map { it.second.asKey() }
                    .forEach { addInjectorValueParameterIfNeeded(it) }
            }

            transformObjectGraphCalls(
                transformedFunction,
                unresolvedGetCalls,
                valueParametersByUnresolvedGetCalls,
                unresolvedInjectCalls,
                valueParametersByUnresolvedInjectCalls,
                objectGraphFunctionCalls
            )
        } else {
            transformObjectGraphCalls(
                transformedFunction,
                emptyList(),
                emptyMap(),
                emptyList(),
                emptyMap(),
                objectGraphFunctionCalls
            )
        }

        return transformedFunction
    }

    private fun transformObjectGraphCalls(
        function: IrFunction,
        unresolvedGetCalls: List<IrCall>,
        valueParametersByUnresolvedProviderType: Map<Key, IrValueParameter>,
        unresolvedInjectCalls: List<IrCall>,
        valueParametersByUnresolvedInjectorType: Map<Key, IrValueParameter>,
        objectGraphFunctionCalls: List<IrCall>
    ) {
        function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                return when (expression) {
                    in unresolvedGetCalls -> {
                        val valueParameter = valueParametersByUnresolvedProviderType
                            .getValue(
                                irBuiltIns.function(1)
                                    .typeWith(
                                        expression.extensionReceiver!!.type,
                                        expression.getTypeArgument(0)!!
                                    )
                                    .asKey()
                            )
                        DeclarationIrBuilder(pluginContext, expression.symbol).run {
                            irCall(
                                valueParameter.type.classOrNull!!
                                    .functions.first { it.owner.name.asString() == "invoke" }
                            ).apply {
                                dispatchReceiver = irGet(valueParameter)
                                putValueArgument(0, expression.extensionReceiver!!)
                            }
                        }
                    }
                    in unresolvedInjectCalls -> {
                        val valueParameter = valueParametersByUnresolvedInjectorType
                            .getValue(
                                irBuiltIns.function(2)
                                    .typeWith(
                                        expression.extensionReceiver!!.type,
                                        expression.getTypeArgument(0)!!,
                                        irBuiltIns.unitType
                                    )
                                    .asKey()
                            )
                        DeclarationIrBuilder(pluginContext, expression.symbol).run {
                            irCall(
                                valueParameter.type.classOrNull!!
                                    .functions.first { it.owner.name.asString() == "invoke" }
                            ).apply {
                                dispatchReceiver = irGet(valueParameter)
                                putValueArgument(0, expression.extensionReceiver!!)
                                putValueArgument(1, expression.getValueArgument(0)!!)
                            }
                        }
                    }
                    in objectGraphFunctionCalls -> {
                        val transformedFunction = transformFunctionIfNeeded(expression.symbol.owner)
                        transformObjectGraphFunctionCall(
                            expression,
                            transformedFunction,
                            valueParametersByUnresolvedProviderType,
                            valueParametersByUnresolvedInjectorType
                        )
                    }
                    else -> super.visitCall(expression)
                }
            }
        })
    }

    private fun transformObjectGraphFunctionCall(
        originalCall: IrCall,
        transformedFunction: IrFunction,
        valueParametersByUnresolvedProviderType: Map<Key, IrValueParameter>,
        valueParametersByUnresolvedInjectorType: Map<Key, IrValueParameter>
    ): IrExpression =
        DeclarationIrBuilder(pluginContext, transformedFunction.symbol).irCall(transformedFunction)
            .apply {
                dispatchReceiver = originalCall.dispatchReceiver
                extensionReceiver = originalCall.extensionReceiver

                originalCall.typeArguments.forEachIndexed { index, it ->
                    putTypeArgument(index, it)
                }

                transformedFunction.valueParameters.forEach { valueParameter ->
                    var valueArgument = try {
                        originalCall.getValueArgument(valueParameter.index)
                    } catch (e: Throwable) {
                        null
                    }

                    if (valueArgument == null) {
                        valueArgument = when {
                            valueParameter.name.asString().startsWith("og_provider\$") -> {
                                val substitutedType = valueParameter.type
                                    .substituteByName(
                                        transformedFunction.typeParameters
                                            .map { it.symbol }
                                            .zip(typeArguments)
                                            .toMap()
                                    )

                                val componentType = substitutedType.typeArguments[0]
                                val instanceType = substitutedType.typeArguments[1]

                                when {
                                    !componentType.isTypeParameter() && !instanceType.isTypeParameter() -> {
                                        InjektDeclarationIrBuilder(pluginContext, symbol).run {
                                            irLambda(substitutedType) { lambda ->
                                                +irReturn(
                                                    IrCallImpl(
                                                        originalCall.startOffset,
                                                        originalCall.endOffset,
                                                        instanceType,
                                                        pluginContext.referenceFunctions(
                                                            FqName("com.ivianuu.injekt.composition.get")
                                                        ).single()
                                                    ).apply {
                                                        extensionReceiver =
                                                            irGet(lambda.valueParameters.first())
                                                        putTypeArgument(0, instanceType)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    else -> {
                                        DeclarationIrBuilder(pluginContext, symbol)
                                            .irGet(
                                                valueParametersByUnresolvedProviderType.getValue(
                                                    substitutedType.asKey()
                                                )
                                            )
                                    }
                                }
                            }
                            valueParameter.name.asString().startsWith("og_injector\$") -> {
                                val substitutedType = valueParameter.type
                                    .substituteByName(
                                        transformedFunction.typeParameters
                                            .map { it.symbol }
                                            .zip(typeArguments)
                                            .toMap()
                                    )

                                val componentType = substitutedType.typeArguments[0]
                                val instanceType = substitutedType.typeArguments[1]

                                when {
                                    !componentType.isTypeParameter() && !instanceType.isTypeParameter() -> {
                                        InjektDeclarationIrBuilder(pluginContext, symbol).run {
                                            irLambda(substitutedType) { lambda ->
                                                +irReturn(
                                                    IrCallImpl(
                                                        originalCall.startOffset,
                                                        originalCall.endOffset,
                                                        irBuiltIns.unitType,
                                                        pluginContext.referenceFunctions(
                                                            FqName("com.ivianuu.injekt.composition.inject")
                                                        ).single()
                                                    ).apply {
                                                        extensionReceiver =
                                                            irGet(lambda.valueParameters[0])
                                                        putTypeArgument(0, instanceType)
                                                        putValueArgument(
                                                            0,
                                                            irGet(lambda.valueParameters[1])
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    else -> {
                                        DeclarationIrBuilder(pluginContext, symbol)
                                            .irGet(
                                                valueParametersByUnresolvedInjectorType.getValue(
                                                    substitutedType.asKey()
                                                )
                                            )
                                    }
                                }
                            }
                            else -> null
                        }
                    }

                    putValueArgument(valueParameter.index, valueArgument)
                }
            }

    // todo remove once compose fixed it's stuff
    private fun IrType.substituteByName(substitutionMap: Map<IrTypeParameterSymbol, IrType>): IrType {
        if (this !is IrSimpleType) return this

        (classifier as? IrTypeParameterSymbol)?.let { typeParam ->
            substitutionMap.toList()
                .firstOrNull { it.first.owner.name == typeParam.owner.name }
                ?.let { return it.second.withAnnotations(annotations) }
        }

        substitutionMap[classifier]?.let {
            return it.withAnnotations(annotations)
        }

        val newArguments = arguments.map {
            if (it is IrTypeProjection) {
                makeTypeProjection(it.type.substituteByName(substitutionMap), it.variance)
            } else {
                it
            }
        }

        val newAnnotations = annotations.map { it.deepCopyWithSymbols() }
        return IrSimpleTypeImpl(
            classifier,
            hasQuestionMark,
            newArguments,
            newAnnotations
        )
    }
}
