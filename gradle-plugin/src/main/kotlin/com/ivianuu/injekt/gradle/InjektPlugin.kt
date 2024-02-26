/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.gradle

import com.google.auto.service.*
import com.google.devtools.ksp.gradle.*
import org.gradle.api.*
import org.gradle.api.provider.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import java.util.Locale.*

@AutoService(KotlinCompilerPluginSupportPlugin::class)
class InjektPlugin : KotlinCompilerPluginSupportPlugin {
  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>) =
    kotlinCompilation.target.project.plugins.hasPlugin(InjektPlugin::class.java)

  override fun apply(target: Project) {
    target.pluginManager.apply("com.google.devtools.ksp")
    target.dependencies.add(
      "ksp",
      "com.ivianuu.injekt:ksp:${BuildConfig.VERSION}"
    )
  }

  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>) =
    kotlinCompilation.target.project.provider {
      listOf(SubpluginOption("dumpDir", "${kotlinCompilation.target.project.buildDir.resolve("injekt/dump/${kotlinCompilation.defaultSourceSet.name}")}"))
    }

  override fun getCompilerPluginId() = "com.ivianuu.injekt"

  override fun getPluginArtifact() = SubpluginArtifact(
    groupId = "com.ivianuu.injekt",
    artifactId = "compiler",
    version = BuildConfig.VERSION
  )
}
