/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  kotlin("jvm")
  id("com.ivianuu.injekt")
}

dependencies {
  ksp(project(":ksp"))

  testImplementation(project(":common"))
  testImplementation(project(":compiler"))
  testImplementation(project(":ksp"))
  testImplementation(Deps.Compose.runtime)

  /*testImplementation(Deps.Ksp.aaEmbeddable) {
    exclude(group = "com.google.devtools.ksp", module = "common-deps")
  }
  testImplementation(Deps.Ksp.commonDeps)
  testImplementation(Deps.Ksp.cmdline)*/
  testImplementation(Deps.Ksp.symbolProcessing)
  testImplementation(Deps.Ksp.api)

  testImplementation(Deps.Kotlin.compilerEmbeddable)
  testImplementation(Deps.KotlinCompileTesting.core)
  testImplementation(Deps.KotlinCompileTesting.ksp)

  testImplementation(Deps.kotestAssertions)
  testImplementation(Deps.junit)
}

tasks.withType<Test> {
  jvmArgs(
    "--add-opens", "java.base/java.lang=ALL-UNNAMED",
    "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED"
  )
}
