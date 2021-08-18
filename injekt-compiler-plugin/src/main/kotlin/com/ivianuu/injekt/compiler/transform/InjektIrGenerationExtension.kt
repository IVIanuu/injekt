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

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.compiler.analysis.AnalysisContext
import com.ivianuu.injekt.compiler.injektContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace

@OptIn(ObsoleteDescriptorBasedAPI::class)
class InjektIrGenerationExtension : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, @Provide pluginContext: IrPluginContext) {
    @Provide val context = AnalysisContext(
      pluginContext.moduleDescriptor.injektContext,
      DelegatingBindingTrace(pluginContext.bindingContext, "IR trace")
    )
    moduleFragment.transform(InjectCallTransformer(), null)
    moduleFragment.transform(IncrementalFixTransformer(), null)
    moduleFragment.patchDeclarationParents()
  }
}
