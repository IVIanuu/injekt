/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

import com.vanniktech.maven.publish.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.atomicFu) apply false
  alias(libs.plugins.dokka) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.mavenPublish) apply false
}

allprojects {
  plugins.withId("com.vanniktech.maven.publish") {
    extensions.getByType<MavenPublishBaseExtension>().run {
      publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
      signAllPublications()
    }
  }

  if (project.name == "compiler" ||
    project.name == "gradle-plugin" ||
    project.name == "ksp")
    return@allprojects

  project.pluginManager.apply("com.google.devtools.ksp")
  dependencies.add("ksp", project(":ksp"))

  fun setupCompilation(compilation: KotlinCompilation<*>) {
    val project = compilation.compileKotlinTask.project
    dependencies.add("kotlinCompilerPluginClasspath", project(":compiler"))

    val sourceSetName = name

    val dumpDir = project.buildDir.resolve("injekt/dump/$sourceSetName")
      .also { it.mkdirs() }

    val pluginOptions = listOf(
      SubpluginOption(
        key = "dumpDir",
        value = dumpDir.absolutePath
      )
    )

    pluginOptions.forEach { option ->
      compilation.kotlinOptions.freeCompilerArgs += listOf(
        "-P", "plugin:injekt:${option.key}=${option.value}"
      )
    }
  }

  afterEvaluate {
    when {
      pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform") -> {
        extensions.getByType(KotlinMultiplatformExtension::class.java).run {
          project.afterEvaluate {
            targets
              .flatMap { it.compilations }
              .forEach { setupCompilation(it) }
          }
        }
      }
      pluginManager.hasPlugin("org.jetbrains.kotlin.android") -> {
        extensions.getByType(KotlinAndroidProjectExtension::class.java).run {
          project.afterEvaluate {
            target.compilations
              .forEach { setupCompilation(it) }
          }
        }
      }
      pluginManager.hasPlugin("org.jetbrains.kotlin.jvm") -> {
        extensions.getByType(KotlinJvmProjectExtension::class.java).run {
          project.afterEvaluate {
            target.compilations
              .forEach { setupCompilation(it) }
          }
        }
      }
    }
  }
}
