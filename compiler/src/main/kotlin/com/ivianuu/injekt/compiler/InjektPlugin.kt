/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler

import com.google.auto.service.*
import com.ivianuu.injekt.compiler.backend.*
import com.ivianuu.injekt.compiler.frontend.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.com.intellij.mock.*
import org.jetbrains.kotlin.com.intellij.openapi.extensions.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.extensions.*
import org.jetbrains.kotlin.resolve.diagnostics.*
import org.jetbrains.kotlin.resolve.extensions.*
import org.jetbrains.kotlin.synthetic.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.io.*
import java.util.*

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class InjektCompilerPluginRegistrar : CompilerPluginRegistrar() {
  override val supportsK2: Boolean
    get() = false

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    StorageComponentContainerContributor
      .registerExtension(InjektStorageComponentContainerContributor())

    if (configuration[CLIConfigurationKeys.METADATA_DESTINATION_DIRECTORY] == null)
      AnalysisHandlerExtension.registerExtension(InjectCallChecker())

    // hack to run ensure we run first
    registeredExtensions.cast<MutableMap<ProjectExtensionDescriptor<*>, MutableList<Any>>>()
      .getOrPut(IrGenerationExtension) { mutableListOf() }.add(
        0,
        InjektIrGenerationExtension(configuration.getNotNull(DumpDirKey))
      )
  }
}

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
