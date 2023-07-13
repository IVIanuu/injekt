/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(InternalNonStableExtensionPoints::class)

package com.ivianuu.injekt.compiler

import com.google.auto.service.AutoService
import com.ivianuu.injekt.compiler.analysis.InjectCallChecker
import com.ivianuu.injekt.compiler.analysis.InjectSyntheticScopeProviderExtension
import com.ivianuu.injekt.compiler.analysis.InjektCallResolutionInterceptorExtension
import com.ivianuu.injekt.compiler.analysis.InjektDiagnosticSuppressor
import com.ivianuu.injekt.compiler.analysis.InjektStorageComponentContainerContributor
import com.ivianuu.injekt.compiler.transform.InjektIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.com.intellij.openapi.extensions.LoadingOrder
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.internal.CandidateInterceptor
import org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.synthetic.SyntheticScopeProviderExtension
import java.util.*

@OptIn(ExperimentalCompilerApi::class)
@AutoService(ComponentRegistrar::class)
class InjektComponentRegistrar : ComponentRegistrar {
  override fun registerProjectComponents(
    project: MockProject,
    configuration: CompilerConfiguration,
  ) {
    StorageComponentContainerContributor.registerExtension(
      project,
      InjektStorageComponentContainerContributor()
    )
    IrGenerationExtension.registerExtensionWithLoadingOrder(
      project,
      LoadingOrder.FIRST,
      InjektIrGenerationExtension(configuration.getNotNull(DumpDirKey))
    )
    CandidateInterceptor.registerExtension(
      project,
      InjektCallResolutionInterceptorExtension()
    )

    if (configuration[CLIConfigurationKeys.METADATA_DESTINATION_DIRECTORY] == null)
      AnalysisHandlerExtension.registerExtension(
        project,
        InjectCallChecker()
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
      .registerExtension(InjektDiagnosticSuppressor(), project)
  }
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
