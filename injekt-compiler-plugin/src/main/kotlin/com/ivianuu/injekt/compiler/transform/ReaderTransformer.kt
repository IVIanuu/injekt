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
import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.copy
import com.ivianuu.injekt.compiler.distinctedType
import com.ivianuu.injekt.compiler.dumpSrc
import com.ivianuu.injekt.compiler.flatMapFix
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.getReaderConstructor
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.isReader
import com.ivianuu.injekt.compiler.jvmNameAnnotation
import com.ivianuu.injekt.compiler.readableName
import com.ivianuu.injekt.compiler.remapTypeParameters
import com.ivianuu.injekt.compiler.substitute
import com.ivianuu.injekt.compiler.thisOfClass
import com.ivianuu.injekt.compiler.transform.component.getReaderInfo
import com.ivianuu.injekt.compiler.transform.component.getReaderSignature
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.uniqueName
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.hasDefaultValue
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

// todo dedup transform code
class ReaderTransformer(pluginContext: IrPluginContext) : AbstractInjektTransformer(pluginContext) {

    private val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()
    private val transformedClasses = mutableSetOf<IrClass>()

    private val readerInfos = mutableSetOf<IrClass>()

    private val globalNameProvider = NameProvider()

    fun getTransformedFunction(reader: IrFunction) =
        transformFunctionIfNeeded(reader)

