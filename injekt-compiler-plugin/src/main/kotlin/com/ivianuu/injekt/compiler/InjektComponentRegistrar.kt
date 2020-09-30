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
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.RootFactory
import com.ivianuu.injekt.compiler.checkers.InjektStorageContainerContributor
import com.ivianuu.injekt.compiler.generator.InjektKtGenerationExtension
import com.ivianuu.injekt.rootFactory
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File

@AutoService(ComponentRegistrar::class)
class InjektComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration,
    ) {
        rootFactory<ApplicationComponentFactory>()(project, configuration, ApplicationModule)
            .registerExtensions()
    }

}

@Given
fun registerExtensions(
    project: Project,
    injektStorageContainerContributor: InjektStorageContainerContributor,
    injektKtGenerationExtension: InjektKtGenerationExtension,
    injektIntrinsicIrExtension: InjektIntrinsicIrExtension,
) {
    StorageComponentContainerContributor.registerExtension(
        project,
        injektStorageContainerContributor
    )

    AnalysisHandlerExtension.registerExtension(
        project,
        injektKtGenerationExtension
    )

    IrGenerationExtension.registerExtension(
        project,
        injektIntrinsicIrExtension
    )
}

interface ApplicationComponent {
    val registerExtensions: registerExtensions
}

@RootFactory
typealias ApplicationComponentFactory = (
    Project, CompilerConfiguration, ApplicationModule,
) -> ApplicationComponent

@Module
object ApplicationModule {
    @Given(ApplicationComponent::class)
    fun srcDir(configuration: CompilerConfiguration): SrcDir =
        File(configuration.getNotNull(SrcDirKey))
            .also { it.mkdirs() }

    @Given
    fun logger(): Logger? = if (loggingEnabled) LoggerImpl else null
}
