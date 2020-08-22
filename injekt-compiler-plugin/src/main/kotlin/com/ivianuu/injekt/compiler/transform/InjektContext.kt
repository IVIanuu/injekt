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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.ClassUniqueNameProvider
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.WeakBindingTrace
import com.ivianuu.injekt.compiler.analysis.ReaderChecker
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace

class InjektContext(
    private val delegate: IrPluginContext,
    val module: IrModuleFragment
) : IrPluginContext by delegate {
    val injektSymbols = InjektSymbols(this)
    val uniqueClassNameProvider = ClassUniqueNameProvider(this)
    val irTrace = WeakBindingTrace()
    val readerChecker = ReaderChecker()
    val bindingTrace = DelegatingBindingTrace(bindingContext, "Injekt IR")
}
