/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("com.google.devtools.ksp")
}

dependencies {
  implementation(Deps.AutoService.annotations)
  ksp(Deps.AutoService.processor)
  compileOnly(Deps.Kotlin.compilerEmbeddable)
  implementation(Deps.KotlinSerialization.json)
}

plugins.apply("com.vanniktech.maven.publish")
