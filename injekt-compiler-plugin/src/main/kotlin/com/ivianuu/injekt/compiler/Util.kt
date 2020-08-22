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

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.transform.InjektContext
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
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.incremental.components.LocationInfo
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.record
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
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrMetadataSourceOwner
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.descriptors.WrappedFunctionDescriptorWithContainerSource
import org.jetbrains.kotlin.ir.descriptors.WrappedPropertyGetterDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedPropertySetterDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
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
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
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
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.isSuspendFunction
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
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
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.withAbbreviation
import kotlin.math.absoluteValue

var lookupTracker: LookupTracker? = null

fun recordLookup(
    source: IrElement,
    lookedUp: IrDeclarationWithName
) {
    val sourceKtElement = (source as? IrDeclarationWithName)?.descriptor?.findPsi() as? KtElement
    val location = sourceKtElement?.let { KotlinLookupLocation(it) }
        ?: object : LookupLocation {
            override val location: LocationInfo?
                get() = object : LocationInfo {
                    override val filePath: String
                        get() = (source as? IrFile)?.path
                            ?: (source as IrDeclarationWithName).file.path
                    override val position: Position
                        get() = Position.NO_POSITION
                }
        }

    lookupTracker!!.record(
        location,
        lookedUp.getPackageFragment()!!.packageFragmentDescriptor,
        lookedUp.name
    )
}

fun IrBuilderWithScope.irCallAndRecordLookup(
    caller: IrDeclarationWithName,
    symbol: IrFunctionSymbol
) = irCall(symbol).apply {
    recordLookup(caller, symbol.owner)
}

fun IrAnnotationContainer.hasAnnotatedAnnotations(
    annotation: FqName
): Boolean = annotations.any { it.type.classOrNull!!.owner.hasAnnotation(annotation) }

fun IrAnnotationContainer.getAnnotatedAnnotations(
    annotation: FqName
): List<IrConstructorCall> =
    annotations.filter {
        it.type.classOrNull!!.owner.hasAnnotation(annotation)
    }

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

fun IrType.copy(
    classifier: IrClassifierSymbol = classifierOrFail,
    isMarkedNullable: Boolean = this.isMarkedNullable(),
    arguments: List<IrTypeArgument> = (this as IrSimpleType).arguments,
    annotations: List<IrConstructorCall> = this.annotations,
    abbreviation: IrTypeAbbreviation? = (this as IrSimpleType).abbreviation,
): IrType {
    return IrSimpleTypeImpl(
        makeKotlinType(classifier, arguments, isMarkedNullable, annotations, abbreviation),
        classifier,
        isMarkedNullable,
        arguments,
        annotations,
    )
}

