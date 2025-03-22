/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.atomicFu)
  id("io.github.ivianuu.injekt")
}

kotlin {
  jvmToolchain(11)

  /*iosArm64()
  iosSimulatorArm64()
  iosX64()*/

  js {
    browser()
  }

  jvm()

  /*linuxX64()

  macosArm64()
  macosX64()

  mingwX64()

  tvosArm64()
  tvosSimulatorArm64()
  tvosX64()

  watchosArm32()
  watchosArm64()
  watchosSimulatorArm64()
  watchosX64()*/

  sourceSets {
    commonMain {
      dependencies {
        api(project(":core"))
      }
    }
    jvmTest {
      dependencies {
        implementation(libs.coroutines.test)
        implementation(libs.junit)
        implementation(libs.kotestAssertions)
      }
    }
  }
}

plugins.apply(libs.plugins.mavenPublish.get().pluginId)
