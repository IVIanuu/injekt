/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.gradle

import org.gradle.api.Project
import org.gradle.api.UnknownProjectException
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class InjektPlugin : KotlinCompilerPluginSupportPlugin {
  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
    kotlinCompilation.target.project.plugins.hasPlugin(InjektPlugin::class.java)

  override fun apply(target: Project) {
    target.pluginManager.apply("com.google.devtools.ksp")
    target.dependencies.add(
      "ksp",
      try {
        target.project(":injekt-ksp")
      } catch (e: UnknownProjectException) {
        "com.ivianuu.injekt:injekt-ksp:${BuildConfig.VERSION}"
      }
    )
  }

  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
    kotlinCompilation.kotlinOptions.freeCompilerArgs += "-Xallow-kotlin-package"

    val project = kotlinCompilation.target.project

    val sourceSetName = kotlinCompilation.defaultSourceSet.name

    return project.provider {
      listOf(SubpluginOption("dumpDir", "injekt/dump/$sourceSetName"))
    }
  }

  override fun getCompilerPluginId(): String = "com.ivianuu.injekt"

  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
    groupId = "com.ivianuu.injekt",
    artifactId = "injekt-compiler-plugin",
    version = BuildConfig.VERSION
  )
}
