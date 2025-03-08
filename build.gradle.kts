/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

import com.vanniktech.maven.publish.*

buildscript {
  dependencies {
    classpath("io.github.ivianuu.injekt:gradle-plugin")
  }
}

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.atomicFu) apply false
  alias(libs.plugins.dokka) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.mavenPublish) apply false
}

allprojects {
  plugins.withId("com.vanniktech.maven.publish") {
    extensions.getByType<MavenPublishBaseExtension>().run {
      publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
      signAllPublications()
    }
  }

  configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
      substitute(module("io.github.ivianuu.injekt:ksp")).using(project(":ksp"))
      substitute(module("io.github.ivianuu.injekt:compiler")).using(project(":compiler"))
      substitute(module("io.github.ivianuu.injekt:core")).using(project(":core"))
      substitute(module("io.github.ivianuu.injekt:common")).using(project(":common"))
    }
  }
}
