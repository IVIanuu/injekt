/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.*

plugins {
  kotlin("jvm")
  id("com.github.johnrengelman.shadow")
}

apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-compiler-args.gradle")

dependencies {
  compileOnly(project(":injekt-compiler-plugin")) {
    exclude(group = "org.jetbrains.kotlin")
  }
}

val shadowJar = tasks.getByName<ShadowJar>("shadowJar") {
  configurations = listOf(project.configurations.compileOnly.get())
  archiveClassifier.set("")
  relocate("com.ivianuu.injekt", "com.ivianuu.shaded_injekt")
  mergeServiceFiles()
}

artifacts {
  runtimeOnly(shadowJar)
  archives(shadowJar)
}

plugins.apply("com.vanniktech.maven.publish")
