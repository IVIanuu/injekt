/*
 * Copyright 2020 Manuel Wrage
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
    const val applicationId = "com.ivianuu.injekt.sample"
    const val applicationIdComparison = "com.ivianuu.injekt.samples.comparison"
    const val compileSdk = 29
    const val minSdk = 21
    const val minSdkComparison = 29
    const val targetSdk = 29

    const val versionCode = 1
    const val versionName = "0.0.1"
}

object Publishing {
    const val groupId = "com.ivianuu.injekt"
    const val vcsUrl = "https://github.com/IVIanuu/injekt"
    const val version = "${Build.versionName}-dev474"
}

object Deps {
    const val androidGradlePlugin = "com.android.tools.build:gradle:4.1.2"

    object AndroidX {
        object Activity {
            private const val version = "1.3.0-alpha03"
            const val activity = "androidx.activity:activity:$version"
            const val compose = "androidx.activity:activity-compose:$version"
        }

        object Compose {
            const val version = "1.0.0-beta01"
            const val compiler = "androidx.compose.compiler:compiler:$version"
            const val material = "androidx.compose.material:material:$version"
            const val runtime = "androidx.compose.runtime:runtime:$version"
        }

        object Lifecycle {
            private const val version = "2.2.0"
            const val extensions = "androidx.lifecycle:lifecycle-extensions:$version"
            const val runtime = "androidx.lifecycle:lifecycle-runtime-ktx:$version"
        }

        object Test {
            const val core = "androidx.test:core-ktx:1.2.0"
            const val junit = "androidx.test.ext:junit:1.0.0"
        }

        const val work = "androidx.work:work-runtime-ktx:2.4.0"
    }

    const val bintrayGradlePlugin =
        "com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.5"

    const val buildConfigGradlePlugin =
        "gradle.plugin.de.fuerstenau:BuildConfigPlugin:1.1.8"

    object Coroutines {
        private const val version = "1.4.2"
        const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
        const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$version"
    }

    object Injekt {
        const val version = "0.0.1-dev439"
        const val gradlePlugin = "com.ivianuu.injekt:injekt-gradle-plugin:$version"
    }

    const val junit = "junit:junit:4.12"

    object Kotlin {
        private const val version = "1.4.30"
        const val compiler = "org.jetbrains.kotlin:kotlin-compiler:$version"
        const val compilerEmbeddable = "org.jetbrains.kotlin:kotlin-compiler-embeddable:$version"
        const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"
        const val gradlePluginApi = "org.jetbrains.kotlin:kotlin-gradle-plugin-api:$version"
        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"
    }

    const val kotlinCompileTesting = "com.github.tschuchortdev:kotlin-compile-testing:1.3.6"

    const val kotestAssertions = "io.kotest:kotest-assertions-core:4.3.0"

    const val mavenGradlePlugin =
        "com.github.dcendents:android-maven-gradle-plugin:2.1"

    object Moshi {
        private const val version = "1.9.2"
        const val adapters = "com.squareup.moshi:moshi-adapters:$version"
        const val moshi = "com.squareup.moshi:moshi:$version"
        const val codegen = "com.squareup.moshi:moshi-kotlin-codegen:$version"
        private const val sealedVersion = "0.7.0"
        const val sealedRuntime = "dev.zacsweers.moshix:moshi-sealed-runtime:$sealedVersion"
        const val sealedCodegen = "dev.zacsweers.moshix:moshi-sealed-codegen:$sealedVersion"
    }

    const val processingX = "com.ivianuu.processingx:processingx:0.0.1-dev4"

    const val roboelectric = "org.robolectric:robolectric:4.4"

    const val shadowGradlePlugin = "com.github.jengelman.gradle.plugins:shadow:5.2.0"

    const val spotlessGradlePlugin = "com.diffplug.spotless:spotless-plugin-gradle:3.26.1"
}
