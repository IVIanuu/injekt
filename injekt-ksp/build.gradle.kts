/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  kotlin("jvm")
  id("com.google.devtools.ksp")
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-compiler-args.gradle")

dependencies {
  implementation(project(":injekt-compiler-plugin"))
  implementation(Deps.AutoService.annotations)
  ksp(Deps.AutoService.processor)
  compileOnly(Deps.Kotlin.compilerEmbeddable)
  compileOnly(Deps.Ksp.api)
  compileOnly(Deps.Ksp.symbolProcessing)
}

plugins.apply("com.vanniktech.maven.publish")
