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
import com.ivianuu.injekt.compiler.analysis.ImplicitChecker
import com.ivianuu.injekt.compiler.analysis.InjektStorageContainerContributor
import com.ivianuu.injekt.compiler.analysis.ReaderTypeInterceptor
import com.ivianuu.injekt.compiler.transform.InjektIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.internal.TypeResolutionInterceptor
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension

@AutoService(ComponentRegistrar::class)
class InjektComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        val implicitChecker = ImplicitChecker()
        StorageComponentContainerContributor.registerExtension(
            project,
            InjektStorageContainerContributor(implicitChecker)
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
        irExtensionPoint.registerExtension(InjektIrGenerationExtension()) {}
        if (composeExtension != null) irExtensionPoint.registerExtension(composeExtension) {}

        AnalysisHandlerExtension.registerExtension(
            project,
            LookupTrackerInitializer()
        )

        TypeResolutionInterceptor.registerExtension(
            project,
            ReaderTypeInterceptor(implicitChecker)
        )
    }

}
