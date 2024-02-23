/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.backend

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.resolve.*
import java.io.*

var dumpAllFiles = false

class InjektIrGenerationExtension(private val dumpDir: File) : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    val ctx = Context(
      pluginContext.moduleDescriptor,
      DelegatingBindingTrace(pluginContext.bindingContext, "IR trace")
    )

    val compilationDeclarations = CompilationDeclarations()
    moduleFragment.transform(compilationDeclarations, null)
    moduleFragment.transform(InjectCallTransformer(compilationDeclarations, pluginContext, ctx), null)
    moduleFragment.patchDeclarationParents()
    moduleFragment.dumpToFiles(dumpDir, ctx)
  }
}

private fun IrModuleFragment.dumpToFiles(dumpDir: File, ctx: Context) {
  files
    .filter {
      dumpAllFiles ||
          ctx.cachedOrNull<_, Unit>(INJECTIONS_OCCURRED_IN_FILE_KEY, it.fileEntry.name) != null
    }
    .forEach { irFile ->
      val file = File(irFile.fileEntry.name)
      val content = try {
        buildString {
          appendLine(
            irFile.dumpKotlinLike(
              KotlinLikeDumpOptions(
                useNamedArguments = true,
                printFakeOverridesStrategy = FakeOverridesStrategy.NONE
              )
            )
          )
        }
      } catch (e: Throwable) {
        e.stackTraceToString()
      }
      val newFile = dumpDir
        .resolve(irFile.packageFqName.asString().replace(".", "/"))
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
