/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  //versionCatalogs { maybeCreate("libs").apply { from(files("../gradle/libs.versions.toml")) } }
  repositories { mavenCentral() }
}
