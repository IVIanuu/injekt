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
import com.ivianuu.injekt.compiler.canUseImplicits
import com.ivianuu.injekt.compiler.copy
import com.ivianuu.injekt.compiler.distinctedType
import com.ivianuu.injekt.compiler.flatMapFix
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.getReaderConstructor
import com.ivianuu.injekt.compiler.getValueArgumentSafe
import com.ivianuu.injekt.compiler.hiddenDeprecatedAnnotation
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.isMarkedAsImplicit
import com.ivianuu.injekt.compiler.isTypeParameter
import com.ivianuu.injekt.compiler.jvmNameAnnotation
import com.ivianuu.injekt.compiler.lookupTracker
import com.ivianuu.injekt.compiler.readableName
import com.ivianuu.injekt.compiler.remapTypeParameters
import com.ivianuu.injekt.compiler.substitute
import com.ivianuu.injekt.compiler.thisOfClass
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.uniqueFqName
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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.incremental.components.LocationInfo
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.record
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.hasDefaultValue
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ImplicitTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()
    private val transformedClasses = mutableSetOf<IrClass>()

    private val readerSignatures = mutableMapOf<IrFunction, IrFunction>()
    private val params = mutableSetOf<IrClass>()

    private val globalNameProvider = NameProvider()

    fun getTransformedFunction(function: IrFunction) =
        transformFunctionIfNeeded(function)

    fun getReaderSignature(function: IrFunction) =
        readerSignatures[transformFunctionIfNeeded(function)]!!

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
                transformClassIfNeeded(super.visitClass(declaration) as IrClass, false)
        })

        module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement =
                transformFunctionIfNeeded(super.visitFunction(declaration) as IrFunction)
        })

        readerSignatures
            .values
            .filterNot { it.isExternalDeclaration() }
            .forEach { readerSignature ->
                val parent = readerSignature.parent as IrDeclarationContainer
                if (readerSignature !in parent.declarations) {
                    parent.addChild(readerSignature)
                }
            }

        params
            .filterNot { it.isExternalDeclaration() }
            .forEach { params ->
                val parent = params.parent as IrDeclarationContainer
                if (params !in parent.declarations) {
                    parent.addChild(params)
                }
            }
    }

    private fun transformClassIfNeeded(
        clazz: IrClass,
        isParams: Boolean
    ): IrClass {
        if (clazz in transformedClasses) return clazz

        val readerConstructor = clazz.getReaderConstructor()

        if (!clazz.isMarkedAsImplicit() && readerConstructor == null) return clazz

        if (readerConstructor == null) return clazz

        transformedClasses += clazz

        if (readerConstructor.valueParameters.any { it.hasAnnotation(InjektFqNames.Implicit) })
            return clazz

        val existingSignature = getExternalReaderSignature(clazz)

        if (clazz.isExternalDeclaration() || existingSignature != null) {
            val readerSignature = getExternalReaderSignature(clazz)!!
            readerSignatures[readerConstructor] = readerSignature

            readerConstructor.copySignatureFrom(readerSignature) {
                it.remapTypeParameters(readerSignature, clazz)
            }

            return clazz
        }

        val fieldsByValueParameters = mutableMapOf<IrValueParameter, IrField>()

        transformDeclaration(
            owner = clazz,
            ownerFunction = readerConstructor,
            isParams = isParams,
            givenValueParameterAdded = { valueParameter ->
                fieldsByValueParameters[valueParameter] = clazz.addField(
                    fieldName = valueParameter.type.readableName(),
                    fieldType = valueParameter.type
                )
            },
            provider = { type, valueParameter, scopes ->
                if (scopes.none { it.irElement == readerConstructor }) {
                    val field = fieldsByValueParameters[valueParameter]!!
                    irGetField(
                        irGet(scopes.thisOfClass(clazz)!!),
                        field
                    )
                } else {
                    irGet(valueParameter)
                }
            }
        )

        readerConstructor.body = DeclarationIrBuilder(pluginContext, clazz.symbol).run {
            irBlockBody {
                readerConstructor.body?.statements?.forEach {
                    +it
                    if (it is IrDelegatingConstructorCall) {
                        fieldsByValueParameters.forEach { (valueParameter, field) ->
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

        return clazz
    }

    private fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        if (function is IrConstructor) {
            return if (function.canUseImplicits()) {
                transformClassIfNeeded(function.constructedClass, false)
                    .getReaderConstructor()!!
            } else function
        }

        transformedFunctions[function]?.let { return it }
        if (function in transformedFunctions.values) return function

        if (!function.canUseImplicits()) return function

        if (function.valueParameters.any { it.hasAnnotation(InjektFqNames.Implicit) }) {
            transformedFunctions[function] = function
            return function
        }

        val existingSignature = getExternalReaderSignature(function)

        if (function.isExternalDeclaration() || existingSignature != null) {
            val transformedFunction = function.copyAsReader()
            transformedFunctions[function] = transformedFunction

            if (!transformedFunction.isGiven) {
                val signature = getExternalReaderSignature(transformedFunction)!!
                readerSignatures[transformedFunction] = signature
                transformedFunction.copySignatureFrom(signature) {
                    it.remapTypeParameters(signature, transformedFunction)
                }
            }

            return transformedFunction
        }

        val transformedFunction = function.copyAsReader()
        transformedFunctions[function] = transformedFunction

        transformDeclaration(
            owner = transformedFunction,
            ownerFunction = transformedFunction,
            isParams = false,
            remapType = { it.remapTypeParameters(function, transformedFunction) },
            provider = { _, valueParameter, _ -> irGet(valueParameter) }
        )

        return transformedFunction
    }

    private fun <T> transformDeclaration(
        owner: T,
        ownerFunction: IrFunction,
        isParams: Boolean,
        remapType: (IrType) -> IrType = { it },
        givenValueParameterAdded: (IrValueParameter) -> Unit = {},
        provider: IrBuilderWithScope.(Any, IrValueParameter, List<ScopeWithIr>) -> IrExpression
    ) where T : IrDeclarationWithName, T : IrDeclarationParent, T : IrTypeParametersContainer {
        val givenCalls = mutableListOf<IrCall>()
        val defaultValueParameterByGivenCalls = mutableMapOf<IrCall, IrValueParameter>()
        val readerCalls = mutableListOf<IrFunctionAccessExpression>()

        owner.transformChildrenVoid(object : IrElementTransformerVoid() {

            private val functionStack = mutableListOf<IrFunction>()
            private val valueParameterStack = mutableListOf<IrValueParameter>()

            init {
                if (owner is IrFunction) functionStack += owner
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                val isReader = declaration.isMarkedAsImplicit()
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
                if (functionStack.isNotEmpty() &&
                    functionStack.lastOrNull() != owner
                ) return result
                if (expression.symbol.owner.canUseImplicits()) {
                    if (result is IrCall && expression.symbol.owner.isGiven) {
                        givenCalls += result
                        valueParameterStack.lastOrNull()
                            ?.takeIf {
                                it.defaultValue is IrExpressionBody &&
                                        it.defaultValue!!.statements.single() == result
                            }
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
                val type = givenCall.getRealGivenType()
                    .let(remapType)
                givenTypes[type.distinctedType] = type
                callsByTypes[type.distinctedType] = givenCall
            }
        readerCalls
            .flatMapFix { readerCall ->
                val transformedCallee = transformFunctionIfNeeded(readerCall.symbol.owner)
                transformedCallee
                    .valueParameters
                    .filter { it.hasAnnotation(InjektFqNames.Implicit) }
                    .filter { readerCall.getValueArgumentSafe(it.index) == null }
                    .map { it.type }
                    .map {
                        it
                            .remapTypeParameters(readerCall.symbol.owner, transformedCallee)
                            .substitute(
                                transformedCallee.typeParameters.map { it.symbol }
                                    .zip(
                                        readerCall.typeArguments
                                            .map(remapType)
                                    )
                                    .toMap()
                            )
                    }
                    .map { readerCall to it }
            }.forEach { (call, type) ->
                givenTypes[type.distinctedType] = type
                callsByTypes[type.distinctedType] = call
            }

        val mergedParams = if (
            !isParams &&
            givenTypes.size > 9 &&
            givenTypes.values
                .none { it.isTypeParameter() }
        )
            createParams(owner, givenTypes.values)
                .also { transformClassIfNeeded(it, true) }
                .also { params += it }
        else null

        val mergedParamsType = mergedParams?.let {
            it.typeWith(owner.typeParameters.map { it.defaultType })
        }

        val givenValueParameters = mutableMapOf<Any, IrValueParameter>()

        if (mergedParams != null) {
            val valueParameter = ownerFunction.addValueParameter(
                name = mergedParamsType!!.readableName().asString(),
                type = mergedParamsType
            ).apply {
                annotations += DeclarationIrBuilder(pluginContext, symbol)
                    .irCall(symbols.implicit.constructors.single())
            }
            givenValueParameterAdded(valueParameter)
            givenValueParameters[mergedParamsType] = valueParameter
        } else {
            givenTypes.values.forEach { givenType ->
                val call = callsByTypes[givenType.distinctedType]
                val defaultValueParameter = defaultValueParameterByGivenCalls[call]

                val valueParameter = (defaultValueParameter
                    ?.also { it.defaultValue = null }
                    ?: ownerFunction.addValueParameter(
                        name = givenType.readableName().asString(),
                        type = givenType
                    )).apply {
                    annotations += DeclarationIrBuilder(pluginContext, symbol)
                        .irCall(symbols.implicit.constructors.single())
                }
                givenValueParameterAdded(valueParameter)
                givenValueParameters[givenType.distinctedType] = valueParameter
            }
        }

        readerSignatures[ownerFunction] = createReaderSignature(owner, ownerFunction, remapType)

        rewriteCalls(
            owner = owner,
            ownerFunction = ownerFunction,
            givenCalls = givenCalls,
            readerCalls = readerCalls
        ) { type, expression, scopes ->
            val finalType = (if (expression.symbol.owner.isGiven) {
                expression.getRealGivenType()
            } else type).let(remapType)
                .substitute(
                    transformFunctionIfNeeded(expression.symbol.owner)
                        .typeParameters
                        .map { it.symbol }
                        .zip(
                            expression.typeArguments
                                .map(remapType)
                        )
                        .toMap()
                )
                .distinctedType

            val providerExpression = if (mergedParams != null) {
                provider(
                    this,
                    mergedParamsType!!.distinctedType,
                    givenValueParameters.values.single(),
                    scopes
                )
            } else {
                provider(this, finalType, givenValueParameters[finalType]!!, scopes)
            }

            if (mergedParams != null) {
                val mergedParamsFunction = mergedParams.functions
                    .single {
                        it.dispatchReceiverParameter?.type?.classOrNull == mergedParams.symbol &&
                                it.returnType
                                    .remapTypeParameters(mergedParams, owner)
                                    .distinctedType == finalType
                    }

                irCall(mergedParamsFunction).apply {
                    dispatchReceiver = providerExpression
                }
            } else {
                providerExpression
            }
        }
    }

    private fun <T> createParams(
        owner: T,
        givenTypes: Collection<IrType>
    ): IrClass where T : IrDeclarationWithName, T : IrTypeParametersContainer = buildClass {
        name = uniqueName(owner, "Params")
    }.apply clazz@{
        parent = owner.file
        createImplicitParameterDeclarationWithWrappedDescriptor()
        addMetadataIfNotLocal()

        annotations += DeclarationIrBuilder(pluginContext, symbol)
            .irCall(symbols.given.constructors.single())
        annotations += DeclarationIrBuilder(pluginContext, symbol)
            .hiddenDeprecatedAnnotation(pluginContext)

        copyTypeParametersFrom(owner)

        givenTypes.forEach { givenType ->
            addFunction {
                name = givenType
                    .remapTypeParameters(owner, this@clazz)
                    .readableName()
                returnType = givenType
                    .remapTypeParameters(owner, this@clazz)
            }.apply {
                dispatchReceiverParameter = thisReceiver!!.copyTo(this)
                addMetadataIfNotLocal()

                body = DeclarationIrBuilder(
                    pluginContext,
                    symbol
                ).run {
                    irExprBody(
                        irCall(
                            pluginContext.referenceFunctions(FqName("com.ivianuu.injekt.given"))
                                .single(),
                            returnType
                        ).apply {
                            putTypeArgument(0, returnType)
                        }
                    )
                }
            }
        }

        addConstructor {
            returnType = defaultType
            isPrimary = true
            visibility = Visibilities.PUBLIC
        }.apply {
            body = DeclarationIrBuilder(
                pluginContext,
                symbol
            ).irBlockBody {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.constructors.single().owner)
                +IrInstanceInitializerCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    this@clazz.symbol,
                    context.irBuiltIns.unitType
                )
            }
        }
    }

    private fun IrFunction.copySignatureFrom(
        signature: IrFunction,
        remapType: (IrType) -> IrType
    ) {
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

    private fun IrFunctionAccessExpression.getRealGivenType(): IrType {
        if (!symbol.owner.isGiven) return type

        val arguments = (getValueArgument(0) as? IrVarargImpl)
            ?.elements
            ?.map { it as IrExpression } ?: emptyList()

        val lazy = getValueArgument(1)
            ?.let { it as IrConst<Boolean> }
            ?.value ?: false

        return when {
            arguments.isNotEmpty() -> pluginContext.tmpFunction(arguments.size)
                .typeWith(arguments.map { it.type } + type)
            lazy -> pluginContext.tmpFunction(0).typeWith(type)
            else -> type
        }
    }

    private fun createReaderSignature(
        owner: IrDeclarationWithName,
        ownerFunction: IrFunction,
        remapType: (IrType) -> IrType
    ) = buildFun {
        this.name = uniqueName(owner, "ReaderSignature")
    }.apply {
        parent = owner.file
        addMetadataIfNotLocal()

        copyTypeParametersFrom(owner as IrTypeParametersContainer)

        annotations += DeclarationIrBuilder(pluginContext, symbol).run {
            irCall(symbols.name.constructors.single()).apply {
                putValueArgument(
                    0,
                    irString(owner.uniqueFqName())
                )
            }
        }

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
                            ownerFunction.valueParameters
                                .filter { it.hasAnnotation(InjektFqNames.Implicit) }
                                .map { it.index }
                                .map { irInt(it) }
                        )
                    )
                }
        }

        annotations += DeclarationIrBuilder(pluginContext, symbol)
            .hiddenDeprecatedAnnotation(pluginContext)

        returnType = remapType(ownerFunction.returnType)
            .remapTypeParameters(owner, this)

        valueParameters = ownerFunction.valueParameters.map {
            it.copyTo(
                this,
                type = remapType(it.type)
                    .remapTypeParameters(owner, this),
                varargElementType = it.varargElementType?.let(remapType)
                    ?.remapTypeParameters(owner, this),
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

    private fun uniqueName(
        owner: IrDeclarationWithName,
        suffix: String
    ) = globalNameProvider.allocateForGroup(
        getJoinedName(
            owner.getPackageFragment()!!.fqName,
            owner.descriptor.fqNameSafe
                .parent()
                .let {
                    if (owner.name.isSpecial) {
                        it.child(globalNameProvider.allocateForGroup("Lambda").asNameId())
                    } else {
                        it.child(owner.name.asString().asNameId())
                    }
                }
        ).asString() + "_" + suffix
    ).asNameId()

    private fun <T> rewriteCalls(
        owner: T,
        ownerFunction: T,
        givenCalls: List<IrCall>,
        readerCalls: List<IrFunctionAccessExpression>,
        provider: IrBuilderWithScope.(IrType, IrFunctionAccessExpression, List<ScopeWithIr>) -> IrExpression
    ) where T : IrDeclarationWithName, T : IrDeclarationParent {
        owner.transform(object : IrElementTransformerVoidWithContext() {
            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                return when (val result =
                    super.visitFunctionAccess(expression) as IrFunctionAccessExpression) {
                    in givenCalls -> {
                        val rawExpression = provider(
                            DeclarationIrBuilder(pluginContext, result.symbol),
                            result.getTypeArgument(0)!!,
                            result,
                            allScopes
                        )

                        val arguments = (result.getValueArgument(0) as? IrVarargImpl)
                            ?.elements
                            ?.map { it as IrExpression } ?: emptyList()

                        val lazy = result.getValueArgument(1)
                            ?.let { it as IrConst<Boolean> }
                            ?.value ?: false

                        when {
                            arguments.isNotEmpty() -> DeclarationIrBuilder(
                                pluginContext,
                                result.symbol
                            ).irCall(
                                rawExpression.type.classOrNull!!
                                    .owner
                                    .functions
                                    .first { it.name.asString() == "invoke" }
                            ).apply {
                                dispatchReceiver = rawExpression
                                arguments.forEachIndexed { index, argument ->
                                    putValueArgument(index, argument)
                                }
                            }
                            lazy -> DeclarationIrBuilder(
                                pluginContext,
                                result.symbol
                            ).irCall(
                                rawExpression.type.classOrNull!!
                                    .owner
                                    .functions
                                    .first { it.name.asString() == "invoke" }
                            ).apply {
                                dispatchReceiver = rawExpression
                            }
                            else -> rawExpression
                        }
                    }
                    in readerCalls -> {
                        val transformedCallee = transformFunctionIfNeeded(result.symbol.owner)
                        val readerSignature = readerSignatures[transformedCallee]!!

                        val ownerKtElement = owner.descriptor.findPsi() as? KtElement
                        val location = ownerKtElement?.let { KotlinLookupLocation(it) }
                            ?: object : LookupLocation {
                                override val location: LocationInfo?
                                    get() = object : LocationInfo {
                                        override val filePath: String
                                            get() = owner.file.path
                                        override val position: Position
                                            get() = Position.NO_POSITION
                                    }
                            }
                        lookupTracker!!.record(
                            location,
                            readerSignature.getPackageFragment()!!.packageFragmentDescriptor,
                            readerSignature.descriptor.name
                        )

                        fun IrFunctionAccessExpression.fillGivenParameters() {
                            transformedCallee.valueParameters.forEach { valueParameter ->
                                val valueArgument = getValueArgument(valueParameter.index)
                                if (valueParameter.hasAnnotation(InjektFqNames.Implicit) &&
                                    valueArgument == null
                                ) {
                                    putValueArgument(
                                        valueParameter.index,
                                        provider(
                                            DeclarationIrBuilder(pluginContext, result.symbol),
                                            valueParameter.type,
                                            result,
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

    private val IrFunction.isGiven: Boolean
        get() = descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.given"

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

    private fun getExternalReaderSignature(owner: IrDeclarationWithName): IrFunction? {
        val declaration = if (owner is IrConstructor)
            owner.constructedClass else owner

        return pluginContext.moduleDescriptor.getPackage(declaration.getPackageFragment()!!.fqName)
            .memberScope
            .getContributedDescriptors()
            .filterIsInstance<FunctionDescriptor>()
            .flatMapFix { pluginContext.referenceFunctions(it.fqNameSafe) }
            .map { it.owner }
            .filter { it.hasAnnotation(InjektFqNames.Name) }
            .singleOrNull { function ->
                function.getAnnotation(InjektFqNames.Name)!!
                    .getValueArgument(0)!!
                    .let { it as IrConst<String> }
                    .value == declaration.uniqueFqName()
            }
    }

}
