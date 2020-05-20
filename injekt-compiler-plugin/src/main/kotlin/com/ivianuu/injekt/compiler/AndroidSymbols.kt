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
import org.jetbrains.kotlin.name.FqName

class AndroidSymbols(val pluginContext: IrPluginContext) {
    val androidEntryPoint = pluginContext.referenceClass(InjektFqNames.AndroidEntryPoint)!!
    val application = pluginContext.referenceClass(FqName("android.app.Application"))!!
    val broadcastReceiver =
        pluginContext.referenceClass(FqName("android.content.BroadcastReceiver"))!!
    val bundle = pluginContext.referenceClass(FqName("android.os.Bundle"))!!
    val componentActivity =
        pluginContext.referenceClass(FqName("androidx.activity.ComponentActivity"))!!
    val fragment =
        pluginContext.referenceClass(FqName("androidx.fragment.app.Fragment"))!!
    val service = pluginContext.referenceClass(FqName("android.app.Service"))!!
}
