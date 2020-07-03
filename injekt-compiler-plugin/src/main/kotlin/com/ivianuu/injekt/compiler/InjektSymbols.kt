/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class InjektSymbols(val pluginContext: IrPluginContext) {

    val injektAst: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.InjektAst)!!

    private fun IrClassSymbol.childClass(name: Name) = owner.declarations
        .filterIsInstance<IrClass>()
        .singleOrNull { it.name == name }
        ?.symbol ?: error("Couldn't find $name in ${owner.dump()}")

    val astAlias: IrClassSymbol
        get() = injektAst.childClass(InjektFqNames.AstAlias.shortName())
    val astBinding: IrClassSymbol
        get() = injektAst.childClass(InjektFqNames.AstBinding.shortName())
    val astChildFactory: IrClassSymbol
        get() = injektAst.childClass(InjektFqNames.AstChildFactory.shortName())
    val astCompositionTypes: IrClassSymbol
        get() = injektAst.childClass(InjektFqNames.AstCompositionTypes.shortName())
    val astDependency: IrClassSymbol
        get() = injektAst.childClass(InjektFqNames.AstDependency.shortName())
    val astEntryPoints: IrClassSymbol
        get() = injektAst.childClass(InjektFqNames.AstEntryPoints.shortName())
    val astMap: IrClassSymbol
        get() = injektAst.childClass(InjektFqNames.AstMap.shortName())
    val astMapEntry: IrClassSymbol
        get() = astMap.childClass(InjektFqNames.AstMapEntry.shortName())
    val astMapClassKey: IrClassSymbol
        get() = astMap.childClass(InjektFqNames.AstMapClassKey.shortName())
    val astMapTypeParameterClassKey: IrClassSymbol
        get() = astMap.childClass(InjektFqNames.AstMapTypeParameterClassKey.shortName())
    val astMapIntKey: IrClassSymbol
        get() = astMap.childClass(InjektFqNames.AstMapIntKey.shortName())
    val astMapLongKey: IrClassSymbol
        get() = astMap.childClass(InjektFqNames.AstMapLongKey.shortName())
    val astMapStringKey: IrClassSymbol
        get() = astMap.childClass(InjektFqNames.AstMapStringKey.shortName())
    val astModule: IrClassSymbol
        get() = injektAst.childClass(InjektFqNames.AstModule.shortName())
    val astName: IrClassSymbol
        get() = injektAst.childClass(InjektFqNames.AstName.shortName())
    val astPath: IrClassSymbol
        get() = injektAst.childClass(InjektFqNames.AstPath.shortName())
    val astClassPath: IrClassSymbol
        get() = astPath.childClass(InjektFqNames.AstClassPath.shortName())
    val astPropertyPath: IrClassSymbol
        get() = astPath.childClass(InjektFqNames.AstPropertyPath.shortName())
    val astTypeParameterPath: IrClassSymbol
        get() = astPath.childClass(InjektFqNames.AstTypeParameterPath.shortName())
    val astParents: IrClassSymbol
        get() = injektAst.childClass(InjektFqNames.AstParents.shortName())
    val astScope: IrClassSymbol
        get() = injektAst.childClass(InjektFqNames.AstScope.shortName())
    val astScoped: IrClassSymbol
        get() = injektAst.childClass(InjektFqNames.AstScoped.shortName())
    val astSet: IrClassSymbol
        get() = injektAst.childClass(InjektFqNames.AstSet.shortName())
    val astSetElement: IrClassSymbol
        get() = astSet.childClass(InjektFqNames.AstSetElement.shortName())

    val assisted: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.Assisted)!!

    val childFactory: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.ChildFactory)!!

    val delegateFactory: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.DelegateFactory)!!

    val doubleCheck: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.DoubleCheck)!!

    val factory: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.Factory)!!

    val lateinitFactory: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.LateinitFactory)!!

    val mapDsl: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.MapDsl)!!
    val mapOfValueFactory: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.MapOfValueFactory)!!
    val mapOfProviderFactory: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.MapOfProviderFactory)!!

    val module: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.Module)!!

    val provider: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.Provider)!!

    val reader: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.Reader)!!

    val setDsl: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.SetDsl)!!
    val setOfValueFactory: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.SetOfValueFactory)!!
    val setOfProviderFactory: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.SetOfProviderFactory)!!

    val singleInstanceFactory: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.SingleInstanceFactory)!!

    val transient: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.Transient)!!
}
