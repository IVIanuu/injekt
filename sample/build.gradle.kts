/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  id("com.android.application")
  kotlin("android")
  id("com.ivianuu.injekt")
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/android-build-app.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8-android.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-source-sets-android.gradle")

dependencies {
  implementation(Deps.AndroidX.Activity.activity)
  implementation(Deps.AndroidX.Activity.compose)
  implementation(project(":common"))
  implementation(Deps.Compose.material)
  kotlinCompilerPluginClasspath(Deps.Compose.compiler)
}