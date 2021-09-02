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

import com.google.auto.service.AutoService
import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.compiler.analysis.InjectSyntheticScopeProviderExtension
import com.ivianuu.injekt.compiler.analysis.InjektDiagnosticSuppressor
import com.ivianuu.injekt.compiler.analysis.InjektStorageComponentContainerContributor
import com.ivianuu.injekt.compiler.incremental.InjektIncrementalDeclarationGenerationExtension
import com.ivianuu.injekt.compiler.transform.InjektIrDumper
import com.ivianuu.injekt.compiler.transform.InjektIrGenerationExtension
import java.io.File
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.com.intellij.openapi.extensions.LoadingOrder
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.synthetic.SyntheticScopeProviderExtension

@AutoService(CommandLineProcessor::class)
class InjektCommandLineProcessor : CommandLineProcessor {
  override val pluginId = "com.ivianuu.injekt"

  override val pluginOptions = listOf(
    CacheDirOption,
    DumpDirOption,
    OutputDirOption
  )

  override fun processOption(
    option: AbstractCliOption,
    value: String,
    configuration: CompilerConfiguration,
  ) {
    when (option.optionName) {
      CacheDirOption.optionName -> configuration.put(CacheDirKey, value)
      DumpDirOption.optionName -> configuration.put(DumpDirKey, value)
      OutputDirOption.optionName -> configuration.put(OutputDirKey, value)
    }
  }
}

val CacheDirOption = CliOption("cacheDir")
val CacheDirKey = CompilerConfigurationKey<String>(CacheDirOption.optionName)
fun cacheDir(configuration: CompilerConfiguration): File =
  File(configuration.getNotNull(CacheDirKey))
    .also { it.mkdirs() }

val DumpDirOption = CliOption("dumpDir")
val DumpDirKey = CompilerConfigurationKey<String>(DumpDirOption.optionName)
fun dumpDir(configuration: CompilerConfiguration): File =
  File(configuration.getNotNull(DumpDirKey))
    .also { it.mkdirs() }

val OutputDirOption = CliOption("outputDir", false)
val OutputDirKey = CompilerConfigurationKey<String>(OutputDirOption.optionName)
fun outputDir(configuration: CompilerConfiguration): File? =
  configuration.get(OutputDirKey)
    ?.let { File(it) }
    ?.also { it.mkdirs() }

private fun CliOption(optionName: String, required: Boolean = true) =
  CliOption(optionName, "<${optionName}>", optionName, required)

@AutoService(ComponentRegistrar::class)
class InjektComponentRegistrar : ComponentRegistrar {
  override fun registerProjectComponents(
    project: MockProject,
    configuration: CompilerConfiguration,
  ) {
    if (isKaptCompilation(configuration)) return
    registerExtensions(project, configuration)
  }
}

private fun registerExtensions(project: MockProject, configuration: CompilerConfiguration) {
  val outputDir = outputDir(configuration)
  if (outputDir != null) {
    AnalysisHandlerExtension.registerExtension(
      project,
      InjektIncrementalDeclarationGenerationExtension(outputDir)
    )
    return
  }

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

  // extension point does not exist CLI for some reason
  // but it's still queried later
  SyntheticScopeProviderExtension.registerExtensionPoint(project)
  SyntheticScopeProviderExtension.registerExtension(
    project,
    InjectSyntheticScopeProviderExtension()
  )

  @Suppress("DEPRECATION")
  Extensions.getRootArea().getExtensionPoint(DiagnosticSuppressor.EP_NAME)
    .registerExtension(InjektDiagnosticSuppressor())
}

private fun isKaptCompilation(configuration: CompilerConfiguration): Boolean {
  val outputDir = configuration[JVMConfigurationKeys.OUTPUT_DIRECTORY]
  val kaptOutputDirs = listOf(
    listOf("tmp", "kapt3", "stubs"),
    listOf("tmp", "kapt3", "incrementalData"),
    listOf("tmp", "kapt3", "incApCache")
  ).map { File(it.joinToString(File.separator)) }
  return kaptOutputDirs.any { outputDir?.parentFile?.endsWith(it) == true }
}

private fun IrGenerationExtension.Companion.registerExtensionWithLoadingOrder(
  @Inject project: MockProject,
  loadingOrder: LoadingOrder,
  extension: IrGenerationExtension,
) {
  project.extensionArea
    .getExtensionPoint(IrGenerationExtension.extensionPointName)
    .registerExtension(extension, loadingOrder, project)
}
