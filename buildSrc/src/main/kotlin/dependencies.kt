/*
 * Copyright 2019 Manuel Wrage
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
    const val buildToolsVersion = "28.0.3"
    const val compileSdk = 29
    const val minSdk = 14
    const val minSdkComparison = 29
    const val targetSdk = 29

    const val versionCode = 1
    const val versionName = "0.0.1"
}

object Publishing {
    const val groupId = "com.ivianuu.injekt"
    const val vcsUrl = "https://github.com/IVIanuu/injekt"
    const val version = "${Build.versionName}-dev84"
}

object Deps {
    const val androidGradlePlugin = "com.android.tools.build:gradle:3.5.1"

    const val androidxAppCompat = "androidx.appcompat:appcompat:1.1.0"

    const val bintrayGradlePlugin =
        "com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4"

    const val buildConfigGradlePlugin =
        "gradle.plugin.de.fuerstenau:BuildConfigPlugin:1.1.8"

    object Dagger2 {
        private const val version = "2.24"
        const val dagger2 = "com.google.dagger:dagger:$version"
        const val compiler = "com.google.dagger:dagger-compiler:$version"
    }

    const val dagger2Reflect = "com.jakewharton.dagger:dagger-reflect:0.1.0-SNAPSHOT"

    const val guava = "com.google.guava:guava:27.1-android"

    const val guice = "com.google.inject:guice:4.2.2"

    const val injektGradlePlugin = "com.ivianuu.injekt:injekt-gradle-plugin:0.0.1-dev84"

    const val junit = "junit:junit:4.12"

    const val katana = "org.rewedigital.katana:katana-core:1.8.1"

    const val kodein = "org.kodein.di:kodein-di-erased-jvm:6.4.1"

    const val koin = "org.koin:koin-core:2.0.1"

    object Kotlin {
        private const val version = "1.3.61"
        const val compilerEmbeddable = "org.jetbrains.kotlin:kotlin-compiler-embeddable:$version"
        const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"
        const val gradlePluginApi = "org.jetbrains.kotlin:kotlin-gradle-plugin-api:$version"
        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$version"
    }

    const val kotlinStatistics = "org.nield:kotlin-statistics:1.2.1"

    const val mavenGradlePlugin =
        "com.github.dcendents:android-maven-gradle-plugin:2.1"

    const val processingX = "com.ivianuu.processingx:processingx:0.0.1-dev4"

    const val spotlessGradlePlugin = "com.diffplug.spotless:spotless-plugin-gradle:3.26.1"

    object Toothpick {
        private const val version = "3.1.0"
        const val toothpick =
            "com.github.stephanenicolas.toothpick:toothpick-runtime:$version"
        const val compiler =
            "com.github.stephanenicolas.toothpick:toothpick-compiler:$version"
    }

}