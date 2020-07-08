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
    val componentFactory: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.ComponentFactory)!!
    val componentFactories: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.ComponentFactories)!!
    val contextMarker: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.ContextMarker)!!
    val delegateFactory: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.DelegateFactory)!!
    val distinctType: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.DistinctType)!!
    val doubleCheck: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.DoubleCheck)!!
    val entryPoint: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.EntryPoint)!!
    val injektInfo: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.InjektInfo)!!
    val lateinitFactory: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.LateinitFactory)!!
    val mapEntries: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.MapEntries)!!
    val name: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.Name)!!
    val reader: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.Reader)!!
    val setElements: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.SetElements)!!
    val singleInstanceFactory: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.SingleInstanceFactory)!!
    val unscoped: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.Unscoped)!!
}
