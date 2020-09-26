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
import com.ivianuu.injekt.ApplicationContext
import com.ivianuu.injekt.InitializeInjekt
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.compiler.checkers.InjektStorageContainerContributor
import com.ivianuu.injekt.compiler.generator.InjektKtGenerationExtension
import com.ivianuu.injekt.compiler.generator.KtFileManager
import com.ivianuu.injekt.compiler.irtransform.InjektIrGenerationExtension
import com.ivianuu.injekt.given
import com.ivianuu.injekt.rootContext
import com.ivianuu.injekt.runReader
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.ContentRoot
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.jvm.compiler.createSourceFilesFromSourceRoots
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File

@InitializeInjekt
@AutoService(ComponentRegistrar::class)
class InjektComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        rootContext<ApplicationContext>(project as Project, configuration).runReader {
            registerExtensions(project)

            val configMap = CompilerConfiguration::class.java
                .declaredFields
                .single { it.name == "map" }
                .also { it.isAccessible = true }[configuration]!! as MutableMap<Any, Any?>

            fun CompilerConfigurationKey<*>.ideaKey(): Any =
                CompilerConfigurationKey::class.java
                    .declaredFields
                    .single { it.name == "ideaKey" }
                    .also { it.isAccessible = true }[this]!!

            val allSourceRoots =
                configMap[CLIConfigurationKeys.CONTENT_ROOTS.ideaKey()] as MutableList<ContentRoot>

            println("all s r ${allSourceRoots.javaClass}")

            val kotlinSourceRoots = allSourceRoots
                .filterIsInstance<KotlinSourceRoot>()
                .distinct()

            val files = createSourceFilesFromSourceRoots(
                configuration,
                project,
                kotlinSourceRoots
            )
            val fileManager = given<KtFileManager>()
            val fileCache = fileManager.fileCache
            fileCache.deleteDependentsOfDeletedFiles()
            files.forEach { fileCache.deleteDependents(File(it.virtualFilePath)) }
            files
                .filter { it.text.contains("@com.ivianuu.injekt.internal.ContextImplMarker") }
                .forEach {
                    fileManager.factoriesToGenerate += it.text
                        .lines()
                        .first()
                        .split("context-impl:")[1]
                        .let { FqName(it) }
                    fileCache.deleteFileAndDependents(File(it.virtualFilePath))
                }

            println("deleted files ${fileManager.deletedFiles.joinToString("\n")}")

            allSourceRoots
                .removeAll {
                    it is KotlinSourceRoot && it.path
                        .also { println("check path $it") } in fileManager.deletedFiles
                }
        }
    }

    @Reader
    private fun registerExtensions(project: Project) {
        StorageComponentContainerContributor.registerExtension(
            project,
            given<InjektStorageContainerContributor>()
        )

        AnalysisHandlerExtension.registerExtension(
            project,
            given<InjektKtGenerationExtension>()
        )

        registerExtensionAtFirst(
            project,
            IrGenerationExtension.extensionPointName,
            given<InjektIrGenerationExtension>()
        )
    }

    fun <T : Any> registerExtensionAtFirst(
        project: Project,
        extensionPointName: ExtensionPointName<T>,
        extension: T
    ) {
        val extensionPoint = Extensions.getArea(project)
            .getExtensionPoint(extensionPointName)

        val registeredExtensions = extensionPoint.extensionList
        registeredExtensions.forEach { extensionPoint.unregisterExtension(it::class.java) }

        extensionPoint.registerExtension(extension, {})
        registeredExtensions.forEach { extensionPoint.registerExtension(it, {}) }
    }

}
