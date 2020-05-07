package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrSpreadElement
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
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
import org.jetbrains.kotlin.ir.util.IrProvider
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
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
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.replace

fun DeclarationDescriptor.hasAnnotatedAnnotations(annotation: FqName): Boolean =
    annotations.any { it.hasAnnotation(annotation, module) }

fun DeclarationDescriptor.getAnnotatedAnnotations(annotation: FqName): List<AnnotationDescriptor> =
    annotations.filter {
        it.hasAnnotation(annotation, module)
    }

fun AnnotationDescriptor.hasAnnotation(annotation: FqName, module: ModuleDescriptor): Boolean {
    val thisFqName = this.fqName ?: return false
    val descriptor =
        module.findClassAcrossModuleDependencies(ClassId.topLevel(thisFqName)) ?: return false
    return descriptor.annotations.hasAnnotation(annotation)
}

fun IrType.withAnnotations(annotations: List<IrConstructorCall>): IrType {
    if (annotations.isEmpty()) return this
    this as IrSimpleType
    return replace(
        newArguments = arguments,
        newAnnotations = this.annotations + annotations
    )
}

fun IrType.withNoArgQualifiers(symbols: InjektSymbols, qualifiers: List<FqName>): IrType {
    this as IrSimpleType
    return replace(
        newArguments = arguments,
        newAnnotations = annotations + qualifiers
            .map { symbols.getTopLevelClass(it) }
            .map {
                DeclarationIrBuilder(symbols.pluginContext, it)
                    .irCall(it.constructors.single())
            }
    )
}

fun IrType.getQualifiers(): List<IrConstructorCall> {
    return annotations
        .filter {
            it.type.classOrFail
                .descriptor
                .annotations
                .hasAnnotation(InjektFqNames.Qualifier)
        }
}

fun IrType.getQualifierFqNames(): List<FqName> =
    getQualifiers().map { it.type.classOrFail.descriptor.fqNameSafe }

private fun IrType.replace(
    newArguments: List<IrTypeArgument>,
    newAnnotations: List<IrConstructorCall>
): IrType {
    val kotlinType = KotlinTypeFactory.simpleType(
        toKotlinType() as SimpleType,
        arguments = newArguments.mapIndexed { index, it ->
            when (it) {
                is IrTypeProjection -> TypeProjectionImpl(it.variance, it.type.toKotlinType())
                is IrStarProjection -> StarProjectionImpl((classifierOrFail.descriptor as ClassDescriptor).typeConstructor.parameters[index])
                else -> error(it)
            }
        },
        annotations = Annotations.create(
            newAnnotations.map { it.toAnnotationDescriptor() }
        )
    )
    return IrSimpleTypeImpl(
        kotlinType,
        classifierOrFail,
        isMarkedNullable(),
        newArguments,
        newAnnotations,
    )
}

val IrType.typeArguments: List<IrType>
    get() = (this as? IrSimpleType)?.arguments?.map { it.type } ?: emptyList()

val IrTypeArgument.type get() = typeOrNull ?: error("Type is null for ${render()}")

val IrType.classOrFail get() = classOrNull ?: error("Cannot get class for ${render()}")

fun IrType.typeWith(vararg arguments: IrType): IrType = classifierOrFail.typeWith(*arguments)

fun IrType.hashCodeWithQualifiers(): Int {
    var result = hashCode()
    result = 31 * result + qualifiersHash()
    return result
}

fun IrType?.equalsWithQualifiers(other: Any?): Boolean {
    if (this === other) return true
    if (this?.javaClass != other?.javaClass) return false
    other as IrType

    if (this != other) return false

    if (!getQualifiers().qualifiersEquals(other.getQualifiers())) return false

    if (this is IrSimpleType) {
        other as IrSimpleType
        arguments
            .forEachIndexed { index, argument ->
                if (!argument.type.equalsWithQualifiers(other.arguments[index].type)) {
                    return false
                }
            }
    }

    return true
}

private fun IrType.qualifiersHash() = getQualifiers()
    .map { it.hash() }
    .hashCode()

