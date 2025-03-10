/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, ExperimentalCompilerApi::class)

package injekt.compiler

import com.google.auto.service.*
import injekt.compiler.fir.*
import injekt.compiler.ir.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.extensions.*
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.io.*

@AutoService(CompilerPluginRegistrar::class)
class InjektPluginRegistrar : CompilerPluginRegistrar() {
  override val supportsK2: Boolean
    get() = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    val context = InjektContext()
    FirExtensionRegistrarAdapter.registerExtension(InjektFirExtensionRegistrar(context))

    // todo we want to run first (before compose) so we have to use this dirty workaround
    //  to register our extension as first
    registeredExtensions.cast<MutableMap<ProjectExtensionDescriptor<*>, MutableList<Any>>>()
      .getOrPut(IrGenerationExtension) { mutableListOf() }
        .add(0, InjektIrGenerationExtension(configuration.getNotNull(DumpDirKey), context))
  }
}

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
