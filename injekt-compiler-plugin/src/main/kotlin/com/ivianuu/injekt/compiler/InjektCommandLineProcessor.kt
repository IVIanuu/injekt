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
        ),
        CliOption(
            optionName = "resourcesDir",
            valueDescription = "resourcesDir",
            description = "resourcesDir"
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
            "srcDir" -> configuration.put(SrcDirKey, value)
            "resourcesDir" -> configuration.put(ResourcesDirKey, value)
            "cacheDir" -> configuration.put(CacheDirKey, value)
        }
    }
}

val SrcDirKey = CompilerConfigurationKey<String>("srcDir")
val ResourcesDirKey = CompilerConfigurationKey<String>("resourcesDir")
val CacheDirKey = CompilerConfigurationKey<String>("cacheDir")

typealias SrcDir = File
typealias ResourcesDir = File
typealias CacheDir = File

object ConfigurationGivens {
    @Given(ApplicationContext::class)
    fun srcDir(): SrcDir = File(given<CompilerConfiguration>().getNotNull(SrcDirKey))
        .also { it.mkdirs() }

    @Given(ApplicationContext::class)
    fun resourcesDir(): ResourcesDir =
        File(given<CompilerConfiguration>().getNotNull(ResourcesDirKey))
            .also { it.mkdirs() }

    @Given(ApplicationContext::class)
    fun cacheDir(): CacheDir = File(given<CompilerConfiguration>().getNotNull(CacheDirKey))
        .also { it.mkdirs() }
}
