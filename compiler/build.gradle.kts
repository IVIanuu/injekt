/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

dependencies {
  compileOnly(libs.compose.gradlePlugin)
  implementation(libs.autoService.annotations)
  ksp(libs.autoService.ksp)
  compileOnly(libs.kotlin.compilerEmbeddable)
  implementation(libs.kotlinXSerializationJson)
}

plugins.apply(libs.plugins.mavenPublish.get().pluginId)
