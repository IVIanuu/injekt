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

import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.removeIllegalChars
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
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
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
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
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
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.isSuspendFunction
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
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.withAbbreviation

@Reader
val isInjektCompiler: Boolean
    get() = irModule.name.asString() == "<injekt-compiler-plugin>"

@Reader
fun IrSymbol.irBuilder() = DeclarationIrBuilder(pluginContext, this)

@Reader
fun IrSymbolOwner.irBuilder() = symbol.irBuilder()

fun IrAnnotationContainer.hasAnnotatedAnnotations(
    annotation: FqName
): Boolean = annotations.any { it.type.classOrNull!!.owner.hasAnnotation(annotation) }

val IrType.typeArguments: List<IrTypeArgument>
    get() = (this as? IrSimpleType)?.arguments?.map { it } ?: emptyList()

val IrTypeArgument.typeOrFail: IrType
    get() = typeOrNull ?: error("Type is null for ${render()}")

fun IrType.isTypeParameter() = toKotlinType().isTypeParameter()

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

@Reader
fun IrType.visitAllFunctionsWithSubstitutionMap(
    visitFunction: (IrFunction, Map<IrTypeParameterSymbol, IrType>) -> Unit
) {
    val clazz = classOrNull!!.owner
    val substitutionMap = clazz.typeParameters
        .map { it.symbol }
        .zip(typeArguments.map { it.typeOrFail })
        .toMap()
    clazz.functions
        .filter { it !is IrConstructor }
        .filter { it.dispatchReceiverParameter?.type != pluginContext.irBuiltIns.anyType }
        .forEach { visitFunction(it, substitutionMap) }
}

fun <T> IrAnnotationContainer.getConstantFromAnnotationOrNull(
    fqName: FqName,
    index: Int
) = getAnnotation(fqName)
    ?.getValueArgument(index)
    ?.let { it as IrConst<T> }
    ?.value

fun IrType.substitute(substitutionMap: Map<IrTypeParameterSymbol, IrType>): IrType {
    if (this !is IrSimpleType) return this

    substitutionMap[classifier]?.let {
        return it
    }

    val newArguments = arguments.map {
        if (it is IrTypeProjection) {
            makeTypeProjection(it.type.substitute(substitutionMap), it.variance)
        } else {
            it
        }
    }

    val newAnnotations = annotations.map { it.deepCopyWithSymbols() }
    return IrSimpleTypeImpl(
        makeKotlinType(classifier, arguments, hasQuestionMark, annotations, abbreviation),
        classifier,
        hasQuestionMark,
        newArguments,
        newAnnotations,
        abbreviation
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

fun <T> T.getClassFromAnnotation(
    fqName: FqName,
    index: Int
): IrClass? where T : IrDeclaration, T : IrAnnotationContainer {
    return getAnnotation(fqName)
        ?.getValueArgument(index)
        ?.let { it as? IrClassReferenceImpl }
        ?.classType
        ?.classOrNull
        ?.owner
}

fun String.asNameId(): Name = Name.identifier(this)

fun IrClass.getReaderConstructor(): IrConstructor? {
    constructors
        .firstOrNull {
            it.isMarkedAsReader()
        }?.let { return it }
    if (!isMarkedAsReader()) return null
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


fun IrType.uniqueTypeName(): Name {
    fun IrType.renderName(includeArguments: Boolean = true): String {
        return buildString {
            val qualifier = getConstantFromAnnotationOrNull<String>(InjektFqNames.Qualifier, 0)
            if (qualifier != null) append("${qualifier}_")

            val fqName = if (this@renderName is IrSimpleType && abbreviation != null)
                abbreviation!!.typeAlias.descriptor.fqNameSafe
            else classifierOrFail.descriptor.fqNameSafe
            append(fqName.pathSegments().joinToString("_") { it.asString() })

            if (includeArguments) {
                typeArguments.forEachIndexed { index, typeArgument ->
                    if (index == 0) append("_")
                    append(typeArgument.typeOrNull?.renderName() ?: "star")
                    if (index != typeArguments.lastIndex) append("_")
                }
            }
        }
    }

    val fullTypeName = renderName()

    // Conservatively shorten the name if the length exceeds 128
    return (if (fullTypeName.length <= 128) fullTypeName
    else ("${renderName(includeArguments = false)}_${fullTypeName.hashCode()}"))
        .removeIllegalChars()
        .asNameId()
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

fun IrDeclarationWithName.isMarkedAsReader(): Boolean =
    hasAnnotation(InjektFqNames.Reader) ||
            hasAnnotation(InjektFqNames.Given) ||
            hasAnnotation(InjektFqNames.GivenMapEntries) ||
            hasAnnotation(InjektFqNames.GivenSetElements) ||
            hasAnnotatedAnnotations(InjektFqNames.Effect)

@Reader
fun IrDeclarationWithName.canUseReaders(): Boolean =
    (this is IrFunction && !isExternalDeclaration() && getContext() != null) ||
            isMarkedAsReader() ||
            (this is IrClass && constructors.any { it.isMarkedAsReader() }) ||
            (this is IrConstructor && constructedClass.isMarkedAsReader()) ||
            (this is IrSimpleFunction && correspondingPropertySymbol?.owner?.isMarkedAsReader() == true)

@Reader
fun IrBuilderWithScope.irLambda(
    type: IrType,
    startOffset: Int = UNDEFINED_OFFSET,
    endOffset: Int = UNDEFINED_OFFSET,
    body: IrBuilderWithScope.(IrFunction) -> IrExpression
): IrExpression {
    val returnType = type.typeArguments.last().typeOrNull!!

    val lambda = buildFun {
        origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        name = Name.special("<anonymous>")
        this.returnType = returnType
        visibility = Visibilities.LOCAL
        isSuspend = type.isSuspendFunction()
    }.apply {
        parent = scope.getLocalDeclarationParent()
        type.typeArguments.dropLast(1).forEachIndexed { index, typeArgument ->
            addValueParameter(
                "p$index",
                typeArgument.typeOrNull!!
            )
        }
        annotations += type.annotations.map {
            it.deepCopyWithSymbols()
        }
        this.body = irBuilder().run { irExprBody(body(this, this@apply)) }
    }

    return IrFunctionExpressionImpl(
        startOffset = startOffset,
        endOffset = endOffset,
        type = type,
        function = lambda,
        origin = IrStatementOrigin.LAMBDA
    )
}

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
