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

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrMetadataSourceOwner
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrSpreadElement
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
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
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

fun Annotated.hasAnnotation(fqName: FqName): Boolean {
    return annotations.hasAnnotation(fqName)
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

fun AnnotationDescriptor.hasAnnotation(annotation: FqName, module: ModuleDescriptor): Boolean {
    val thisFqName = this.fqName ?: return false
    val descriptor =
        module.findClassAcrossModuleDependencies(ClassId.topLevel(thisFqName)) ?: return false
    return descriptor.hasAnnotation(annotation)
}

fun IrType.withAnnotations(annotations: List<IrConstructorCall>): IrType {
    if (annotations.isEmpty()) return this
    this as IrSimpleType
    return copy(
        arguments = arguments,
        annotations = this.annotations + annotations.map { it.deepCopyWithSymbols() }
    )
}

fun IrType.remapTypeParameters(
    source: IrTypeParametersContainer,
    target: IrTypeParametersContainer,
    srcToDstParameterMap: Map<IrTypeParameter, IrTypeParameter>? = null
): IrType = when (this) {
    is IrSimpleType -> {
        val classifier = classifier.owner
        when {
            classifier is IrTypeParameter -> {
                val newClassifier =
                    srcToDstParameterMap?.get(classifier) ?: if (classifier.parent == source)
                        target.typeParameters[classifier.index]
                    else
                        classifier
                IrSimpleTypeImpl(
                    makeKotlinType(
                        newClassifier.symbol,
                        arguments,
                        hasQuestionMark,
                        annotations
                    ),
                    newClassifier.symbol,
                    hasQuestionMark,
                    arguments,
                    annotations
                )
            }

            classifier is IrClass -> {
                val arguments = arguments.map {
                    when (it) {
                        is IrTypeProjection -> makeTypeProjection(
                            it.type.remapTypeParameters(source, target, srcToDstParameterMap),
                            it.variance
                        )
                        else -> it
                    }
                }
                IrSimpleTypeImpl(
                    makeKotlinType(classifier.symbol, arguments, hasQuestionMark, annotations),
                    classifier.symbol,
                    hasQuestionMark,
                    arguments,
                    annotations
                )
            }

            else -> this
        }
    }
    else -> this
}

fun IrType.remapTypeParametersByName(
    source: IrTypeParametersContainer,
    target: IrTypeParametersContainer,
    srcToDstParameterMap: Map<IrTypeParameter, IrTypeParameter>? = null
): IrType = when (this) {
    is IrSimpleType -> {
        val classifier = classifier.owner
        when {
            classifier is IrTypeParameter -> {
                val newClassifier =
                    target.typeParameters.firstOrNull {
                        it.descriptor.name == classifier.descriptor.name
                    } ?: if (classifier.parent == source)
                        target.typeParameters[classifier.index]
                    else classifier
                IrSimpleTypeImpl(
                    makeKotlinType(
                        newClassifier.symbol,
                        arguments,
                        hasQuestionMark,
                        annotations
                    ),
                    newClassifier.symbol,
                    hasQuestionMark,
                    arguments,
                    annotations
                )
            }

            classifier is IrClass -> {
                val arguments = arguments.map {
                    when (it) {
                        is IrTypeProjection -> makeTypeProjection(
                            it.type.remapTypeParametersByName(source, target, srcToDstParameterMap),
                            it.variance
                        )
                        else -> it
                    }
                }
                IrSimpleTypeImpl(
                    makeKotlinType(classifier.symbol, arguments, hasQuestionMark, annotations),
                    classifier.symbol,
                    hasQuestionMark,
                    arguments,
                    annotations
                )
            }

            else -> this
        }
    }
    else -> this
}


fun IrType.substituteAndKeepQualifiers(substitutionMap: Map<IrTypeParameterSymbol, IrType>): IrType {
    if (this !is IrSimpleType) return this

    substitutionMap[classifier]?.let {
        return it.withAnnotations(annotations.map { it.deepCopyWithSymbols() })
    }

    val newArguments = arguments.map {
        if (it is IrTypeProjection) {
            makeTypeProjection(it.type.substituteAndKeepQualifiers(substitutionMap), it.variance)
        } else {
            it
        }
    }

    return IrSimpleTypeImpl(
        makeKotlinType(classifier, newArguments, hasQuestionMark, annotations),
        classifier,
        hasQuestionMark,
        newArguments,
        annotations.map { it.deepCopyWithSymbols() }
    )
}

fun makeKotlinType(
    classifier: IrClassifierSymbol,
    arguments: List<IrTypeArgument>,
    hasQuestionMark: Boolean,
    annotations: List<IrConstructorCall>
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
}

fun IrType.withNoArgAnnotations(pluginContext: IrPluginContext, qualifiers: List<FqName>): IrType {
    this as IrSimpleType
    return copy(
        arguments = arguments,
        annotations = annotations + qualifiers
            .map { pluginContext.referenceClass(it)!! }
            .map {
                DeclarationIrBuilder(pluginContext, it)
                    .irCall(it.constructors.single())
            }
    )
}

fun IrType.getQualifiers(): List<IrConstructorCall> {
    return annotations
        .filter {
            it.type.getClass()!!
                .descriptor
                .annotations
                .hasAnnotation(InjektFqNames.Qualifier)
        }
}

private fun IrType.copy(
    arguments: List<IrTypeArgument>,
    annotations: List<IrConstructorCall>
): IrType {
    return IrSimpleTypeImpl(
        makeKotlinType(classifierOrFail, arguments, isMarkedNullable(), annotations),
        classifierOrFail,
        isMarkedNullable(),
        arguments,
        annotations,
    )
}

val IrType.typeArguments: List<IrTypeArgument>
    get() = (this as? IrSimpleType)?.arguments?.map { it } ?: emptyList()

val IrTypeArgument.typeOrFail: IrType
    get() = typeOrNull ?: error("Type is null for ${render()}")

fun IrType.typeWith(vararg arguments: IrType): IrType = classifierOrFail.typeWith(*arguments)

fun IrClass.findPropertyGetter(
    name: String
): IrFunction {
    return functions
        .singleOrNull { function ->
            function.name.asString() == "get${name.capitalize()}" // todo fix
        } ?: error("Couldn't find property '$name' in ${dump()}")
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

fun IrType.isTypeParameter() = toKotlinType().isTypeParameter()

fun IrTypeArgument.hasAnnotation(fqName: FqName): Boolean =
    typeOrNull?.hasAnnotation(fqName) == true

fun IrType.getFunctionReturnType(): IrType = typeArguments.last().typeOrFail

fun IrType.getFunctionParameterTypes(): List<IrType> = typeArguments.dropLast(1)
    .map { it.typeOrFail }

val IrMemberAccessExpression.typeArguments: List<IrType>
    get() =
        (0 until typeArgumentsCount).map { getTypeArgument(it)!! }

fun <T> T.getClassesFromSingleArrayValueAnnotation(
    fqName: FqName,
    pluginContext: IrPluginContext
): List<IrClass> where T : IrDeclaration, T : IrAnnotationContainer {
    return getAnnotation(fqName)
        ?.getValueArgument(0)
        ?.let { it as IrVarargImpl }
        ?.elements
        ?.map { it as IrClassReference }
        ?.map { it.classType.classOrNull!! }
        ?.map { it.owner }
        ?: descriptor
            .annotations
            .findAnnotation(fqName)
            ?.allValueArguments
            ?.values
            ?.single()
            ?.let { it as ArrayValue }
            ?.value
            ?.filterIsInstance<KClassValue>()
            ?.map { it.getIrClass(pluginContext).symbol.owner }
            .let { it ?: emptyList() }
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
        ?.getClass()
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

val IrPluginContext.compositionsEnabled: Boolean
    get() = referenceClass(InjektFqNames.CompositionFactory) != null

fun String.asNameId(): Name = Name.identifier(this)

fun FqName.child(name: String) = child(name.asNameId())

fun IrClass.getInjectConstructor(): IrConstructor? {
    if (kind == ClassKind.OBJECT) return null
    constructors
        .firstOrNull {
            it.hasAnnotation(InjektFqNames.Transient) ||
                    it.hasAnnotation(InjektFqNames.Scoped)
        }?.let { return it }
    return constructors.singleOrNull()
}

fun <T> T.addMetadataIfNotLocal() where T : IrMetadataSourceOwner, T : IrDeclarationWithVisibility {
    if (visibility == Visibilities.LOCAL) return
    when (this) {
        is IrClassImpl -> metadata = MetadataSource.Class(descriptor)
        is IrFunctionImpl -> metadata = MetadataSource.Function(descriptor)
        is IrPropertyImpl -> metadata = MetadataSource.Property(descriptor)
    }
}

fun IrDeclaration.addToFileOrAbove(other: IrDeclarationWithVisibility) {
    if (other.visibility != Visibilities.LOCAL) {
        parent = other.file
        other.file.addChild(this)
    } else {
        parent = other.parent
        var block: IrStatementContainer? = null
        other.file.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitBlockBody(body: IrBlockBody): IrBody {
                if (body.statements.any { it === other }) {
                    block = body
                }
                return super.visitBlockBody(body)
            }

            override fun visitBlock(expression: IrBlock): IrExpression {
                if (expression.statements.any { it === other }) {
                    block = expression
                }
                return super.visitBlock(expression)
            }
        })

        if (block != null) {
            val index = block!!.statements.indexOf(other)
            block!!.statements.add(index, this)
        } else {
            error(
                "${dumpSrc()} has a corrupt parent\n${other.dumpSrc()} ours is \n ${parent.dumpSrc()}"
            )
        }
    }
}

fun IrDeclaration.addToParentOrAbove(other: IrDeclarationWithVisibility) {
    if (other.visibility != Visibilities.LOCAL) {
        parent = other.parent
        (other.parent as IrDeclarationContainer).addChild(this)
    } else {
        parent = other.parent
        var block: IrStatementContainer? = null
        other.file.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitBlockBody(body: IrBlockBody): IrBody {
                if (body.statements.any { it === other }) {
                    block = body
                }
                return super.visitBlockBody(body)
            }

            override fun visitBlock(expression: IrBlock): IrExpression {
                if (expression.statements.any { it === other }) {
                    block = expression
                }
                return super.visitBlock(expression)
            }
        })

        if (block != null) {
            val index = block!!.statements.indexOf(other)
            block!!.statements.add(index, this)
        } else {
            error(
                "${dumpSrc()} has a corrupt parent\n${other.dumpSrc()} ours is \n ${parent.dumpSrc()}"
            )
        }
    }
}

