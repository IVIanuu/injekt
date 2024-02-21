import org.jetbrains.kotlin.gradle.tasks.*

/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  kotlin("multiplatform")
  id("com.ivianuu.injekt")
}

kotlin {
  /*iosArm64()
  iosSimulatorArm64()
  iosX64()*/

  /*js {
    browser()
  }*/

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
    named("jvmTest") {
      dependencies {
        implementation(Deps.junit)
        implementation(Deps.kotestAssertions)
      }
    }
  }
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
}

plugins.apply("com.vanniktech.maven.publish")
