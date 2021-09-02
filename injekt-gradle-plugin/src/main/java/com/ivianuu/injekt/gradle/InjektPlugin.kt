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

package com.ivianuu.injekt.gradle

import com.google.auto.service.AutoService
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

@AutoService(KotlinCompilerPluginSupportPlugin::class)
open class InjektPlugin : KotlinCompilerPluginSupportPlugin {
  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
    kotlinCompilation.target.project.plugins.hasPlugin(InjektPlugin::class.java)

  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> =
    kotlinCompilation.setupForInjekt()

  override fun apply(target: Project) {
    if (!target.plugins.hasPlugin("com.google.devtools.ksp")) {
      target.plugins.apply("com.google.devtools.ksp")
      target.configurations.getByName("ksp")
        .dependencies
        .add(
          target
            .dependencies
            .create("com.ivianuu.injekt:injekt-symbol-processor:${BuildConfig.VERSION}"
            )
        )
    }
  }

  override fun getCompilerPluginId(): String = "com.ivianuu.injekt"

  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
    groupId = "com.ivianuu.injekt",
    artifactId = "injekt-compiler-plugin",
    version = BuildConfig.VERSION
  )
}
