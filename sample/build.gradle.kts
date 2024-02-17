/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  id("com.android.application")
  kotlin("android")
  id("com.ivianuu.injekt")
}

android {
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions { jvmTarget = "17" }

  defaultConfig {
    namespace = "com.ivianuu.injekt.samples.android"
    applicationId = Build.applicationId
    compileSdk = Build.compileSdk
    minSdk = Build.minSdk
    targetSdk = Build.targetSdk
    versionCode = Build.versionCode
    versionName = Build.versionName
  }
}

dependencies {
  implementation(Deps.AndroidX.Activity.activity)
  implementation(Deps.AndroidX.Activity.compose)
  implementation(project(":common"))
  implementation(Deps.Compose.material)
  kotlinCompilerPluginClasspath(Deps.Compose.compiler)
}
