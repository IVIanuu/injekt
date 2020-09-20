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
import com.ivianuu.injekt.compiler.backend.InjektIrGenerationExtension
import com.ivianuu.injekt.compiler.frontend.InjektStorageContainerContributor
import com.ivianuu.injekt.given
import com.ivianuu.injekt.rootContext
import com.ivianuu.injekt.runReader
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension

@InitializeInjekt
@AutoService(ComponentRegistrar::class)
class InjektComponentRegistrar : ComponentRegistrar {

    private lateinit var applicationContext: ApplicationContext

    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        applicationContext = rootContext(project as Project, configuration)

        applicationContext.runReader {
            StorageComponentContainerContributor.registerExtension(
                project,
                given<InjektStorageContainerContributor>()
            )

            AnalysisHandlerExtension.registerExtension(project, given<LookupTrackerInitializer>())
            AnalysisHandlerExtension.registerExtension(project, given<IrFileGenerator>())

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
            irExtensionPoint.registerExtension(given<InjektIrGenerationExtension>()) {}
            if (composeExtension != null) irExtensionPoint.registerExtension(composeExtension) {}
        }
    }

}


