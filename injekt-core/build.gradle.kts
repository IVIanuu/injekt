/*
 * Copyright 2021 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
  kotlin("multiplatform")
}

kotlin {
  jvm()
  js {
    nodejs()
    browser()
  }

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
    named("jvmTest") {
      dependencies {
        implementation(Deps.junit)
        implementation(Deps.kotestAssertions)
      }
    }
    /*val commonMain by getting

    val nativeMain by creating {
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
