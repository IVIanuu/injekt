/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.ide

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.ivianuu.injekt.compiler.analysis.InjectSyntheticScopeProviderExtension
import com.ivianuu.injekt.compiler.analysis.InjektDiagnosticSuppressor
import com.ivianuu.injekt.compiler.analysis.InjektStorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import org.jetbrains.kotlin.synthetic.SyntheticScopeProviderExtension

class InjektProjectInitializer : ProjectManagerListener {
  override fun projectOpened(project: Project) {
    StorageComponentContainerContributor.registerExtension(
      project,
      InjektStorageComponentContainerContributor()
    )
    SyntheticScopeProviderExtension.registerExtension(
      project,
      InjectSyntheticScopeProviderExtension()
    )
    @Suppress("DEPRECATION")
    Extensions.getRootArea().getExtensionPoint(DiagnosticSuppressor.EP_NAME)
      .registerExtension(InjektDiagnosticSuppressor())
  }
}
