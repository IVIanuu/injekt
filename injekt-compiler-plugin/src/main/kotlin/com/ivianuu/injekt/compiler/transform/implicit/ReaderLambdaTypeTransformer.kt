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

package com.ivianuu.injekt.compiler.transform.implicit

import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.tmpSuspendFunction
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.typeArguments
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedFieldDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isSuspendFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.types.Variance

// branching expressions??

class ReaderLambdaTypeTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val transformedFields = mutableMapOf<IrField, IrField>()
    private val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()
    private val transformedValueParameters = mutableMapOf<IrValueParameter, IrValueParameter>()
    private val transformedVariables = mutableMapOf<IrVariable, IrVariable>()
    private val contexts = mutableListOf<IrClass>()

    override fun lower() {
        module.transformChildrenVoid(object : IrElementTransformerVoid() {

            override fun visitField(declaration: IrField): IrStatement =
                transformFieldIfNeeded(declaration)

            override fun visitFunction(declaration: IrFunction): IrStatement =
                transformFunctionIfNeeded(super.visitFunction(declaration) as IrFunction)

            override fun visitVariable(declaration: IrVariable): IrStatement =
                transformVariableIfNeeded(super.visitVariable(declaration) as IrVariable)

            override fun visitValueParameter(declaration: IrValueParameter): IrStatement =
                transformValueParameterIfNeeded(super.visitValueParameter(declaration) as IrValueParameter)

        })

        val fieldMap = transformedFields
            .mapKeys { it.key.symbol }
        val valueParameterMap = transformedValueParameters
            .mapKeys { it.key.symbol }
        val variableMap = transformedVariables
            .mapKeys { it.key.symbol }

        module.transformChildrenVoid(object : IrElementTransformerVoid() {

            override fun visitReturn(expression: IrReturn): IrExpression {
                val result = super.visitReturn(expression) as IrReturn
                return if (result.value.type.isTransformedReaderLambda() &&
                    result.value.type != result.type
                ) IrReturnImpl(
                    expression.startOffset,
                    expression.endOffset,
                    result.value.type,
                    result.returnTargetSymbol,
                    result.value
                ).copyAttributes(result)
                else result
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val result = super.visitCall(expression) as IrCall
                return if (result.symbol.owner.returnType.isTransformedReaderLambda() &&
                    result.type != result.symbol.owner.returnType
                ) IrCallImpl(
                    result.startOffset,
                    result.endOffset,
                    result.symbol.owner.returnType,
                    result.symbol,
                    result.typeArgumentsCount,
                    result.valueArgumentsCount,
                    result.origin,
                    result.superQualifierSymbol
                ).apply {
                    copyTypeAndValueArgumentsFrom(result)
                    copyAttributes(expression)
                } else result
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                return (variableMap[expression.symbol] ?: valueParameterMap[expression.symbol])
                    ?.let {
                        IrGetValueImpl(
                            expression.startOffset,
                            expression.endOffset,
                            it.symbol,
                            expression.origin
                        )
                    }
                    ?: super.visitGetValue(expression)
            }

            override fun visitGetField(expression: IrGetField): IrExpression {
                return fieldMap[expression.symbol]
                    ?.let {
                        IrGetFieldImpl(
                            expression.startOffset,
                            expression.endOffset,
                            it.symbol,
                            it.type,
                            expression.receiver,
                            expression.origin,
                            expression.superQualifierSymbol
                        )
                    } ?: super.visitGetField(expression)
            }

            override fun visitSetField(expression: IrSetField): IrExpression {
                return fieldMap[expression.symbol]
                    ?.let {
                        IrSetFieldImpl(
                            expression.startOffset,
                            expression.endOffset,
                            it.symbol,
                            expression.receiver,
                            expression.value,
                            it.type,
                            expression.origin,
                            expression.superQualifierSymbol
                        )
                    } ?: super.visitSetField(expression)
            }
        })

        contexts.forEach { context ->
            val parent = context.parent as IrDeclarationContainer
            if (context !in parent.declarations) {
                parent.addChild(context)
            }
        }
    }

    private fun transformFieldIfNeeded(
        declaration: IrField
    ): IrField {
        val type = declaration.type
        if (!type.isReaderLambda()) return declaration
        if (type.isTransformedReaderLambda()) return declaration

        transformedFields[declaration]?.let { return it }
        if (declaration in transformedFields.values) return declaration

        val newType = createReaderLambdaType(declaration.type, declaration)

        return IrFieldImpl(
            declaration.startOffset,
            declaration.endOffset,
            declaration.origin,
            IrFieldSymbolImpl(WrappedFieldDescriptor()),
            declaration.name,
            newType,
            declaration.visibility,
            declaration.isFinal,
            declaration.isExternal,
            declaration.isStatic,
            declaration.isFakeOverride
        ).apply {
            (descriptor as WrappedFieldDescriptor).bind(this)
            correspondingPropertySymbol = declaration.correspondingPropertySymbol
            correspondingPropertySymbol?.owner?.backingField = this
            annotations += declaration.annotations
            initializer = declaration.initializer
            transformedFields[declaration] = this
        }
    }

    private fun transformFunctionIfNeeded(
        declaration: IrFunction
    ): IrFunction {
        val returnType = declaration.returnType
        if (!returnType.isReaderLambda()) return declaration
        if (returnType.isTransformedReaderLambda()) return declaration

        transformedFunctions[declaration]?.let { return it }
        if (declaration in transformedFunctions.values) return declaration
        transformedFunctions[declaration] = declaration

        declaration.returnType = createReaderLambdaType(returnType, declaration)

        return declaration
    }

    private fun transformVariableIfNeeded(
        declaration: IrVariable
    ): IrVariable {
        val type = declaration.type
        if (!type.isReaderLambda()) return declaration
        if (type.isTransformedReaderLambda()) return declaration

        transformedVariables[declaration]?.let { return it }
        if (declaration in transformedVariables.values) return declaration

        val newType = createReaderLambdaType(declaration.type, declaration)

        return IrVariableImpl(
            declaration.startOffset,
            declaration.endOffset,
            declaration.origin,
            IrVariableSymbolImpl(WrappedVariableDescriptor()),
            declaration.name,
            newType,
            declaration.isVar,
            declaration.isConst,
            declaration.isLateinit
        ).apply {
            (descriptor as WrappedVariableDescriptor).bind(this)
            annotations += declaration.annotations
            initializer = declaration.initializer
            transformedVariables[declaration] = this
        }
    }

    private fun transformValueParameterIfNeeded(
        declaration: IrValueParameter
    ): IrValueParameter {
        val type = declaration.type
        if (!type.isReaderLambda()) return declaration
        if (type.isTransformedReaderLambda()) return declaration

        transformedValueParameters[declaration]?.let { return it }
        if (declaration in transformedValueParameters.values) return declaration

        val newType = createReaderLambdaType(type, declaration)

        return IrValueParameterImpl(
            declaration.startOffset, declaration.endOffset,
            declaration.origin,
            IrValueParameterSymbolImpl(WrappedValueParameterDescriptor()),
            declaration.name,
            declaration.index,
            newType,
            declaration.varargElementType,
            declaration.isCrossinline,
            declaration.isNoinline
        ).apply {
            (descriptor as WrappedValueParameterDescriptor).bind(this)
            annotations += declaration.annotations
            defaultValue = declaration.defaultValue
            transformedValueParameters[declaration] = this
        }
    }

    private fun createReaderLambdaType(
        oldType: IrType,
        declaration: IrDeclarationWithName
    ): IrType {
        val context = createContext(declaration, null, pluginContext, symbols)
        contexts += context

        val oldTypeArguments = oldType.typeArguments

        return (if (oldType.isSuspendFunction())
            pluginContext.tmpSuspendFunction(oldTypeArguments.size) else
            pluginContext.tmpFunction(oldTypeArguments.size))
            .defaultType
            .let {
                IrSimpleTypeImpl(
                    it.classifierOrFail,
                    oldType.isMarkedNullable(),
                    oldTypeArguments.subList(0, oldTypeArguments.size - 1) +
                            makeTypeProjection(context.defaultType, Variance.INVARIANT) +
                            oldTypeArguments.last(),
                    oldType.annotations,
                    (oldType as? IrSimpleType)?.abbreviation
                )
            }
    }

}
