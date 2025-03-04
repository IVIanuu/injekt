/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("ClassName", "unused")

object Build {
  const val applicationId = "injekt.samples.android"
  const val compileSdk = 35
  const val minSdk = 27
  const val targetSdk = 35
  const val versionCode = 1
  const val versionName = "0.0.1"
}

object Deps {
  const val androidGradlePlugin = "com.android.tools.build:gradle:8.6.0"

  const val androidxActivityCompose = "androidx.activity:activity-compose:1.10.0"

  const val atomicFuGradlePlugin = "org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.27.0"

  object AutoService {
    private const val version = "1.1.0"
    const val annotations = "com.google.auto.service:auto-service-annotations:$version"
    const val processor = "dev.zacsweers.autoservice:auto-service-ksp:$version"
  }

  object Compose {
    const val version = "1.7.3"
    const val gradlePlugin = "org.jetbrains.kotlin:kotlin-compose-compiler-plugin-embeddable:${Kotlin.version}"
    const val material = "org.jetbrains.compose.material:material:$version"
    const val runtime = "org.jetbrains.compose.runtime:runtime:$version"
  }

  object Coroutines {
    private const val version = "1.10.1"
    const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
    const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$version"
  }

  const val dokkaGradlePlugin = "org.jetbrains.dokka:dokka-gradle-plugin:2.0.0"

  object Injekt {
    const val version = "0.0.1-dev747"
    const val gradlePlugin = "injekt:gradle-plugin:$version"
  }

  const val junit = "junit:junit:4.12"

  object Kotlin {
    const val version = "2.1.0"
    const val compilerEmbeddable = "org.jetbrains.kotlin:kotlin-compiler-embeddable:$version"
    const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"
    const val gradlePluginApi = "org.jetbrains.kotlin:kotlin-gradle-plugin-api:$version"
  }

  object KotlinCompileTesting {
    private const val version = "0.7.0"
    const val core = "dev.zacsweers.kctfork:core:$version"
    const val ksp = "dev.zacsweers.kctfork:ksp:$version"
  }

  object KotlinSerialization {
    const val gradlePlugin = "org.jetbrains.kotlin:kotlin-serialization:${Kotlin.version}"
    const val json = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3"
  }

  const val kotestAssertions = "io.kotest:kotest-assertions-core:5.9.1"

  object Ksp {
    const val version = "2.1.0-1.0.29"
    const val api = "com.google.devtools.ksp:symbol-processing-api:$version"
    const val gradlePlugin = "com.google.devtools.ksp:symbol-processing-gradle-plugin:$version"
    const val symbolProcessing = "com.google.devtools.ksp:symbol-processing:$version"
  }

  const val mavenPublishGradlePlugin = "com.vanniktech:gradle-maven-publish-plugin:0.30.0"
}
