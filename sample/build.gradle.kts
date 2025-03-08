/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  id("io.github.ivianuu.injekt")
}

android {
  defaultConfig {
    namespace = "injekt.samples.android"
    applicationId = "injekt.samples.android"
    compileSdk = 35
    minSdk = 27
    targetSdk = 35
    versionCode = 1
    versionName = "0.0.1"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions { jvmTarget = "17" }
}

dependencies {
  implementation(project(":common"))
  implementation(libs.androidxActivityCompose)
  implementation(libs.compose.material)
}
