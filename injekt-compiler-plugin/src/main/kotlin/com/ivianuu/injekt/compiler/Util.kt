package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.IrProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
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

fun <T : IrSymbol> T.ensureBound(irProviders: List<IrProvider>): T {
    if (!this.isBound) irProviders.forEach { it.getDeclaration(this) }
    check(this.isBound) { "$this is not bound" }
    return this
}

fun IrType.isFullyResolved(): Boolean =
    this is IrSimpleType && this.classifier is IrClassSymbol && arguments.all {
        it.typeOrNull?.isFullyResolved() == true
    }

fun String.removeIllegalChars(): String {
    return replace("<", "")
        .replace(">", "")
        .replace(" ", "")
        .replace(",", "")
        .replace("*", "")
        .replace(".", "")
        .replace("-", "")

}
