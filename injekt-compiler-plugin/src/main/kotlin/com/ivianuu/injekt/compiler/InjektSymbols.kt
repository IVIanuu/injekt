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

class InjektSymbols(val pluginContext: IrPluginContext) {
    val effect = pluginContext.referenceClass(InjektFqNames.Effect)!!
    val given = pluginContext.referenceClass(InjektFqNames.Given)!!
    val mapEntries = pluginContext.referenceClass(InjektFqNames.MapEntries)!!
    val reader = pluginContext.referenceClass(InjektFqNames.Reader)!!
    val scoping = pluginContext.referenceClass(InjektFqNames.Scoping)!!
    val setElements = pluginContext.referenceClass(InjektFqNames.SetElements)!!

    val context = pluginContext.referenceClass(InjektFqNames.Context)!!
    val genericContext = pluginContext.referenceClass(InjektFqNames.GenericContext)!!
    val index = pluginContext.referenceClass(InjektFqNames.Index)!!
    val name = pluginContext.referenceClass(InjektFqNames.Name)!!
    val qualifier = pluginContext.referenceClass(InjektFqNames.Qualifier)!!
    val readerImpl = pluginContext.referenceClass(InjektFqNames.ReaderImpl)!!
    val readerInvocation = pluginContext.referenceClass(InjektFqNames.ReaderInvocation)!!
    val runReaderContext = pluginContext.referenceClass(InjektFqNames.RunReaderContext)!!
    val signature = pluginContext.referenceClass(InjektFqNames.Signature)!!
}
