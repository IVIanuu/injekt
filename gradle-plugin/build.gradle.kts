/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  kotlin("jvm")
  id("com.github.gmazzo.buildconfig") version "3.0.2"
}

buildConfig {
  className("BuildConfig")
  packageName("com.ivianuu.injekt.gradle")
  buildConfigField("String", "VERSION", "\"${property("VERSION_NAME")}\"")
}

dependencies {
  compileOnly(gradleApi())
  compileOnly(Deps.Kotlin.compilerEmbeddable)
  compileOnly(Deps.Kotlin.gradlePlugin)
  compileOnly(Deps.Kotlin.gradlePluginApi)
}

plugins.apply("com.vanniktech.maven.publish")
