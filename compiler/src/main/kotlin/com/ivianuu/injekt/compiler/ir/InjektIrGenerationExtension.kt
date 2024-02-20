/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.ir

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.fir.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.io.*

var dumpAllFiles = false

class InjektIrGenerationExtension(
  private val cache: InjektCache,
  private val dumpDir: File
) : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    pluginContext as Fir2IrPluginContext
    pluginContext.symbolTable
    moduleFragment.transform(InjectCallTransformer(pluginContext, cache), null)
    moduleFragment.fixLookupDeclarations(pluginContext)
    moduleFragment.dumpToFiles(dumpDir, cache)
  }
}

private fun IrModuleFragment.fixLookupDeclarations(pluginContext: IrPluginContext) {
  transform(
    object : IrElementTransformerVoid() {
      override fun visitFunction(declaration: IrFunction): IrStatement {
        if (declaration.origin.safeAs<IrDeclarationOrigin.GeneratedByPlugin>()
            ?.pluginKey == InjektLookupDeclarationGenerationExtension.Key)
          declaration.body = DeclarationIrBuilder(pluginContext, declaration.symbol)
            .irBlockBody {  }
        return super.visitFunction(declaration)
      }
    },
    null
  )
}

private fun IrModuleFragment.dumpToFiles(dumpDir: File, cache: InjektCache) {
  files
    .filter {
      dumpAllFiles ||
          cache.cachedOrNull<_, Unit>(INJECTIONS_OCCURRED_IN_FILE_KEY, it.fileEntry.name) != null
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
