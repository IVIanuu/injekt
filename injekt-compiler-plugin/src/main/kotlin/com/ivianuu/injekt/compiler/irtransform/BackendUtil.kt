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

package com.ivianuu.injekt.compiler.irtransform

import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.isMarkedAsReader
import com.ivianuu.injekt.compiler.checkers.isReader
import com.ivianuu.injekt.compiler.removeIllegalChars
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.ir.copyBodyTo
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedFunctionDescriptorWithContainerSource
import org.jetbrains.kotlin.ir.descriptors.WrappedPropertyGetterDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedPropertySetterDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrSpreadElement
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.AnnotationValue
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.BooleanValue
import org.jetbrains.kotlin.resolve.constants.ByteValue
import org.jetbrains.kotlin.resolve.constants.CharValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.DoubleValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.FloatValue
import org.jetbrains.kotlin.resolve.constants.IntValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.constants.LongValue
import org.jetbrains.kotlin.resolve.constants.NullValue
import org.jetbrains.kotlin.resolve.constants.ShortValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.withAbbreviation

@Reader
fun IrSymbol.irBuilder() = DeclarationIrBuilder(pluginContext, this)

@Reader
fun IrSymbolOwner.irBuilder() = symbol.irBuilder()

val IrType.typeArguments: List<IrTypeArgument>
    get() = (this as? IrSimpleType)?.arguments?.map { it } ?: emptyList()

val IrTypeArgument.typeOrFail: IrType
    get() = typeOrNull ?: error("Type is null for ${render()}")

fun IrTypeArgument.hasAnnotation(fqName: FqName): Boolean =
    typeOrNull?.hasAnnotation(fqName) == true

val IrMemberAccessExpression.typeArguments: List<IrType>
    get() =
        (0 until typeArgumentsCount).map { getTypeArgument(it)!! }

fun IrType.remapTypeParametersByName(
    source: IrTypeParametersContainer,
    target: IrTypeParametersContainer
): IrType =
    remapTypeParametersByName(
        source.typeParameters
            .map { it.descriptor.fqNameSafe }
            .zip(target.typeParameters)
            .toMap()
    )

fun IrType.remapTypeParametersByName(parametersMap: Map<FqName, IrTypeParameter>): IrType =
    when (this) {
        is IrSimpleType -> {
            when (val classifier = classifier.owner) {
                is IrTypeParameter -> {
                    val newClassifier =
                        parametersMap[classifier.descriptor.fqNameSafe] ?: classifier
                    IrSimpleTypeImpl(
                        makeKotlinType(
                            newClassifier.symbol,
                            arguments,
                            hasQuestionMark,
                            annotations,
                            abbreviation
                        ),
                        newClassifier.symbol,
                        hasQuestionMark,
                        arguments,
                        annotations,
                        abbreviation
                    )
                }
                is IrClass -> {
                    val arguments = arguments.map {
                        when (it) {
                            is IrTypeProjection -> makeTypeProjection(
                                it.type.remapTypeParametersByName(parametersMap),
                                it.variance
                            )
                            else -> it
                        }
                    }
                    IrSimpleTypeImpl(
                        makeKotlinType(
                            classifier.symbol,
                            arguments,
                            hasQuestionMark,
                            annotations,
                            abbreviation
                        ),
                        classifier.symbol,
                        hasQuestionMark,
                        arguments,
                        annotations,
                        abbreviation
                    )
                }
                else -> this
            }
        }
        else -> this
    }

fun IrType.substituteByFqName(substitutionMap: Map<FqName, IrType>): IrType {
    if (this !is IrSimpleType) return this

    substitutionMap.forEach { (fqName, type) ->
        if (fqName == classifier.descriptor.fqNameSafe)
            return type
    }

    val newArguments = arguments.map {
        if (it is IrTypeProjection) {
            makeTypeProjection(it.type.substituteByFqName(substitutionMap), it.variance)
        } else {
            it
        }
    }
    val newAnnotations = annotations.map { it.deepCopyWithSymbols() }

    val newAbbreviation = abbreviation?.substituteByFqName(substitutionMap)

    return IrSimpleTypeImpl(
        makeKotlinType(classifier, newArguments, hasQuestionMark, annotations, newAbbreviation),
        classifier,
        hasQuestionMark,
        newArguments,
        newAnnotations,
        newAbbreviation
    )
}

fun IrTypeAbbreviation.substituteByFqName(substitutionMap: Map<FqName, IrType>): IrTypeAbbreviation {
    val newArguments = arguments.map {
        if (it is IrTypeProjection) {
            makeTypeProjection(it.type.substituteByFqName(substitutionMap), it.variance)
        } else {
            it
        }
    }
    val newAnnotations = annotations.map { it.deepCopyWithSymbols() }

    return IrTypeAbbreviationImpl(
        typeAlias,
        hasQuestionMark,
        newArguments,
        newAnnotations
    )
}

