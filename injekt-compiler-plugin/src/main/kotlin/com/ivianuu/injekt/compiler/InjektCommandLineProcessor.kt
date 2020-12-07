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
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Scoped
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
        CliOption(
            optionName = "generateComponents",
            valueDescription = "generateComponents",
            description = "generateComponents"
        ),
        CliOption(
            optionName = "generateMergeComponents",
            valueDescription = "generateMergeComponents",
            description = "generateMergeComponents"
        ),
        CliOption(
            optionName = "srcDir",
            valueDescription = "srcDir",
            description = "srcDir"
        ),
        CliOption(
            optionName = "cacheDir",
            valueDescription = "cacheDir",
            description = "cacheDir"
        )
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) {
        when (option.optionName) {
            "generateComponents" -> configuration.put(GenerateComponentsKey, value.toBoolean())
            "generateMergeComponents" -> configuration.put(GenerateMergeComponentsKey, value.toBoolean())
            "srcDir" -> configuration.put(SrcDirKey, value)
            "cacheDir" -> configuration.put(CacheDirKey, value)
        }
    }
}

val GenerateComponentsKey = CompilerConfigurationKey<Boolean>("generateComponents")
typealias GenerateComponents = Boolean

@Scoped(ApplicationComponent::class)
@Binding fun generateComponents(configuration: CompilerConfiguration): GenerateComponents =
    configuration.getNotNull(GenerateComponentsKey)

val GenerateMergeComponentsKey = CompilerConfigurationKey<Boolean>("generateMergeComponents")
typealias GenerateMergeComponents = Boolean

@Scoped(ApplicationComponent::class)
@Binding fun generateMergeComponents(configuration: CompilerConfiguration): GenerateMergeComponents =
    configuration.getNotNull(GenerateMergeComponentsKey)

val SrcDirKey = CompilerConfigurationKey<String>("srcDir")
typealias SrcDir = File

@Scoped(ApplicationComponent::class)
@Binding fun srcDir(configuration: CompilerConfiguration): SrcDir =
    File(configuration.getNotNull(SrcDirKey))
        .also { it.mkdirs() }

val CacheDirKey = CompilerConfigurationKey<String>("cacheDir")
typealias CacheDir = File

@Scoped(ApplicationComponent::class)
@Binding fun cacheDir(configuration: CompilerConfiguration): CacheDir =
    File(configuration.getNotNull(CacheDirKey))
        .also { it.mkdirs() }