private val kindField by lazy {
    IrClassImpl::class.java
        .declaredFields
        .single { it.name == "kind" }
        .also { it.isAccessible = true }!!
}

fun IrClass.setClassKind(kind: ClassKind) {
    kindField.set(this as IrClassImpl, kind)
}

fun IrDeclaration.isExternalDeclaration() = origin ==
        IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB ||
        origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB

fun IrFunction.getFunctionType(pluginContext: IrPluginContext): IrType {
    return pluginContext.tmpFunction(valueParameters.size)
        .typeWith(valueParameters.map { it.type } + returnType)
}

fun IrFunction.getSuspendFunctionType(pluginContext: IrPluginContext): IrType {
    return pluginContext.tmpSuspendFunction(valueParameters.size)
        .typeWith(valueParameters.map { it.type } + returnType)
}

fun IrPluginContext.tmpFunction(n: Int): IrClassSymbol =
    referenceClass(builtIns.getFunction(n).fqNameSafe)!!

fun IrPluginContext.tmpSuspendFunction(n: Int): IrClassSymbol =
    referenceClass(builtIns.getSuspendFunction(n).fqNameSafe)!!

fun IrType.isProvider() = isFunction() && hasAnnotation(InjektFqNames.Provider)

fun IrType.isNoArgProvider() = isProvider() && typeArguments.size == 1

fun IrType.isAssistedProvider() = isProvider() && typeArguments.size > 1

inline fun <K, V> BindingTrace.getOrPut(
    slice: WritableSlice<K, V>,
    key: K,
    defaultValue: () -> V
): V {
    get(slice, key)?.let { return it }
    val value = defaultValue()
    record(slice, key, value)
    return value
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

fun nameWithoutIllegalChars(name: String): Name = name
    .replace(".", "")
    .replace("<", "")
    .replace(">", "")
    .replace(" ", "")
    .replace("[", "")
    .replace("]", "")
    .replace("@", "")
    .replace(",", "")
    .asNameId()
