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

class CompositionSymbols(val pluginContext: IrPluginContext) {
    val bindingAdapter: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.BindingAdapter)!!
    val compositionComposition: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.CompositionComponent)!!
    val compositionFactory: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.CompositionFactory)!!
    val compositionFactories: IrClassSymbol
        get() = pluginContext.referenceClass(InjektFqNames.CompositionFactories)!!
}
