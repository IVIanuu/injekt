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
plugins {
    id("com.android.library")
    kotlin("android")
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/android-build-lib.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8-android.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-compiler-args.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-lint.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-source-sets-android.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/mvn-publish.gradle")

android {
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    api(project(":injekt-core"))
    api(project(":injekt-common"))
    api(project(":injekt-merge"))
    kotlinCompilerPluginClasspath(project(":injekt-compiler-plugin"))
    api(Deps.AndroidX.appCompat)
    api(Deps.AndroidX.Lifecycle.extensions)
    api(Deps.AndroidX.Lifecycle.runtime)
    testImplementation(Deps.AndroidX.Test.core)
    testImplementation(Deps.AndroidX.Test.junit)
    testImplementation(Deps.roboelectric)
}
