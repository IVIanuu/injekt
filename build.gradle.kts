/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

import com.vanniktech.maven.publish.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.tasks.*
import java.util.*

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
  pluginManager.withPlugin("java") {
    configure<JavaPluginExtension> {
      toolchain { languageVersion.set(libs.versions.jdk.map(JavaLanguageVersion::of)) }
    }
    tasks.withType<JavaCompile>().configureEach {
      options.release.set(libs.versions.jvmTarget.map(String::toInt))
    }
  }

  plugins.withType<KotlinBasePlugin> {
    project.tasks.withType<KotlinCompilationTask<*>>().configureEach {
      compilerOptions {
        this.freeCompilerArgs.add("-Xcontext-parameters")
        if (this is KotlinJvmCompilerOptions)
          if (project.name != "sample")
            jvmTarget.set(libs.versions.jvmTarget.map(JvmTarget::fromTarget))
      }
    }
  }

  rootProject.file("/gradle/publish.properties").reader().use { reader ->
    Properties().apply { load(reader) }.forEach { key, value ->
      project.extensions.extraProperties.set(key.toString(), value)
    }
  }

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
