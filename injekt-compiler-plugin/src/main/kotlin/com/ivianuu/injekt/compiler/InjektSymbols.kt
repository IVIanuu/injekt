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
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol

class InjektSymbols(val pluginContext: IrPluginContext) {
    val component: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.Component)!!
    val distinct: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.Distinct)!!
    val effect: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.Effect)!!
    val given: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.Given)!!
    val mapEntries: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.MapEntries)!!
    val reader: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.Reader)!!
    val setElements: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.SetElements)!!

    val childComponentFactory: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.ChildComponentFactory)!!
    val entryPoint: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.EntryPoint)!!
    val implicit: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.Implicit)!!
    val implicits: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.Implicits)!!
    val index: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.Index)!!
    val name: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.Name)!!
    val qualifier: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.Qualifier)!!
    val rootComponentFactory: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.RootComponentFactory)!!
    val rootComponentFactories: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.RootComponentFactories)!!

}
