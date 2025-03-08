/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.compose)
  id("io.github.ivianuu.injekt")
}

dependencies {
  testImplementation(project(":common"))
  testImplementation(project(":compiler"))
  testImplementation(project(":ksp"))

  testImplementation(libs.compose.gradlePlugin)
  testImplementation(libs.compose.runtime)

  testImplementation(libs.ksp.symbolProcessing)
  testImplementation(libs.ksp.api)

  testImplementation(libs.kotlin.compilerEmbeddable)

  testImplementation(libs.kotlinCompileTesting.core)
  testImplementation(libs.kotlinCompileTesting.ksp)

  testImplementation(libs.kotestAssertions)
  testImplementation(libs.junit)
}
