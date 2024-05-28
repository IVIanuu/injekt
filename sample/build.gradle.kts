/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  id("com.android.application")
  kotlin("android")
  id("org.jetbrains.kotlin.plugin.compose") version Deps.Kotlin.version
  id("com.ivianuu.injekt")
}

android {
  defaultConfig {
    namespace = "com.ivianuu.injekt.samples.android"
    applicationId = Build.applicationId
    compileSdk = Build.compileSdk
    minSdk = Build.minSdk
    targetSdk = Build.targetSdk
    versionCode = Build.versionCode
    versionName = Build.versionName
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions { jvmTarget = "17" }
}

dependencies {
  implementation(Deps.androidxActivityCompose)
  implementation(project(":common"))
  implementation(Deps.Compose.material)
}
