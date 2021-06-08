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

@file:Suppress("ClassName", "unused")

object Build {
  const val applicationId = "com.ivianuu.injekt.samples.android"
  const val compileSdk = 29
  const val minSdk = 21
  const val minSdkComparison = 29
  const val targetSdk = 29
  const val versionCode = 1
  const val versionName = "0.0.1"
}

object Deps {
  const val androidGradlePlugin = "com.android.tools.build:gradle:4.2.0"

  object AndroidX {
    object Activity {
      private const val version = "1.3.0-alpha08"
      const val activity = "androidx.activity:activity:$version"
      const val compose = "androidx.activity:activity-compose:$version"
    }

    object Compose {
      const val version = "1.0.0-beta08"
      const val compiler = "androidx.compose.compiler:compiler:$version"
      const val material = "androidx.compose.material:material:$version"
      const val runtime = "androidx.compose.runtime:runtime:$version"
      const val test = "androidx.compose.ui:ui-test-junit4:$version"
    }

    object Lifecycle {
      private const val version = "2.3.1"
      const val process = "androidx.lifecycle:lifecycle-process:$version"
      const val runtime = "androidx.lifecycle:lifecycle-runtime-ktx:$version"
    }

    object Test {
      const val core = "androidx.test:core-ktx:1.2.0"
      const val junit = "androidx.test.ext:junit:1.0.0"
    }

    const val work = "androidx.work:work-runtime-ktx:2.5.0"
  }

  const val autoService = "com.google.auto.service:auto-service:1.0-rc7"

  const val buildConfigGradlePlugin =
    "gradle.plugin.de.fuerstenau:BuildConfigPlugin:1.1.8"

  object Coroutines {
    private const val version = "1.5.0"
    const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
    const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
    const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$version"
  }

  const val dokkaGradlePlugin = "org.jetbrains.dokka:dokka-gradle-plugin:1.4.20"

  object Injekt {
    const val version = "0.0.1-dev575"
    const val gradlePlugin = "com.ivianuu.injekt:injekt-gradle-plugin:$version"
    const val scope = "com.ivianuu.injekt:injekt-scope:$version"
  }

  const val junit = "junit:junit:4.12"

  object Kotlin {
    const val version = "1.5.10"
    const val compiler = "org.jetbrains.kotlin:kotlin-compiler:$version"
    const val compilerEmbeddable = "org.jetbrains.kotlin:kotlin-compiler-embeddable:$version"
    const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"
    const val gradlePluginApi = "org.jetbrains.kotlin:kotlin-gradle-plugin-api:$version"
  }

  object KotlinSerialization {
    const val gradlePlugin = "org.jetbrains.kotlin:kotlin-serialization:${Kotlin.version}"
    const val json = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1"
  }

  const val kotlinCompileTesting = "com.github.tschuchortdev:kotlin-compile-testing:1.3.6"

  const val kotestAssertions = "io.kotest:kotest-assertions-core:4.4.3"

  object Ktor {
    private const val version = "1.6.0"
    const val core = "io.ktor:ktor-server-core:$version"
    const val serverTestHost = "io.ktor:ktor-server-test-host:$version"
  }

  const val mavenPublishGradlePlugin = "com.vanniktech:gradle-maven-publish-plugin:0.14.2"

  const val mockk = "io.mockk:mockk:1.11.0"

  const val roboelectric = "org.robolectric:robolectric:4.4"

  const val shadowGradlePlugin = "com.github.jengelman.gradle.plugins:shadow:6.1.0"
}
