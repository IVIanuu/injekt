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

package com.ivianuu.injekt.ide

import com.intellij.ide.ApplicationInitializedListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.RootPackageOption
import com.ivianuu.injekt.compiler.analysis.InjectSyntheticScopeProviderExtension
import com.ivianuu.injekt.compiler.analysis.InjektDiagnosticSuppressor
import com.ivianuu.injekt.compiler.analysis.InjektStorageComponentContainerContributor
import org.jetbrains.kotlin.analyzer.moduleInfo
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.idea.core.unwrapModuleSourceInfo
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import org.jetbrains.kotlin.synthetic.SyntheticScopeProviderExtension

@Suppress("UnstableApiUsage")
class AppInitializer : ApplicationInitializedListener {
  override fun componentsInitialized() {
    val app = ApplicationManager.getApplication()
    app
      .messageBus.connect(app)
      .subscribe(
        ProjectManager.TOPIC,
        object : ProjectManagerListener {
          override fun projectOpened(project: Project) {
            val fqNamesProvider: (ModuleDescriptor) -> InjektFqNames = provider@ {
              it.moduleInfo?.unwrapModuleSourceInfo()?.module
                ?.getOptionValueInFacet(RootPackageOption)
                ?.let { InjektFqNames(FqName(it)) }
                ?: injektFqNames().Default
            }
            StorageComponentContainerContributor.registerExtension(
              project,
              InjektStorageComponentContainerContributor(fqNamesProvider)
            )
            SyntheticScopeProviderExtension.registerExtension(
              project,
              InjectSyntheticScopeProviderExtension(injektFqNames = fqNamesProvider) {
                it.moduleInfo?.unwrapModuleSourceInfo()?.module
                  ?.getOptionValueInFacet(RootPackageOption) != null
              }
            )
            @Suppress("DEPRECATION")
            Extensions.getRootArea().getExtensionPoint(DiagnosticSuppressor.EP_NAME)
              .registerExtension(InjektDiagnosticSuppressor())
          }
        }
      )
  }
}

fun Module.getOptionValueInFacet(option: AbstractCliOption): String? {
  val kotlinFacet = KotlinFacet.get(this) ?: return null
  val commonArgs = kotlinFacet.configuration.settings.compilerArguments ?: return null

  val prefix = "plugin:com.ivianuu.injekt:${option.optionName}="

  val optionValue = commonArgs.pluginOptions
    ?.firstOrNull { it.startsWith(prefix) }
    ?.substring(prefix.length)

  return optionValue
}
