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

class CompositionSymbols(val pluginContext: IrPluginContext) {
    val bindingAdapter = pluginContext.referenceClass(InjektFqNames.BindingAdapter)!!
    val compositionComposition = pluginContext.referenceClass(InjektFqNames.CompositionComponent)!!
    val compositionFactory = pluginContext.referenceClass(InjektFqNames.CompositionFactory)!!
    val compositionFactories = pluginContext.referenceClass(InjektFqNames.CompositionFactories)!!
    val readable = pluginContext.referenceClass(InjektFqNames.Readable)!!
}
