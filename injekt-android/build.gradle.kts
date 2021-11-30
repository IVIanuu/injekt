/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  id("com.android.library")
  kotlin("android")
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/android-build-lib.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8-android.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-compiler-args.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-source-sets-android.gradle")

android {
  testOptions {
    unitTests.isIncludeAndroidResources = true
  }
}

dependencies {
  api(project(":injekt-coroutines"))
  api(Deps.AndroidX.Activity.activity)
  api(Deps.AndroidX.Lifecycle.runtime)
  testImplementation(Deps.AndroidX.Test.core)
  testImplementation(Deps.AndroidX.Test.junit)
  testImplementation(Deps.kotestAssertions)
  testImplementation(Deps.mockk)
  testImplementation(Deps.roboelectric)
}

plugins.apply("com.vanniktech.maven.publish")

