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

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.isForTypeKey
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.cast

class TypeKeyTransformer(
    private val context: InjektContext,
    private val trace: BindingTrace,
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {

    private val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()

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
        if (clazz.descriptor.declaredTypeParameters.none { it.isForTypeKey(context, trace) })
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
            if (!function.descriptor.isExternalDeclaration()) return function
            val typeKeyParameters = function.constructedClass
                .descriptor
                .toClassifierRef(context, trace)
                .forTypeKeyTypeParameters
            typeKeyParameters.forEach {
                function.addValueParameter(
                    "_${it}TypeKey",
                    pluginContext.irBuiltIns.stringType
                )
            }
            transformedFunctions[function] = function
            return function
        }

        if (function.descriptor.typeParameters.none {
                it.isForTypeKey(context, trace)
        })
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
            if (callee.constructedClass.descriptor.declaredTypeParameters.none {
                    it.isForTypeKey(context, trace)
                }) return expression
        } else {
            if (callee.descriptor.typeParameters.none {
                    it.isForTypeKey(context, trace)
                }) return expression
        }

        val transformedCallee = transformFunctionIfNeeded(callee)
        if (expression is IrCall &&
            expression.symbol == transformedCallee.symbol) return expression
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
        fun IrType.collectExpressions() {
            check(this@collectExpressions is IrSimpleType)

            val typeAnnotations = listOfNotNull(
                if ((abbreviation?.hasAnnotation(InjektFqNames.Given) ?:
                    hasAnnotation(InjektFqNames.Given))) "@Given" else null,
                if ((abbreviation?.hasAnnotation(InjektFqNames.Composable) ?:
                    hasAnnotation(InjektFqNames.Composable))) "@Composable" else null,
                *(abbreviation?.getAnnotatedAnnotations(InjektFqNames.Qualifier) ?:
                    getAnnotatedAnnotations(InjektFqNames.Qualifier))
                    .map { qualifier ->
                        "@" + qualifier.type.classifierOrFail.descriptor.fqNameSafe.asString() +
                                if (qualifier.valueArgumentsCount > 0) {
                                    (0 until qualifier.valueArgumentsCount)
                                        .map { i -> qualifier.getValueArgument(i) as IrConst<*> }
                                        .map { it.value }
                                        .hashCode()
                                        .toString()
                                        .let { "($it)" }
                                } else ""
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
                    expressions += typeParameterKeyExpressions[classifierOrFail.owner]
                        ?.invoke(scopes)
                        ?: error("")
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

