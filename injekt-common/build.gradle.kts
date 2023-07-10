/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  kotlin("multiplatform")
  id("kotlinx-atomicfu")
}

kotlin {
  jvm()
  /*js(IR) {
    nodejs()
    browser()
  }*/

  /*macosX64()
  mingwX64()
  linuxX64()
  linuxArm32Hfp()
  linuxMips32()
  iosArm32()
  iosArm64()
  iosX64()
  watchosArm32()
  watchosArm64()
  watchosX86()
  watchosX64()
  tvosArm64()
  tvosX64()*/

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":injekt-core"))
        api(Deps.Coroutines.core)
      }
    }
    named("jvmTest") {
      dependencies {
        implementation(Deps.Coroutines.test)
        implementation(Deps.junit)
        implementation(Deps.kotestAssertions)
      }
    }

    /*val nativeMain by creating {
      dependsOn(commonMain)
    }

    val macosX64Main by getting {
      dependsOn(nativeMain)
    }

    val mingwX64Main by getting {
      dependsOn(nativeMain)
    }

    val linuxX64Main by getting {
      dependsOn(nativeMain)
    }

    val linuxArm32HfpMain by getting {
      dependsOn(nativeMain)
    }

    val linuxMips32Main by getting {
      dependsOn(nativeMain)
    }

    val iosX64Main by getting {
      dependsOn(nativeMain)
    }

    val iosArm64Main by getting {
      dependsOn(nativeMain)
    }

    val iosArm32Main by getting {
      dependsOn(nativeMain)
    }

    val watchosX86Main by getting {
      dependsOn(nativeMain)
    }

    val watchosArm32Main by getting {
      dependsOn(nativeMain)
    }

    val watchosArm64Main by getting {
      dependsOn(nativeMain)
    }

    val watchosX64Main by getting {
      dependsOn(nativeMain)
    }

    val tvosArm64Main by getting {
      dependsOn(nativeMain)
    }

    val tvosX64Main by getting {
      dependsOn(nativeMain)
    }*/
  }
}

plugins.apply("com.vanniktech.maven.publish")