fun IrClass.typeWith(arguments: List<IrType>): IrSimpleType {
    val finalArguments = arguments.map { makeTypeProjection(it, Variance.INVARIANT) }
    return IrSimpleTypeImpl(
        makeKotlinType(symbol, finalArguments, false, emptyList(), null),
        symbol,
        false,
        finalArguments,
        emptyList(),
        null
    )
}

fun makeKotlinType(
    classifier: IrClassifierSymbol,
    arguments: List<IrTypeArgument>,
    hasQuestionMark: Boolean,
    annotations: List<IrConstructorCall>,
    abbreviation: IrTypeAbbreviation?
): SimpleType {
    val kotlinTypeArguments = arguments.mapIndexed { index, it ->
        when (it) {
            is IrTypeProjection -> TypeProjectionImpl(it.variance, it.type.toKotlinType())
            is IrStarProjection -> StarProjectionImpl((classifier.descriptor as ClassDescriptor).typeConstructor.parameters[index])
            else -> error(it)
        }
    }
    return classifier.descriptor.defaultType
        .replace(
            newArguments = kotlinTypeArguments,
            newAnnotations = if (annotations.isEmpty()) Annotations.EMPTY
            else Annotations.create(annotations.map { it.toAnnotationDescriptor() })
        )
        .makeNullableAsSpecified(hasQuestionMark)
        .let { type ->
            if (abbreviation != null) {
                type.withAbbreviation(
                    abbreviation.typeAlias.descriptor.defaultType
                        .replace(
                            newArguments = abbreviation.arguments.mapIndexed { index, it ->
                                when (it) {
                                    is IrTypeProjection -> TypeProjectionImpl(
                                        it.variance,
                                        it.type.toKotlinType()
                                    )
                                    is IrStarProjection -> StarProjectionImpl((classifier.descriptor as ClassDescriptor).typeConstructor.parameters[index])
                                    else -> error(it)
                                }
                            },
                            newAnnotations = if (annotations.isEmpty()) Annotations.EMPTY
                            else Annotations.create(annotations.map { it.toAnnotationDescriptor() })
                        )
                )
            } else {
                type
            }
        }
}

fun IrConstructorCall.toAnnotationDescriptor(): AnnotationDescriptor {
    return AnnotationDescriptorImpl(
        symbol.owner.parentAsClass.defaultType.toKotlinType(),
        symbol.owner.valueParameters.map { it.name to getValueArgument(it.index) }
            .filter { it.second != null }
            .associate { it.first to it.second!!.toConstantValue() },
        /*TODO*/ SourceElement.NO_SOURCE
    )
}

fun IrElement.toConstantValue(): ConstantValue<*> {
    return when (this) {
        is IrConst<*> -> when (kind) {
            IrConstKind.Null -> NullValue()
            IrConstKind.Boolean -> BooleanValue(value as Boolean)
            IrConstKind.Char -> CharValue(value as Char)
            IrConstKind.Byte -> ByteValue(value as Byte)
            IrConstKind.Short -> ShortValue(value as Short)
            IrConstKind.Int -> IntValue(value as Int)
            IrConstKind.Long -> LongValue(value as Long)
            IrConstKind.String -> StringValue(value as String)
            IrConstKind.Float -> FloatValue(value as Float)
            IrConstKind.Double -> DoubleValue(value as Double)
        }
        is IrVararg -> {
            val elements =
                elements.map { if (it is IrSpreadElement) error("$it is not expected") else it.toConstantValue() }
            ArrayValue(elements) { moduleDescriptor ->
                // TODO: substitute.
                moduleDescriptor.builtIns.array.defaultType
            }
        }
        is IrGetEnumValue -> EnumValue(
            symbol.owner.parentAsClass.descriptor.classId!!,
            symbol.owner.name
        )
        is IrClassReference -> KClassValue(
            classType.classifierOrFail.descriptor.classId!!, /*TODO*/
            0
        )
        is IrConstructorCall -> AnnotationValue(this.toAnnotationDescriptor())
        else -> error("$this is not expected: ${this.dump()}")
    }
}

fun String.asNameId(): Name = Name.identifier(this)

@Reader
fun IrClass.getReaderConstructor(): IrConstructor? {
    constructors
        .firstOrNull {
            it.descriptor.isMarkedAsReader(given())
        }?.let { return it }
    if (!descriptor.isMarkedAsReader(given())) return null
    return primaryConstructor
}