private fun List<IrConstructorCall>.qualifiersEquals(other: List<IrConstructorCall>): Boolean {
    if (size != other.size) return false
    for (i in indices) {
        val thisAnnotation = this[i].toAnnotationDescriptor()
        val otherAnnotation = other[i].toAnnotationDescriptor()
        if (thisAnnotation.fqName != otherAnnotation.fqName) return false
        val thisValues = thisAnnotation.allValueArguments.entries.toList()
        val otherValues = otherAnnotation.allValueArguments.entries.toList()
        if (thisValues.size != otherValues.size) return false
        for (j in thisValues.indices) {
            val thisValue = thisValues[j]
            val otherValue = otherValues[j]
            if (thisValue.key != otherValue.key) return false
            if (thisValue.value.value != otherValue.value.value) return false
        }
    }

    return true
}

private fun IrConstructorCall.hash(): Int {
    var result = type.hashCode()
    result = 31 * result + toAnnotationDescriptor()
        .allValueArguments
        .map { it.key to it.value.value }
        .hashCode()
    return result
}


fun <T : IrSymbol> T.ensureBound(irProviders: List<IrProvider>): T {
    if (!this.isBound) irProviders.forEach { it.getDeclaration(this) }
    check(this.isBound) { "$this is not bound" }
    return this
}

val SymbolTable.allUnbound: List<IrSymbol>
    get() {
        val r = mutableListOf<IrSymbol>()
        r.addAll(unboundClasses)
        r.addAll(unboundConstructors)
        r.addAll(unboundEnumEntries)
        r.addAll(unboundFields)
        r.addAll(unboundSimpleFunctions)
        r.addAll(unboundProperties)
        r.addAll(unboundTypeParameters)
        r.addAll(unboundTypeAliases)
        return r
    }

fun generateSymbols(pluginContext: IrPluginContext) {
    lateinit var unbound: List<IrSymbol>
    val visited = mutableSetOf<IrSymbol>()
    do {
        unbound = pluginContext.symbolTable.allUnbound

        for (symbol in unbound) {
            if (visited.contains(symbol)) {
                continue
            }
            // Symbol could get bound as a side effect of deserializing other symbols.
            if (!symbol.isBound) {
                pluginContext.irProviders.forEach { it.getDeclaration(symbol) }
            }
            if (!symbol.isBound) {
                visited.add(symbol)
            }
        }
    } while ((unbound - visited).isNotEmpty())
}

fun IrAnnotationContainer.hasAnnotation(fqName: FqName): Boolean =
    annotations.any { (it.symbol.descriptor.constructedClass.fqNameSafe == fqName) }

fun IrClass.findPropertyGetter(
    name: String
): IrFunction {
    return properties
        .singleOrNull { it.name.asString() == name }
        ?.getter ?: functions
        .singleOrNull { function ->
            function.name.asString() == "<get-$name>"
        } ?: error("Couldn't find property '$name' in ${dump()}")
}

fun IrType.substituteByName(substitutionMap: Map<IrTypeParameterSymbol, IrType>): IrType {
    if (this !is IrSimpleType) return this

    (classifier as? IrTypeParameterSymbol)?.let { typeParam ->
        substitutionMap.toList()
            .firstOrNull { it.first.owner.name == typeParam.owner.name }
            ?.let { return it.second }
    }

    substitutionMap[classifier]?.let { return it }

    val newArguments = arguments.map {
        if (it is IrTypeProjection) {
            makeTypeProjection(it.type.substituteByName(substitutionMap), it.variance)
        } else {
            it
        }
    }

    val newAnnotations = annotations.map { it.deepCopyWithSymbols() }
    return IrSimpleTypeImpl(
        classifier,
        hasQuestionMark,
        newArguments,
        newAnnotations
    )
}

fun IrType.toKotlinType(): KotlinType {
    return when (this) {
        is IrSimpleType -> makeKotlinType(
            classifier,
            arguments,
            annotations,
            hasQuestionMark
        )
        else -> TODO(toString())
    }
}

private fun makeKotlinType(
    classifier: IrClassifierSymbol,
    arguments: List<IrTypeArgument>,
    annotations: List<IrConstructorCall>,
    hasQuestionMark: Boolean
): SimpleType {
    val kotlinTypeArguments = arguments.mapIndexed { index, it ->
        when (it) {
            is IrTypeProjection -> TypeProjectionImpl(it.variance, it.type.toKotlinType())
            is IrStarProjection -> StarProjectionImpl((classifier.descriptor as ClassDescriptor).typeConstructor.parameters[index])
            else -> error(it)
        }
    }
    return classifier.descriptor.defaultType.replace(
        newArguments = kotlinTypeArguments,
        newAnnotations = Annotations.create(
            annotations
                .map { it.toAnnotationDescriptor() }
        )
    ).makeNullableAsSpecified(hasQuestionMark)
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
