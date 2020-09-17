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
import com.ivianuu.injekt.compiler.analysis.InjektStorageContainerContributor
import com.ivianuu.injekt.compiler.transform.InjektIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File

@AutoService(ComponentRegistrar::class)
class InjektComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        val srcDir = File(configuration.getNotNull(SrcDirKey))
        val resourcesDir = File(configuration.getNotNull(ResourcesDirKey))
        val cacheDir = File(configuration.getNotNull(CacheDirKey))

        StorageComponentContainerContributor.registerExtension(
            project,
            InjektStorageContainerContributor()
        )

        val irFileStore = IrFileStore()
        val lookupManager = LookupManager()

        AnalysisHandlerExtension.registerExtension(
            project,
            LookupTrackerInitializer(lookupManager)
        )

        AnalysisHandlerExtension.registerExtension(
            project,
            IrFileGenerator(srcDir, cacheDir, irFileStore, lookupManager)
        )

        // make sure that our plugin always runs before the Compose plugin
        // otherwise it will break @Reader @Composable functions
        val irExtensionPoint = Extensions.getArea(project)
            .getExtensionPoint(IrGenerationExtension.extensionPointName)

        val composeIrExtensionClass = try {
            Class.forName("androidx.compose.compiler.plugins.kotlin.ComposeIrGenerationExtension")
        } catch (t: Throwable) {
            null
        }
        val composeExtension = if (composeIrExtensionClass != null) {
            irExtensionPoint.extensionList.singleOrNull {
                it.javaClass == composeIrExtensionClass
            }
        } else null
        if (composeExtension != null) irExtensionPoint
            .unregisterExtension(composeIrExtensionClass as Class<out IrGenerationExtension>)
        irExtensionPoint.registerExtension(
            InjektIrGenerationExtension(
                irFileStore,
                lookupManager
            )
        ) {}
        if (composeExtension != null) irExtensionPoint.registerExtension(composeExtension) {}
    }
}

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
