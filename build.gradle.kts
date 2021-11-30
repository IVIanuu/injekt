/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

import com.ivianuu.injekt.gradle.*
import com.vanniktech.maven.publish.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*

buildscript {
  repositories {
    mavenLocal()
    google()
    mavenCentral()
    jcenter()
    maven("https://plugins.gradle.org/m2")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
  dependencies {
    classpath(Deps.androidGradlePlugin)
    classpath(Deps.atomicFuGradlePlugin)
    classpath(Deps.Compose.gradlePlugin)
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
