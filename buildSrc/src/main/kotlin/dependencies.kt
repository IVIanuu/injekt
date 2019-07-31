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
    const val compileSdk = 28
    const val minSdk = 14
    const val minSdkComparison = 28
    const val targetSdk = 28

    const val versionCode = 1
    const val versionName = "0.0.1"
}

object Publishing {
    const val groupId = "com.ivianuu.injekt"
    const val vcsUrl = "https://github.com/IVIanuu/injekt"
    const val version = "${Build.versionName}-dev70"
}

object Versions {
    const val androidGradlePlugin = "3.6.0-alpha05"
    const val androidxAppCompat = "1.1.0-alpha04"
    const val bintray = "1.8.4"
    const val dagger2 = "2.22.1"
    const val dagger2Reflect = "0.1.0-SNAPSHOT"
    const val guava = "27.1-android"
    const val guice = "4.2.2"
    const val junit = "4.12"
    const val kotlin = "1.3.40"
    const val kotlinStatistics = "1.2.1"
    const val katana = "1.6.0"
    const val kodein = "6.1.0"
    const val koin = "2.0.0-rc-2"
    const val mavenGradle = "2.1"
    const val processingX = "0.0.1-dev3"
    const val toothpick = "2.1.0"
}

object Deps {
    const val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.androidGradlePlugin}"

    const val androidxAppCompat = "androidx.appcompat:appcompat:${Versions.androidxAppCompat}"

    const val bintrayGradlePlugin =
        "com.jfrog.bintray.gradle:gradle-bintray-plugin:${Versions.bintray}"

    const val dagger2 = "com.google.dagger:dagger:${Versions.dagger2}"
    const val dagger2Compiler = "com.google.dagger:dagger-compiler:${Versions.dagger2}"
    const val dagger2Reflect = "com.jakewharton.dagger:dagger-reflect:${Versions.dagger2Reflect}"

    const val guava = "com.google.guava:guava:${Versions.guava}"

    const val guice = "com.google.inject:guice:${Versions.guice}"

    const val junit = "junit:junit:${Versions.junit}"

    const val katana = "org.rewedigital.katana:katana-core:${Versions.katana}"

    const val kodein = "org.kodein.di:kodein-di-erased-jvm:${Versions.kodein}"

    const val koin = "org.koin:koin-core:${Versions.koin}"

    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"

    const val kotlinStatistics = "org.nield:kotlin-statistics:${Versions.kotlinStatistics}"

    const val mavenGradlePlugin =
        "com.github.dcendents:android-maven-gradle-plugin:${Versions.mavenGradle}"

    const val processingX = "com.ivianuu.processingx:processingx:${Versions.processingX}"

    const val toothpick =
        "com.github.stephanenicolas.toothpick:toothpick-runtime:${Versions.toothpick}"
    const val toothpickCompiler =
        "com.github.stephanenicolas.toothpick:toothpick-compiler:${Versions.toothpick}"

}