/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalCompilerApi::class)

package com.ivianuu.injekt.integrationtests

import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test
import java.nio.file.Files

class IncrementalTest {
  @Test fun testLol() {
    val project = Files.createTempDirectory("root").toFile()

    val injektVersion = "0.0.1-dev686"
    val kotlinVersion = "1.7.10"

    project.resolve("build.gradle.kts")
      .also {
        it.parentFile.mkdirs()
        it.createNewFile()
        it.writeText(
          """
              buildscript {
                repositories {
                  mavenLocal()
                  google()
                  mavenCentral()
                }
                dependencies {
                  classpath("com.ivianuu.injekt:injekt-gradle-plugin:$injektVersion")
                  classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                }
              }
              
              allprojects {
                repositories {
                  mavenLocal()
                  google()
                  mavenCentral()
                }
              }
        """
        )
      }

    project.resolve("settings.gradle.kts")
      .also {
        it.parentFile.mkdirs()
        it.createNewFile()
        it.writeText(
          """
              buildCache {
                local {
                  directory = java.nio.file.Files.createTempDirectory("build_cache")
                }
              }
            
              include(":project")
        """
        )
      }

    project.resolve("project/build.gradle.kts")
      .also {
        it.parentFile.mkdirs()
        it.createNewFile()
        it.writeText(
          """
              plugins {
                kotlin("multiplatform")
                id("com.ivianuu.injekt")
              }
              
              kotlin {
                jvm()
              
                sourceSets {
                  val commonMain by getting {
                    dependencies {
                      api("com.ivianuu.injekt:injekt-core:$injektVersion")
                    }
                  }
                }
              }
        """
        )
      }

    var a = project.resolve("project/src/commonMain/kotlin/a/a/a.kt")
      .also {
        it.parentFile.mkdirs()
        it.createNewFile()
        it.writeText(
          """
          package a.a
          
          import com.ivianuu.injekt.Provide
          
          @Provide val something: Int = 0
        """
        )
      }

    val b = project.resolve("project/src/commonMain/kotlin/b/b.kt")
      .also {
        it.parentFile.mkdirs()
        it.createNewFile()
        it.writeText(
          """
          package b
          
          import com.ivianuu.injekt.inject
          import com.ivianuu.injekt.Providers
          
          @Providers("a.a.**")
          fun main() {
            inject<Int>()
          }
        """
        )
      }

    val gradleRunner = GradleRunner.create()
      .withGradleVersion("7.4")
      .withProjectDir(project)
      .forwardOutput()

    // initial

    gradleRunner
      .withArguments("injektKotlinJvm", "--info", "--build-cache")
      .build()
      .let { result ->
        result.task(":project:injektKotlinJvm")
          ?.outcome shouldBe TaskOutcome.SUCCESS
      }

    return

    // no change
    gradleRunner
      .withArguments("compileKotlinJvm", "--info", "--build-cache")
      .build()
      .let { result ->
        result.task(":project:compileKotlinJvm")
          ?.outcome shouldBe TaskOutcome.UP_TO_DATE
      }

    // injectable change

    a = project.resolve("project/src/commonMain/kotlin/a/a/a.kt")
      .also {
        it.parentFile.mkdirs()
        it.createNewFile()
        it.writeText(
          """
          package a.a

          import com.ivianuu.injekt.Provide

          @Provide val something: Int = 0
          
          @Provide val other: String = ""
        """
        )
      }

    gradleRunner
      .withArguments("compileKotlinJvm", "--info", "--build-cache")
      .build()
      .let { result ->
        result.task(":project:compileKotlinJvm")
          ?.outcome shouldBe TaskOutcome.SUCCESS
      }

    // non injectable change

    a = project.resolve("project/src/commonMain/kotlin/a/a/a.kt")
      .also {
        it.parentFile.mkdirs()
        it.createNewFile()
        it.writeText(
          """
          package a.a

          import com.ivianuu.injekt.Provide

          @Provide val something: Int = 0
          
          @Provide val other: String = ""
          
          val nonProvide = ""
        """
        )
      }

    gradleRunner
      .withArguments("compileKotlinJvm", "--info", "--build-cache")
      .build()
      .let { result ->
        result.task(":project:compileKotlinJvm")
          ?.outcome shouldBe TaskOutcome.SUCCESS
      }

    // delete

    a.delete()

    gradleRunner
      .withArguments("compileKotlinJvm", "--info", "--build-cache")
      .buildAndFail()
      .let { result ->
        result.task(":project:compileKotlinJvm")
          ?.outcome shouldBe TaskOutcome.FAILED
      }

    a = project.resolve("project/src/commonMain/kotlin/a/a/a.kt")
      .also {
        it.parentFile.mkdirs()
        it.createNewFile()
        it.writeText(
          """
          package a.a

          import com.ivianuu.injekt.Provide

          @Provide val something: Int = 0
          
          @Provide val other: String = ""
          
          val lol = ""
          """
        )
      }

    gradleRunner
      .withArguments("compileKotlinJvm", "--info", "--build-cache")
      .build()
      .let { result ->
        result.task(":project:compileKotlinJvm")
          ?.outcome shouldBe TaskOutcome.SUCCESS
      }
  }
}
