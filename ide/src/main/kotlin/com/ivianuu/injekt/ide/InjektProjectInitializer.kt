/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.ide

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.ivianuu.injekt.compiler.analysis.InjektDiagnosticSuppressor
import com.ivianuu.injekt.compiler.analysis.InjektStorageComponentContainerContributor
import com.ivianuu.injekt.compiler.updatePrivateFinalField
import org.jetbrains.kotlin.analyzer.moduleInfo
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.idea.base.projectStructure.languageVersionSettings
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class InjektProjectInitializer : ProjectManagerListener {
  override fun projectOpened(project: Project) {
    StorageComponentContainerContributor.registerExtension(
      project,
      object : StorageComponentContainerContributor {
        override fun registerModuleComponents(
          container: StorageComponentContainer,
          platform: TargetPlatform,
          moduleDescriptor: ModuleDescriptor
        ) {
          moduleDescriptor.moduleInfo
            ?.safeAs<ModuleSourceInfo>()
            ?.module
            ?.languageVersionSettings
            ?.updatePrivateFinalField<MutableMap<LanguageFeature, LanguageFeature.State>>(
              LanguageVersionSettingsImpl::class, "specificFeatures") {
              toMutableMap()
                .apply { put(LanguageFeature.ContextReceivers, LanguageFeature.State.ENABLED) }
            }
        }
      }
    )

    StorageComponentContainerContributor.registerExtension(
      project,
      InjektStorageComponentContainerContributor()
    )

    @Suppress("DEPRECATION")
    Extensions.getRootArea().getExtensionPoint(DiagnosticSuppressor.EP_NAME)
      .registerExtension(InjektDiagnosticSuppressor())
  }
}
