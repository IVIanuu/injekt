/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.gradle

import org.gradle.api.*
import org.gradle.api.provider.*
import org.jetbrains.kotlin.gradle.plugin.*

class InjektPlugin : KotlinCompilerPluginSupportPlugin {
  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
    kotlinCompilation.target.project.plugins.hasPlugin(InjektPlugin::class.java)

  override fun apply(target: Project) {
    target.pluginManager.apply("com.google.devtools.ksp")
    target.dependencies.add(
      "ksp",
      "com.ivianuu.injekt:ksp:${BuildConfig.VERSION}"
    )
    target.dependencies.add(
      "implementation",
      "com.ivianuu.injekt:core:${BuildConfig.VERSION}"
    )
    target.dependencies.add(
      "implementation",
      "com.ivianuu.injekt:common:${BuildConfig.VERSION}"
    )
  }

  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> =
    kotlinCompilation.target.project.provider {
      listOf(SubpluginOption("dumpDir", "${kotlinCompilation.target.project.buildDir.resolve("injekt/dump/${kotlinCompilation.defaultSourceSet.name}")}"))
    }

  override fun getCompilerPluginId(): String = "com.ivianuu.injekt"

  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
    groupId = "com.ivianuu.injekt",
    artifactId = "compiler",
    version = BuildConfig.VERSION
  )
}
