/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  kotlin("multiplatform")
}

kotlin {
  jvm()
  sourceSets {
    named("jvmMain") {
      dependencies {
        api(project(":injekt-common"))
      }
    }
  }
}
