/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.*
import com.ivianuu.shaded_injekt.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import java.io.*

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrModuleFragment.dumpToFiles(dumpDir: File, @Inject irCtx: IrPluginContext) {
  files
    .filter {
      dumpAllFiles || irCtx.bindingContext[InjektWritableSlices.INJECTIONS_OCCURRED_IN_FILE,
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
