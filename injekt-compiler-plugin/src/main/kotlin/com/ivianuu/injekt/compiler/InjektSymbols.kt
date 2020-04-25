package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.findTypeAliasAcrossModuleDependencies
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class InjektSymbols(
    private val context: IrPluginContext
) {

    val injektPackage = getPackage(InjektFqNames.InjektPackage)

    val key = getTopLevelClass(InjektFqNames.Key)
    val linkable = getTopLevelClass(InjektFqNames.Linkable)
    val linker = getTopLevelClass(InjektFqNames.Linker)
    val parameterizedKey = key.owner.declarations
        .filterIsInstance<IrClass>()
        .single { it.name == InjektFqNames.ParameterizedKey.shortName() }
    val provider = getTopLevelClass(InjektFqNames.Provider)
    val simpleKey = key.owner.declarations
        .filterIsInstance<IrClass>()
        .single { it.name == InjektFqNames.SimpleKey.shortName() }

    fun getTopLevelClass(fqName: FqName): IrClassSymbol =
        context.symbolTable.referenceClass(
            context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(fqName))
                ?: error("No class found for $fqName")
        ).ensureBound(context.irProviders)

    fun getTypeAlias(fqName: FqName): IrTypeAliasSymbol =
        context.symbolTable.referenceTypeAlias(
            context.moduleDescriptor.findTypeAliasAcrossModuleDependencies(ClassId.topLevel(fqName))
                ?: error("No class found for $fqName")
        ).ensureBound(context.irProviders)

    fun getPackage(fqName: FqName): PackageViewDescriptor =
        context.moduleDescriptor.getPackage(fqName)
}
