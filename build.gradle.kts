/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

import com.ivianuu.injekt.gradle.InjektPlugin
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.dokka.utilities.cast
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.CompilerPluginConfig
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.tasks.BaseKotlinCompile

buildscript {
  repositories {
    mavenLocal()
    google()
    mavenCentral()
    maven("https://plugins.gradle.org/m2")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://androidx.dev/storage/compose-compiler/repository")
  }
  dependencies {
    classpath(Deps.androidGradlePlugin)
    classpath(Deps.atomicFuGradlePlugin)
    classpath(Deps.dokkaGradlePlugin)
    classpath(Deps.injektGradlePlugin)
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
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://plugins.gradle.org/m2")
    maven("https://androidx.dev/storage/compose-compiler/repository")
  }

  plugins.withId("com.vanniktech.maven.publish") {
    extensions.getByType<MavenPublishBaseExtension>().run {
      publishToMavenCentral(SonatypeHost.S01)
      signAllPublications()
    }
  }

  if (project.name == "injekt-compiler-plugin" ||
    project.name == "injekt-gradle-plugin" ||
    project.name == "injekt-ide-plugin" ||
    project.name == "injekt-ksp")
    return@allprojects

  project.pluginManager.apply("com.google.devtools.ksp")
  project.dependencies.add("ksp", project(":injekt-ksp"))

  fun setupCompilation(compilation: KotlinCompilation<*>) {
    configurations["kotlinCompilerPluginClasspath"]
      .dependencies.add(dependencies.project(":injekt-compiler-plugin"))

    compilation.kotlinOptions.freeCompilerArgs += "-Xcontext-receivers"

    compilation.compileTaskProvider.get().cast<BaseKotlinCompile>().pluginOptions.add(
      CompilerPluginConfig().apply {
        InjektPlugin().applyToCompilation(compilation).get().forEach {
          addPluginArgument("com.ivianuu.injekt", it)
        }
      }
    )
  }

  afterEvaluate {
    when {
      pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform") -> {
        afterEvaluate {
          extensions.getByType(KotlinMultiplatformExtension::class.java).run {
            targets
              .flatMap { it.compilations }
              .forEach { setupCompilation(it) }
          }
        }
      }
      pluginManager.hasPlugin("org.jetbrains.kotlin.android") -> {
        afterEvaluate {
          extensions.getByType(KotlinAndroidProjectExtension::class.java).run {
            target.compilations
              .forEach { setupCompilation(it) }
          }
        }
      }
      pluginManager.hasPlugin("org.jetbrains.kotlin.jvm") -> {
        afterEvaluate {
          extensions.getByType(KotlinJvmProjectExtension::class.java).run {
            target.compilations
              .forEach { setupCompilation(it) }
          }
        }
      }
    }
  }
}
