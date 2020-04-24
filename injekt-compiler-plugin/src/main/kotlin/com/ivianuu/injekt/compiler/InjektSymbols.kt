package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.cast

class InjektSymbols(
    private val context: IrPluginContext
) {

    val injektPackage = getPackage(InjektFqNames.InjektPackage)
    val injektInternalPackage = getPackage(InjektFqNames.InjektInternalPackage)

    val component = getTopLevelClass(InjektFqNames.Component)
    val componentFactory = component.ensureBound(context.irProviders)
        .owner
        .declarations
        .filterIsInstance<IrClass>()
        .single()
        .symbol

    val lazy = getTopLevelClass(InjektFqNames.Lazy)

    val module = getTopLevelClass(InjektFqNames.Module)

    val qualifier = getTopLevelClass(InjektFqNames.Qualifier)

    val provide = getTopLevelClass(InjektFqNames.Provide)
    val provider = getTopLevelClass(InjektFqNames.Provider)

    val singleProvider = getTopLevelClass(InjektFqNames.SingleProvider)

    val scope = getTopLevelClass(InjektFqNames.Scope)

    fun getTopLevelClass(fqName: FqName): IrClassSymbol =
        context.symbolTable.referenceClass(
            context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(fqName))
                ?: error("No class found for $fqName")
        ).ensureBound(context.irProviders)

    fun getPackage(fqName: FqName): PackageViewDescriptor =
        context.moduleDescriptor.getPackage(fqName)
}