fun IrType.remapTypeParametersByName(
    source: IrTypeParametersContainer,
    target: IrTypeParametersContainer,
    srcToDstParameterMap: Map<IrTypeParameter, IrTypeParameter>? = null
): IrType =
    when (this) {
        is IrSimpleType -> {
            when (val classifier = classifier.owner) {
                is IrTypeParameter -> {
                    val newClassifier =
                        srcToDstParameterMap?.get(classifier)
                            ?: if ((classifier.parent as IrDeclarationWithName).descriptor.fqNameSafe ==
                                (source as IrDeclarationWithName).descriptor.fqNameSafe
                            )
                                target.typeParameters[classifier.index]
                            else
                                classifier
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
                                it.type.remapTypeParametersByName(
                                    source,
                                    target,
                                    srcToDstParameterMap
                                ),
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

fun IrClass.getAllClasses(): Set<IrClass> {
    val classes = mutableSetOf<IrClass>()

    fun collect(clazz: IrClass) {
        classes += clazz
        clazz
            .superTypes
            .map { it.classOrNull!!.owner }
            .forEach { collect(it) }
    }

    collect(this)

    return classes
}

fun IrClass.getAllFunctions(): List<IrFunction> {
    val functions = mutableListOf<IrFunction>()
    val processedSuperTypes = mutableSetOf<IrType>()
    fun collectFunctions(superType: IrClass) {
        if (superType.defaultType in processedSuperTypes) return
        processedSuperTypes += superType.defaultType

        for (declaration in superType.declarations.toList()) {
            if (declaration !is IrFunction) continue
            if (declaration is IrConstructor) continue
            if (declaration.dispatchReceiverParameter?.type?.classOrNull?.descriptor?.fqNameSafe?.asString() ==
                "kotlin.Any"
            ) continue
            functions += declaration
        }

        superType.superTypes
            .map { it.classOrNull!!.owner }
            .forEach { collectFunctions(it) }
    }

    collectFunctions(this)

    return functions
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

    substitutionMap[classifier]?.let { return it }

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

fun IrType.substituteByName(substitutionMap: Map<IrTypeParameterSymbol, IrType>): IrType {
    if (this !is IrSimpleType) return this

    substitutionMap.toList()
        .firstOrNull { it.first.descriptor.name == classifier.descriptor.name }
        ?.second
        ?.let { return it }

    val newArguments = arguments.map {
        if (it is IrTypeProjection) {
            makeTypeProjection(it.type.substituteByName(substitutionMap), it.variance)
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
        .let {
            if (abbreviation != null) {
                it.withAbbreviation(
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
                it
            }
        }
}

fun IrConstructorCall.toAnnotationDescriptor(): AnnotationDescriptor {
    assert(symbol.owner.parentAsClass.isAnnotationClass) {
        "Expected call to constructor of annotation class but was: ${this.dump()}"
    }

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

fun IrBuilderWithScope.irClassReference(
    clazz: IrClass
) = IrClassReferenceImpl(
    UNDEFINED_OFFSET,
    UNDEFINED_OFFSET,
    context.irBuiltIns.kClassClass.typeWith(clazz.defaultType),
    clazz.symbol,
    clazz.defaultType
)

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

fun IrClass.getReaderConstructor(injektContext: InjektContext): IrConstructor? {
    constructors
        .firstOrNull {
            it.isMarkedAsReader(injektContext)
        }?.let { return it }
    if (!isMarkedAsReader(injektContext)) return null
    return primaryConstructor
}

fun <T> T.addMetadataIfNotLocal() where T : IrMetadataSourceOwner, T : IrDeclarationWithVisibility {
    if (visibility == Visibilities.LOCAL) return
    when (this) {
        is IrClassImpl -> metadata = MetadataSource.Class(descriptor)
        is IrFunctionImpl -> metadata = MetadataSource.Function(descriptor)
        is IrPropertyImpl -> metadata = MetadataSource.Property(descriptor)
    }
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

fun IrPluginContext.tmpKFunction(n: Int): IrClassSymbol =
    referenceClass(builtIns.getKFunction(n).fqNameSafe)!!

fun IrPluginContext.tmpFunction(n: Int): IrClassSymbol =
    referenceClass(builtIns.getFunction(n).fqNameSafe)!!

fun IrPluginContext.tmpSuspendFunction(n: Int): IrClassSymbol =
    referenceClass(builtIns.getSuspendFunction(n).fqNameSafe)!!

fun IrPluginContext.tmpSuspendKFunction(n: Int): IrClassSymbol =
    referenceClass(builtIns.getKSuspendFunction(n).fqNameSafe)!!

fun IrFunction.getFunctionType(injektContext: InjektContext): IrType {
    return (if (isSuspend) injektContext.tmpSuspendFunction(valueParameters.size)
    else injektContext.tmpFunction(valueParameters.size))
        .typeWith(valueParameters.map { it.type } + returnType)
}

fun getJoinedName(
    packageFqName: FqName,
    fqName: FqName
): Name {
    val joinedSegments = fqName.asString()
        .removePrefix(packageFqName.asString() + ".")
        .split(".")
    return joinedSegments.joinToString("_").asNameId()
}

fun String.removeIllegalChars() =
    replace(".", "")
        .replace("<", "")
        .replace(">", "")
        .replace(" ", "")
        .replace("[", "")
        .replace("]", "")
        .replace("@", "")
        .replace(",", "")
        .replace(" ", "")
        .replace("-", "")

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

fun IrFunction.copy(injektContext: InjektContext): IrSimpleFunction {
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
                        ?.let { DeclarationIrBuilder(injektContext, fn.symbol).irGet(it) }
                        ?: super.visitGetValue(expression)
                }
            })
        }
    }
}

fun IrDeclarationWithName.uniqueKey() = when (this) {
    is IrClass -> "${descriptor.fqNameSafe}__class"
    is IrField -> "${descriptor.fqNameSafe}__field"
    is IrFunction -> "${descriptor.fqNameSafe}__function${
    ((metadata as? MetadataSource.Function)?.descriptor ?: descriptor).valueParameters
        .filterNot { it.name == getContextValueParameter()?.name }
        .map { it.type }.map {
            it.constructor.declarationDescriptor!!.fqNameSafe
        }.hashCode().absoluteValue
    }${if (visibility == Visibilities.LOCAL) "_$startOffset" else ""}"
    is IrProperty -> "${descriptor.fqNameSafe}__property"
    is IrValueParameter -> "${descriptor.fqNameSafe}__valueparameter"
    is IrVariableImpl -> "${descriptor.fqNameSafe}__variable"
    else -> error("Unsupported declaration ${dump()}")
}

fun IrDeclarationWithName.isMarkedAsReader(injektContext: InjektContext): Boolean =
    isReader(injektContext) ||
            hasAnnotation(InjektFqNames.Given) ||
            hasAnnotation(InjektFqNames.GivenMapEntries) ||
            hasAnnotation(InjektFqNames.GivenSetElements) ||
            hasAnnotatedAnnotations(InjektFqNames.Effect)

private fun IrDeclarationWithName.isReader(injektContext: InjektContext): Boolean {
    if (hasAnnotation(InjektFqNames.Reader)) return true
    return try {
        injektContext.readerChecker.isReader(descriptor, injektContext.bindingTrace)
    } catch (e: Exception) {
        false
    }
}

fun IrFunctionAccessExpression.isReaderLambdaInvoke(
    injektContext: InjektContext
): Boolean {
    return symbol.owner.name.asString() == "invoke" &&
            (dispatchReceiver?.type?.hasAnnotation(InjektFqNames.Reader) == true ||
                    injektContext.irTrace[InjektWritableSlices.IS_READER_LAMBDA_INVOKE, this] == true)
}

fun IrDeclarationWithName.canUseReaders(
    injektContext: InjektContext
): Boolean =
    (!hasAnnotation(InjektFqNames.Signature) && (this is IrFunction && !isExternalDeclaration() && getContext() != null) ||
            isMarkedAsReader(injektContext) ||
            (this is IrConstructor && constructedClass.isMarkedAsReader(injektContext)) ||
            (this is IrSimpleFunction && correspondingPropertySymbol?.owner?.isMarkedAsReader(
                injektContext
            ) == true))

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
        this.body =
            DeclarationIrBuilder(context, symbol).run {
                irExprBody(body(this, this@apply))
            }
    }

    return IrFunctionExpressionImpl(
        startOffset = startOffset,
        endOffset = endOffset,
        type = type,
        function = lambda,
        origin = IrStatementOrigin.LAMBDA
    )
}

fun IrBuilderWithScope.jvmNameAnnotation(
    name: String,
    injektContext: InjektContext
): IrConstructorCall {
    val jvmName = injektContext.referenceClass(DescriptorUtils.JVM_NAME)!!
    return irCall(jvmName.constructors.single()).apply {
        putValueArgument(0, irString(name))
    }
}

fun IrFunction.getContext(): IrClass? = getContextValueParameter()?.type?.classOrNull?.owner

fun IrFunction.getContextValueParameter() = valueParameters.singleOrNull {
    it.type.classOrNull?.owner?.hasAnnotation(InjektFqNames.ContextMarker) == true
}

fun IrModuleFragment.addFile(
    injektContext: InjektContext,
    fqName: FqName
): IrFile {
    val file = IrFileImpl(
        fileEntry = NaiveSourceBasedFileEntryImpl(
            fqName.shortName().asString() + ".kt",
            intArrayOf()
        ),
        symbol = IrFileSymbolImpl(
            object : PackageFragmentDescriptorImpl(
                injektContext.moduleDescriptor,
                fqName.parent()
            ) {
                override fun getMemberScope(): MemberScope = MemberScope.Empty
            }
        ),
        fqName = fqName.parent()
    ).apply {
        metadata = MetadataSource.File(emptyList())
    }

    files += file

    return file
}
