/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  kotlin("multiplatform")
}

kotlin {
  jvm()
  
  sourceSets {
    named("jvmMain") {
      dependencies {
        api(project(":injekt-core"))
        api(project(":injekt-common"))
        api(project(":injekt-compiler-plugin"))

        api(Deps.Compose.compiler)
        api(Deps.Compose.runtime)

        api(Deps.classGraph)

        api(Deps.Coroutines.core)
        api(Deps.Coroutines.test)

        api(Deps.Kotlin.compilerEmbeddable)
        api(Deps.kotlinCompileTesting)

        api(Deps.kotestAssertions)

        api(Deps.junit)
        api(Deps.AndroidX.Test.core)
        api(Deps.AndroidX.Test.junit)
        api(Deps.roboelectric)
      }
    }
    named("jvmTest") {
      dependencies {
        implementation(Deps.junit)
        implementation(Deps.kotestAssertions)
      }
    }
  }
}

dependencies {
  kotlinCompilerPluginClasspath(Deps.Compose.compiler)
}
