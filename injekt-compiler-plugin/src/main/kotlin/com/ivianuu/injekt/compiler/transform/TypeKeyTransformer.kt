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

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.asSequence
import kotlin.collections.associateWith
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.emptyMap
import kotlin.collections.filter
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.collections.get
import kotlin.collections.isNotEmpty
import kotlin.collections.lastIndex
import kotlin.collections.listOfNotNull
import kotlin.collections.map
import kotlin.collections.mapValues
import kotlin.collections.minusAssign
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.none
import kotlin.collections.plus
import kotlin.collections.plusAssign
import kotlin.collections.reduce
import kotlin.collections.reversed
import kotlin.collections.set
import kotlin.collections.single
import kotlin.collections.sortedBy
import kotlin.collections.toList

class TypeKeyTransformer(
    private val context: InjektContext,
    private val trace: BindingTrace,
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {
    private val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()
    private val transformedClasses = mutableSetOf<IrClass>()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        super.visitModuleFragment(declaration)
        declaration.transformCallsWithForTypeKey(emptyMap())
        return declaration
    }

    override fun visitClass(declaration: IrClass): IrStatement =
        super.visitClass(transformClassIfNeeded(declaration))

    override fun visitFunction(declaration: IrFunction): IrStatement =
        super.visitFunction(transformFunctionIfNeeded(declaration))

    private fun transformClassIfNeeded(clazz: IrClass): IrClass {
        if (clazz in transformedClasses) return clazz
        transformedClasses += clazz

        if (clazz.typeParameters.none { it.descriptor.isForTypeKey(context, trace) })
            return clazz

        val typeKeyFields = clazz.typeParameters
            .filter { it.descriptor.isForTypeKey(context, trace) }
            .associateWith {
                val field = clazz.addField(
                    "_${it.name}TypeKey",
                    pluginContext.irBuiltIns.stringType
                )
                clazz.declarations -= field
                clazz.declarations.add(0, field)
                field
            }

        clazz.constructors.forEach { constructor ->
            val typeKeyParamsWithFields = typeKeyFields.values.associateWith { field ->
                constructor.addValueParameter(
                    field.name.asString(),
                    field.type
                )
            }
            (constructor.body!! as IrBlockBody).run {
                typeKeyParamsWithFields
                    .toList()
                    .forEachIndexed { index, (field, param) ->
                        statements.add(
                            index + 1,
                            DeclarationIrBuilder(pluginContext, constructor.symbol).run {
                                irSetField(
                                    irGet(clazz.thisReceiver!!),
                                    field,
                                    irGet(param)
                                )
                            }
                        )
                    }
            }
        }

        clazz.transformCallsWithForTypeKey(
            typeKeyFields
                .mapValues { (_, field) ->
                    { scopes ->
                        DeclarationIrBuilder(pluginContext, clazz.symbol)
                            .run {
                                val constructor = scopes
                                    .firstOrNull {
                                        it.irElement in clazz.constructors
                                    }
                                    ?.irElement
                                    ?.cast<IrConstructor>()
                                if (constructor == null) {
                                    irGetField(
                                        irGet(scopes.thisOfClass(clazz)!!),
                                        field
                                    )
                                } else {
                                    irGet(constructor.valueParameters
                                        .single { it.name == field.name })
                                }
                            }
                    }
                }
        )

        return clazz
    }

    private fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function

        if (function is IrConstructor) {
            if (!function.descriptor.original.isExternalDeclaration()) {
                transformClassIfNeeded(function.constructedClass)
                return function
            }
            val typeKeyParameters = function.constructedClass
                .descriptor
                .declaredTypeParameters
                .filter { it.isForTypeKey(context, trace) }
            typeKeyParameters.forEach {
                function.addValueParameter(
                    "_${it.name}TypeKey",
                    pluginContext.irBuiltIns.stringType
                )
            }
            transformedFunctions[function] = function
            return function
        }

        if (function.descriptor.original.isExternalDeclaration()) {
            val typeKeyParameters = function
                .typeParameters
                .filter { it.descriptor.isForTypeKey(context, trace) }
            typeKeyParameters.forEach {
                function.addValueParameter(
                    "_${it.name}TypeKey",
                    pluginContext.irBuiltIns.stringType
                )
            }
            transformedFunctions[function] = function
            return function
        }

        if (function.typeParameters.none { it.descriptor.isForTypeKey(context, trace) })
            return function

        val transformedFunction = function.copyWithTypeKeyParams()
        transformedFunctions[function] = transformedFunction
        val typeKeyParams = transformedFunction.typeParameters
            .filter { it.descriptor.isForTypeKey(context, trace) }
            .associateWith {
                transformedFunction.addValueParameter(
                    "_${it.name}TypeKey",
                    pluginContext.irBuiltIns.stringType
                )
            }

        transformedFunction.transformCallsWithForTypeKey(
            typeKeyParams
                .mapValues { (_, param) ->
                    {
                        DeclarationIrBuilder(pluginContext, transformedFunction.symbol)
                            .irGet(param)
                    }
                }
        )

        return transformedFunction
    }

    private fun IrElement.transformCallsWithForTypeKey(
        typeParameterKeyExpressions: Map<IrTypeParameter, (List<ScopeWithIr>) -> IrExpression>
    ) {
        transform(object : IrElementTransformerVoidWithContext() {
            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression =
                super.visitFunctionAccess(
                    transformCallIfNeeded(expression, allScopes, typeParameterKeyExpressions))
        }, null)
    }

    private fun transformCallIfNeeded(
        expression: IrFunctionAccessExpression,
        scopes: List<ScopeWithIr>,
        typeParameterKeyExpressions: Map<IrTypeParameter, (List<ScopeWithIr>) -> IrExpression>
    ): IrFunctionAccessExpression {
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
                    typeArgument.typeKeyStringExpression(expression.symbol, scopes,
                        typeParameterKeyExpressions)
                )
            }
        }
        val callee = expression.symbol.owner
        if (callee is IrConstructor) {
            if (callee.constructedClass.typeParameters.none {
                    it.descriptor.isForTypeKey(context, trace)
                }) return expression
        } else {
            if (callee.typeParameters.none {
                    it.descriptor.isForTypeKey(context, trace)
                }) return expression
        }

        val transformedCallee = transformFunctionIfNeeded(callee)
        if (expression is IrCall &&
            expression.valueArgumentsCount == transformedCallee.valueParameters.size) return expression
        return when (expression) {
            is IrCall -> {
                IrCallImpl(
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
                        .asSequence()
                        .map { transformedCallee.typeParameters[it] to getTypeArgument(it)!! }
                        .filter { it.first.descriptor.isForTypeKey(context, trace) }
                        .forEach { (_, typeArgument) ->
                            putValueArgument(
                                currentIndex++,
                                typeArgument.typeKeyStringExpression(
                                    expression.symbol,
                                    scopes,
                                    typeParameterKeyExpressions
                                )
                            )
                        }
                }
            }
            is IrDelegatingConstructorCallImpl -> {
                if (expression.valueArgumentsCount == transformedCallee.valueParameters.size)
                    return expression
                IrDelegatingConstructorCallImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.type,
                    expression.symbol,
                    expression.typeArgumentsCount,
                    transformedCallee.valueParameters.size
                ).apply {
                    copyTypeAndValueArgumentsFrom(expression)
                    var currentIndex = expression.valueArgumentsCount
                    (0 until typeArgumentsCount)
                        .asSequence()
                        .map {
                            transformedCallee as IrConstructor
                            transformedCallee.constructedClass.typeParameters[it] to getTypeArgument(it)!!
                        }
                        .filter { it.first.descriptor.isForTypeKey(context, trace) }
                        .forEach { (_, typeArgument) ->
                            putValueArgument(
                                currentIndex++,
                                typeArgument.typeKeyStringExpression(
                                    expression.symbol,
                                    scopes,
                                    typeParameterKeyExpressions
                                )
                            )
                        }
                }
            }
            is IrConstructorCall -> {
                if (expression.valueArgumentsCount == transformedCallee.valueParameters.size)
                    return expression
                IrConstructorCallImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.type,
                    expression.symbol,
                    expression.typeArgumentsCount,
                    expression.constructorTypeArgumentsCount,
                    transformedCallee.valueParameters.size,
                    expression.origin
                ).apply {
                    copyTypeAndValueArgumentsFrom(expression)
                    var currentIndex = expression.valueArgumentsCount
                    (0 until typeArgumentsCount)
                        .asSequence()
                        .map {
                            transformedCallee as IrConstructor
                            transformedCallee.constructedClass.typeParameters[it] to getTypeArgument(it)!!
                        }
                        .filter { it.first.descriptor.isForTypeKey(context, trace) }
                        .forEach { (_, typeArgument) ->
                            putValueArgument(
                                currentIndex++,
                                typeArgument.typeKeyStringExpression(
                                    expression.symbol,
                                    scopes,
                                    typeParameterKeyExpressions
                                )
                            )
                        }
                }
            }
            else -> expression
        }
    }

    private fun IrType.typeKeyStringExpression(
        symbol: IrSymbol,
        scopes: List<ScopeWithIr>,
        typeParameterKeyExpressions: Map<IrTypeParameter, (List<ScopeWithIr>) -> IrExpression>
    ): IrExpression {
        val builder = DeclarationIrBuilder(pluginContext, symbol)
        val expressions = mutableListOf<IrExpression>()
        var currentString = ""
        fun commitCurrentString() {
            if (currentString.isNotEmpty()) {
                expressions += builder.irString(currentString)
                currentString = ""
            }
        }
        fun appendToCurrentString(value: String) {
            currentString += value
        }
        fun appendTypeParameterExpression(expression: IrExpression) {
            commitCurrentString()
            expressions += expression
        }

        fun IrType.collectExpressions() {
            check(this@collectExpressions is IrSimpleType)

            val typeAnnotations = (abbreviation?.getAnnotatedAnnotations(InjektFqNames.Qualifier)
                ?: getAnnotatedAnnotations(InjektFqNames.Qualifier)
                .sortedBy { it.type.classifierOrFail.descriptor.fqNameSafe.asString() }) +
                    listOfNotNull(
                        (abbreviation ?: this).annotations.firstOrNull {
                            it.symbol.owner.constructedClass.descriptor.fqNameSafe ==
                                    InjektFqNames.Composable
                        }
                    )

            if (typeAnnotations.isNotEmpty()) {
                appendToCurrentString("[")
                typeAnnotations.forEachIndexed { index, annotation ->
                    appendToCurrentString("@")
                    annotation.type.collectExpressions()
                    if (index != typeAnnotations.lastIndex) appendToCurrentString(", ")
                }
                appendToCurrentString("]")
            }

            when {
                abbreviation != null -> appendToCurrentString(abbreviation!!.typeAlias.descriptor.fqNameSafe.asString())
                classifierOrFail is IrTypeParameterSymbol -> appendTypeParameterExpression(
                    typeParameterKeyExpressions[classifierOrFail.owner]!!(scopes)
                )
                else -> appendToCurrentString(classifierOrFail.descriptor.fqNameSafe.asString())
            }

            val arguments = abbreviation?.arguments ?: arguments

            if (arguments.isNotEmpty()) {
                appendToCurrentString("<")
                arguments.forEachIndexed { index, typeArgument ->
                    if (typeArgument.typeOrNull != null)
                        typeArgument.typeOrNull?.collectExpressions()
                    else appendToCurrentString("*")
                    if (index != arguments.lastIndex) appendToCurrentString(", ")
                }
                appendToCurrentString(">")
            }

            if ((abbreviation != null && abbreviation!!.hasQuestionMark) ||
                (abbreviation == null && hasQuestionMark)) appendToCurrentString("?")
        }

        collectExpressions()
        commitCurrentString()

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
                annotations = annotations + DeclarationIrBuilder(pluginContext, symbol)
                    .jvmNameAnnotation(name, pluginContext)
                correspondingPropertySymbol?.owner?.getter = this
            }
            if (descriptor is PropertySetterDescriptor &&
                annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.setterName(descriptor.correspondingProperty.name.identifier)
                annotations = annotations + DeclarationIrBuilder(pluginContext, symbol)
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

    private fun List<ScopeWithIr>.thisOfClass(declaration: IrClass): IrValueParameter? {
        for (scope in reversed()) {
            when (val element = scope.irElement) {
                is IrFunction ->
                    element.dispatchReceiverParameter?.let { if (it.type.classOrNull == declaration.symbol) return it }
                is IrClass -> if (element == declaration) return element.thisReceiver
            }
        }
        return null
    }
}

