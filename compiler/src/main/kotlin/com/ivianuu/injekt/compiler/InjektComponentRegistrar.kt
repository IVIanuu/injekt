/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler

import com.google.auto.service.*
import com.ivianuu.injekt.compiler.analysis.*
import com.ivianuu.injekt.compiler.transform.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.com.intellij.mock.*
import org.jetbrains.kotlin.com.intellij.openapi.extensions.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.extensions.*
import org.jetbrains.kotlin.resolve.diagnostics.*
import org.jetbrains.kotlin.resolve.extensions.*
import org.jetbrains.kotlin.synthetic.*
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

    if (configuration[CLIConfigurationKeys.METADATA_DESTINATION_DIRECTORY] == null)
      AnalysisHandlerExtension.registerExtension(
        project,
        InjectCallChecker()
      )
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
