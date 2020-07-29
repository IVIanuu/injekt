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

import com.ivianuu.injekt.compiler.analysis.ImplicitChecker
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.ir.copyBodyTo
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
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
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrMetadataSourceOwner
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
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
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
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
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
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
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.withAbbreviation
import kotlin.math.absoluteValue

var lookupTracker: LookupTracker? = null

fun recordLookup(
    source: IrDeclarationWithName,
    lookedUp: IrDeclarationWithName
) {
    val sourceKtElement = source.descriptor.findPsi() as? KtElement
    val location = sourceKtElement?.let { KotlinLookupLocation(it) }
        ?: object : LookupLocation {
            override val location: LocationInfo?
                get() = object : LocationInfo {
                    override val filePath: String
                        get() = source.file.path
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

fun Annotated.hasAnnotation(fqName: FqName): Boolean {
    return annotations.hasAnnotation(fqName)
}

fun AnnotationDescriptor.hasAnnotation(annotation: FqName, module: ModuleDescriptor): Boolean {
    val thisFqName = this.fqName ?: return false
    val descriptor =
        module.findClassAcrossModuleDependencies(ClassId.topLevel(thisFqName)) ?: return false
    return descriptor.hasAnnotation(annotation)
}

fun Annotated.hasAnnotatedAnnotations(
    annotation: FqName,
    module: ModuleDescriptor
): Boolean = annotations.any { it.hasAnnotation(annotation, module) }

fun Annotated.getAnnotatedAnnotations(
    annotation: FqName,
    module: ModuleDescriptor
): List<AnnotationDescriptor> =
    annotations.filter {
        it.hasAnnotation(annotation, module)
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

inline fun <T, R> Iterable<T>.flatMapFix(
    block: (T) -> Iterable<R>
): List<R> = flatMap { block(it) }.toList()

val IrType.typeArguments: List<IrTypeArgument>
    get() = (this as? IrSimpleType)?.arguments?.map { it } ?: emptyList()

val IrTypeArgument.typeOrFail: IrType
    get() = typeOrNull ?: error("Type is null for ${render()}")

fun IrType.typeWith(vararg arguments: IrType): IrType = classifierOrFail.typeWith(*arguments)

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
            val classifier = classifier.owner
            when {
                classifier is IrTypeParameter -> {
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

                classifier is IrClass -> {
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

fun <T> T.getClassFromSingleValueAnnotation(
    fqName: FqName,
    pluginContext: IrPluginContext
): IrClass where T : IrDeclaration, T : IrAnnotationContainer {
    return getClassFromSingleValueAnnotationOrNull(fqName, pluginContext)
        ?: error("Cannot get class value for $fqName for ${dump()}")
}

fun <T> T.getClassFromSingleValueAnnotationOrNull(
    fqName: FqName,
    pluginContext: IrPluginContext
): IrClass? where T : IrDeclaration, T : IrAnnotationContainer {
    return getAnnotation(fqName)
        ?.getValueArgument(0)
        ?.let { it as IrClassReferenceImpl }
        ?.classType
        ?.classOrNull
        ?.owner
        ?: descriptor.annotations.findAnnotation(fqName)
            ?.allValueArguments
            ?.values
            ?.singleOrNull()
            ?.let { it as KClassValue }
            ?.getIrClass(pluginContext)
}

fun KClassValue.getIrClass(
    pluginContext: IrPluginContext
): IrClass {
    return (value as KClassValue.Value.NormalClass).classId.asSingleFqName()
        .let { FqName(it.asString().replace("\$", ".")) }
        .let { pluginContext.referenceClass(it)!!.owner }
}

fun String.asNameId(): Name = Name.identifier(this)

fun IrClass.getReaderConstructor(pluginContext: IrPluginContext): IrConstructor? {
    constructors
        .firstOrNull {
            it.isMarkedAsImplicit(pluginContext)
        }?.let { return it }
    if (!isMarkedAsImplicit(pluginContext)) return null
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

fun IrPluginContext.tmpFunction(n: Int): IrClassSymbol =
    referenceClass(builtIns.getFunction(n).fqNameSafe)!!

fun IrPluginContext.tmpSuspendFunction(n: Int): IrClassSymbol =
    referenceClass(builtIns.getSuspendFunction(n).fqNameSafe)!!

fun IrFunction.getFunctionType(pluginContext: IrPluginContext): IrType {
    return (if (isSuspend) pluginContext.tmpSuspendFunction(valueParameters.size)
    else pluginContext.tmpFunction(valueParameters.size))
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

fun IrType.readableName(): Name = buildString {
    fun IrType.renderName() {
        val qualifier = getAnnotation(InjektFqNames.Qualifier)
            ?.getValueArgument(0)
            ?.let { it as IrConst<String> }
            ?.value

        if (qualifier != null) {
            append("${qualifier}_")
        }

        val fqName = if (this is IrSimpleType && abbreviation != null &&
            abbreviation!!.typeAlias.descriptor.hasAnnotation(InjektFqNames.Distinct)
        ) abbreviation!!.typeAlias.descriptor.fqNameSafe
        else classifierOrFail.descriptor.fqNameSafe
        append(
            fqName.pathSegments().map { it.asString() }
                .joinToString("_")
        )

        typeArguments.forEachIndexed { index, typeArgument ->
            if (index == 0) append("_")
            typeArgument.typeOrNull?.renderName() ?: append("star")
            if (index != typeArguments.lastIndex) append("_")
        }
    }

    renderName()
}.removeIllegalChars().asNameId()

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

fun IrFunction.copy(pluginContext: IrPluginContext): IrSimpleFunction {
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
                        ?.let { DeclarationIrBuilder(pluginContext, fn.symbol).irGet(it) }
                        ?: super.visitGetValue(expression)
                }
            })
        }
    }
}

fun IrDeclarationWithName.uniqueName() = when (this) {
    is IrClass -> "${descriptor.fqNameSafe}__c"
    is IrFunction -> "${descriptor.fqNameSafe}__f${
    descriptor.valueParameters
        .filterNot { it.name.asString() == "_context" }
        .map { it.type }.map {
            it.constructor.declarationDescriptor!!.fqNameSafe
        }.hashCode().absoluteValue
    }${if (visibility == Visibilities.LOCAL) "_$startOffset" else ""}"
    is IrProperty -> "${descriptor.fqNameSafe}__p"
    else -> error("Unsupported declaration ${dump()}")
}

fun Annotated.isMarkedAsImplicit(): Boolean =
    hasAnnotation(InjektFqNames.Reader) ||
            hasAnnotation(InjektFqNames.Given) ||
            hasAnnotation(InjektFqNames.MapEntries) ||
            hasAnnotation(InjektFqNames.SetElements)

fun IrDeclarationWithName.isMarkedAsImplicit(pluginContext: IrPluginContext): Boolean =
    isReader(pluginContext) ||
            hasAnnotation(InjektFqNames.Given) ||
            hasAnnotation(InjektFqNames.MapEntries) ||
            hasAnnotation(InjektFqNames.SetElements)

private fun IrDeclarationWithName.isReader(pluginContext: IrPluginContext): Boolean {
    if (hasAnnotation(InjektFqNames.Reader)) return true
    val implicitChecker = ImplicitChecker()
    val bindingTrace = DelegatingBindingTrace(pluginContext.bindingContext, "Injekt IR")
    return try {
        implicitChecker.isImplicit(descriptor, bindingTrace)
    } catch (e: Exception) {
        false
    }
}

fun IrFunctionAccessExpression.isReaderLambdaInvoke(
    pluginContext: IrPluginContext
): Boolean {
    return symbol.owner.name.asString() == "invoke" &&
            (dispatchReceiver?.type?.hasAnnotation(InjektFqNames.Reader) == true ||
                    pluginContext.irTrace[InjektWritableSlices.IS_READER_LAMBDA_INVOKE, this] == true)
}

val IrType.distinctedType: Any
    get() = (this as? IrSimpleType)?.abbreviation
        ?.typeAlias
        ?.takeIf {
            it.descriptor.hasAnnotation(InjektFqNames.Distinct)
        }
        ?: this

fun IrDeclarationWithName.canUseImplicits(
    pluginContext: IrPluginContext
): Boolean =
    (this is IrFunction && valueParameters.any { it.name.asString() == "_context" }) ||
            isMarkedAsImplicit(pluginContext) ||
            (this is IrConstructor && constructedClass.isMarkedAsImplicit(pluginContext)) ||
            (this is IrSimpleFunction && correspondingPropertySymbol?.owner?.isMarkedAsImplicit(
                pluginContext
            ) == true)

fun compareTypeWithDistinct(
    a: IrType?,
    b: IrType?
): Boolean = a?.hashWithDistinct() == b?.hashWithDistinct()

fun IrType.hashWithDistinct(): Int {
    var result = 0
    val distinctedType = distinctedType
    if (distinctedType is IrSimpleType) {
        result += 31 * distinctedType.classifier.hashCode()
        result += 31 * distinctedType.arguments.map { it.typeOrNull?.hashWithDistinct() ?: 0 }
            .hashCode()
    } else {
        result += 31 * distinctedType.hashCode()
    }

    val qualifier = getAnnotation(InjektFqNames.Qualifier)
        ?.getValueArgument(0)
        ?.let { it as IrConst<String> }
        ?.value

    if (qualifier != null) {
        result += 31 * qualifier.hashCode()
    }

    return result
}

fun IrBuilderWithScope.singleClassArgConstructorCall(
    clazz: IrClassSymbol,
    value: IrClassifierSymbol
): IrConstructorCall =
    irCall(clazz.constructors.single()).apply {
        putValueArgument(
            0,
            IrClassReferenceImpl(
                startOffset, endOffset,
                context.irBuiltIns.kClassClass.typeWith(value.defaultType),
                value,
                value.defaultType
            )
        )
    }

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
    pluginContext: IrPluginContext
): IrConstructorCall {
    val jvmName = pluginContext.referenceClass(DescriptorUtils.JVM_NAME)!!
    return irCall(jvmName.constructors.single()).apply {
        putValueArgument(0, irString(name))
    }
}

fun FunctionDescriptor.getFunctionType(): KotlinType {
    return (if (isSuspend) builtIns.getSuspendFunction(valueParameters.size)
    else builtIns.getFunction(valueParameters.size))
        .defaultType
        .replace(newArguments = valueParameters.map { it.type.asTypeProjection() } + returnType!!.asTypeProjection())
}

fun IrFunction.getContext(): IrClass? = valueParameters.singleOrNull {
    it.name.asString() == "_context"
}?.type?.classOrNull?.owner