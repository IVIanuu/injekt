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
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.isTypeParameter
import com.ivianuu.injekt.compiler.remapTypeParameters
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.withNoArgQualifiers
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.referenceClassifier
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.AnnotationValue
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.BooleanValue
import org.jetbrains.kotlin.resolve.constants.ByteValue
import org.jetbrains.kotlin.resolve.constants.CharValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.DoubleValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.ErrorValue
import org.jetbrains.kotlin.resolve.constants.FloatValue
import org.jetbrains.kotlin.resolve.constants.IntValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.constants.LongValue
import org.jetbrains.kotlin.resolve.constants.NullValue
import org.jetbrains.kotlin.resolve.constants.ShortValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.constants.UByteValue
import org.jetbrains.kotlin.resolve.constants.UIntValue
import org.jetbrains.kotlin.resolve.constants.ULongValue
import org.jetbrains.kotlin.resolve.constants.UShortValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class InjektDeclarationIrBuilder(
    val pluginContext: IrPluginContext,
    val symbol: IrSymbol
) {

    val builder = DeclarationIrBuilder(pluginContext, symbol)

    val symbols = InjektSymbols(pluginContext)

    val symbolTable = pluginContext.symbolTable
    val irBuiltIns = pluginContext.irBuiltIns
    val typeTranslator = pluginContext.typeTranslator
    fun KotlinType.toIrType() = typeTranslator.translateType(this)

    fun emptyClass(name: Name): IrClass = buildClass {
        this.name = name
        visibility = Visibilities.PUBLIC
    }.apply clazz@{
        createImplicitParameterDeclarationWithWrappedDescriptor()

        (this as IrClassImpl).metadata = MetadataSource.Class(descriptor)

        addConstructor {
            this.returnType = defaultType
            isPrimary = true
            this.visibility = Visibilities.PUBLIC
        }.apply {
            body = InjektDeclarationIrBuilder(pluginContext, symbol).run {
                builder.irBlockBody {
                    initializeClassWithAnySuperClass(this@clazz.symbol)
                }
            }
        }
    }

    fun irInjektIntrinsicUnit(): IrExpression {
        return builder.irCall(
            pluginContext.referenceFunctions(
                InjektFqNames.InternalPackage.child(Name.identifier("injektIntrinsic"))
            ).single()
        )
    }

    fun noArgSingleConstructorCall(clazz: IrClassSymbol): IrConstructorCall =
        builder.irCall(clazz.constructors.single())

    fun singleClassArgConstructorCall(
        clazz: IrClassSymbol,
        value: IrClassifierSymbol
    ): IrConstructorCall =
        builder.irCall(clazz.constructors.single()).apply {
            putValueArgument(
                0,
                IrClassReferenceImpl(
                    startOffset, endOffset,
                    irBuiltIns.kClassClass.typeWith(value.defaultType),
                    value,
                    value.defaultType
                )
            )
        }

    fun IrBlockBodyBuilder.initializeClassWithAnySuperClass(symbol: IrClassSymbol) {
        +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
        +IrInstanceInitializerCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            symbol,
            irBuiltIns.unitType
        )
    }

    fun irMapKeyConstructorForKey(expression: IrExpression): IrConstructorCall {
        return when {
            expression is IrClassReference -> irClassKey(expression.classType)
            expression is IrGetValue && expression.symbol.descriptor.name.asString()
                .startsWith("class\$") -> {
                irClassKey(expression.type.typeArguments.single())
            }
            expression is IrCall && expression.symbol.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.classOf" -> {
                irClassKey(expression.type.typeArguments.single())
            }
            expression is IrConst<*> -> {
                when (expression.kind) {
                    is IrConstKind.Int -> builder.irCall(symbols.astMapIntKey.constructors.single())
                        .apply {
                            putValueArgument(0, expression.deepCopyWithVariables())
                        }
                    is IrConstKind.Long -> builder.irCall(symbols.astMapLongKey.constructors.single())
                        .apply {
                            putValueArgument(0, expression.deepCopyWithVariables())
                        }
                    is IrConstKind.String -> builder.irCall(symbols.astMapStringKey.constructors.single())
                        .apply {
                            putValueArgument(0, expression.deepCopyWithVariables())
                        }
                    else -> error("Unexpected expression ${expression.dump()}")
                }
            }
            else -> error("Unexpected expression ${expression.dump()}")
        }
    }

    private fun irClassKey(type: IrType): IrConstructorCall {
        return if (type.isTypeParameter()) {
            builder.irCall(symbols.astMapTypeParameterClassKey.constructors.single())
                .apply {
                    putValueArgument(
                        0,
                        builder.irString(
                            (type.toKotlinType().constructor.declarationDescriptor as TypeParameterDescriptor)
                                .name.asString()
                        )
                    )
                }
        } else {
            singleClassArgConstructorCall(
                symbols.astMapClassKey,
                type.classifierOrFail
            )
        }
    }

    fun irLambda(
        type: IrType,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
        body: IrBlockBodyBuilder.(IrFunction) -> Unit
    ): IrExpression {
        type as IrSimpleType
        val returnType = type.arguments.last().typeOrNull!!

        val lambda = buildFun {
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            name = Name.special("<anonymous>")
            this.returnType = returnType
            visibility = Visibilities.LOCAL
        }.apply {
            type.arguments.dropLast(1).forEachIndexed { index, typeArgument ->
                addValueParameter(
                    "p$index",
                    typeArgument.typeOrNull!!
                )
            }
            parent = builder.scope.getLocalDeclarationParent()
            this.body =
                DeclarationIrBuilder(pluginContext, symbol).irBlockBody { body(this, this@apply) }
        }

        return builder.irBlock(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = IrStatementOrigin.LAMBDA,
            resultType = type
        ) {
            +lambda
            +IrFunctionReferenceImpl(
                startOffset = startOffset,
                endOffset = endOffset,
                type = type,
                symbol = lambda.symbol,
                typeArgumentsCount = lambda.typeParameters.size,
                reflectionTarget = null,
                origin = IrStatementOrigin.LAMBDA
            )
        }
    }

    data class FactoryParameter(
        val name: String,
        val type: IrType,
        val assisted: Boolean,
        val requirement: Boolean
    )

    fun IrType.nullableRemapTypeParameters(
        source: IrTypeParametersContainer?,
        target: IrTypeParametersContainer
    ): IrType {
        return if (source != null) remapTypeParameters(source, target) else this
    }

    fun factory(
        name: Name,
        visibility: Visibility,
        typeParametersContainer: IrTypeParametersContainer?,
        parameters: List<FactoryParameter>,
        membersInjector: IrClass?,
        returnType: IrType,
        createExpr: IrBuilderWithScope.(IrFunction) -> IrExpression
    ): IrClass {
        val (assistedParameters, nonAssistedParameters) = parameters
            .partition { it.assisted }
        val membersInjectorParameters =
            membersInjector?.constructors?.singleOrNull()?.valueParameters
                ?.map {
                    FactoryParameter(
                        name = it.name.asString(),
                        type = it.type.typeArguments.single(),
                        assisted = false,
                        requirement = false
                    )
                } ?: emptyList()
        return buildClass {
            kind = if (nonAssistedParameters.isNotEmpty()) ClassKind.CLASS else ClassKind.OBJECT
            this.name = name
            this.visibility = Visibilities.PUBLIC // todo visibility
        }.apply clazz@{
            (this as IrClassImpl).metadata = MetadataSource.Class(descriptor)

            if (typeParametersContainer != null) {
                copyTypeParametersFrom(typeParametersContainer)
            }

            val superType = irBuiltIns.function(assistedParameters.size)
                .typeWith(
                    assistedParameters
                        .map {
                            it.type
                                .nullableRemapTypeParameters(typeParametersContainer, this)
                        } + returnType
                        .nullableRemapTypeParameters(typeParametersContainer, this)
                )
            superTypes += superType

            createImplicitParameterDeclarationWithWrappedDescriptor()

            val fieldsByNonAssistedParameter = (nonAssistedParameters + membersInjectorParameters)
                .associateWith { parameter ->
                    addField(
                        parameter.name,
                        if (parameter.requirement) {
                            parameter.type
                                .nullableRemapTypeParameters(typeParametersContainer, this)
                        } else {
                            irBuiltIns.function(0)
                                .typeWith(
                                    parameter.type
                                        .nullableRemapTypeParameters(typeParametersContainer, this)
                                )
                                .withNoArgQualifiers(pluginContext, listOf(InjektFqNames.Provider))
                        }
                    )
                }

            addConstructor {
                this.returnType = defaultType
                isPrimary = true
                this.visibility = Visibilities.PUBLIC
            }.apply {
                fieldsByNonAssistedParameter.forEach { (_, field) ->
                    addValueParameter(
                        field.name.asString(),
                        field.type
                    )
                }

                body = builder.irBlockBody {
                    initializeClassWithAnySuperClass(this@clazz.symbol)
                    valueParameters
                        .forEach { valueParameter ->
                            +irSetField(
                                irGet(thisReceiver!!),
                                fieldsByNonAssistedParameter.values.toList()[valueParameter.index],
                                irGet(valueParameter)
                            )
                        }
                }
            }

            val companion = if (nonAssistedParameters.isNotEmpty()) {
                providerCompanion(
                    typeParametersContainer,
                    parameters,
                    membersInjector,
                    membersInjectorParameters,
                    returnType,
                    createExpr
                ).also { addChild(it) }
            } else null

            val createFunction = if (nonAssistedParameters.isEmpty()) {
                factoryCreateFunction(
                    typeParametersContainer,
                    parameters,
                    membersInjector,
                    membersInjectorParameters,
                    returnType,
                    this,
                    createExpr
                )
            } else {
                null
            }

            addFunction {
                this.name = Name.identifier("invoke")
                this.returnType = returnType
                this.visibility = Visibilities.PUBLIC
            }.apply func@{
                dispatchReceiverParameter = thisReceiver?.copyTo(this, type = defaultType)

                overriddenSymbols += superType.getClass()!!
                    .functions
                    .single { it.name.asString() == "invoke" }
                    .symbol

                val valueParametersByAssistedParameter = assistedParameters.associateWith {
                    addValueParameter(
                        it.name,
                        it.type
                    )
                }

                body = builder.irExprBody(
                    builder.irCall(companion?.functions?.single() ?: createFunction!!).apply {
                        dispatchReceiver =
                            if (companion != null) builder.irGetObject(companion.symbol) else builder.irGet(
                                dispatchReceiverParameter!!
                            )

                        (parameters + membersInjectorParameters).forEachIndexed { index, parameter ->
                            putValueArgument(
                                index,
                                if (parameter in assistedParameters) {
                                    builder.irGet(
                                        valueParametersByAssistedParameter.getValue(
                                            parameter
                                        )
                                    )
                                } else {
                                    if (parameter.requirement) {
                                        builder.irGetField(
                                            builder.irGet(dispatchReceiverParameter!!),
                                            fieldsByNonAssistedParameter.getValue(parameter)
                                        )
                                    } else if (parameter.type.isFunction() &&
                                        parameter.type.hasAnnotation(InjektFqNames.Provider)
                                    ) {
                                        builder.irGetField(
                                            builder.irGet(dispatchReceiverParameter!!),
                                            fieldsByNonAssistedParameter.getValue(parameter)
                                        )
                                    } else {
                                        builder.irCall(
                                                irBuiltIns.function(0)
                                                    .functions
                                                    .single { it.owner.name.asString() == "invoke" })
                                            .apply {
                                                dispatchReceiver = builder.irGetField(
                                                    builder.irGet(dispatchReceiverParameter!!),
                                                    fieldsByNonAssistedParameter.getValue(parameter)
                                                )
                                            }
                                    }
                                }
                            )
                        }
                    }
                )
            }
        }
    }

    private fun providerCompanion(
        typeParametersContainer: IrTypeParametersContainer?,
        parameters: List<FactoryParameter>,
        membersInjector: IrClass?,
        membersInjectorParameters: List<FactoryParameter>,
        returnType: IrType,
        createExpr: IrBuilderWithScope.(IrFunction) -> IrExpression
    ) = buildClass {
        kind = ClassKind.OBJECT
        name = SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
        isCompanion = true
        this.visibility = Visibilities.PUBLIC
    }.apply clazz@{
        createImplicitParameterDeclarationWithWrappedDescriptor()

        (this as IrClassImpl).metadata = MetadataSource.Class(descriptor)

        addConstructor {
            this.returnType = defaultType
            isPrimary = true
            this.visibility = Visibilities.PUBLIC
        }.apply {
            body = builder.irBlockBody {
                initializeClassWithAnySuperClass(this@clazz.symbol)
            }
        }

        factoryCreateFunction(
            typeParametersContainer,
            parameters,
            membersInjector,
            membersInjectorParameters,
            returnType,
            this,
            createExpr
        )
    }

    private fun factoryCreateFunction(
        typeParametersContainer: IrTypeParametersContainer?,
        parameters: List<FactoryParameter>,
        membersInjector: IrClass?,
        membersInjectorParameters: List<FactoryParameter>,
        returnType: IrType,
        owner: IrClass,
        createExpr: IrBuilderWithScope.(IrFunction) -> IrExpression
    ): IrFunction {
        return owner.addFunction {
            name = Name.identifier("create")
            visibility = Visibilities.PUBLIC
            isInline = true
        }.apply {
            metadata = MetadataSource.Function(descriptor)
            dispatchReceiverParameter = owner.thisReceiver?.copyTo(this, type = owner.defaultType)

            if (typeParametersContainer != null) copyTypeParametersFrom(typeParametersContainer)
            this.returnType = returnType.nullableRemapTypeParameters(typeParametersContainer, this)

            parameters
                .forEach { (name, type) ->
                    addValueParameter(
                        name,
                        type.nullableRemapTypeParameters(typeParametersContainer, this)
                    )
                }

            val membersInjectorValueParametersByParameter = membersInjectorParameters
                .associateWith {
                    addValueParameter(
                        it.name,
                        it.type
                    )
                }

            body = DeclarationIrBuilder(pluginContext, symbol).run {
                if (membersInjector == null) {
                    builder.irExprBody(createExpr(this, this@apply))
                } else {
                    builder.irBlockBody {
                        val instance = irTemporary(createExpr(this, this@apply))

                        membersInjectorParameters.forEach { parameter ->
                            val injectFunction = membersInjector
                                .companionObject().let { it as IrClass }
                                .functions
                                .single {
                                    it.name.asString().startsWith("inject\$${parameter.name}")
                                }

                            +irCall(injectFunction).apply {
                                dispatchReceiver =
                                    irGetObject((membersInjector.companionObject() as IrClass).symbol)
                                putValueArgument(0, irGet(instance))
                                putValueArgument(
                                    1,
                                    builder.irGet(
                                        membersInjectorValueParametersByParameter
                                            .getValue(parameter)
                                    )
                                )
                            }
                        }

                        +DeclarationIrBuilder(pluginContext, symbol)
                            .irReturn(irGet(instance))
                    }
                }
            }
        }
    }

    fun fieldBakedProperty(
        clazz: IrClass,
        name: Name,
        type: IrType
    ) = clazz.addProperty {
        this.name = name
    }.apply {
        parent = clazz

        backingField = buildField {
            this.name = this@apply.name
            this.type = type
            visibility = Visibilities.PRIVATE
        }.apply {
            parent = clazz
        }
        addGetter {
            this.name = Name.identifier("get$name") // todo remove once fixed
            returnType = type
        }.apply {
            dispatchReceiverParameter = clazz.thisReceiver!!.copyTo(this)
            body = DeclarationIrBuilder(pluginContext, symbol).run {
                irExprBody(
                    irReturn(
                        irGetField(
                            irGet(dispatchReceiverParameter!!),
                            backingField!!
                        )
                    )
                )
            }
        }
    }

    fun generateAnnotationConstructorCall(annotationDescriptor: AnnotationDescriptor): IrConstructorCall? {
        val annotationType = annotationDescriptor.type
        val annotationClassDescriptor = annotationType.constructor.declarationDescriptor
        if (annotationClassDescriptor !is ClassDescriptor) return null
        if (annotationClassDescriptor is NotFoundClasses.MockClassDescriptor) return null

        assert(DescriptorUtils.isAnnotationClass(annotationClassDescriptor)) {
            "Annotation class expected: $annotationClassDescriptor"
        }

        val primaryConstructorDescriptor = annotationClassDescriptor.unsubstitutedPrimaryConstructor
            ?: annotationClassDescriptor.constructors.singleOrNull()
            ?: throw AssertionError("No constructor for annotation class $annotationClassDescriptor")
        val primaryConstructorSymbol =
            pluginContext.referenceConstructors(annotationClassDescriptor.fqNameSafe)
                .single()

        val psi = annotationDescriptor.source.safeAs<PsiSourceElement>()?.psi
        val startOffset =
            psi?.takeUnless { it.containingFile.fileType.isBinary }?.startOffset ?: UNDEFINED_OFFSET
        val endOffset =
            psi?.takeUnless { it.containingFile.fileType.isBinary }?.endOffset ?: UNDEFINED_OFFSET

        val irCall = IrConstructorCallImpl.fromSymbolDescriptor(
            startOffset, endOffset,
            annotationType.toIrType(),
            primaryConstructorSymbol
        )

        for (valueParameter in primaryConstructorDescriptor.valueParameters) {
            val argumentIndex = valueParameter.index
            val argumentValue =
                annotationDescriptor.allValueArguments[valueParameter.name] ?: continue
            val irArgument = generateConstantOrAnnotationValueAsExpression(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                argumentValue,
                valueParameter.varargElementType
            )
            if (irArgument != null) {
                irCall.putValueArgument(argumentIndex, irArgument)
            }
        }

        return irCall
    }

    private fun generateConstantOrAnnotationValueAsExpression(
        startOffset: Int,
        endOffset: Int,
        constantValue: ConstantValue<*>,
        varargElementType: KotlinType? = null
    ): IrExpression? {
        val constantKtType = constantValue.getType(pluginContext.moduleDescriptor)
        val constantType = constantKtType.toIrType()

        return when (constantValue) {
            is StringValue -> IrConstImpl.string(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is IntValue -> IrConstImpl.int(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is UIntValue -> IrConstImpl.int(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is NullValue -> IrConstImpl.constNull(startOffset, endOffset, constantType)
            is BooleanValue -> IrConstImpl.boolean(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is LongValue -> IrConstImpl.long(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is ULongValue -> IrConstImpl.long(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is DoubleValue -> IrConstImpl.double(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is FloatValue -> IrConstImpl.float(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is CharValue -> IrConstImpl.char(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is ByteValue -> IrConstImpl.byte(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is UByteValue -> IrConstImpl.byte(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is ShortValue -> IrConstImpl.short(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )
            is UShortValue -> IrConstImpl.short(
                startOffset,
                endOffset,
                constantType,
                constantValue.value
            )

            is ArrayValue -> {
                val arrayElementType =
                    varargElementType ?: pluginContext.builtIns.getArrayElementType(constantKtType)
                IrVarargImpl(
                    startOffset, endOffset,
                    constantType,
                    arrayElementType.toIrType(),
                    constantValue.value.mapNotNull {
                        generateConstantOrAnnotationValueAsExpression(
                            startOffset,
                            endOffset,
                            it,
                            null
                        )
                    }
                )
            }

            is EnumValue -> {
                val enumEntryDescriptor =
                    constantKtType.memberScope.getContributedClassifier(
                        constantValue.enumEntryName,
                        NoLookupLocation.FROM_BACKEND
                    )
                        ?: throw AssertionError("No such enum entry ${constantValue.enumEntryName} in $constantType")
                if (enumEntryDescriptor !is ClassDescriptor) {
                    throw AssertionError("Enum entry $enumEntryDescriptor should be a ClassDescriptor")
                }
                if (!DescriptorUtils.isEnumEntry(enumEntryDescriptor)) {
                    // Error class descriptor for an unresolved entry.
                    // TODO this `null` may actually reach codegen if the annotation is on an interface member's default implementation,
                    //      as any bridge generated in an implementation of that interface will have a copy of the annotation. See
                    //      `missingEnumReferencedInAnnotationArgumentIr` in `testData/compileKotlinAgainstCustomBinaries`: replace
                    //      `open class B` with `interface B` and watch things break. (`KClassValue` below likely has a similar problem.)
                    return null
                }
                IrGetEnumValueImpl(
                    startOffset, endOffset,
                    constantType,
                    symbolTable.referenceEnumEntry(enumEntryDescriptor)
                )
            }

            is AnnotationValue -> generateAnnotationConstructorCall(constantValue.value)

            is KClassValue -> {
                val classifierKtType = constantValue.getArgumentType(pluginContext.moduleDescriptor)
                if (classifierKtType.isError) null
                else {
                    val classifierDescriptor = classifierKtType.constructor.declarationDescriptor
                        ?: throw AssertionError("Unexpected KClassValue: $classifierKtType")

                    IrClassReferenceImpl(
                        startOffset, endOffset,
                        constantValue.getType(pluginContext.moduleDescriptor).toIrType(),
                        symbolTable.referenceClassifier(classifierDescriptor),
                        classifierKtType.toIrType()
                    )
                }
            }

            is ErrorValue -> null

            else -> TODO("Unexpected constant value: ${constantValue.javaClass.simpleName} $constantValue")
        }
    }

}