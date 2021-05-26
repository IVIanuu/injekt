/*
 * Copyright 2021 Manuel Wrage
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
import com.ivianuu.injekt.compiler.analysis.*
import com.ivianuu.injekt.compiler.transform.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.com.intellij.mock.*
import org.jetbrains.kotlin.com.intellij.openapi.extensions.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.extensions.*
import org.jetbrains.kotlin.extensions.internal.*
import org.jetbrains.kotlin.resolve.diagnostics.*
import java.io.*

@AutoService(ComponentRegistrar::class)
class InjektComponentRegistrar : ComponentRegistrar {
  override fun registerProjectComponents(
    project: MockProject,
    configuration: CompilerConfiguration,
  ) {
    val outputDir = configuration[JVMConfigurationKeys.OUTPUT_DIRECTORY]
    val kaptOutputDirs = listOf(
      listOf("tmp", "kapt3", "stubs"),
      listOf("tmp", "kapt3", "incrementalData"),
      listOf("tmp", "kapt3", "incApCache")
    ).map { File(it.joinToString(File.separator)) }
    val isGenerateKaptStubs = kaptOutputDirs.any { outputDir?.parentFile?.endsWith(it) == true }
    if (isGenerateKaptStubs) return

    StorageComponentContainerContributor.registerExtension(
      project,
      InjektStorageComponentContainerContributor()
    )
    IrGenerationExtension.registerExtensionWithLoadingOrder(
      project,
      LoadingOrder.FIRST,
      InjektIrGenerationExtension()
    )
    IrGenerationExtension.registerExtensionWithLoadingOrder(
      project,
      LoadingOrder.LAST,
      InjektIrDumper(dumpDir(configuration))
    )
    TypeResolutionInterceptor.registerExtension(
      project,
      InjektTypeResolutionInterceptorExtension()
    )
    @Suppress("DEPRECATION")
    Extensions.getRootArea().getExtensionPoint(DiagnosticSuppressor.EP_NAME)
      .registerExtension(InjektDiagnosticSuppressor())
  }
}

private fun IrGenerationExtension.Companion.registerExtensionWithLoadingOrder(
  project: MockProject,
  loadingOrder: LoadingOrder,
  extension: IrGenerationExtension,
) {
  project.extensionArea
    .getExtensionPoint(IrGenerationExtension.extensionPointName)
    .registerExtension(extension, loadingOrder, project)
}
