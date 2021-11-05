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

import com.ivianuu.injekt.compiler.analysis.InjectCallCheckerExtension
import com.ivianuu.injekt.compiler.analysis.InjektDeclarationGeneratorExtension
import com.ivianuu.injekt.compiler.analysis.InjectSyntheticScopeProviderExtension
import com.ivianuu.injekt.compiler.analysis.InjectTypeResolutionInterceptorExtension
import com.ivianuu.injekt.compiler.analysis.InjektDiagnosticSuppressor
import com.ivianuu.injekt.compiler.analysis.InjektStorageComponentContainerContributor
import com.ivianuu.injekt.compiler.transform.InjektIrGenerationExtension
import com.ivianuu.shaded_injekt.Inject
import com.ivianuu.shaded_injekt.Provide
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.com.intellij.openapi.extensions.LoadingOrder
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.internal.TypeResolutionInterceptor
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.synthetic.SyntheticScopeProviderExtension
import java.io.File

class InjektComponentRegistrar : ComponentRegistrar {
  override fun registerProjectComponents(
    project: MockProject,
    @Provide configuration: CompilerConfiguration,
  ) {
    if (configuration.isKaptCompilation()) return

    configuration.put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, true)

    @Provide val injektFqNames = InjektFqNames(configuration.getNotNull(RootPackageKey))

    if (configuration.get(SrcDirKey) != null)
      project.registerCodegenExtensions()

    if (configuration.get(DumpDirKey) != null)
      project.registerAnalysisExtensions()
  }
}

private fun MockProject.registerCodegenExtensions(
  @Inject configuration: CompilerConfiguration,
  injektFqNames: InjektFqNames
) {
  val srcDir = configuration.getNotNull(SrcDirKey)
  val cacheDir = configuration.getNotNull(CacheDirKey)
  val modifiedFiles = configuration.get(ModifiedFilesKey)
  val removedFiles = configuration.get(RemovedFilesKey)
  val withCompilation = configuration.get(WithCompilationKey) ?: false
  AnalysisHandlerExtension.registerExtension(
    this,
    InjektDeclarationGeneratorExtension(srcDir, cacheDir, modifiedFiles, removedFiles,
    withCompilation)
  )
}

private fun MockProject.registerAnalysisExtensions(
  @Inject configuration: CompilerConfiguration,
  injektFqNames: InjektFqNames
) {
  StorageComponentContainerContributor.registerExtension(
    this,
    InjektStorageComponentContainerContributor { injektFqNames }
  )
  IrGenerationExtension.registerExtensionWithLoadingOrder(
    this,
    LoadingOrder.FIRST,
    InjektIrGenerationExtension(configuration.getNotNull(DumpDirKey))
  )

  TypeResolutionInterceptor.registerExtension(
    this,
    InjectTypeResolutionInterceptorExtension()
  )

  AnalysisHandlerExtension.registerExtension(
    this,
    InjectCallCheckerExtension()
  )

  // extension point does not exist CLI for some reason
  // but it's still queried later
  SyntheticScopeProviderExtension.registerExtensionPoint(this)
  SyntheticScopeProviderExtension.registerExtension(
    this,
    InjectSyntheticScopeProviderExtension({ injektFqNames })
  )

  @Suppress("DEPRECATION")
  Extensions.getRootArea().getExtensionPoint(DiagnosticSuppressor.EP_NAME)
    .registerExtension(InjektDiagnosticSuppressor(), this)
}

private fun CompilerConfiguration.isKaptCompilation(): Boolean {
  val outputDir = this[JVMConfigurationKeys.OUTPUT_DIRECTORY]
  val kaptOutputDirs = listOf(
    listOf("tmp", "kapt3", "stubs"),
    listOf("tmp", "kapt3", "incrementalData"),
    listOf("tmp", "kapt3", "incApCache")
  ).map { File(it.joinToString(File.separator)) }
  return kaptOutputDirs.any { outputDir?.parentFile?.endsWith(it) == true }
}

fun IrGenerationExtension.Companion.registerExtensionWithLoadingOrder(
  project: MockProject,
  loadingOrder: LoadingOrder,
  extension: IrGenerationExtension,
) {
  project.extensionArea
    .getExtensionPoint(IrGenerationExtension.extensionPointName)
    .registerExtension(extension, loadingOrder, project)
}
