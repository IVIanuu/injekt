/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("com.github.johnrengelman.shadow")
}

//apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/java-8.gradle")
apply(from = "https://raw.githubusercontent.com/IVIanuu/gradle-scripts/master/kt-compiler-args.gradle")

val shadowJar = tasks.getByName<ShadowJar>("shadowJar") {
  relocate("org.jetbrains.kotlin.com.intellij", "com.intellij")
  dependencies {
    exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
    exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib-common"))
    exclude(dependency("org.jetbrains:annotations"))

    exclude(dependency("com.intellij:openapi"))
    exclude(dependency("com.intellij:extensions"))
    exclude(dependency("com.intellij:annotations"))
  }
}

artifacts {
  archives(shadowJar)
}

dependencies {
  compileOnly(Deps.Kotlin.compilerEmbeddable)
  compileOnly(Deps.Compose.compiler)
  implementation(Deps.KotlinSerialization.json)
}

plugins.apply("com.vanniktech.maven.publish")