fun List<ScopeWithIr>.thisOfClass(declaration: IrClass): IrValueParameter? {
    for (scope in reversed()) {
        when (val element = scope.irElement) {
            is IrFunction ->
                element.dispatchReceiverParameter?.let { if (it.type.classOrNull == declaration.symbol) return it }
            is IrClass -> if (element == declaration) return element.thisReceiver
        }
    }
    return null
}

fun IrDeclaration.isExternalDeclaration() = origin ==
        IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB ||
        origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB

fun IrPluginContext.tmpFunction(n: Int): IrClassSymbol =
    referenceClass(builtIns.getFunction(n).fqNameSafe)!!

fun IrPluginContext.tmpSuspendFunction(n: Int): IrClassSymbol =
    referenceClass(builtIns.getSuspendFunction(n).fqNameSafe)!!

@Reader
fun IrFunction.getFunctionType(skipContext: Boolean = false): IrType {
    val valueParameters = listOfNotNull(extensionReceiverParameter) + valueParameters
        .filter { !skipContext || it.name.asString() != "_context" }
    return (if (isSuspend) pluginContext.tmpSuspendFunction(valueParameters.size)
    else pluginContext.tmpFunction(valueParameters.size))
        .owner
        .typeWith(valueParameters.map { it.type } + returnType)
}

fun IrType.toKotlinType(): SimpleType {
    this as IrSimpleType
    return makeKotlinType(
        classifier,
        arguments,
        hasQuestionMark,
        annotations,
        abbreviation
    )
}

fun wrapDescriptor(descriptor: FunctionDescriptor): WrappedSimpleFunctionDescriptor {
    return when (descriptor) {
        is PropertyGetterDescriptor ->
            WrappedPropertyGetterDescriptor(
                descriptor.annotations,
                descriptor.source
            )
        is PropertySetterDescriptor ->
            WrappedPropertySetterDescriptor(
                descriptor.annotations,
                descriptor.source
            )
        is DescriptorWithContainerSource ->
            WrappedFunctionDescriptorWithContainerSource(descriptor.containerSource)
        else ->
            WrappedSimpleFunctionDescriptor(sourceElement = descriptor.source)
    }
}

@Reader
fun IrFunction.copy(): IrSimpleFunction {
    val descriptor = descriptor
    val newDescriptor = wrapDescriptor(descriptor)

    return IrFunctionImpl(
        startOffset,
        endOffset,
        origin,
        IrSimpleFunctionSymbolImpl(newDescriptor),
        name,
        visibility,
        descriptor.modality,
        returnType,
        isInline,
        isExternal,
        descriptor.isTailrec,
        isSuspend,
        descriptor.isOperator,
        isExpect,
        isFakeOverride
    ).also { fn ->
        newDescriptor.bind(fn)
        if (this is IrSimpleFunction) {
            fn.correspondingPropertySymbol = correspondingPropertySymbol
        }
        fn.parent = parent
        fn.copyTypeParametersFrom(this)
        fn.dispatchReceiverParameter = dispatchReceiverParameter?.copyTo(fn)
        fn.extensionReceiverParameter = extensionReceiverParameter?.copyTo(fn)
        fn.valueParameters = valueParameters.map { p ->
            p.copyTo(
                fn,
                name = p.name.asString().removeIllegalChars().asNameId(),
                type = p.type.remapTypeParametersByName(this, fn)
            )
        }
        fn.annotations = annotations.map { a -> a }
        fn.metadata = metadata
        fn.body = copyBodyTo(fn)
        fn.allParameters.forEach { valueParameter ->
            valueParameter.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    return allParameters
                        .mapIndexed { index, valueParameter -> valueParameter to index }
                        .singleOrNull { it.first.symbol == expression.symbol }
                        ?.let { fn.allParameters[it.second] }
                        ?.let { fn.symbol.irBuilder().irGet(it) }
                        ?: super.visitGetValue(expression)
                }
            })
        }
    }
}

@Reader
fun IrDeclarationWithName.canUseReaders(): Boolean =
    descriptor.isReader(given())

@Reader
fun IrBuilderWithScope.jvmNameAnnotation(
    name: String
): IrConstructorCall {
    val jvmName = pluginContext.referenceClass(DescriptorUtils.JVM_NAME)!!
    return irCall(jvmName.constructors.single()).apply {
        putValueArgument(0, irString(name))
    }
}

fun IrFunction.getContext(): IrClass? = getContextValueParameter()?.type?.classOrNull?.owner

fun IrFunction.getContextValueParameter() = valueParameters.singleOrNull {
    it.type.classOrNull?.owner?.hasAnnotation(InjektFqNames.ContextMarker) == true ||
            it.name.asString() == "_context"
}
