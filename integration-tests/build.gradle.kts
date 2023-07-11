/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  kotlin("multiplatform")
  id("com.ivianuu.injekt")
}

kotlin {
  jvm()

  targets.forEach {
    it.compilations.forEach {
      it.kotlinOptions.freeCompilerArgs += "-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi"
    }
  }

  sourceSets {
    named("jvmTest") {
      dependencies {
        implementation(project(":common"))
        implementation(project(":compiler"))
        implementation(project(":ksp"))

        implementation(Deps.Compose.runtime)

        implementation(Deps.Ksp.api)
        implementation(Deps.Ksp.symbolProcessing)

        implementation(Deps.classGraph)

        implementation(Deps.Kotlin.compilerEmbeddable)
        implementation(Deps.KotlinCompileTesting.kotlinCompileTesting)
        implementation(Deps.KotlinCompileTesting.ksp)

        implementation(Deps.kotestAssertions)

        implementation(Deps.junit)
        implementation(Deps.AndroidX.Test.core)
        implementation(Deps.AndroidX.Test.junit)
        implementation(Deps.roboelectric)
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
