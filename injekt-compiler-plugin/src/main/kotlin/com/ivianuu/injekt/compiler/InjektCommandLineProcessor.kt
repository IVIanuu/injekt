package com.ivianuu.injekt.compiler

import com.google.auto.service.AutoService
import com.ivianuu.injekt.ApplicationContext
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.given
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
            optionName = "srcDir",
            valueDescription = "srcDir",
            description = "srcDir"
        )
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) {
        when (option.optionName) {
            "srcDir" -> configuration.put(SrcDirKey, value)
        }
    }
}

val SrcDirKey = CompilerConfigurationKey<String>("srcDir")

typealias SrcDir = File

object ConfigurationGivens {
    @Given(ApplicationContext::class)
    fun srcDir(): SrcDir = File(given<CompilerConfiguration>().getNotNull(SrcDirKey))
        .also { it.mkdirs() }
}
