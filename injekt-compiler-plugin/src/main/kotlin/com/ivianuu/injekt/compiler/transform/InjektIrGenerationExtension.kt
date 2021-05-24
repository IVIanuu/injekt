/*
 * Copyright 2021 Manuel Wrage
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

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.resolve.*

class InjektIrGenerationExtension : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    val context = pluginContext.moduleDescriptor.injektContext
    val trace = DelegatingBindingTrace(
      pluginContext.bindingContext, "IR trace"
    )
    moduleFragment.transform(InjectCallTransformer(context, pluginContext), null)
    moduleFragment.transform(SingletonTransformer(context, trace, pluginContext), null)
    moduleFragment.transform(WithProvidersTransformer(), null)
    moduleFragment.transform(IncrementalFixTransformer(context, trace, pluginContext), null)
    moduleFragment.patchDeclarationParents()
  }
}
