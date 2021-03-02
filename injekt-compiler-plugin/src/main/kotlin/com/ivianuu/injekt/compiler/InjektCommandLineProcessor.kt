/*
 * Copyright 2020 Manuel Wrage
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

package com.ivianuu.injekt.compiler

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import java.io.File

@AutoService(CommandLineProcessor::class)
class InjektCommandLineProcessor : CommandLineProcessor {
    override val pluginId = "com.ivianuu.injekt"

    override val pluginOptions = listOf(
        SrcDirOption,
        CacheDirOption,
        DumpDirOption
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration,
    ) {
        when (option.optionName) {
            SrcDirOption.optionName -> configuration.put(SrcDirKey, value)
            CacheDirOption.optionName -> configuration.put(CacheDirKey, value)
            DumpDirOption.optionName -> configuration.put(DumpDirKey, value)
        }
    }
}

val SrcDirOption = CliOption(
    optionName = "srcDir",
    valueDescription = "srcDir",
    description = "srcDir"
)
val CacheDirOption = CliOption(
    optionName = "cacheDir",
    valueDescription = "cacheDir",
    description = "cacheDir"
)
val DumpDirOption = CliOption(
    optionName = "dumpDir",
    valueDescription = "dumpDir",
    description = "dumpDir"
)

val SrcDirKey = CompilerConfigurationKey<String>("srcDir")
typealias SrcDir = File

fun srcDir(configuration: CompilerConfiguration): SrcDir =
    File(configuration.getNotNull(SrcDirKey))
        .also { it.mkdirs() }

val CacheDirKey = CompilerConfigurationKey<String>("cacheDir")
typealias CacheDir = File

fun cacheDir(configuration: CompilerConfiguration): CacheDir =
    File(configuration.getNotNull(CacheDirKey))
        .also { it.mkdirs() }

val DumpDirKey = CompilerConfigurationKey<String>("dumpDir")
typealias DumpDir = File

fun dumpDir(configuration: CompilerConfiguration): DumpDir =
    File(configuration.getNotNull(CacheDirKey))
        .also { it.mkdirs() }
