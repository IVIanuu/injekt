package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.module

object InjektClassNames {
    val InjektPackage = FqName("com.ivianuu.injekt")
    val InjektInternalPackage = FqName("com.ivianuu.injekt.internal")

    val ApplicationScope = FqName("com.ivianuu.injekt.ApplicationScope")
    val Behavior = FqName("com.ivianuu.injekt.Behavior")
    val Component = FqName("com.ivianuu.injekt.Component")
    val Injekt = FqName("com.ivianuu.injekt.Injekt")
    val Key = FqName("com.ivianuu.injekt.Key")
    val Module = FqName("com.ivianuu.injekt.Module")
    val ModuleImpl = FqName("com.ivianuu.injekt.ModuleImpl")
    val Param = FqName("com.ivianuu.injekt.Param")
    val Parameters = FqName("com.ivianuu.injekt.Parameters")
    val Qualifier = FqName("com.ivianuu.injekt.Qualifier")
    val Scope = FqName("com.ivianuu.injekt.Scope")
}

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
