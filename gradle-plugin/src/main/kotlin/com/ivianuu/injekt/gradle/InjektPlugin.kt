/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.gradle

import org.gradle.api.provider.*
import org.jetbrains.kotlin.gradle.plugin.*

class InjektPlugin : KotlinCompilerPluginSupportPlugin {
  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
    kotlinCompilation.target.project.plugins.hasPlugin(InjektPlugin::class.java)

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
