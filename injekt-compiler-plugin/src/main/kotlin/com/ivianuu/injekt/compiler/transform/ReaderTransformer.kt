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
import com.ivianuu.injekt.compiler.flatMapFix
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.getReaderConstructor
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.isReader
import com.ivianuu.injekt.compiler.jvmNameAnnotation
import com.ivianuu.injekt.compiler.remapTypeParameters
import com.ivianuu.injekt.compiler.substitute
import com.ivianuu.injekt.compiler.thisOfClass
import com.ivianuu.injekt.compiler.transform.component.getReaderInfo
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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
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
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

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

        if (readerConstructor.valueParameters.any { it.name.asString().startsWith("_") })
            return clazz

        if (clazz.isExternalDeclaration()) {
            val readerInfo = pluginContext.getReaderInfo(clazz)!!
            readerInfos += readerInfo

            readerInfo.declarations
                .filterIsInstance<IrFunction>()
                .filterNot { it is IrConstructor }
                .filterNot { it.isFakeOverride }
                .filterNot { it.dispatchReceiverParameter?.type == irBuiltIns.anyType }
                .forEach { readerValueParameter ->
                    readerConstructor.addValueParameter(
                        readerValueParameter.name.asString(),
                        readerValueParameter.returnType
                            .remapTypeParameters(
                                readerInfo,
                                clazz
                            )
                    )
                }

            return clazz
        }

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

        val givenCalls = mutableListOf<IrCall>()
        val readerCalls = mutableListOf<IrFunctionAccessExpression>()

        clazz.transformChildrenVoid(object : IrElementTransformerVoid() {

            private val functionStack = mutableListOf<IrFunction>()

            override fun visitFunction(declaration: IrFunction): IrStatement {
                val isReader = declaration.isReader() && declaration != readerConstructor
                if (isReader) functionStack.push(declaration)
                return super.visitFunction(declaration)
                    .also { if (isReader) functionStack.pop() }
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
                    } else {
                        readerCalls += result
                    }
                }
                return result
            }

        })

        val givenTypes = mutableMapOf<Any, IrType>()

        givenCalls
            .map { givenCall ->
                givenCall.type
                    .remapTypeParameters(clazz, readerInfo)
            }
            .forEach {
                givenTypes[it.distinctedType] = it
            }

        readerCalls.flatMapFix { readerCall ->
            val callReaderInfo = getReaderInfoForDeclaration(readerCall.symbol.owner)
            callReaderInfo
                .declarations
                .filterIsInstance<IrFunction>()
                .filterNot { it is IrConstructor }
                .filterNot { it.isFakeOverride }
                .filterNot { it.dispatchReceiverParameter?.type == irBuiltIns.anyType }
                .map {
                    it.returnType
                        .remapTypeParameters(callReaderInfo, readerCall.symbol.owner)
                        .substitute(
                            readerCall.symbol.owner.typeParameters.map { it.symbol }
                                .zip(readerCall.typeArguments)
                                .toMap()
                        )
                }
        }.forEach {
            givenTypes[it.distinctedType] = it
        }

        val givenFields = mutableMapOf<Any, IrField>()

        givenTypes.values.forEach { givenType ->
            givenFields[givenType.distinctedType] = clazz.addField(
                fieldName = getNameForType(givenType),
                fieldType = givenType
            )

            readerInfo.addReaderInfoForType(
                givenType.remapTypeParameters(clazz, readerInfo)
            )
        }

        val valueParametersByFields = givenFields.values.associateWith { field ->
            readerConstructor.addValueParameter(
                field.name.asString(),
                field.type
            )
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
            val field = givenFields.getValue(
                type.substitute(
                    transformFunctionIfNeeded(expression.symbol.owner)
                        .typeParameters
                        .map { it.symbol }
                        .zip(expression.typeArguments)
                        .toMap()
                ).distinctedType
            )

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

        if (function.valueParameters.any { it.name.asString().startsWith("_") }) {
            transformedFunctions[function] = function
            return function
        }

        if (function.isExternalDeclaration()) {
            val transformedFunction = function.copyAsReader()
            transformedFunctions[function] = transformedFunction

            if (transformedFunction.descriptor.fqNameSafe.asString() != "com.ivianuu.injekt.given") {
                val readerInfo = pluginContext.getReaderInfo(transformedFunction)!!
                readerInfos += readerInfo

                readerInfo.declarations
                    .filterIsInstance<IrFunction>()
                    .filterNot { it is IrConstructor }
                    .filterNot { it.isFakeOverride }
                    .filterNot { it.dispatchReceiverParameter?.type == irBuiltIns.anyType }
                    .forEach { readerValueParameter ->
                        transformedFunction.addValueParameter(
                            readerValueParameter.name.asString(),
                            readerValueParameter.returnType
                                .remapTypeParameters(
                                    readerInfo,
                                    transformedFunction
                                )
                        )
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
        val readerCalls = mutableListOf<IrFunctionAccessExpression>()

        transformedFunction.transformChildrenVoid(object : IrElementTransformerVoid() {

            private val functionStack = mutableListOf(transformedFunction)

            override fun visitFunction(declaration: IrFunction): IrStatement {
                val isReader = declaration.isReader()
                if (isReader) functionStack.push(declaration)
                return super.visitFunction(declaration)
                    .also { if (isReader) functionStack.pop() }
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
                    } else {
                        readerCalls += result
                    }
                }
                return result
            }
        })

        val givenTypes = mutableMapOf<Any, IrType>()
        givenCalls
            .map { givenCall ->
                givenCall.type
                    .remapTypeParameters(function, transformedFunction)
            }
            .forEach {
                givenTypes[it.distinctedType] = it
            }
        readerCalls.flatMapFix { readerCall ->
            val callReaderInfo = getReaderInfoForDeclaration(readerCall.symbol.owner)
            callReaderInfo
                .declarations
                .filterIsInstance<IrFunction>()
                .filterNot { it is IrConstructor }
                .filterNot { it.isFakeOverride }
                .filterNot { it.dispatchReceiverParameter?.type == irBuiltIns.anyType }
                .map {
                    it.returnType
                        .substitute(
                            callReaderInfo.typeParameters.map { it.symbol }
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
        }.forEach {
            givenTypes[it.distinctedType] = it
        }

        val givenValueParameters = mutableMapOf<Any, IrValueParameter>()

        givenTypes.values.forEach { givenType ->
            givenValueParameters[givenType.distinctedType] = transformedFunction.addValueParameter(
                name = getNameForType(givenType).asString(),
                type = givenType
            )

            readerInfo.addReaderInfoForType(
                givenType
                    .remapTypeParameters(transformedFunction, readerInfo)
            )
        }

        rewriteCalls(
            transformedFunction,
            givenCalls,
            readerCalls
        ) { type, expression ->
            val valueParameter = givenValueParameters.getValue(type
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
                ).distinctedType
            )
            return@rewriteCalls { irGet(valueParameter) }
        }

        return transformedFunction
    }

    private fun IrClass.addReaderInfoForType(type: IrType) {
        addFunction {
            this.name = getNameForType(type)
            returnType = type
            modality = Modality.ABSTRACT
        }.apply {
            dispatchReceiverParameter = thisReceiver?.copyTo(this)
            addMetadataIfNotLocal()
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
                            (result.valueArgumentsCount until valueArgumentsCount).forEach {
                                putValueArgument(
                                    it,
                                    provider(
                                        transformedCallee.valueParameters[it].type,
                                        result
                                    )(
                                        DeclarationIrBuilder(pluginContext, result.symbol),
                                        allScopes
                                    )
                                )
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

    private fun getNameForType(type: IrType): Name = buildString {
        append("_")
        fun IrType.render() {
            val fqName = if (this is IrSimpleType && abbreviation != null &&
                abbreviation!!.typeAlias.descriptor.hasAnnotation(InjektFqNames.Distinct)
            ) abbreviation!!.typeAlias.descriptor.fqNameSafe
            else classifierOrFail.descriptor.fqNameSafe
            append(
                (listOfNotNull(if (isMarkedNullable()) "nullable" else null) +
                        fqName.pathSegments().map { it.asString() })
                    .joinToString("_")
                    .decapitalize()
            )

            typeArguments.forEachIndexed { index, typeArgument ->
                if (index == 0) append("_")
                typeArgument.typeOrNull?.render() ?: append("star")
                if (index != typeArguments.lastIndex) append("_")
            }
        }

        type.render()
    }.asNameId()

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

    fun getReaderInfoForDeclaration(reader: IrDeclarationWithName): IrClass {
        val reader =
            when (reader) {
                is IrClass -> transformClassIfNeeded(reader)
                is IrConstructor -> transformClassIfNeeded(reader.constructedClass)
                else -> transformFunctionIfNeeded(reader as IrFunction)
            }
        return if (reader.isExternalDeclaration()) {
            module.descriptor.getPackage(reader.getPackageFragment()!!.fqName)
                .memberScope
                .getContributedDescriptors()
                .filterIsInstance<ClassDescriptor>()
                .filter { it.hasAnnotation(InjektFqNames.Name) }
                .single {
                    it.annotations.findAnnotation(InjektFqNames.Name)!!
                        .argumentValue("value")
                        .let { it as StringValue }
                        .value == reader.uniqueName()
                }
                .let { pluginContext.referenceClass(it.fqNameSafe)!!.owner }
        } else {
            readerInfos.single {
                it.annotations.findAnnotation(InjektFqNames.Name)!!
                    .getValueArgument(0)!!
                    .let { it as IrConst<String> }
                    .value == reader.uniqueName()
            }
        }
    }

}
