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

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt_shaded.Inject
import com.ivianuu.injekt_shaded.Provide
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.FakeOverridesStrategy
import org.jetbrains.kotlin.ir.util.KotlinLikeDumpOptions
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import java.io.File

var dumpAllFiles = true

@OptIn(ObsoleteDescriptorBasedAPI::class)
class InjektIrGenerationExtension(
  private val dumpDir: File,
  @Inject private val injektFqNames: InjektFqNames
) : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, @Provide pluginContext: IrPluginContext) {
    @Provide val context = InjektContext(
      pluginContext.moduleDescriptor,
      injektFqNames,
      DelegatingBindingTrace(pluginContext.bindingContext, "IR trace")
    )
    @Provide val entryPointTransformer = EntryPointTransformer()
    moduleFragment.transform(entryPointTransformer, null)
    moduleFragment.transform(InjectCallTransformer(), null)
    moduleFragment.patchDeclarationParents()
    moduleFragment.dumpToFiles(dumpDir, pluginContext)
  }
}

@OptIn(ObsoleteDescriptorBasedAPI::class) fun IrModuleFragment.dumpToFiles(
  dumpDir: File,
  pluginContext: IrPluginContext
) {
  files
    .filter {
      dumpAllFiles || pluginContext.bindingContext[InjektWritableSlices.INJECTIONS_OCCURRED_IN_FILE,
          it.fileEntry.name] != null
    }
    .forEach { irFile ->
      val file = File(irFile.fileEntry.name)
      val content = try {
        irFile.dumpKotlinLike(
          KotlinLikeDumpOptions(
            useNamedArguments = true,
            printFakeOverridesStrategy = FakeOverridesStrategy.NONE
          )
        )
      } catch (e: Throwable) {
        e.stackTraceToString()
      }
      val newFile = dumpDir
        .resolve(irFile.fqName.asString().replace(".", "/"))
        .also { it.mkdirs() }
        .resolve(file.name.removeSuffix(".kt"))
      try {
        newFile.createNewFile()
        newFile.writeText(content)
        println("Generated $newFile:\n$content")
      } catch (e: Throwable) {
        throw RuntimeException("Failed to create file ${newFile.absolutePath}\n$content")
      }
    }
}
