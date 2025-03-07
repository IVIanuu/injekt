/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  kotlin("jvm")
  id("org.jetbrains.kotlin.plugin.compose") version Deps.Kotlin.version
}

dependencies {
  testImplementation(Deps.Compose.gradlePlugin)
  testImplementation(project(":common"))
  testImplementation(project(":compiler"))
  testImplementation(project(":ksp"))
  testImplementation(Deps.Compose.runtime)

  testImplementation(Deps.Ksp.symbolProcessing)
  testImplementation(Deps.Ksp.api)

  testImplementation(Deps.Kotlin.compilerEmbeddable)
  testImplementation(Deps.KotlinCompileTesting.core)
  testImplementation(Deps.KotlinCompileTesting.ksp)

  testImplementation(Deps.kotestAssertions)
  testImplementation(Deps.junit)
}
