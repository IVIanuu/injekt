/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.ksp)
}

dependencies {
  implementation(project(":compiler"))
  implementation(libs.autoService.annotations)
  ksp(libs.autoService.ksp)
  compileOnly(libs.kotlin.compilerEmbeddable)
  compileOnly(libs.ksp.api)
}

plugins.apply(libs.plugins.mavenPublish.get().pluginId)
