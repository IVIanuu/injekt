/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.analysis.InjectCallCheckerExtension
import com.ivianuu.injekt.compiler.analysis.InjectSyntheticScopeProviderExtension
import com.ivianuu.injekt.compiler.analysis.InjektDeclarationGeneratorExtension
import com.ivianuu.injekt.compiler.analysis.InjektDiagnosticSuppressor
import com.ivianuu.injekt.compiler.analysis.InjektStorageComponentContainerContributor
import com.ivianuu.injekt.compiler.transform.InjektIrGenerationExtension
import com.ivianuu.injekt.compiler.transform.fixComposeFunInterfacesPostCompose
import com.ivianuu.shaded_injekt.Inject
import com.ivianuu.shaded_injekt.Provide
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.com.intellij.openapi.extensions.LoadingOrder
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
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
  IrGenerationExtension.registerExtensionWithLoadingOrder(
    this,
    LoadingOrder.LAST,
    object : IrGenerationExtension {
      @OptIn(ObsoleteDescriptorBasedAPI::class)
      override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.fixComposeFunInterfacesPostCompose(
          ctx = Context(
            pluginContext.moduleDescriptor,
            injektFqNames,
            DelegatingBindingTrace(pluginContext.bindingContext, "IR trace")
          )
        )
      }
    }
  )

  if (configuration[CLIConfigurationKeys.METADATA_DESTINATION_DIRECTORY] == null)
    AnalysisHandlerExtension.registerExtension(
      this,
      InjectCallCheckerExtension(configuration.get(WithCompilationKey) ?: false)
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
