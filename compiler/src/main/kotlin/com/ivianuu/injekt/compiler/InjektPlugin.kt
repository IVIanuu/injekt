/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import com.google.auto.service.AutoService
import com.ivianuu.injekt.compiler.backend.InjektIrGenerationExtension
import com.ivianuu.injekt.compiler.frontend.InjektFirCheckersExtensions
import com.ivianuu.injekt.compiler.frontend.InjektFirDeclarationGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import java.io.File
import java.util.*

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CommandLineProcessor::class)
class InjektCommandLineProcessor : CommandLineProcessor {
  override val pluginId = "com.ivianuu.injekt"
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

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class InjektCompilerPluginRegistrar : CompilerPluginRegistrar() {
  override val supportsK2: Boolean
    get() = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    IrGenerationExtension.registerExtension(
      InjektIrGenerationExtension(configuration.getNotNull(DumpDirKey))
    )

    FirExtensionRegistrarAdapter.registerExtension(InjektFirExtensionRegistrar())
  }
}

class InjektFirExtensionRegistrar : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {
    +::InjektFirCheckersExtensions
    +::InjektFirDeclarationGenerationExtension
  }
}
