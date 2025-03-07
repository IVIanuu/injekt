/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.compiler

import com.google.auto.service.*
import injekt.compiler.fir.*
import injekt.compiler.ir.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.com.intellij.mock.*
import org.jetbrains.kotlin.com.intellij.openapi.extensions.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.extensions.*
import java.io.*

@OptIn(ExperimentalCompilerApi::class)
@AutoService(ComponentRegistrar::class)
class InjektComponentRegistrar : ComponentRegistrar {
  override val supportsK2: Boolean
    get() = true

  override fun registerProjectComponents(
    project: MockProject,
    configuration: CompilerConfiguration,
  ) {
    val context = InjektContext()
    FirExtensionRegistrarAdapter.registerExtension(project, InjektFirExtensionRegistrar(context))

    project.extensionArea
      .getExtensionPoint(IrGenerationExtension.extensionPointName)
      .registerExtension(
        InjektIrGenerationExtension(configuration.getNotNull(DumpDirKey), context),
        LoadingOrder.FIRST,
        project
      )
  }
}

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CommandLineProcessor::class)
class InjektCommandLineProcessor : CommandLineProcessor {
  override val pluginId = "io.github.ivianuu.injekt"
  override val pluginOptions = listOf(DumpDirOption)

  override fun processOption(
    option: AbstractCliOption,
    value: String,
    configuration: CompilerConfiguration,
  ) {
    when (option.optionName) {
      DumpDirOption.optionName -> configuration.put(DumpDirKey, File(value))
    }
  }
}

val DumpDirOption = CliOption(
  optionName = "dumpDir",
  valueDescription = "dumpDir",
  description = "dumpDir",
  required = true
)
val DumpDirKey = CompilerConfigurationKey<File>("dumpDir")
