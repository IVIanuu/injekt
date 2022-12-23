/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.analysis.InjektCallChecker
import com.ivianuu.injekt.compiler.analysis.ContextSyntheticScopeProviderExtension
import com.ivianuu.injekt.compiler.analysis.InjektDeclarationGeneratorExtension
import com.ivianuu.injekt.compiler.analysis.InjektDiagnosticSuppressor
import com.ivianuu.injekt.compiler.analysis.InjektStorageComponentContainerContributor
import com.ivianuu.injekt.compiler.transform.InjektIrGenerationExtension
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.com.intellij.openapi.extensions.LoadingOrder
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.synthetic.SyntheticScopeProviderExtension
import java.text.SimpleDateFormat
import java.util.*

class InjektComponentRegistrar : ComponentRegistrar {
  override fun registerProjectComponents(
    project: MockProject,
    configuration: CompilerConfiguration,
  ) {
    configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS)!!
      .updatePrivateFinalField<MutableMap<LanguageFeature, LanguageFeature.State>>(LanguageVersionSettingsImpl::class, "specificFeatures") {
        toMutableMap()
          .apply { put(LanguageFeature.ContextReceivers, LanguageFeature.State.ENABLED) }
      }

    if (configuration.get(SrcDirKey) != null)
      project.registerCodegenExtensions(configuration)

    if (configuration.get(DumpDirKey) != null)
      project.registerAnalysisExtensions(configuration)
  }
}

private fun MockProject.registerCodegenExtensions(configuration: CompilerConfiguration) {
  val srcDir = configuration.getNotNull(SrcDirKey)
  val cacheDir = configuration.getNotNull(CacheDirKey)
  val modifiedFiles = configuration.get(ModifiedFilesKey)
  val removedFiles = configuration.get(RemovedFilesKey)
  val withCompilation = configuration.get(WithCompilationKey) ?: false
  AnalysisHandlerExtension.registerExtension(
    this,
    InjektDeclarationGeneratorExtension(srcDir, cacheDir, modifiedFiles, removedFiles, withCompilation)
  )
}

private fun MockProject.registerAnalysisExtensions(configuration: CompilerConfiguration) {
  AnalysisHandlerExtension.registerExtension(
    this,
    object : AnalysisHandlerExtension {
      private var finished = false

      override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
      ): AnalysisResult? {
        if (finished) return null
        finished = true

        val logOutputFile = configuration.getNotNull(DumpDirKey)
          .resolve("analysis-log-${
            SimpleDateFormat("dd-MM-yyyy-HH-mm-ss-SSS").format(
            Date(System.currentTimeMillis())
          )}")
        val logOutput = StringBuilder()

        logOutput.appendLine("files:")
        files.forEach { logOutput.appendLine("file: ${it.virtualFilePath}") }

        logOutputFile.parentFile.mkdirs()
        logOutputFile.createNewFile()
        logOutputFile.writeText(logOutput.toString())

        return null
      }
    }
  )
  StorageComponentContainerContributor.registerExtension(
    this,
    InjektStorageComponentContainerContributor()
  )
  IrGenerationExtension.registerExtensionWithLoadingOrder(
    this,
    LoadingOrder.FIRST,
    InjektIrGenerationExtension(configuration.getNotNull(DumpDirKey))
  )

  if (configuration[CLIConfigurationKeys.METADATA_DESTINATION_DIRECTORY] == null)
    AnalysisHandlerExtension.registerExtension(
      this,
      InjektCallChecker(configuration.get(WithCompilationKey) ?: false)
    )

  // extension point does not exist CLI for some reason
  // but it's still queried later
  SyntheticScopeProviderExtension.registerExtensionPoint(this)
  SyntheticScopeProviderExtension.registerExtension(
    this,
    ContextSyntheticScopeProviderExtension()
  )

  @Suppress("DEPRECATION")
  Extensions.getRootArea().getExtensionPoint(DiagnosticSuppressor.EP_NAME)
    .registerExtension(InjektDiagnosticSuppressor(), this)
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
