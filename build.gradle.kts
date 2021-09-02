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

import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*

buildscript {
  repositories {
    mavenLocal()
    google()
    mavenCentral()
    jcenter()
    maven("https://plugins.gradle.org/m2")
  }
  dependencies {
    classpath(Deps.androidGradlePlugin)
    classpath(Deps.dokkaGradlePlugin)
    classpath(Deps.Injekt.gradlePlugin)
    classpath(Deps.Kotlin.gradlePlugin)
    classpath(Deps.KotlinSerialization.gradlePlugin)
    classpath(Deps.Ksp.gradlePlugin)
    classpath(Deps.mavenPublishGradlePlugin)
    classpath(Deps.shadowGradlePlugin)
  }
}

allprojects {
  repositories {
    mavenLocal()
    google()
    mavenCentral()
    jcenter()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://plugins.gradle.org/m2")
  }

  if (project.name == "injekt-compiler-plugin" ||
    project.name == "injekt-gradle-plugin" ||
    project.name == "injekt-symbol-processor")
      return@allprojects

  if (project.name != "injekt-common") {
    if (!plugins.hasPlugin("com.google.devtools.ksp"))
      plugins.apply("com.google.devtools.ksp")

    configurations["ksp"]
      .dependencies.add(dependencies.project(":injekt-symbol-processor"))
  }

  fun setupCompilation(compilation: KotlinCompilation<*>) {
    val sourceSetName = name

    val project = compilation.compileKotlinTask.project

    val dumpDir = project.buildDir.resolve("injekt/dump/$sourceSetName")
      .also { it.mkdirs() }

    if (project.name != "injekt-common") {
      configurations["kotlinCompilerPluginClasspath"]
        .dependencies.add(dependencies.project(":injekt-compiler-plugin"))
    }

    val pluginOptions = listOf(
      SubpluginOption(
        key = "dumpDir",
        value = dumpDir.absolutePath
      )
    )

    pluginOptions.forEach { option ->
      compilation.kotlinOptions.freeCompilerArgs += listOf(
        "-P", "plugin:com.ivianuu.injekt:${option.key}=${option.value}"
      )
    }
  }

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
