package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.findTypeAliasAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.resolveClassByFqName
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class InjektSymbols(val pluginContext: IrPluginContext) {

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
    val astInline = injektAst.childClass(InjektFqNames.AstInline.shortName())
    val astMap = injektAst.childClass(InjektFqNames.AstMap.shortName())
    val astMapEntry = astMap.childClass(InjektFqNames.AstMapEntry.shortName())
    val astMapClassKey = astMap.childClass(InjektFqNames.AstMapClassKey.shortName())
    val astMapTypeParameterClassKey =
        astMap.childClass(InjektFqNames.AstMapTypeParameterClassKey.shortName())
    val astMapIntKey = astMap.childClass(InjektFqNames.AstMapIntKey.shortName())
    val astMapLongKey = astMap.childClass(InjektFqNames.AstMapLongKey.shortName())
    val astMapStringKey = astMap.childClass(InjektFqNames.AstMapStringKey.shortName())
    val astModule = injektAst.childClass(InjektFqNames.AstModule.shortName())
    val astPath = injektAst.childClass(InjektFqNames.AstPath.shortName())
    val astClassPath = astPath.childClass(InjektFqNames.AstClassPath.shortName())
    val astPropertyPath = astPath.childClass(InjektFqNames.AstPropertyPath.shortName())
    val astTypeParameterPath = astPath.childClass(InjektFqNames.AstTypeParameterPath.shortName())
    val astValueParameterPath = astPath.childClass(InjektFqNames.AstValueParameterPath.shortName())
    val astScope = injektAst.childClass(InjektFqNames.AstScope.shortName())
    val astScoped = injektAst.childClass(InjektFqNames.AstScoped.shortName())
    val astSet = injektAst.childClass(InjektFqNames.AstSet.shortName())
    val astSetElement = astSet.childClass(InjektFqNames.AstSetElement.shortName())
    val astTyped = injektAst.childClass(InjektFqNames.AstTyped.shortName())

    val assisted = getTopLevelClass(InjektFqNames.Assisted)
    val assistedParameters = getTopLevelClass(InjektFqNames.AssistedParameters)

    val childFactory = getTopLevelClass(InjektFqNames.ChildFactory)
    val doubleCheck = getTopLevelClass(InjektFqNames.DoubleCheck)
    val factory = getTopLevelClass(InjektFqNames.Factory)

    val instanceProvider = getTopLevelClass(InjektFqNames.InstanceProvider)

    val lazy = getTopLevelClass(InjektFqNames.Lazy)

    val mapDsl = getTopLevelClass(InjektFqNames.MapDsl)
    val mapProvider = getTopLevelClass(InjektFqNames.MapProvider)

    val module = getTopLevelClass(InjektFqNames.Module)

    val provider = getTopLevelClass(InjektFqNames.Provider)
    val providerDefinition = getTypeAlias(InjektFqNames.ProviderDefinition)
    val providerDsl = getTopLevelClass(InjektFqNames.ProviderDsl)
    val providerOfLazy = getTopLevelClass(InjektFqNames.ProviderOfLazy)

    val setDsl = getTopLevelClass(InjektFqNames.SetDsl)
    val setProvider = getTopLevelClass(InjektFqNames.SetProvider)

    val transient = getTopLevelClass(InjektFqNames.Transient)

    fun getFunction(parameterCount: Int) = pluginContext.builtIns.getFunction(parameterCount)
        .let { pluginContext.symbolTable.referenceClass(it).ensureBound(pluginContext.irProviders) }

    fun getTopLevelClass(fqName: FqName): IrClassSymbol =
        pluginContext.symbolTable.referenceClass(
            pluginContext.moduleDescriptor.resolveClassByFqName(
                fqName,
                NoLookupLocation.FROM_BACKEND
            )
                ?: error("No class found for $fqName")
        ).ensureBound(pluginContext.irProviders)

    fun getTypeAlias(fqName: FqName): IrTypeAliasSymbol =
        pluginContext.symbolTable.referenceTypeAlias(
            pluginContext.moduleDescriptor.findTypeAliasAcrossModuleDependencies(
                ClassId.topLevel(
                    fqName
                )
            )
                ?: error("No class found for $fqName")
        ).ensureBound(pluginContext.irProviders)

    fun getPackage(fqName: FqName): PackageViewDescriptor =
        pluginContext.moduleDescriptor.getPackage(fqName)
}
