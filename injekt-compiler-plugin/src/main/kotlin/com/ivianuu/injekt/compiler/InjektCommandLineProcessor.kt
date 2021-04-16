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

import com.google.auto.service.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.*
import java.io.*

@AutoService(CommandLineProcessor::class)
class InjektCommandLineProcessor : CommandLineProcessor {
    override val pluginId = "com.ivianuu.injekt"

    override val pluginOptions = listOf(
        AllowGivenCallsOption,
        DumpDirOption
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration,
    ) {
        when (option.optionName) {
            AllowGivenCallsOption.optionName -> configuration.put(AllowGivenCallsKey, value.toBoolean())
            DumpDirOption.optionName -> configuration.put(DumpDirKey, value)
        }
    }
}

val AllowGivenCallsOption = CliOption(
    optionName = "allowGivenCalls",
    valueDescription = "allowGivenCalls",
    description = "allowGivenCalls",
    required = false
)
val DumpDirOption = CliOption(
    optionName = "dumpDir",
    valueDescription = "dumpDir",
    description = "dumpDir"
)

val AllowGivenCallsKey = CompilerConfigurationKey<Boolean>("allowGivenCalls")
fun allowGivenCalls(configuration: CompilerConfiguration): Boolean =
    configuration.get(AllowGivenCallsKey) ?: false

val DumpDirKey = CompilerConfigurationKey<String>("dumpDir")
fun dumpDir(configuration: CompilerConfiguration): File =
    File(configuration.getNotNull(DumpDirKey))
        .also { it.mkdirs() }
