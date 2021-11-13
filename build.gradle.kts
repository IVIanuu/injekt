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

import com.ivianuu.injekt.gradle.InjektExtension
import com.ivianuu.injekt.gradle.InjektPlugin
import com.vanniktech.maven.publish.MavenPublishPluginExtension
import com.vanniktech.maven.publish.SonatypeHost
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
    classpath(Deps.injektGradlePlugin)
    classpath(Deps.injektGradlePluginShaded)
    classpath(Deps.Kotlin.gradlePlugin)
    classpath(Deps.KotlinSerialization.gradlePlugin)
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

  plugins.withId("com.vanniktech.maven.publish") {
    extensions.getByType<MavenPublishPluginExtension>()
      .sonatypeHost = SonatypeHost.S01
  }

  if (project.name == "injekt-compiler-plugin" ||
    project.name == "injekt-compiler-plugin-shaded" ||
    project.name == "injekt-gradle-plugin" ||
    project.name == "injekt-gradle-plugin-shaded")
    return@allprojects

  project.extensions.add("injekt", InjektExtension())

  fun setupCompilation(compilation: KotlinCompilation<*>) {
    configurations["kotlinCompilerPluginClasspath"]
      .dependencies.add(dependencies.project(":injekt-compiler-plugin"))
    InjektPlugin().applyToCompilation(compilation)
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
