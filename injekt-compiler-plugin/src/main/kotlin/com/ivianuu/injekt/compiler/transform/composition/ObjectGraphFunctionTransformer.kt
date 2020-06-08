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

import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.dumpSrc
import com.ivianuu.injekt.compiler.isTypeParameter
import com.ivianuu.injekt.compiler.remapTypeParameters
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.transform.AbstractFunctionTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.factory.Key
import com.ivianuu.injekt.compiler.transform.factory.asKey
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import com.ivianuu.injekt.compiler.withAnnotations
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.ir.copyBodyTo
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyToWithoutSuperTypes
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
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
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ObjectGraphFunctionTransformer(pluginContext: IrPluginContext) :
    AbstractFunctionTransformer(pluginContext, TransformOrder.BottomUp) {

    override fun needsTransform(function: IrFunction): Boolean = true

    override fun transform(
        function: IrFunction,
        callback: (IrFunction) -> Unit
    ) {
        callback(function)

        val originalUnresolvedGetCalls = mutableListOf<IrCall>()
        val originalUnresolvedInjectCalls = mutableListOf<IrCall>()
        val originalObjectGraphFunctionCalls = mutableListOf<IrCall>()
        var hasUnresolvedCalls = false

        fun IrType.isTypeParameterOfFunction(function: IrFunction): Boolean {
            return isTypeParameter() && (classifierOrFail as IrTypeParameterSymbol).owner
                .parent == function
        }

        function.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                val callee = transformFunctionIfNeeded(expression.symbol.owner)
                when {
                    callee.isObjectGraphGet -> {
                        if (expression.extensionReceiver?.type?.isTypeParameterOfFunction(function) == true ||
                            expression.getTypeArgument(0)!!.isTypeParameterOfFunction(function)
                        ) {
                            originalUnresolvedGetCalls += expression
                            hasUnresolvedCalls = true
                        }
                    }
                    callee.isObjectGraphInject -> {
                        if (expression.extensionReceiver?.type?.isTypeParameterOfFunction(function) == true ||
                            expression.getTypeArgument(0)!!.isTypeParameterOfFunction(function)
                        ) {
                            originalUnresolvedInjectCalls += expression
                            hasUnresolvedCalls = true
                        }
                    }
                    else -> {
                        if (expression.symbol != callee.symbol) {
                            originalObjectGraphFunctionCalls += expression
                            if (expression.typeArguments.any {
                                    it.isTypeParameterOfFunction(
                                        function
                                    )
                                }
                            ) {
                                hasUnresolvedCalls = true
                            }
                        }
                    }
                }
                return super.visitCall(expression)
            }
        })

        if (!hasUnresolvedCalls) {
            transformObjectGraphCalls(
                function,
                function,
                emptyList(),
                emptyMap(),
                emptyList(),
                emptyMap(),
                originalObjectGraphFunctionCalls
            )
            return
        }

        val transformedFunction = buildFun {
            name = InjektNameConventions.getTransformedModuleFunctionNameForObjectGraphFunction(
                function.getPackageFragment()!!.fqName,
                function.descriptor.fqNameSafe
            )
            returnType = function.returnType
            isInline = function.isInline
        }.apply {
            parent = function.file
            addMetadataIfNotLocal()

            annotations = function.annotations.map { it.deepCopyWithSymbols() }

            typeParameters = function.typeParameters.map { typeParameter ->
                typeParameter.copyToWithoutSuperTypes(this)
            }
            typeParameters.forEach {
                it.superTypes += function.typeParameters[it.index].superTypes
                    .map { it.remapTypeParameters(function, this) }
            }

            valueParameters = function.allParameters.mapIndexed { index, valueParameter ->
                valueParameter.copyTo(
                    this,
                    index = index,
                    type = valueParameter.type.remapTypeParameters(function, this),
                    varargElementType = valueParameter.varargElementType?.remapTypeParameters(
                        function,
                        this
                    )
                )
            }

            body = function.copyBodyTo(this)
        }

        transformedFunction.returnType = function.returnType
            .remapTypeParameters(function, transformedFunction)

        callback(transformedFunction)

        val unresolvedGetCalls = mutableListOf<IrCall>()
        val unresolvedInjectCalls = mutableListOf<IrCall>()
        val objectGraphFunctionCalls = mutableListOf<IrCall>()

        transformedFunction.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val callee = transformFunctionIfNeeded(expression.symbol.owner)
                when {
                    callee.isObjectGraphGet -> {
                        if (expression.extensionReceiver?.type
                                ?.remapTypeParameters(function, transformedFunction)
                                ?.isTypeParameterOfFunction(transformedFunction) == true ||
                            expression.getTypeArgument(0)!!
                                .remapTypeParameters(function, transformedFunction)
                                .isTypeParameterOfFunction(transformedFunction)
                        ) {
                            unresolvedGetCalls += expression
                            hasUnresolvedCalls = true
                        }
                    }
                    callee.isObjectGraphInject -> {
                        if (expression.extensionReceiver?.type
                                ?.remapTypeParameters(function, transformedFunction)
                                ?.isTypeParameterOfFunction(transformedFunction) == true ||
                            expression.getTypeArgument(0)!!
                                .remapTypeParameters(function, transformedFunction)
                                .isTypeParameterOfFunction(transformedFunction)
                        ) {
                            unresolvedInjectCalls += expression
                            hasUnresolvedCalls = true
                        }
                    }
                    else -> {
                        if (expression.symbol != callee.symbol) {
                            objectGraphFunctionCalls += expression
                            if (expression.typeArguments.any {
                                    it.remapTypeParameters(function, transformedFunction)
                                        .isTypeParameterOfFunction(transformedFunction)
                                }) {
                                hasUnresolvedCalls = true
                            }
                        }
                    }
                }
                return super.visitCall(expression)
            }
        })

        val valueParametersByUnresolvedGetCalls =
            mutableMapOf<Key, IrValueParameter>()

        fun addProviderValueParameterIfNeeded(providerKey: Key) {
            if (providerKey !in valueParametersByUnresolvedGetCalls) {
                valueParametersByUnresolvedGetCalls[providerKey] =
                    transformedFunction.addValueParameter(
                        "og_provider${valueParametersByUnresolvedGetCalls.size}",
                        providerKey.type
                    )
            }
        }

        unresolvedGetCalls
            .map {
                pluginContext.tmpFunction(1)
                    .typeWith(
                        it.extensionReceiver!!.type,
                        it.getTypeArgument(0)!!
                    )
                    .remapTypeParameters(function, transformedFunction)
                    .asKey()
            }
            .forEach { addProviderValueParameterIfNeeded(it) }

        objectGraphFunctionCalls.forEach { objectGraphFunctionCall ->
            val callee = transformFunctionIfNeeded(objectGraphFunctionCall.symbol.owner)
            callee
                .valueParameters
                .filter { it.name.asString().startsWith("og_provider") }
                .map {
                    it to it.type.substitute(
                        callee.typeParameters,
                        objectGraphFunctionCall.typeArguments
                    ).remapTypeParameters(function, transformedFunction)
                }
                .filter { it.second.typeArguments.any { it.typeOrNull?.isTypeParameter() == true } }
                .map { it.second.asKey() }
                .forEach { addProviderValueParameterIfNeeded(it) }
        }

        val valueParametersByUnresolvedInjectCalls =
            mutableMapOf<Key, IrValueParameter>()

        fun addInjectorValueParameterIfNeeded(injectorKey: Key) {
            if (injectorKey !in valueParametersByUnresolvedInjectCalls) {
                valueParametersByUnresolvedInjectCalls[injectorKey] =
                    transformedFunction.addValueParameter(
                        "og_injector${valueParametersByUnresolvedInjectCalls.size}",
                        injectorKey.type
                    )
            }
        }

        unresolvedInjectCalls
            .map {
                pluginContext.tmpFunction(2)
                    .typeWith(
                        it.extensionReceiver!!.type,
                        it.getTypeArgument(0)!!,
                        irBuiltIns.unitType
                    )
                    .remapTypeParameters(function, transformedFunction)
                    .asKey()
            }
            .forEach { addInjectorValueParameterIfNeeded(it) }

        objectGraphFunctionCalls.forEach { objectGraphFunctionCall ->
            val callee = transformFunctionIfNeeded(objectGraphFunctionCall.symbol.owner)
            callee
                .valueParameters
                .filter { it.name.asString().startsWith("og_injector") }
                .map {
                    it to it.type.substitute(
                        callee.typeParameters,
                        objectGraphFunctionCall.typeArguments
                    ).remapTypeParameters(function, transformedFunction)
                }
                .filter { it.second.typeArguments.any { it.typeOrNull?.isTypeParameter() == true } }
                .map { it.second.asKey() }
                .forEach { addInjectorValueParameterIfNeeded(it) }
        }

        transformObjectGraphCalls(
            function,
            transformedFunction,
            unresolvedGetCalls,
            valueParametersByUnresolvedGetCalls,
            unresolvedInjectCalls,
            valueParametersByUnresolvedInjectCalls,
            objectGraphFunctionCalls
        )
    }

    override fun transformExternal(function: IrFunction, callback: (IrFunction) -> Unit) {
        callback(
            pluginContext.referenceFunctions(function.descriptor.fqNameSafe)
                .map { it.owner }
                .filter { other ->
                    other.valueParameters.any {
                        it.name.asString().startsWith("og_provider") ||
                                it.name.asString().startsWith("og_injector")
                    }
                }
                .singleOrNull { other ->
                    other != function &&
                            other.name == function.name &&
                            other.typeParameters.size == function.typeParameters.size &&
                            other.typeParameters.all { it.name == function.typeParameters[it.index].name } &&
                            other.valueParameters.all { otherValueParameter ->
                                val thisValueParameter =
                                    function.valueParameters.getOrNull(otherValueParameter.index)
                                // todo the name check is unsafe compare types instead
                                (otherValueParameter.name == thisValueParameter?.name ||
                                        (otherValueParameter.name.asString()
                                            .startsWith("og_provider") ||
                                                otherValueParameter.name.asString()
                                                    .startsWith("og_injector")))
                            }
                } ?: function
        )
    }

    private fun transformObjectGraphCalls(
        originalFunction: IrFunction,
        transformedFunction: IrFunction,
        unresolvedGetCalls: List<IrCall>,
        valueParametersByUnresolvedProviderType: Map<Key, IrValueParameter>,
        unresolvedInjectCalls: List<IrCall>,
        valueParametersByUnresolvedInjectorType: Map<Key, IrValueParameter>,
        objectGraphFunctionCalls: List<IrCall>
    ) {
        transformedFunction.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                return when (expression) {
                    in unresolvedGetCalls -> {
                        val valueParameter = valueParametersByUnresolvedProviderType
                            .getValue(
                                pluginContext.tmpFunction(1)
                                    .typeWith(
                                        expression.extensionReceiver!!.type,
                                        expression.getTypeArgument(0)!!
                                    )
                                    .remapTypeParameters(originalFunction, transformedFunction)
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
                                pluginContext.tmpFunction(2)
                                    .typeWith(
                                        expression.extensionReceiver!!.type,
                                        expression.getTypeArgument(0)!!,
                                        irBuiltIns.unitType
                                    )
                                    .remapTypeParameters(originalFunction, transformedFunction)
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
                        val transformedCallee = transformFunctionIfNeeded(expression.symbol.owner)
                        transformObjectGraphFunctionCall(
                            originalFunction,
                            transformedFunction,
                            expression,
                            transformedCallee,
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
        originalFunction: IrFunction,
        transformedFunction: IrFunction,
        originalCall: IrCall,
        transformedCallee: IrFunction,
        valueParametersByUnresolvedProviderType: Map<Key, IrValueParameter>,
        valueParametersByUnresolvedInjectorType: Map<Key, IrValueParameter>
    ): IrExpression {
        return DeclarationIrBuilder(pluginContext, transformedCallee.symbol).irCall(
            transformedCallee
        )
            .apply {
                originalCall.typeArguments.forEachIndexed { index, it ->
                    putTypeArgument(
                        index,
                        it.remapTypeParameters(originalFunction, transformedFunction)
                    )
                }

                transformedCallee.allParameters.forEach { valueParameter ->
                    var valueArgument = try {
                        originalCall.getArgumentsWithIr()[valueParameter.index].second
                    } catch (e: Throwable) {
                        null
                    }

                    if (valueArgument == null) {
                        valueArgument = when {
                            valueParameter.name.asString().startsWith("og_provider") -> {
                                val substitutedType = valueParameter.type
                                    .remapTypeParameters(originalFunction, transformedFunction)
                                    .substituteByName(
                                        transformedCallee.typeParameters
                                            .map { it.symbol }
                                            .zip(typeArguments)
                                            .toMap()
                                    )

                                val componentType = substitutedType.typeArguments[0].typeOrFail
                                val instanceType = substitutedType.typeArguments[1].typeOrFail

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
                                                valueParametersByUnresolvedProviderType.get(
                                                    substitutedType.asKey()
                                                )
                                                    ?: error("${substitutedType.asKey()} is missing we only have${valueParametersByUnresolvedProviderType.keys}")
                                            )
                                    }
                                }
                            }
                            valueParameter.name.asString().startsWith("og_injector") -> {
                                val substitutedType = valueParameter.type
                                    .remapTypeParameters(originalFunction, transformedFunction)
                                    .substituteByName(
                                        transformedCallee.typeParameters
                                            .map { it.symbol }
                                            .zip(typeArguments)
                                            .toMap()
                                    )

                                val componentType = substitutedType.typeArguments[0].typeOrFail
                                val instanceType = substitutedType.typeArguments[1].typeOrFail

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
