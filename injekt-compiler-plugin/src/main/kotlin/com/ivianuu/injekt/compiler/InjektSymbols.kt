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
import org.jetbrains.kotlin.name.Name

class InjektSymbols(private val context: IrPluginContext) {

    val injektPackage = getPackage(InjektFqNames.InjektPackage)
    val internalPackage = getPackage(InjektFqNames.InternalPackage)

    val injektAst = getTopLevelClass(InjektFqNames.InjektAst)

    private fun IrClassSymbol.childClass(name: Name) = owner.declarations
        .filterIsInstance<IrClass>()
        .single { it.name == name }
        .symbol

    val astAlias = injektAst.childClass(InjektFqNames.AstAlias.shortName())
    val astAssisted = getTopLevelClass(InjektFqNames.AstAssisted)
    val astBinding = injektAst.childClass(InjektFqNames.AstBinding.shortName())
    val astChildFactory = injektAst.childClass(InjektFqNames.AstChildFactory.shortName())
    val astDependency = injektAst.childClass(InjektFqNames.AstDependency.shortName())
    val astMap = injektAst.childClass(InjektFqNames.AstMap.shortName())
    val astMapEntry = astMap.childClass(InjektFqNames.AstMapEntry.shortName())
    val astMapClassKey = astMap.childClass(InjektFqNames.AstMapClassKey.shortName())
    val astMapIntKey = astMap.childClass(InjektFqNames.AstMapIntKey.shortName())
    val astMapLongKey = astMap.childClass(InjektFqNames.AstMapLongKey.shortName())
    val astMapStringKey = astMap.childClass(InjektFqNames.AstMapStringKey.shortName())
    val astModule = injektAst.childClass(InjektFqNames.AstModule.shortName())
    val astPath = injektAst.childClass(InjektFqNames.AstPath.shortName())
    val astFieldPath = astPath.childClass(InjektFqNames.AstFieldPath.shortName())
    val astClassPath = astPath.childClass(InjektFqNames.AstClassPath.shortName())
    val astScope = injektAst.childClass(InjektFqNames.AstScope.shortName())
    val astScoped = injektAst.childClass(InjektFqNames.AstScoped.shortName())
    val astSet = injektAst.childClass(InjektFqNames.AstSet.shortName())
    val astSetElement = astSet.childClass(InjektFqNames.AstSetElement.shortName())

    val assisted = getTopLevelClass(InjektFqNames.Assisted)

    val childFactory = getTopLevelClass(InjektFqNames.ChildFactory)
    val factory = getTopLevelClass(InjektFqNames.Factory)

    val doubleCheck = getTopLevelClass(InjektFqNames.DoubleCheck)

    val instanceProvider = getTopLevelClass(InjektFqNames.InstanceProvider)

    val lazy = getTopLevelClass(InjektFqNames.Lazy)

    val mapDsl = getTopLevelClass(InjektFqNames.MapDsl)

    val module = getTopLevelClass(InjektFqNames.Module)

    val provider = getTopLevelClass(InjektFqNames.Provider)
    val providerDefinition = getTypeAlias(InjektFqNames.ProviderDefinition)
    val providerDsl = getTopLevelClass(InjektFqNames.ProviderDsl)

    val setDsl = getTopLevelClass(InjektFqNames.SetDsl)

    val transient = getTopLevelClass(InjektFqNames.Transient)

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
