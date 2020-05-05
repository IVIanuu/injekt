package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeBuilder
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.IrProvider
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module

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
    return buildSimpleType {
        qualifiers.forEach { qualifier ->
            annotations += DeclarationIrBuilder(symbols.context, classifier!!).irCall(
                symbols.getTopLevelClass(qualifier).constructors.single()
            )
        }
    }
}

val IrType.typeArguments: List<IrType>
    get() = (this as? IrSimpleType)?.arguments?.map { it.type } ?: emptyList()

fun IrType.buildSimpleType(b: IrSimpleTypeBuilder.() -> Unit): IrSimpleType =
    (this as IrSimpleType).buildSimpleType(b)

val IrTypeArgument.type get() = typeOrNull!!

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

fun IrType.getQualifiers(): List<FqName> {
    return annotations
        .filter {
            it.type.classOrFail
                .descriptor
                .annotations
                .hasAnnotation(InjektFqNames.Qualifier)
        }
        .map { it.type.classifierOrFail.descriptor.fqNameSafe }
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
