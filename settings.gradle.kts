/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
  }
}

include(
  ":common",
  ":compiler",
  ":core",
  ":ksp",
  ":integration-tests",
  ":sample"
)

includeBuild("gradle-plugin") {
  dependencySubstitution {
    substitute(module("io.github.ivianuu.injekt:gradle-plugin")).using(project(":"))
  }
}
