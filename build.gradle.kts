/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

import com.vanniktech.maven.publish.*

buildscript {
  repositories {
    mavenLocal()
    google()
    mavenCentral()
    maven("https://plugins.gradle.org/m2")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://androidx.dev/storage/compose-compiler/repository")
  }
  dependencies {
    classpath(Deps.androidGradlePlugin)
    classpath(Deps.atomicFuGradlePlugin)
    classpath(Deps.Compose.gradlePlugin)
    classpath(Deps.dokkaGradlePlugin)
    classpath(Deps.Injekt.gradlePlugin)
    classpath(Deps.Kotlin.gradlePlugin)
    classpath(Deps.KotlinSerialization.gradlePlugin)
    classpath(Deps.Ksp.gradlePlugin)
    classpath(Deps.mavenPublishGradlePlugin)
  }
}

allprojects {
  repositories {
    mavenLocal()
    google()
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://plugins.gradle.org/m2")
    maven("https://androidx.dev/storage/compose-compiler/repository")
  }

  plugins.withId("com.vanniktech.maven.publish") {
    extensions.getByType<MavenPublishBaseExtension>().run {
      publishToMavenCentral(SonatypeHost.S01)
      //signAllPublications()
    }
  }

  configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
      substitute(module("injekt:ksp:${Deps.Injekt.version}")).using(project(":ksp"))
      substitute(module("injekt:compiler:${Deps.Injekt.version}")).using(project(":compiler"))
      substitute(module("injekt:core:${Deps.Injekt.version}")).using(project(":core"))
      substitute(module("injekt:common:${Deps.Injekt.version}")).using(project(":common"))
    }
  }
}
