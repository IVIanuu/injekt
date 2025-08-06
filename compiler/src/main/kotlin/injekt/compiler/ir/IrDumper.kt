/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.compiler.ir

import injekt.compiler.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import java.io.*

var dumpAllFiles = false

context(_: InjektContext)
fun IrModuleFragment.dumpToFiles(dumpDir: File) {
  files
    .filter {
      dumpAllFiles ||
          cachedOrNull<_, Unit>(INJECTIONS_OCCURRED_IN_FILE_KEY, it.fileEntry.name) != null
    }
    .forEach { irFile ->
      val sourceFile = File(irFile.fileEntry.name)
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
      val dumpFile = dumpDir
        .resolve(irFile.packageFqName.asString().replace(".", "/"))
        .also { it.mkdirs() }
        .resolve(sourceFile.name.removeSuffix(".kt") + ".dump")
      try {
        dumpFile.createNewFile()
        dumpFile.writeText(content)
        println("Generated $dumpFile:\n$content")
      } catch (e: Throwable) {
        throw RuntimeException("Failed to create file ${dumpFile.absolutePath}\n$content", e)
      }
    }
}
