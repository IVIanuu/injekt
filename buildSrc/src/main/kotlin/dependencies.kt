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
    const val applicationIdComparison = "com.ivianuu.injekt.comparison"
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
    const val version = "${Build.versionName}-dev278"
}

object Deps {
    const val androidGradlePlugin = "com.android.tools.build:gradle:4.0.1"

    object AndroidX {
        const val appCompat = "androidx.appcompat:appcompat:1.1.0"

        object Compose {
            const val version = "0.1.0-dev15"
            const val compiler = "androidx.compose:compose-compiler:$version"
            const val core = "androidx.compose.ui:ui:$version"
            const val material = "androidx.compose.material:material:$version"
            const val materialIconsExtended =
                "androidx.compose.material:material-icons-extended:$version"
            const val runtime = "androidx.compose.runtime:runtime:$version"
            const val test = "androidx.compose.test:test-core:$version"
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
        private const val version = "1.3.7-SNAPSHOT"
        const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
        const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$version"
    }

    object Dagger2 {
        private const val version = "2.26"
        const val dagger2 = "com.google.dagger:dagger:$version"
        const val compiler = "com.google.dagger:dagger-compiler:$version"
    }

    const val dagger2Reflect = "com.jakewharton.dagger:dagger-reflect:0.2.0"

    const val guava = "com.google.guava:guava:27.1-android"

    const val guice = "com.google.inject:guice:4.2.2"

    const val junit = "junit:junit:4.12"

    const val katana = "org.rewedigital.katana:katana-core:1.13.1"

    const val kodein = "org.kodein.di:kodein-di-erased-jvm:6.5.3"

    const val koin = "org.koin:koin-core:2.1.5"

    object Kotlin {
        private const val version = "1.4.0-rc"
        const val compilerEmbeddable = "org.jetbrains.kotlin:kotlin-compiler-embeddable:$version"
        const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"
        const val gradlePluginApi = "org.jetbrains.kotlin:kotlin-gradle-plugin-api:$version"
        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$version"
    }

    const val kotlinCompileTesting = "com.github.tschuchortdev:kotlin-compile-testing:1.2.7"

    const val mavenGradlePlugin =
        "com.github.dcendents:android-maven-gradle-plugin:2.1"

    const val processingX = "com.ivianuu.processingx:processingx:0.0.1-dev4"

    const val roboelectric = "org.robolectric:robolectric:4.4-SNAPSHOT"

    const val spotlessGradlePlugin = "com.diffplug.spotless:spotless-plugin-gradle:3.26.1"

    object Toothpick {
        private const val version = "3.1.0"
        const val toothpick =
            "com.github.stephanenicolas.toothpick:toothpick-runtime:$version"
        const val compiler =
            "com.github.stephanenicolas.toothpick:toothpick-compiler:$version"
    }
}
