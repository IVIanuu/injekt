package com.ivianuu.injekt.compiler.transform

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

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.classOrFail
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.withNoArgQualifiers
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.NotFoundClasses
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
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.referenceClassifier
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.findSingleFunction
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
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

abstract class AbstractInjektTransformer(
    val context: IrPluginContext
) : IrElementTransformerVoid(), ModuleLoweringPass {

    val symbols = InjektSymbols(context)

    val irProviders = context.irProviders
    protected val symbolTable = context.symbolTable
    val irBuiltIns = context.irBuiltIns
    protected val builtIns = context.builtIns
    protected val typeTranslator = context.typeTranslator
    protected open fun KotlinType.toIrType() = typeTranslator.translateType(this)

    lateinit var moduleFragment: IrModuleFragment

    override fun lower(module: IrModuleFragment) {
        moduleFragment = module
        visitModuleFragment(module, null)
        module.patchDeclarationParents()
    }

    fun IrBuilderWithScope.irInjektIntrinsicUnit(): IrExpression {
        return irCall(
            symbolTable.referenceFunction(
                symbols.internalPackage.memberScope
                    .findSingleFunction(Name.identifier("injektIntrinsic"))
            ),
            this@AbstractInjektTransformer.context.irBuiltIns.unitType
        )
    }

    fun IrBuilderWithScope.noArgSingleConstructorCall(clazz: IrClassSymbol): IrConstructorCall =
        irCall(clazz.constructors.single())

    fun IrBuilderWithScope.propertyPathAnnotation(property: IrProperty): IrConstructorCall =
        irCall(symbols.astPropertyPath.constructors.single()).apply {
            putValueArgument(0, irString(property.name.asString()))
        }

    fun IrBuilderWithScope.classPathAnnotation(clazz: IrClass): IrConstructorCall =
        irCall(symbols.astClassPath.constructors.single()).apply {
            putValueArgument(
                0,
                IrClassReferenceImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    context.irBuiltIns.kClassClass.typeWith(clazz.defaultType),
                    clazz.symbol,
                    clazz.defaultType
                )
            )
        }

    fun IrBlockBodyBuilder.initializeClassWithAnySuperClass(symbol: IrClassSymbol) {
        +IrDelegatingConstructorCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            irBuiltIns.unitType,
            symbolTable.referenceConstructor(
                builtIns.any
                    .unsubstitutedPrimaryConstructor!!
            )
        )
        +IrInstanceInitializerCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            symbol,
            irBuiltIns.unitType
        )
    }

    fun IrBuilderWithScope.irMapKeyConstructorForKey(
        expression: IrExpression
    ): IrConstructorCall {
        return when (expression) {
            is IrClassReference -> {
                irCall(symbols.astMapClassKey.constructors.single())
                    .apply {
                        putValueArgument(0, expression.deepCopyWithVariables())
                    }
            }
            is IrConst<*> -> {
                when (expression.kind) {
                    is IrConstKind.Int -> irCall(symbols.astMapIntKey.constructors.single())
                        .apply {
                            putValueArgument(0, expression.deepCopyWithVariables())
                        }
                    is IrConstKind.Long -> irCall(symbols.astMapLongKey.constructors.single())
                        .apply {
                            putValueArgument(0, expression.deepCopyWithVariables())
                        }
                    is IrConstKind.String -> irCall(symbols.astMapStringKey.constructors.single())
                        .apply {
                            putValueArgument(0, expression.deepCopyWithVariables())
                        }
                    else -> error("Unexpected expression ${expression.dump()}")
                }
            }
            else -> error("Unexpected expression ${expression.dump()}")
        }
    }

    data class ProviderParameter(
        val name: String,
        val type: IrType,
        val assisted: Boolean
    )

    fun IrBuilderWithScope.provider(
        name: Name,
        visibility: Visibility,
        parameters: List<ProviderParameter>,
        returnType: IrType,
        createBody: IrBuilderWithScope.(IrFunction) -> IrBody
    ): IrClass {
        val (assistedParameters, nonAssistedParameters) = parameters
            .partition { it.assisted }
        return buildClass {
            kind = if (nonAssistedParameters.isNotEmpty()) ClassKind.CLASS else ClassKind.OBJECT
            this.name = name
            this.visibility = visibility
        }.apply clazz@{
            val superType = symbols.getFunction(assistedParameters.size)
                .typeWith(
                    assistedParameters
                        .map { it.type } + returnType
                )
            superTypes += superType

            (this as IrClassImpl).metadata = MetadataSource.Class(descriptor)

            createImplicitParameterDeclarationWithWrappedDescriptor()

            val fieldsByNonAssistedParameter = nonAssistedParameters
                .toList()
                .associateWith { (name, type) ->
                    addField(
                        name,
                        symbols.getFunction(0)
                            .typeWith(type)
                            .withNoArgQualifiers(symbols, listOf(InjektFqNames.Provider))
                    )
                }

            addConstructor {
                this.returnType = defaultType
                isPrimary = true
                this.visibility = visibility
            }.apply {
                fieldsByNonAssistedParameter.forEach { (_, field) ->
                    addValueParameter(
                        field.name.asString(),
                        field.type
                    )
                }

                body = irBlockBody {
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
                    visibility,
                    parameters,
                    returnType,
                    createBody
                ).also { addChild(it) }
            } else null

            val createFunction = if (nonAssistedParameters.isEmpty()) {
                providerCreateFunction(parameters, returnType, this, createBody)
            } else {
                null
            }

            addFunction {
                this.name = Name.identifier("invoke")
                this.returnType = returnType
                this.visibility = Visibilities.PUBLIC
            }.apply func@{
                dispatchReceiverParameter = thisReceiver?.copyTo(this, type = defaultType)

                overriddenSymbols += superType.classOrFail
                    .ensureBound(this@AbstractInjektTransformer.context.irProviders)
                    .owner
                    .functions
                    .single { it.name.asString() == "invoke" }
                    .symbol

                val valueParametersByAssistedParameter = assistedParameters.associateWith {
                    addValueParameter(
                        it.name,
                        it.type
                    )
                }

                body = irExprBody(
                    irCall(companion?.functions?.single() ?: createFunction!!).apply {
                        dispatchReceiver =
                            if (companion != null) irGetObject(companion.symbol) else irGet(
                                dispatchReceiverParameter!!
                            )

                        parameters.forEachIndexed { index, parameter ->
                            putValueArgument(
                                index,
                                if (parameter in assistedParameters) {
                                    irGet(valueParametersByAssistedParameter.getValue(parameter))
                                } else {
                                    irCall(
                                        symbols.getFunction(0)
                                            .functions
                                            .single { it.owner.name.asString() == "invoke" })
                                        .apply {
                                            dispatchReceiver = irGetField(
                                                irGet(dispatchReceiverParameter!!),
                                                fieldsByNonAssistedParameter.getValue(parameter)
                                            )
                                        }
                                }
                            )
                        }
                    }
                )
            }
        }
    }

    private fun IrBuilderWithScope.providerCompanion(
        visibility: Visibility,
        parameters: List<ProviderParameter>,
        returnType: IrType,
        createBody: IrBuilderWithScope.(IrFunction) -> IrBody
    ) = buildClass {
        kind = ClassKind.OBJECT
        name = SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
        isCompanion = true
        this.visibility = visibility
    }.apply clazz@{
        createImplicitParameterDeclarationWithWrappedDescriptor()

        (this as IrClassImpl).metadata = MetadataSource.Class(descriptor)

        addConstructor {
            this.returnType = defaultType
            isPrimary = true
            this.visibility = visibility
        }.apply {
            body = irBlockBody {
                initializeClassWithAnySuperClass(this@clazz.symbol)
            }
        }

        providerCreateFunction(parameters, returnType, this, createBody)
    }

    fun IrClass.fieldBakedProperty(
        name: Name,
        type: IrType
    ) = addProperty {
        this.name = name
    }.apply {
        parent = this@fieldBakedProperty

        backingField = buildField {
            this.name = this@apply.name
            this.type = type
        }.apply {
            parent = this@fieldBakedProperty
        }
        addGetter {
            returnType = type
        }.apply {
            dispatchReceiverParameter = thisReceiver!!.copyTo(this)
            body = DeclarationIrBuilder(context, symbol).run {
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

    private fun IrBuilderWithScope.providerCreateFunction(
        parameters: List<ProviderParameter>,
        returnType: IrType,
        owner: IrClass,
        createBody: IrBuilderWithScope.(IrFunction) -> IrBody
    ): IrFunction {
        return owner.addFunction {
            name = Name.identifier("create")
            this.returnType = returnType
            visibility = Visibilities.PUBLIC
            isInline = true
        }.apply {
            dispatchReceiverParameter = owner.thisReceiver?.copyTo(this, type = owner.defaultType)

            parameters
                .forEach { (name, type) ->
                    addValueParameter(
                        name,
                        type
                    )
                }

            body = createBody(this@providerCreateFunction, this)
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
            symbolTable.referenceConstructor(primaryConstructorDescriptor)

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

    fun generateConstantValueAsExpression(
        startOffset: Int,
        endOffset: Int,
        constantValue: ConstantValue<*>,
        varargElementType: KotlinType? = null
    ): IrExpression =
        generateConstantOrAnnotationValueAsExpression(
            startOffset,
            endOffset,
            constantValue,
            varargElementType
        )!!

    private fun generateConstantOrAnnotationValueAsExpression(
        startOffset: Int,
        endOffset: Int,
        constantValue: ConstantValue<*>,
        varargElementType: KotlinType? = null
    ): IrExpression? {
        val constantKtType = constantValue.getType(context.moduleDescriptor)
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
                    varargElementType ?: context.builtIns.getArrayElementType(constantKtType)
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
                val classifierKtType = constantValue.getArgumentType(context.moduleDescriptor)
                if (classifierKtType.isError) null
                else {
                    val classifierDescriptor = classifierKtType.constructor.declarationDescriptor
                        ?: throw AssertionError("Unexpected KClassValue: $classifierKtType")

                    IrClassReferenceImpl(
                        startOffset, endOffset,
                        constantValue.getType(context.moduleDescriptor).toIrType(),
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
