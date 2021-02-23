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
import com.ivianuu.injekt.compiler.analysis.GivenCallResolutionInterceptorExtension
import com.ivianuu.injekt.compiler.analysis.InjektDiagnosticSuppressor
import com.ivianuu.injekt.compiler.analysis.InjektKtGenerationExtension
import com.ivianuu.injekt.compiler.analysis.InjektStorageComponentContainerContributor
import com.ivianuu.injekt.compiler.analysis.InjektTypeResolutionInterceptor
import com.ivianuu.injekt.compiler.index.CliIndexStoreFactory
import com.ivianuu.injekt.compiler.transform.InjektIrDumper
import com.ivianuu.injekt.compiler.transform.InjektIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.com.intellij.openapi.extensions.LoadingOrder
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.internal.CandidateInterceptor
import org.jetbrains.kotlin.extensions.internal.TypeResolutionInterceptor
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File

@AutoService(ComponentRegistrar::class)
class InjektComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration,
    ) {
        // Don't bother with KAPT tasks.
        // There is no way to pass KSP options to compileKotlin only. Have to workaround here.
        val outputDir = configuration[JVMConfigurationKeys.OUTPUT_DIRECTORY]
        val kaptOutputDirs = listOf(
            listOf("tmp", "kapt3", "stubs"),
            listOf("tmp", "kapt3", "incrementalData"),
            listOf("tmp", "kapt3", "incApCache")
        ).map { File(it.joinToString(File.separator)) }
        val isGenerateKaptStubs = kaptOutputDirs.any { outputDir?.parentFile?.endsWith(it) == true }
        if (isGenerateKaptStubs) return

        val cacheDir = cacheDir(configuration)
        val srcDir = srcDir(configuration)
        val setInvalidationFile = setInvalidationFile(configuration)
        val dumpDir = dumpDir(configuration, srcDir)

        AnalysisHandlerExtension.registerExtension(
            project,
            InjektKtGenerationExtension(srcDir, cacheDir,
            setInvalidationFile)
        )
        StorageComponentContainerContributor.registerExtension(
            project,
            InjektStorageComponentContainerContributor(CliIndexStoreFactory)
        )
        IrGenerationExtension.registerExtensionFirst(
            project,
            InjektIrGenerationExtension()
        )
        IrGenerationExtension.registerExtensionLast(
            project,
            InjektIrDumper(cacheDir, dumpDir, setInvalidationFile)
        )
        CandidateInterceptor.registerExtension(
            project,
            GivenCallResolutionInterceptorExtension(CliIndexStoreFactory)
        )
        TypeResolutionInterceptor.registerExtension(
            project,
            InjektTypeResolutionInterceptor()
        )
        @Suppress("DEPRECATION")
        Extensions.getRootArea().getExtensionPoint(DiagnosticSuppressor.EP_NAME)
            .registerExtension(InjektDiagnosticSuppressor())
    }
}

private fun IrGenerationExtension.Companion.registerExtensionFirst(
    project: MockProject,
    extension: IrGenerationExtension,
) {
    project.extensionArea
        .getExtensionPoint(IrGenerationExtension.extensionPointName)
        .registerExtension(extension, LoadingOrder.FIRST, project)
}

private fun IrGenerationExtension.Companion.registerExtensionLast(
    project: MockProject,
    extension: IrGenerationExtension,
) {
    project.extensionArea
        .getExtensionPoint(IrGenerationExtension.extensionPointName)
        .registerExtension(extension, LoadingOrder.LAST, project)
}
