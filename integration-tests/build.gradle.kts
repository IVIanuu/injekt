/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  kotlin("multiplatform")
}

kotlin {
  jvm()

  sourceSets {
    named("jvmTest") {
      dependencies {
        implementation(Deps.AndroidX.Activity.compose)
        implementation(Deps.Compose.runtime)
        implementation(Deps.gradleTestKit)
        implementation(project(":injekt-common"))
        implementation(project(":injekt-core"))
        implementation(project(":test-util"))
        implementation(Deps.junit)
        implementation(Deps.kotestAssertions)
      }
    }
  }
}

tasks.withType<Test> {
  jvmArgs(
    "--add-opens", "java.base/java.lang=ALL-UNNAMED",
    "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED"
  )
}

dependencies {
  kotlinCompilerPluginClasspath(Deps.Compose.compiler)
}
