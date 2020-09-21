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

package com.ivianuu.injekt.compiler.backend

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.WeakBindingTrace
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.copyTypeParameters
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Given(IrContext::class)
class ReaderContextParamTransformer : IrLowering {

    private val transformedClasses = mutableSetOf<IrClass>()
    private val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()
    private val newContexts = mutableSetOf<IrClass>()
    private var capturedTypeParameters = emptyList<IrTypeParameter>()
    private val typeParametersMap = mutableMapOf<IrTypeParameter, IrTypeParameter>()
    private inline fun <R> withCapturedTypeParameters(
        typeParameters: List<IrTypeParameter>,
        block: () -> R
    ): R {
        val prev = capturedTypeParameters
        capturedTypeParameters = typeParameters
        val result = block()
        capturedTypeParameters = prev
        return result
    }

    fun getTransformedFunction(function: IrFunction) =
        transformFunctionIfNeeded(function)

    fun getTransformedContext(context: IrClass): IrClass {
        return if (!context.isExternalDeclaration()) context
        else newContexts.singleOrNull {
            it.descriptor.fqNameSafe == context.descriptor.fqNameSafe
        } ?: context
    }

    private val transformer = object : IrElementTransformerVoidWithContext() {
        override fun visitClassNew(declaration: IrClass): IrStatement {
            val transformed = transformClassIfNeeded(declaration)
            return withCapturedTypeParameters(
                if (transformed.canUseReaders()) transformed.typeParameters else capturedTypeParameters
            ) {
                super.visitClassNew(transformed)
            }
        }

        override fun visitFunctionNew(declaration: IrFunction): IrStatement {
            val transformed = transformFunctionIfNeeded(declaration)
            return withCapturedTypeParameters(
                if (transformed.canUseReaders()) transformed.typeParameters else capturedTypeParameters
            ) {
                super.visitFunctionNew(transformed)
            }
        }
    }

    override fun lower() {
        module.transformChildrenVoid(
            object : IrElementTransformerVoidWithContext() {
                override fun visitCall(expression: IrCall): IrExpression {
                    if (expression.symbol.descriptor.fqNameSafe.asString() ==
                        "com.ivianuu.injekt.runReader"
                    ) {
                        (expression.getValueArgument(0) as IrFunctionExpression)
                            .function.annotations += expression.symbol.irBuilder()
                            .irCall(injektSymbols.reader.constructors.single())
                    }
                    return super.visitCall(expression)
                }
            }
        )
        module.transformChildrenVoid(transformer)
        module.rewriteTransformedReferences()

        newContexts
            .filterNot { it.isExternalDeclaration() }
            .forEach { (it.parent as IrFile).addChildAndUpdateMetadata(it) }
    }

    private fun transformClassIfNeeded(clazz: IrClass): IrClass {
        if (clazz in transformedClasses) return clazz

        val readerConstructor = clazz.getReaderConstructor()

        if (!clazz.isMarkedAsReader() && readerConstructor == null) return clazz

        if (readerConstructor == null) return clazz

        if (readerConstructor.getContext() != null) return clazz

        transformedClasses += clazz

        if (clazz.isExternalDeclaration()) {
            val context = getContextFromExternalDeclaration(clazz)!!
            readerConstructor.addValueParameter(
                "_context",
                context.typeWith(readerConstructor.typeParameters.map { it.defaultType })
            )
            return clazz
        }

        val context =
            createContext(
                clazz,
                clazz.descriptor.fqNameSafe,
                readerConstructor.typeParameters
            ).also { newContexts += it }
        val contextParameter = readerConstructor.addContextParameter(context)
        val contextField = clazz.addField(
            fieldName = "_context",
            fieldType = contextParameter.type
        )

        readerConstructor.body = clazz.irBuilder().run {
            irBlockBody {
                readerConstructor.body?.statements?.forEach {
                    +it
                    if (it is IrDelegatingConstructorCall) {
                        +irSetField(
                            irGet(clazz.thisReceiver!!),
                            contextField,
                            irGet(contextParameter)
                        )
                    }
                }
            }
        }

        return clazz
    }

    private fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        if (function.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.given" ||
            function.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.childContext" ||
            function.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.runReader"
        ) return function

        if (function is IrConstructor) {
            return if (function.canUseReaders()) {
                transformClassIfNeeded(function.constructedClass)
                function
            } else function
        }

        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function

        if (!function.canUseReaders()) return function

        if (function.getContext() != null) return function

        if (function.isExternalDeclaration()) {
            val context = getContextFromExternalDeclaration(function)!!
            val transformedFunction = function.copyAsReader()
            transformedFunctions[function] = transformedFunction
            transformedFunction.addValueParameter(
                "_context",
                context.typeWith(transformedFunction.typeParameters.map { it.defaultType })
            )
            return transformedFunction
        }

        val transformedFunction = function.copyAsReader()
        typeParametersMap += transformedFunction.typeParameters
            .zip(function.typeParameters).toMap()
        transformedFunctions[function] = transformedFunction