    override fun lower() {
        module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.owner.descriptor.fqNameSafe.asString() ==
                    "com.ivianuu.injekt.runReader"
                ) {
                    (expression.getValueArgument(0) as IrFunctionExpression)
                        .function.annotations += DeclarationIrBuilder(
                        pluginContext,
                        expression.symbol
                    ).irCall(symbols.reader.constructors.single())
                }
                return super.visitCall(expression)
            }
        })

        module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement =
                transformClassIfNeeded(super.visitClass(declaration) as IrClass)
        })

        module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement =
                transformFunctionIfNeeded(super.visitFunction(declaration) as IrFunction)
        })

        readerInfos
            .filterNot { it.isExternalDeclaration() }
            .forEach { readerInfo ->
                val parent = readerInfo.parent as IrDeclarationContainer
                if (readerInfo !in parent.declarations) {
                    parent.addChild(readerInfo)
                }
            }
    }

    private fun transformClassIfNeeded(clazz: IrClass): IrClass {
        if (clazz in transformedClasses) return clazz

        val readerConstructor = clazz.getReaderConstructor()

        if (!clazz.hasAnnotation(InjektFqNames.Reader) &&
            readerConstructor == null
        ) return clazz

        if (readerConstructor == null) return clazz

        transformedClasses += clazz

        if (readerConstructor.valueParameters.any { it.hasAnnotation(InjektFqNames.Implicit) })
            return clazz

        if (clazz.isExternalDeclaration()) {
            val readerInfo = pluginContext.getReaderInfo(clazz)!!
            readerInfos += readerInfo

            readerConstructor.copySignatureFrom(readerInfo) {
                it.remapTypeParameters(readerInfo, clazz)
            }

            return clazz
        }

        val givenCalls = mutableListOf<IrCall>()
        val defaultValueParameterByGivenCalls = mutableMapOf<IrCall, IrValueParameter>()
        val readerCalls = mutableListOf<IrFunctionAccessExpression>()

        clazz.transformChildrenVoid(object : IrElementTransformerVoid() {

            private val functionStack = mutableListOf<IrFunction>()
            private val valueParameterStack = mutableListOf<IrValueParameter>()

            override fun visitFunction(declaration: IrFunction): IrStatement {
                val isReader = declaration.isReader() && declaration != readerConstructor
                if (isReader) functionStack.push(declaration)
                return super.visitFunction(declaration)
                    .also { if (isReader) functionStack.pop() }
            }

            override fun visitValueParameter(declaration: IrValueParameter): IrStatement {
                valueParameterStack += declaration
                return super.visitValueParameter(declaration)
                    .also { valueParameterStack -= declaration }
            }

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression
                if (functionStack.isNotEmpty()) return result
                if (result !is IrCall &&
                    result !is IrConstructorCall &&
                    result !is IrDelegatingConstructorCall
                ) return result
                if (expression.symbol.owner.isReader()) {
                    if (result is IrCall &&
                        result.symbol.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.given"
                    ) {
                        givenCalls += result
                        valueParameterStack.lastOrNull()
                            ?.takeIf { it.type == result.type }
                            ?.let {
                                defaultValueParameterByGivenCalls[result] = it
                            }
                    } else {
                        readerCalls += result
                    }
                }
                return result
            }

        })

        val readerInfo = buildClass {
            kind = ClassKind.INTERFACE
            name = globalNameProvider.getInfoClassName(clazz)
        }.apply {
            parent = clazz.file
            createImplicitParameterDeclarationWithWrappedDescriptor()
            addMetadataIfNotLocal()
            copyTypeParametersFrom(clazz)

            annotations += DeclarationIrBuilder(pluginContext, symbol).run {
                irCall(symbols.name.constructors.single()).apply {
                    putValueArgument(
                        0,
                        irString(clazz.uniqueName())
                    )
                }
            }

            readerInfos += this
        }

        val givenTypes = mutableMapOf<Any, IrType>()
        val callsByTypes = mutableMapOf<Any, IrFunctionAccessExpression>()

        givenCalls
            .forEach { givenCall ->
                val type = givenCall.type
                givenTypes[type.distinctedType] = type
                callsByTypes[type.distinctedType] = givenCall
            }
        readerCalls.flatMapFix { readerCall ->
            val transformedCallee = transformFunctionIfNeeded(readerCall.symbol.owner)

            transformedCallee
                .valueParameters
                .filter { it.hasAnnotation(InjektFqNames.Implicit) }
                .map { it.type }
                .map {
                    it
                        .remapTypeParameters(readerCall.symbol.owner, transformedCallee)
                        .substitute(
                            transformedCallee.typeParameters.map { it.symbol }
                                .zip(readerCall.typeArguments)
                                .toMap()
                        )
                }
                .map { readerCall to it }
        }.forEach { (call, type) ->
            givenTypes[type.distinctedType] = type
            callsByTypes[type.distinctedType] = call
        }

        val givenFields = mutableMapOf<Any, IrField>()
        val valueParametersByFields = mutableMapOf<IrField, IrValueParameter>()

        givenTypes.values.forEach { givenType ->
            val field = clazz.addField(
                fieldName = givenType.readableName(),
                fieldType = givenType
            )
            givenFields[givenType.distinctedType] = field

            val call = callsByTypes[givenType.distinctedType]
            val defaultValueParameter = defaultValueParameterByGivenCalls[call]

            val valueParameter = (defaultValueParameter ?: readerConstructor.addValueParameter(
                field.name.asString(),
                field.type
            )).apply {
                annotations += DeclarationIrBuilder(pluginContext, symbol)
                    .irCall(symbols.implicit.constructors.single())
            }
            valueParametersByFields[field] = valueParameter
        }

        readerInfo.addSignature(readerConstructor) {
            it.remapTypeParameters(clazz, readerInfo)
        }

        readerConstructor.body = DeclarationIrBuilder(pluginContext, clazz.symbol).run {
            irBlockBody {
                readerConstructor.body?.statements?.forEach {
                    +it
                    if (it is IrDelegatingConstructorCall) {
                        valueParametersByFields.forEach { (field, valueParameter) ->
                            +irSetField(
                                irGet(clazz.thisReceiver!!),
                                field,
                                irGet(valueParameter)
                            )
                        }
                    }
                }
            }
        }

        rewriteCalls(
            clazz,
            givenCalls,
            readerCalls
        ) { type, expression ->
            val finalType = type.substitute(
                transformFunctionIfNeeded(expression.symbol.owner)
                    .typeParameters
                    .map { it.symbol }
                    .zip(expression.typeArguments)
                    .toMap()
            ).distinctedType
            val field = givenFields[finalType]!!

            return@rewriteCalls { scopes ->
                if (scopes.none { it.irElement == readerConstructor }) {
                    irGetField(
                        irGet(scopes.thisOfClass(clazz)!!),
                        field
                    )
                } else {
                    irGet(valueParametersByFields.getValue(field))
                }
            }
        }

        return clazz
    }

    private fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        if (function is IrConstructor) {
            return if (function.isReader()) {
                transformClassIfNeeded(function.constructedClass)
                    .getReaderConstructor()!!
            } else function
        }

        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function

        if (!function.isReader()) return function

        if (function.valueParameters.any { it.hasAnnotation(InjektFqNames.Implicit) }) {
            transformedFunctions[function] = function
            return function
        }

        if (function.isExternalDeclaration()) {
            val transformedFunction = function.copyAsReader()
            transformedFunctions[function] = transformedFunction

            if (transformedFunction.descriptor.fqNameSafe.asString() != "com.ivianuu.injekt.given") {
                val readerInfo = pluginContext.getReaderInfo(transformedFunction)!!
                readerInfos += readerInfo

                transformedFunction.copySignatureFrom(readerInfo) {
                    it.remapTypeParameters(readerInfo, transformedFunction)
                }
            }

            return transformedFunction
        }

        val transformedFunction = function.copyAsReader()
        transformedFunctions[function] = transformedFunction

        val readerInfo = buildClass {
            kind = ClassKind.INTERFACE
            name = globalNameProvider.getInfoClassName(function)
        }.apply {
            parent = function.file
            createImplicitParameterDeclarationWithWrappedDescriptor()
            addMetadataIfNotLocal()
            copyTypeParametersFrom(transformedFunction)

            annotations += DeclarationIrBuilder(pluginContext, symbol).run {
                irCall(symbols.name.constructors.single()).apply {
                    putValueArgument(
                        0,
                        irString(transformedFunction.uniqueName())
                    )
                }
            }

            readerInfos += this
        }

        val givenCalls = mutableListOf<IrCall>()
        val defaultValueParameterByGivenCalls = mutableMapOf<IrCall, IrValueParameter>()
        val readerCalls = mutableListOf<IrFunctionAccessExpression>()

        transformedFunction.transformChildrenVoid(object : IrElementTransformerVoid() {

            private val functionStack = mutableListOf(transformedFunction)
            private val valueParameterStack = mutableListOf<IrValueParameter>()

            override fun visitFunction(declaration: IrFunction): IrStatement {
                val isReader = declaration.isReader()
                if (isReader) functionStack.push(declaration)
                return super.visitFunction(declaration)
                    .also { if (isReader) functionStack.pop() }
            }

            override fun visitValueParameter(declaration: IrValueParameter): IrStatement {
                valueParameterStack += declaration
                return super.visitValueParameter(declaration)
                    .also { valueParameterStack -= declaration }
            }

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression
                if (result !is IrCall &&
                    result !is IrConstructorCall &&
                    result !is IrDelegatingConstructorCall
                ) return result
                if (functionStack.lastOrNull() != transformedFunction) return result
                if (expression.symbol.owner.isReader()) {
                    if (result is IrCall &&
                        expression.symbol.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.given"
                    ) {
                        givenCalls += result
                        valueParameterStack.lastOrNull()
                            ?.takeIf { it.type == result.type }
                            ?.let {
                                defaultValueParameterByGivenCalls[result] = it
                            }
                    } else {
                        readerCalls += result
                    }
                }
                return result
            }
        })

        val givenTypes = mutableMapOf<Any, IrType>()
        val callsByTypes = mutableMapOf<Any, IrFunctionAccessExpression>()

        givenCalls
            .forEach { givenCall ->
                val type = givenCall.type
                    .remapTypeParameters(function, transformedFunction)
                givenTypes[type.distinctedType] = type
                callsByTypes[type.distinctedType] = givenCall
            }
        readerCalls.flatMapFix { readerCall ->
            val transformedCallee = transformFunctionIfNeeded(readerCall.symbol.owner)

            println("found reader call ${readerCall.dumpSrc()}\n${transformedCallee.dumpSrc()}")

            transformedCallee
                .valueParameters
                .filter { it.hasAnnotation(InjektFqNames.Implicit) }
                .map { it.type }
                .map {
                    it
                        .remapTypeParameters(readerCall.symbol.owner, transformedCallee)
                        .substitute(
                            transformedCallee.typeParameters.map { it.symbol }
                                .zip(
                                    readerCall.typeArguments
                                        .map {
                                            it.remapTypeParameters(
                                                function, transformedFunction
                                            )
                                        }
                                )
                                .toMap()
                        )
                }
                .map { readerCall to it }
        }.forEach { (call, type) ->
            givenTypes[type.distinctedType] = type
            callsByTypes[type.distinctedType] = call
        }

        val givenValueParameters = mutableMapOf<Any, IrValueParameter>()

        givenTypes.values.forEach { givenType ->
            val call = callsByTypes[givenType.distinctedType]
            val defaultValueParameter = defaultValueParameterByGivenCalls[call]

            val valueParameter = (defaultValueParameter ?: transformedFunction.addValueParameter(
                name = givenType.readableName().asString(),
                type = givenType
            )).apply {
                annotations += DeclarationIrBuilder(pluginContext, symbol)
                    .irCall(symbols.implicit.constructors.single())
            }
            givenValueParameters[givenType.distinctedType] = valueParameter
        }

        readerInfo.addSignature(transformedFunction) {
            it
                .remapTypeParameters(function, transformedFunction)
                .remapTypeParameters(transformedFunction, readerInfo)
        }

        rewriteCalls(
            transformedFunction,
            givenCalls,
            readerCalls
        ) { type, expression ->
            val finalType = type
                .remapTypeParameters(function, transformedFunction)
                .substitute(
                    transformFunctionIfNeeded(expression.symbol.owner)
                        .typeParameters
                        .map { it.symbol }
                        .zip(
                            expression.typeArguments
                                .map { it.remapTypeParameters(function, transformedFunction) }
                        )
                        .toMap()
                )
                .distinctedType
            val valueParameter = givenValueParameters[finalType]!!
            return@rewriteCalls { irGet(valueParameter) }
        }

        println("transformed ${function.dumpSrc()}\n${transformedFunction.dumpSrc()}")

        return transformedFunction
    }

    private fun IrFunction.copySignatureFrom(
        readerInfo: IrClass,
        remapType: (IrType) -> IrType
    ) {
        val signature = readerInfo.getReaderSignature()

        val implicitIndices = signature.getAnnotation(InjektFqNames.Implicits)!!
            .getValueArgument(0)
            .let { it as IrVarargImpl }
            .elements
            .map { it as IrConst<Int> }
            .map { it.value }

        valueParameters = signature.valueParameters.map {
            it.copyTo(
                this,
                type = remapType(it.type),
                varargElementType = it.varargElementType?.let(remapType)
            )
        }.onEach {
            if (it.index in implicitIndices) {
                it.annotations += DeclarationIrBuilder(pluginContext, it.symbol)
                    .irCall(symbols.implicit.constructors.single())
            }
        }
    }

    private fun IrClass.addSignature(
        function: IrFunction,
        remapType: (IrType) -> IrType
    ) {
        addFunction(
            name = "signature",
            returnType = remapType(function.returnType),
            modality = Modality.ABSTRACT
        ).apply {
            addMetadataIfNotLocal()

            annotations += DeclarationIrBuilder(pluginContext, symbol).run {
                irCall(symbols.implicits.constructors.single())
                    .apply {
                        val intArray = pluginContext.referenceClass(
                            FqName("kotlin.IntArray")
                        )!!
                        putValueArgument(
                            0,
                            IrVarargImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                intArray.defaultType,
                                irBuiltIns.intType,
                                function.valueParameters
                                    .filter { it.hasAnnotation(InjektFqNames.Implicit) }
                                    .map { it.index }
                                    .map { irInt(it) }
                            )
                        )
                    }
            }

            dispatchReceiverParameter = thisReceiver!!.copyTo(this)

            valueParameters = function.valueParameters.map {
                it.copyTo(
                    this,
                    type = remapType(it.type),
                    varargElementType = it.varargElementType?.let(remapType),
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
        }
    }

    private fun <T> rewriteCalls(
        owner: T,
        givenCalls: List<IrCall>,
        readerCalls: List<IrFunctionAccessExpression>,
        provider: (IrType, IrFunctionAccessExpression) -> IrBuilderWithScope.(List<ScopeWithIr>) -> IrExpression
    ) where T : IrDeclaration, T : IrDeclarationParent {
        owner.transform(object : IrElementTransformerVoidWithContext() {
            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression
                return when (result) {
                    in givenCalls -> {
                        provider(result.getTypeArgument(0)!!, result)(
                            DeclarationIrBuilder(pluginContext, result.symbol),
                            allScopes
                        )
                    }
                    in readerCalls -> {
                        val transformedCallee = transformFunctionIfNeeded(result.symbol.owner)
                        fun IrFunctionAccessExpression.fillGivenParameters() {
                            transformedCallee.valueParameters.forEach { valueParameter ->
                                val valueArgument = getValueArgument(valueParameter.index)
                                if (valueParameter.hasAnnotation(InjektFqNames.Implicit) &&
                                    valueArgument == null
                                ) {
                                    putValueArgument(
                                        valueParameter.index,
                                        provider(
                                            valueParameter.type,
                                            result
                                        )(
                                            DeclarationIrBuilder(pluginContext, result.symbol),
                                            allScopes
                                        )
                                    )
                                }
                            }
                        }
                        when (result) {
                            is IrConstructorCall -> {
                                IrConstructorCallImpl(
                                    result.startOffset,
                                    result.endOffset,
                                    transformedCallee.returnType,
                                    transformedCallee.symbol as IrConstructorSymbol,
                                    result.typeArgumentsCount,
                                    transformedCallee.typeParameters.size,
                                    transformedCallee.valueParameters.size,
                                    result.origin
                                ).apply {
                                    copyTypeAndValueArgumentsFrom(result)
                                    fillGivenParameters()
                                }
                            }
                            is IrDelegatingConstructorCall -> {
                                IrDelegatingConstructorCallImpl(
                                    result.startOffset,
                                    result.endOffset,
                                    result.type,
                                    transformedCallee.symbol as IrConstructorSymbol,
                                    result.typeArgumentsCount,
                                    transformedCallee.valueParameters.size
                                ).apply {
                                    copyTypeAndValueArgumentsFrom(result)
                                    fillGivenParameters()
                                }
                            }
                            else -> {
                                result as IrCall
                                IrCallImpl(
                                    result.startOffset,
                                    result.endOffset,
                                    transformedCallee.returnType,
                                    transformedCallee.symbol,
                                    result.origin,
                                    result.superQualifierSymbol
                                ).apply {
                                    copyTypeAndValueArgumentsFrom(result)
                                    fillGivenParameters()
                                }
                            }
                        }
                    }
                    else -> result
                }
            }
        }, null)
    }

    private fun NameProvider.getInfoClassName(declaration: IrDeclarationWithName): Name =
        getBaseName(declaration, "ReaderInfo")

    private fun NameProvider.getBaseName(
        declaration: IrDeclarationWithName,
        suffix: String
    ): Name = allocateForGroup(
        getJoinedName(
            declaration.getPackageFragment()!!.fqName,
            declaration.descriptor.fqNameSafe
                .parent()
                .let {
                    if (declaration.name.isSpecial) {
                        it.child(allocateForGroup("Lambda").asNameId())
                    } else {
                        it.child(declaration.name.asString().asNameId())
                    }
                }
        ).asString() + "_$suffix"
    ).asNameId()

    private fun IrFunction.copyAsReader(): IrFunction {
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
        }
    }

}
