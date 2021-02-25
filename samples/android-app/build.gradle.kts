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
    id("com.android.application")
    kotlin("android")
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/android-build-app.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/android-proguard.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8-android.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-compiler-args.gradle")
////apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-lint.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-source-sets-android.gradle")

injekt {
    generateMergeComponents = true
}

dependencies {
    implementation(Deps.AndroidX.Activity.activity)
    implementation(Deps.AndroidX.Activity.compose)
    implementation(Deps.AndroidX.appCompat)
    implementation(project(":injekt-android"))
    implementation(project(":injekt-android-work"))
    implementation(project(":injekt-core"))
    kotlinCompilerPluginClasspath(project(":injekt-compiler-plugin"))

    implementation(Deps.AndroidX.Compose.runtime)
    kotlinCompilerPluginClasspath(Deps.AndroidX.Compose.compiler)
    implementation(Deps.AndroidX.Compose.material)
}