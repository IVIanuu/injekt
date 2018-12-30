/*
 * Copyright 2018 Manuel Wrage
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

import org.jetbrains.kotlin.kapt3.base.Kapt.kapt

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
    id("kotlin-kapt")
}

android {
    compileSdkVersion(Build.compileSdk)

    defaultConfig {
        applicationId = Build.applicationId
        buildToolsVersion = Build.buildToolsVersion
        minSdkVersion(Build.minSdk)
        targetSdkVersion(Build.targetSdk)
        versionCode = Build.versionCode
        versionName = Build.versionName
    }

    androidExtensions {
        isExperimental = true
    }

    kapt { correctErrorTypes = true }
}

dependencies {
    implementation(Deps.androidxAppCompat)
    implementation(project(":injekt"))
    implementation(project(":injekt-android"))
    implementation(project(":injekt-codegen"))
    implementation(project(":injekt-reflect"))
    kapt(project(":injekt-compiler"))
}