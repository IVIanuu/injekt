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

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.canUseImplicits
import com.ivianuu.injekt.compiler.copy
import com.ivianuu.injekt.compiler.getFunctionType
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.getReaderConstructor
import com.ivianuu.injekt.compiler.irTrace
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.isMarkedAsImplicit
import com.ivianuu.injekt.compiler.isReaderLambdaInvoke
import com.ivianuu.injekt.compiler.jvmNameAnnotation
import com.ivianuu.injekt.compiler.remapTypeParametersByName
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.tmpSuspendFunction
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.Indexer
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.uniqueName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.remapTypeParameters
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
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
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
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
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.hasDefaultValue
import org.jetbrains.kotlin.ir.util.isSuspendFunction
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.Variance

class ImplicitContextParamTransformer(
    pluginContext: IrPluginContext,
    private val indexer: Indexer
) : AbstractInjektTransformer(pluginContext) {

    private val transformedClasses = mutableSetOf<IrClass>()
    private val transformedFields = mutableMapOf<IrField, IrField>()
    private val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()
    private val transformedValueParameters = mutableMapOf<IrValueParameter, IrValueParameter>()
    private val transformedVariables = mutableMapOf<IrVariable, IrVariable>()

    private val newDeclarations = mutableListOf<IrDeclaration>()
    private val nameProvider = NameProvider()

    fun getTransformedFunction(function: IrFunction) =
        transformFunctionIfNeeded(function)

    private val transformer = object : IrElementTransformerVoid() {

        override fun visitClass(declaration: IrClass): IrStatement =
            super.visitClass(transformClassIfNeeded(declaration))

        override fun visitField(declaration: IrField): IrStatement =
            super.visitField(transformFieldIfNeeded(declaration))

        override fun visitFunction(declaration: IrFunction): IrStatement =
            super.visitFunction(transformFunctionIfNeeded(declaration))

        override fun visitValueParameter(declaration: IrValueParameter): IrStatement =
            super.visitValueParameter(transformValueParameterIfNeeded(declaration))

        override fun visitVariable(declaration: IrVariable): IrStatement =
            super.visitVariable(transformVariableIfNeeded(declaration))

    }

    override fun lower() {
        module.transformChildrenVoid(transformer)

        module.rewriteTransformedReferences()

        newDeclarations.forEach { newDeclaration ->
            val parent = newDeclaration.parent as IrDeclarationContainer
            if (newDeclaration !in parent.declarations) {
                parent.addChild(newDeclaration)
                if (newDeclaration is IrFunction) {
                    indexer.index(newDeclaration)
                }
            }
        }
    }

    private fun transformClassIfNeeded(clazz: IrClass): IrClass {
        if (clazz in transformedClasses) return clazz

        val readerConstructor = clazz.getReaderConstructor(pluginContext)

        if (!clazz.isMarkedAsImplicit(pluginContext) && readerConstructor == null) return clazz

        if (readerConstructor == null) return clazz

        transformedClasses += clazz

        val existingSignature = getExternalReaderSignature(clazz)

        if (clazz.isExternalDeclaration() || existingSignature != null) {
            readerConstructor.copySignatureFrom(existingSignature!!)
            return clazz
        }

        lateinit var contextField: IrField
        lateinit var contextParameter: IrValueParameter

        transformImplicitFunction(
            owner = clazz,
            ownerFunction = readerConstructor,
            onContextParameterCreated = {
                contextParameter = it
                contextField = clazz.addField(
                    fieldName = "_context",
                    fieldType = it.type
                )
            }
        )

        val signature = createReaderSignature(clazz, readerConstructor, null)
        newDeclarations += signature

        readerConstructor.body = DeclarationIrBuilder(pluginContext, clazz.symbol).run {
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
        if (function.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.given")
            return function

        if (function is IrConstructor) {
            return if (function.canUseImplicits(pluginContext)) {
                transformClassIfNeeded(function.constructedClass)
                function
            } else function
        }

        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function

        val existingSignature = getExternalReaderSignature(function)

        if (function.isExternalDeclaration() || existingSignature != null) {
            if (existingSignature == null &&
                !function.canUseImplicits(pluginContext)
            ) return function
            val transformedFunction = function.copyAsReader()
            transformedFunctions[function] = transformedFunction
            transformedFunction.copySignatureFrom(existingSignature!!)
            return transformedFunction
        }

        var needsTransform = function.canUseImplicits(pluginContext)

        if (!needsTransform) {
            val returnType = function.returnType
            needsTransform = returnType.isReaderLambda() && !returnType.isTransformedReaderLambda()
        }

        if (!needsTransform) {
            needsTransform = function.valueParameters.any {
                it.type.isReaderLambda() && !it.type.isTransformedReaderLambda()
            }
        }

        if (!needsTransform) return function

        val transformedFunction = function.copyAsReader()
            .also { it.transformChildrenVoid(transformer) }
        transformedFunctions[function] = transformedFunction

        if (function.returnType.isReaderLambda() &&
            !function.returnType.isTransformedReaderLambda()
        ) {
            transformedFunction.returnType =
                createReaderLambdaType(transformedFunction.returnType, transformedFunction)
        }

        if (function.canUseImplicits(pluginContext)) {
            transformImplicitFunction(
                owner = transformedFunction,
                ownerFunction = transformedFunction
            )
        }

        val signature = createReaderSignature(transformedFunction, transformedFunction, null)
        newDeclarations += signature

        return transformedFunction
    }

    private fun <T> transformImplicitFunction(
        owner: T,
        ownerFunction: IrFunction,
        onContextParameterCreated: (IrValueParameter) -> Unit = {}
    ) where T : IrDeclarationWithName, T : IrDeclarationWithVisibility, T : IrDeclarationParent, T : IrTypeParametersContainer {
        val parentFunction =
            if (owner.visibility == Visibilities.LOCAL && owner.parent is IrFunction)
                owner.parent as IrFunction else null

        val context = createContext(owner, parentFunction, pluginContext, symbols)
        newDeclarations += context
        val contextParameter = ownerFunction.addContextParameter(context)
        onContextParameterCreated(contextParameter)
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
            parent = declaration.parent
            correspondingPropertySymbol = declaration.correspondingPropertySymbol
            correspondingPropertySymbol?.owner?.backingField = this
            annotations += declaration.annotations
            initializer = declaration.initializer
            transformedFields[declaration] = this
        }
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
            parent = declaration.parent
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
            parent = declaration.parent
            annotations += declaration.annotations
            defaultValue = declaration.defaultValue
            transformedValueParameters[declaration] = this
        }
    }

    private fun createReaderLambdaType(
        oldType: IrType,
        declaration: IrDeclarationWithName
    ): IrType {
        val context = createContext(
            declaration,
            null,
            pluginContext,
            symbols
        )
        newDeclarations += context

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

    private fun IrFunction.addContextParameter(context: IrClass): IrValueParameter {
        return addValueParameter(
            name = "_context",
            type = context.typeWith(
                typeParameters.map { it.defaultType }
            )
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
        return copy(pluginContext, IMPLICIT_CONTEXT_PARAM_ORIGIN).apply {
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

            if (this@copyAsReader is IrOverridableDeclaration<*>) {
                overriddenSymbols = this@copyAsReader.overriddenSymbols.map {
                    val owner = it.owner as IrFunction
                    val newOwner = transformFunctionIfNeeded(owner)
                    newOwner.symbol as IrSimpleFunctionSymbol
                }
            }
        }
    }

    private fun getExternalReaderSignature(owner: IrDeclarationWithName): IrFunction? {
        val declaration = if (owner is IrConstructor)
            owner.constructedClass else owner
        return indexer.externalFunctionIndices
            .filter { it.hasAnnotation(InjektFqNames.Signature) }
            .filter { it.hasAnnotation(InjektFqNames.Name) }
            .singleOrNull { function ->
                function.getAnnotation(InjektFqNames.Name)!!
                    .getValueArgument(0)!!
                    .let { it as IrConst<String> }
                    .value == declaration.uniqueName()
            }
    }

    private fun IrFunction.copySignatureFrom(signature: IrFunction) {
        valueParameters = signature.valueParameters.map {
            it.copyTo(
                this,
                type = it.type,
                varargElementType = it.varargElementType
            )
        }
    }

    private fun createReaderSignature(
        owner: IrDeclarationWithName,
        ownerFunction: IrFunction,
        parentFunction: IrFunction?
    ) = buildFun {
        this.name = nameProvider.allocateForGroup(
            getJoinedName(
                owner.getPackageFragment()!!.fqName,
                owner.descriptor.fqNameSafe
                    .parent().child(owner.name.asString().asNameId())
            ).asString() + "${owner.uniqueName().hashCode()}Signature"
        ).asNameId()
        visibility = Visibilities.INTERNAL
    }.apply {
        parent = owner.file
        addMetadataIfNotLocal()

        copyTypeParametersFrom(owner as IrTypeParametersContainer)
        //parentFunction?.let { copyTypeParametersFrom(it) }

        annotations += DeclarationIrBuilder(pluginContext, symbol)
            .irCall(symbols.signature.constructors.single())

        annotations += DeclarationIrBuilder(pluginContext, symbol).run {
            irCall(
                symbols.name.constructors.single()
            ).apply {
                putValueArgument(
                    0,
                    irString(owner.uniqueName())
                )
            }
        }

        returnType = ownerFunction.returnType
            .remapTypeParametersByName(owner, this)
            .let {
                if (parentFunction != null) it.remapTypeParametersByName(
                    parentFunction, this
                ) else it
            }

        valueParameters = ownerFunction.valueParameters.map {
            it.copyTo(
                this,
                type = it.type
                    .remapTypeParametersByName(owner, this)
                    .let {
                        if (parentFunction != null) it.remapTypeParameters(
                            parentFunction, this
                        ) else it
                    },
                varargElementType = it.varargElementType
                    ?.remapTypeParametersByName(owner, this)
                    ?.let {
                        if (parentFunction != null) it.remapTypeParameters(
                            parentFunction, this
                        ) else it
                    },
                defaultValue = if (it.hasDefaultValue()) DeclarationIrBuilder(
                    pluginContext,
                    it.symbol
                ).run {
                    irExprBody(
                        irCall(
                            pluginContext.referenceFunctions(
                                FqName("com.ivianuu.injekt.internal.injektIntrinsic")
                            )
                                .single()
                        ).apply {
                            putTypeArgument(0, it.type)
                        }
                    )
                } else null
            )
        }

        body = DeclarationIrBuilder(pluginContext, symbol).run {
            irExprBody(
                irCall(
                    pluginContext.referenceFunctions(
                        FqName("com.ivianuu.injekt.internal.injektIntrinsic")
                    )
                        .single()
                ).apply {
                    putTypeArgument(0, returnType)
                }
            )
        }
    }

    private fun IrElement.rewriteTransformedReferences() {
        val fieldMap = transformedFields
            .mapKeys { it.key.symbol }
        val valueParameterMap = transformedValueParameters
            .mapKeys { it.key.symbol }
        val variableMap = transformedVariables
            .mapKeys { it.key.symbol }

        transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression {
                val result = super.visitFunctionExpression(expression) as IrFunctionExpression
                val transformed = transformFunctionIfNeeded(result.function)
                return if (transformed in transformedFunctions.values) IrFunctionExpressionImpl(
                    result.startOffset,
                    result.endOffset,
                    result.function.getFunctionType(pluginContext),
                    result.function,
                    result.origin
                )
                else result
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                val result = super.visitFunctionReference(expression) as IrFunctionReference
                val transformed = transformFunctionIfNeeded(result.symbol.owner)
                return if (transformed in transformedFunctions.values) IrFunctionExpressionImpl(
                    result.startOffset,
                    result.endOffset,
                    result.symbol.owner.getFunctionType(pluginContext),
                    result.symbol.owner as IrSimpleFunction,
                    result.origin!!
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
                return (if (transformed in transformedFunctions.values) transformCall(
                    transformed,
                    result
                )
                else result)
                    .also {
                        if (it.isReaderLambdaInvoke(pluginContext)) {
                            pluginContext.irTrace.record(
                                InjektWritableSlices.IS_READER_LAMBDA_INVOKE,
                                it,
                                true
                            )
                        }
                    }
            }

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
    }

}

object IMPLICIT_CONTEXT_PARAM_ORIGIN : IrDeclarationOrigin