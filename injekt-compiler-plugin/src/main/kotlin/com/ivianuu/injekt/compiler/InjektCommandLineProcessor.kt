/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import java.io.File

class InjektCommandLineProcessor : CommandLineProcessor {
  override val pluginId = "com.ivianuu.injekt"

  override val pluginOptions = listOf(
    DumpDirOption,
    SrcDirOption,
    CacheDirOption,
    ModifiedFilesOption,
    RemovedFilesOption,
    WithCompilationOption
  )

  override fun processOption(
    option: AbstractCliOption,
    value: String,
    configuration: CompilerConfiguration,
  ) {
    when (option.optionName) {
      DumpDirOption.optionName -> configuration.put(DumpDirKey, File(value))
      CacheDirOption.optionName -> configuration.put(CacheDirKey, File(value))
      SrcDirOption.optionName -> configuration.put(SrcDirKey, File(value))
      ModifiedFilesOption.optionName -> configuration.put(
        ModifiedFilesKey,
        value.split(File.pathSeparator).map { File(it) }
      )
      RemovedFilesOption.optionName -> configuration.put(
        RemovedFilesKey,
        value.split(File.pathSeparator).map { File(it) }
      )
      WithCompilationOption.optionName -> configuration.put(
        WithCompilationKey, value.toBoolean()
      )
    }
  }
}

val DumpDirOption = CliOption(
  optionName = "dumpDir",
  valueDescription = "dumpDir",
  description = "dumpDir",
  required = false
)
val DumpDirKey = CompilerConfigurationKey<File>("dumpDir")

val SrcDirOption = CliOption(
  optionName = "srcDir",
  valueDescription = "srcDir",
  description = "srcDir",
  required = false
)
val SrcDirKey = CompilerConfigurationKey<File>("srcDir")

val CacheDirOption = CliOption(
  optionName = "cacheDir",
  valueDescription = "cacheDir",
  description = "cacheDir",
  required = false
)
val CacheDirKey = CompilerConfigurationKey<File>("cacheDir")

val ModifiedFilesOption = CliOption(
  optionName = "modifiedFiles",
  valueDescription = "modifiedFiles",
  description = "modifiedFiles",
  required = false
)
val ModifiedFilesKey = CompilerConfigurationKey<List<File>>("modifiedFiles")

val RemovedFilesOption = CliOption(
  optionName = "removedFiles",
  valueDescription = "removedFiles",
  description = "removedFiles",
  required = false
)
val RemovedFilesKey = CompilerConfigurationKey<List<File>>("removedFiles")

val WithCompilationOption = CliOption(
  optionName = "withCompilation",
  valueDescription = "withCompilation",
  description = "withCompilation",
  required = false
)
val WithCompilationKey = CompilerConfigurationKey<Boolean>("withCompilation")
