/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.analysis.*
import com.ivianuu.injekt.compiler.transform.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.com.intellij.mock.*
import org.jetbrains.kotlin.com.intellij.openapi.extensions.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.extensions.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.diagnostics.*
import org.jetbrains.kotlin.resolve.extensions.*
import java.io.*

class InjektComponentRegistrar : ComponentRegistrar {
  override fun registerProjectComponents(
    project: MockProject,
    configuration: CompilerConfiguration,
  ) {
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
      InjectCallChecker(configuration.get(WithCompilationKey) ?: false)
    )

  // todo remove once compose is fixed
  @Suppress("DEPRECATION")
  Extensions.getRootArea().getExtensionPoint(DiagnosticSuppressor.EP_NAME)
    .registerExtension(
      object : DiagnosticSuppressor {
        override fun isSuppressed(diagnostic: Diagnostic): Boolean =
          isSuppressed(diagnostic, null)

        override fun isSuppressed(
          diagnostic: Diagnostic,
          bindingContext: BindingContext?
        ): Boolean = diagnostic.factory.name == "COMPOSABLE_INVOCATION"
      },
      this
    )
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
