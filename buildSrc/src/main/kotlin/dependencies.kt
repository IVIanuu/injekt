/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("ClassName", "unused")

object Build {
  const val applicationId = "com.ivianuu.injekt.samples.android"
  const val compileSdk = 33
  const val minSdk = 27
  const val targetSdk = 30
  const val versionCode = 1
  const val versionName = "0.0.1"
}

object Deps {
  const val androidGradlePlugin = "com.android.tools.build:gradle:7.4.0"

  object AndroidX {
    object Activity {
      private const val version = "1.5.1"
      const val activity = "androidx.activity:activity:$version"
      const val compose = "androidx.activity:activity-compose:$version"
    }

    object Lifecycle {
      private const val version = "2.5.1"
      const val runtime = "androidx.lifecycle:lifecycle-runtime-ktx:$version"
    }

    object Test {
      const val core = "androidx.test:core-ktx:1.4.0"
      const val junit = "androidx.test.ext:junit:1.0.0"
    }

    const val work = "androidx.work:work-runtime-ktx:2.7.1"
  }

  const val atomicFuGradlePlugin = "org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.20.2"

  const val classGraph = "io.github.classgraph:classgraph:4.8.108"

  object Compose {
    const val version = "1.4.0"
    const val compiler = "org.jetbrains.compose.compiler:compiler:1.4.5"
    const val material = "org.jetbrains.compose.material:material:$version"
    const val runtime = "org.jetbrains.compose.runtime:runtime:$version"
  }

  object Coroutines {
    private const val version = "1.6.4"
    const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
    const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
    const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$version"
  }

  const val dokkaGradlePlugin = "org.jetbrains.dokka:dokka-gradle-plugin:1.8.10"

  const val gradleTestKit = "dev.gradleplugins:gradle-test-kit:7.3.3"

  const val injektGradlePlugin = "com.ivianuu.injekt:injekt-gradle-plugin:0.0.1-dev705"

  const val junit = "junit:junit:4.12"

  object Kotlin {
    const val version = "1.8.20"
    const val compilerEmbeddable = "org.jetbrains.kotlin:kotlin-compiler-embeddable:$version"
    const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"
    const val gradlePluginApi = "org.jetbrains.kotlin:kotlin-gradle-plugin-api:$version"
  }

  object KotlinSerialization {
    const val gradlePlugin = "org.jetbrains.kotlin:kotlin-serialization:${Kotlin.version}"
    const val json = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0"
  }

  const val kotlinCompileTesting = "dev.zacsweers.kctfork:core:0.2.1"

  const val kotestAssertions = "io.kotest:kotest-assertions-core:4.4.3"

  const val mavenPublishGradlePlugin = "com.vanniktech:gradle-maven-publish-plugin:0.24.0"

  const val mockk = "io.mockk:mockk:1.11.0"

  const val roboelectric = "org.robolectric:robolectric:4.4"

  const val shadowGradlePlugin = "gradle.plugin.com.github.johnrengelman:shadow:7.1.2"
}
