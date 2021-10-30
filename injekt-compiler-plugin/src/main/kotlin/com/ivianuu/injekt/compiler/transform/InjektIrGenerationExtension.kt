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
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.FakeOverridesStrategy
import org.jetbrains.kotlin.ir.util.KotlinLikeDumpOptions
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File

var dumpAllFiles = false

@OptIn(ObsoleteDescriptorBasedAPI::class)
class InjektIrGenerationExtension(
  private val dumpDir: File,
  @Inject private val injektFqNames: InjektFqNames
) : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, @Provide pluginContext: IrPluginContext) {
    @Provide val context = InjektContext(pluginContext.moduleDescriptor, injektFqNames)
    @Provide val trace = DelegatingBindingTrace(pluginContext.bindingContext, "IR trace")
    @Provide var localClassCollector = LocalClassCollector()
    moduleFragment.transform(localClassCollector, null)

    @Provide val injectNTransformer = InjectNTransformer()
    moduleFragment.transform(injectNTransformer, null)

    @Provide val symbolRemapper = InjectSymbolRemapper()
    moduleFragment.acceptVoid(symbolRemapper)

    val typeRemapper = InjectNTypeRemapper(symbolRemapper)
    // for each declaration, we create a deepCopy transformer It is important here that we
    // use the "preserving metadata" variant since we are using this copy to *replace* the
    // originals, or else the module we would produce wouldn't have any metadata in it.
    val transformer = DeepCopyIrTreeWithSymbolsPreservingMetadata(
      symbolRemapper,
      typeRemapper
    ).also { typeRemapper.deepCopy = it }
    moduleFragment.transformChildren(transformer, null)
    moduleFragment.patchDeclarationParents()

    localClassCollector = LocalClassCollector()
    moduleFragment.transform(localClassCollector, null)

    moduleFragment.transform(InjectCallTransformer(), null)

    moduleFragment.patchDeclarationParents()
    moduleFragment.dumpToFiles(dumpDir, pluginContext)
  }

  @OptIn(ObsoleteDescriptorBasedAPI::class)
  override fun resolveSymbol(symbol: IrSymbol, context: TranslationPluginContext): IrDeclaration? {
    val descriptor = symbol.descriptor
    return if (descriptor is TypeParameterDescriptor)
      IrTypeParameterImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        IrDeclarationOrigin.DEFINED,
        symbol.cast(),
        descriptor.name,
        descriptor.index,
        descriptor.isReified,
        descriptor.variance
      ) else null
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
