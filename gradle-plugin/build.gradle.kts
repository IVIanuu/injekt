/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  alias(libs.plugins.kotlin.jvm)
  id("java-gradle-plugin")
  alias(libs.plugins.ksp)
  alias(libs.plugins.buildConfig)
  alias(libs.plugins.mavenPublish) apply false
}

gradlePlugin {
  plugins {
    create("injekt") {
      id = "io.github.ivianuu.injekt"
      implementationClass = "injekt.gradle.InjektPlugin"
    }
  }
}

buildConfig {
  className("BuildConfig")
  packageName("injekt.gradle")
  buildConfigField("String", "VERSION", "\"${property("VERSION_NAME")}\"")
}

dependencies {
  implementation(libs.autoService.annotations)
  ksp(libs.autoService.ksp)
  compileOnly(gradleApi())
  compileOnly(libs.kotlin.compilerEmbeddable)
  compileOnly(libs.kotlin.gradlePlugin)
  compileOnly(libs.kotlin.gradlePluginApi)
  api(libs.ksp.gradlePlugin)
}

plugins.apply(libs.plugins.mavenPublish.get().pluginId)
