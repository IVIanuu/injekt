/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  id("org.jetbrains.compose")
  kotlin("multiplatform")
}

kotlin {
  jvm()

  sourceSets {
    named("jvmTest") {
      dependencies {
        implementation(Deps.AndroidX.Activity.compose)
        implementation(Deps.Compose.runtime)
        implementation(project(":injekt-common"))
        implementation(project(":injekt-core"))
        implementation(project(":injekt-coroutines"))
        implementation(project(":test-util"))
        implementation(Deps.junit)
        implementation(Deps.kotestAssertions)
      }
    }
  }
}