        transformedFunction.copyTypeParameters(capturedTypeParameters)
        val parameterMap = capturedTypeParameters
            .map { typeParametersMap.getOrElse(it) { it } }
            .zip(transformedFunction.typeParameters.takeLast(capturedTypeParameters.size))
            .toMap()
        given<WeakBindingTrace>().record(
            InjektWritableSlices.TYPE_PARAMETER_MAP,
            transformedFunction as IrSimpleFunction,
            parameterMap
        )

        val context =
            createContext(
                transformedFunction,
                transformedFunction.descriptor.fqNameSafe,
                transformedFunction.typeParameters
            ).also { newContexts += it }
        transformedFunction.addContextParameter(context)

        return transformedFunction
    }

    private fun IrFunction.addContextParameter(context: IrClass): IrValueParameter {
        return addValueParameter(
            name = "_context",
            type = context.typeWith(typeParameters.map { it.defaultType })
        )
    }

    private fun transformCall(
        transformedCallee: IrFunction,
        expression: IrFunctionAccessExpression
    ): IrFunctionAccessExpression {
        return when (expression) {
            is IrConstructorCall -> {
                IrConstructorCallImpl(
                    expression.startOffset,
                    expression.endOffset,
                    transformedCallee.returnType,
                    transformedCallee.symbol as IrConstructorSymbol,
                    expression.typeArgumentsCount,
                    transformedCallee.typeParameters.size,
                    transformedCallee.valueParameters.size,
                    expression.origin
                ).apply {
                    copyTypeAndValueArgumentsFrom(expression)
                }
            }
            is IrDelegatingConstructorCall -> {
                IrDelegatingConstructorCallImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.type,
                    transformedCallee.symbol as IrConstructorSymbol,
                    expression.typeArgumentsCount,
                    transformedCallee.valueParameters.size
                ).apply {
                    copyTypeAndValueArgumentsFrom(expression)
                }
            }
            else -> {
                expression as IrCall
                IrCallImpl(
                    expression.startOffset,
                    expression.endOffset,
                    transformedCallee.returnType,
                    transformedCallee.symbol,
                    expression.origin,
                    expression.superQualifierSymbol
                ).apply {
                    copyTypeAndValueArgumentsFrom(expression)
                }
            }
        }
    }

    private fun IrFunction.copyAsReader(): IrFunction {
        return copy().apply {
            val descriptor = descriptor
            if (descriptor is PropertyGetterDescriptor &&
                annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.getterName(descriptor.correspondingProperty.name.identifier)
                annotations += irBuilder().jvmNameAnnotation(name)
                correspondingPropertySymbol?.owner?.getter = this
            }

            if (descriptor is PropertySetterDescriptor &&
                annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
            ) {
                val name = JvmAbi.setterName(descriptor.correspondingProperty.name.identifier)
                annotations += irBuilder().jvmNameAnnotation(name)
                correspondingPropertySymbol?.owner?.setter = this
            }

            if (this@copyAsReader is IrOverridableDeclaration<*>) {
                overriddenSymbols = this@copyAsReader.overriddenSymbols.map {
                    val owner = it.owner as IrFunction
                    val newOwner = transformFunctionIfNeeded(owner)
                    newOwner.symbol as IrSimpleFunctionSymbol
                }
            }
        }
    }

    private fun getContextFromExternalDeclaration(owner: IrDeclarationWithName): IrClass? {
        return pluginContext.referenceClass(
            owner.getPackageFragment()!!.fqName.child(owner.getContextName())
        )?.owner
    }

    private fun IrModuleFragment.rewriteTransformedReferences() {
        transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression {
                val result = super.visitFunctionExpression(expression) as IrFunctionExpression
                val transformed = transformFunctionIfNeeded(result.function)
                return if (transformed in transformedFunctions.values) IrFunctionExpressionImpl(
                    result.startOffset,
                    result.endOffset,
                    transformed.getFunctionType(),
                    transformed as IrSimpleFunction,
                    result.origin
                )
                else result
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                val result = super.visitFunctionReference(expression) as IrFunctionReference
                val transformed = transformFunctionIfNeeded(result.symbol.owner)
                return if (transformed in transformedFunctions.values) IrFunctionReferenceImpl(
                    result.startOffset,
                    result.endOffset,
                    transformed.getFunctionType(),
                    transformed.symbol,
                    transformed.typeParameters.size,
                    transformed.valueParameters.size,
                    result.reflectionTarget,
                    result.origin
                )
                else result
            }

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression
                if (result !is IrCall &&
                    result !is IrConstructorCall &&
                    result !is IrDelegatingConstructorCall
                ) return result
                val transformed = transformFunctionIfNeeded(result.symbol.owner)
                return if (transformed in transformedFunctions.values) transformCall(
                    transformed,
                    result
                )
                else result
            }
        })
    }

}
