package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.util.toAnnotationDescriptor
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.IrProvider
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.Variance

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

fun IrType.withQualifiers(symbols: InjektSymbols, qualifiers: List<FqName>): IrType {
    this as IrSimpleType
    return replace(
        newArguments = arguments.map {
            if (it is IrStarProjection) it
            else makeTypeProjection(
                it.type.ensureQualifiers(symbols),
                (it as? IrTypeProjection)?.variance ?: Variance.INVARIANT
            )
        },
        newAnnotations = listOf(
            DeclarationIrBuilder(symbols.context, classOrFail)
                .irCall(symbols.astQualifiers.constructors.single()).apply {
                    putValueArgument(
                        0,
                        IrVarargImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            symbols.context.irBuiltIns.arrayClass
                                .typeWith(
                                    symbols.context.irBuiltIns.kClassClass
                                        .starProjectedType
                                ),
                            symbols.context.irBuiltIns.kClassClass
                                .starProjectedType,
                            qualifiers
                                .map { symbols.getTopLevelClass(it) }
                                .map {
                                    IrClassReferenceImpl(
                                        UNDEFINED_OFFSET,
                                        UNDEFINED_OFFSET,
                                        symbols.context.irBuiltIns.kClassClass.typeWith(it.defaultType),
                                        it,
                                        it.defaultType
                                    )
                                }
                        )
                    )
                }
        )
    )
}

fun IrType.ensureQualifiers(symbols: InjektSymbols): IrType {
    return if (hasAnnotation(InjektFqNames.AstQualifiers)) this
    else withQualifiers(symbols, getUserQualifiers())
}

fun IrType.getQualifiers(): List<FqName> {
    return annotations
        .singleOrNull { it.type.classOrFail.descriptor.fqNameSafe == InjektFqNames.AstQualifiers }
        ?.getValueArgument(0)
        ?.let { it as IrVarargImpl }
        ?.elements
        ?.filterIsInstance<IrClassReference>()
        ?.map { it.classType.classOrFail.descriptor.fqNameSafe }
        ?: emptyList()
}

fun IrType.getUserQualifiers(): List<FqName> {
    return annotations
        .filter {
            it.type.classOrFail
                .descriptor
                .annotations
                .hasAnnotation(InjektFqNames.Qualifier)
        }
        .map { it.type.classifierOrFail.descriptor.fqNameSafe }
}

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

val IrType.classOrFail get() = classOrNull!!

fun IrType.typeWith(vararg arguments: IrType): IrType = classifierOrFail.typeWith(*arguments)

fun IrType.hashCodeWithQualifiers(): Int {
    var result = hashCode()
    result = 31 * result + getQualifiers().hashCode()
    return result
}

fun IrType?.equalsWithQualifiers(other: Any?): Boolean {
    if (this === other) return true
    if (this?.javaClass != other?.javaClass) return false
    other as IrType

    if (this != other) return false

    if (getQualifiers() != other.getQualifiers()) return false

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

fun IrAnnotationContainer.getQualifiers(): List<FqName> {
    return getAnnotation(InjektFqNames.AstQualifiers)!!.getValueArgument(0)
        .let { it as IrVarargImpl }
        .elements
        .filterIsInstance<IrClassReference>()
        .map { it.classType.classOrFail.descriptor.fqNameSafe }
}

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
