package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class InjektSymbols(
    private val context: IrPluginContext
) {

    val injektPackage = getPackage(InjektFqNames.InjektPackage)
    val injektInternalPackage = getPackage(InjektFqNames.InjektInternalPackage)

    val bindingMetadata = getTopLevelClass(InjektFqNames.BindingMetadata)

    val component = getTopLevelClass(InjektFqNames.Component)
    val componentMetadata = getTopLevelClass(InjektFqNames.ComponentMetadata)
    val componentOwner = getTopLevelClass(InjektFqNames.ComponentOwner)

    val module = getTopLevelClass(InjektFqNames.Module)
    val moduleMetadata = getTopLevelClass(InjektFqNames.ModuleMetadata)

    val provider = getTopLevelClass(InjektFqNames.Provider)

    val providerDsl = getTopLevelClass(InjektFqNames.ProviderDsl)
    val providerFieldMetadata = getTopLevelClass(InjektFqNames.ProviderFieldMetadata)
    val providerMetadata = getTopLevelClass(InjektFqNames.ProviderMetadata)

    val singleProvider = getTopLevelClass(InjektFqNames.SingleProvider)

    fun getTopLevelClass(fqName: FqName): IrClassSymbol =
        context.symbolTable.referenceClass(
            context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(fqName))
                ?: error("No class found for $fqName")
        ).ensureBound(context.irProviders)

    fun getPackage(fqName: FqName): PackageViewDescriptor =
        context.moduleDescriptor.getPackage(fqName)
}