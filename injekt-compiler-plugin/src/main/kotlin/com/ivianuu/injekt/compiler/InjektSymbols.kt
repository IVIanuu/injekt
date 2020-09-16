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

class InjektSymbols(private val pluginContext: IrPluginContext) {
    val context = pluginContext.referenceClass(InjektFqNames.Context)!!
    val effect = pluginContext.referenceClass(InjektFqNames.Effect)!!
    val given = pluginContext.referenceClass(InjektFqNames.Given)!!
    val givenMapEntries = pluginContext.referenceClass(InjektFqNames.GivenMapEntries)!!
    val givenSet = pluginContext.referenceClass(InjektFqNames.GivenSet)!!
    val givenSetElements = pluginContext.referenceClass(InjektFqNames.GivenSetElements)!!
    val reader = pluginContext.referenceClass(InjektFqNames.Reader)!!

    val childContextFactory = pluginContext.referenceClass(InjektFqNames.ChildContextFactory)!!
    val contextMarker = pluginContext.referenceClass(InjektFqNames.ContextMarker)!!
    val index = pluginContext.referenceClass(InjektFqNames.Index)!!
    val origin = pluginContext.referenceClass(InjektFqNames.Origin)!!
    val qualifier = pluginContext.referenceClass(InjektFqNames.Qualifier)!!
    val rootContextFactory = pluginContext.referenceClass(InjektFqNames.RootContextFactory)!!
    val runReaderCall = pluginContext.referenceClass(InjektFqNames.RunReaderCall)!!
    val signature = pluginContext.referenceClass(InjektFqNames.Signature)!!
}
