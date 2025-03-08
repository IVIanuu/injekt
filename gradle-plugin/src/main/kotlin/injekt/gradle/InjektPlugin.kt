/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.gradle

import com.google.auto.service.*
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.*

@AutoService(KotlinCompilerPluginSupportPlugin::class)
class InjektPlugin : KotlinCompilerPluginSupportPlugin {
  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>) =
    kotlinCompilation.target.project.plugins.hasPlugin(InjektPlugin::class.java)

  override fun apply(target: Project) {
    target.pluginManager.apply("com.google.devtools.ksp")
    target.dependencies.add(
      "ksp",
      "io.github.ivianuu.injekt:ksp:${BuildConfig.VERSION}"
    )
  }

  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>) =
    kotlinCompilation.target.project.provider {
      listOf(SubpluginOption("dumpDir", "${kotlinCompilation.target.project.buildDir.resolve("injekt/dump/${kotlinCompilation.defaultSourceSet.name}")}"))
    }

  override fun getCompilerPluginId() = "io.github.ivianuu.injekt"

  override fun getPluginArtifact() = SubpluginArtifact(
    groupId = "io.github.ivianuu.injekt",
    artifactId = "compiler",
    version = BuildConfig.VERSION
  )
}
