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
import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.child
import com.ivianuu.injekt.compiler.getInjectConstructor
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.isTypeParameter
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import com.ivianuu.injekt.compiler.withNoArgAnnotations
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
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
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
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
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.referenceClassifier
import org.jetbrains.kotlin.name.Name
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

    fun irInjektIntrinsicUnit(): IrExpression {
        return builder.irCall(
            pluginContext.referenceFunctions(
                InjektFqNames.InternalPackage.child("injektIntrinsic")
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
                irClassKey(expression.type.typeArguments.single().typeOrFail)
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
        val returnType = type.typeArguments.last().typeOrNull!!

        val lambda = buildFun {
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            name = Name.special("<anonymous>")
            this.returnType = returnType
            visibility = Visibilities.LOCAL
        }.apply {
            type.typeArguments.dropLast(1).forEachIndexed { index, typeArgument ->
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
        val assisted: Boolean
    )

    class FieldWithGetter(
        val field: IrField,
        val getter: IrFunction
    )

    fun fieldBackedProperty(
        clazz: IrClass,
        name: Name,
        type: IrType
    ): FieldWithGetter {
        val field = buildField {
            this.name = name
            this.type = type
            visibility = Visibilities.PRIVATE
        }.apply {
            parent = clazz
        }
        clazz.addChild(field)

        val getter = buildFun {
            this.name =
                Name.identifier("get${name.asString().capitalize()}") // todo remove once fixed
            returnType = type
        }.apply {
            addMetadataIfNotLocal()
            parent = clazz
            dispatchReceiverParameter = clazz.thisReceiver!!.copyTo(this)
            body = DeclarationIrBuilder(pluginContext, symbol).run {
                irExprBody(
                    irGetField(
                        irGet(dispatchReceiverParameter!!),
                        field
                    )
                )
            }
        }

        clazz.addChild(getter)

        return FieldWithGetter(field, getter)
    }

    fun classFactoryLambda(clazz: IrClass, membersInjector: IrFunction?): IrExpression {
        val parametersNameProvider = NameProvider()

        val constructor = clazz.getInjectConstructor()

        val constructorParameters = constructor?.valueParameters?.map { valueParameter ->
            FactoryParameter(
                name = parametersNameProvider.allocateForGroup(valueParameter.name).asString(),
                type = valueParameter.type,
                assisted = valueParameter.descriptor.hasAnnotation(InjektFqNames.Assisted)
            )
        } ?: emptyList()

        val membersInjectorParameters =
            membersInjector
                ?.valueParameters
                ?.drop(1)
                ?.map { valueParameter ->
                    FactoryParameter(
                        name = parametersNameProvider.allocateForGroup(valueParameter.name)
                            .asString(),
                        type = valueParameter.type,
                        assisted = false
                    )
                } ?: emptyList()

        val allParameters = constructorParameters + membersInjectorParameters

        return factoryLambda(
            allParameters,
            clazz.defaultType
        ) { lambda, parametersMap ->
            fun createExpr() = if (clazz.kind == ClassKind.OBJECT) {
                irGetObject(clazz.symbol)
            } else {
                irCall(constructor!!).apply {
                    constructorParameters
                        .map { parametersMap.getValue(it) }
                        .forEach { putValueArgument(it.index, irGet(it)) }
                }
            }

            if (membersInjector == null) {
                createExpr()
            } else {
                builder.irBlock {
                    val instance = irTemporary(createExpr())

                    val membersInjectorValueParametersByParameter = membersInjectorParameters
                        .map { it to parametersMap.getValue(it) }
                        .toMap()

                    +irCall(membersInjector).apply {
                        putValueArgument(0, irGet(instance))
                        membersInjectorParameters.forEachIndexed { index, valueParameter ->
                            putValueArgument(
                                index + 1,
                                irGet(
                                    membersInjectorValueParametersByParameter.getValue(
                                        valueParameter
                                    )
                                )
                            )
                        }
                    }

                    +irGet(instance)
                }
            }
        }
    }

    fun factoryLambda(
        parameters: List<FactoryParameter>,
        returnType: IrType,
        createExpr: IrBuilderWithScope.(
            IrFunction,
            Map<FactoryParameter, IrValueParameter>
        ) -> IrExpression
    ): IrExpression {
        val lambdaType = pluginContext.tmpFunction(parameters.size)
            .typeWith(
                parameters.map { parameter ->
                    if (parameter.assisted) parameter.type.withNoArgAnnotations(
                        pluginContext, listOf(InjektFqNames.AstAssisted)
                    ) else parameter.type
                } + returnType)

        return irLambda(lambdaType) { lambda ->
            var parameterIndex = 0
            val parametersMap = parameters.associateWith {
                lambda.valueParameters[parameterIndex++]
            }

            +irReturn(createExpr(this, lambda, parametersMap))
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
