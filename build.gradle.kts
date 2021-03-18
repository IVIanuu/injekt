/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.ivianuu.injekt.gradle.setupForInjekt

buildscript {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        jcenter()
        maven("https://plugins.gradle.org/m2")
    }
    dependencies {
        classpath(Deps.androidGradlePlugin)
        classpath(Deps.buildConfigGradlePlugin)
        classpath(Deps.dokkaGradlePlugin)
        classpath(Deps.Injekt.gradlePlugin)
        classpath(Deps.Kotlin.gradlePlugin)
        classpath(Deps.mavenPublishGradlePlugin)
        classpath(Deps.shadowGradlePlugin)
    }
}

allprojects {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        jcenter()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://plugins.gradle.org/m2")
    }

    afterEvaluate {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile<*>> {
            val kotlinOptions = when (this) {
                is org.jetbrains.kotlin.gradle.tasks.KotlinCompile -> kotlinOptions
                is org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon -> kotlinOptions
                else -> null
            } ?: return@withType
            kotlinOptions.run {
                if (this is org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions)
                    useIR = true
                if (project.name != "injekt-compiler-plugin" &&
                        project.name != "injekt-gradle-plugin") {
                    val pluginOptions = (this@withType as org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>)
                        .setupForInjekt().get()
                    pluginOptions.forEach {
                        freeCompilerArgs += listOf(
                            "-P", "plugin:com.ivianuu.injekt:${it.key}=${it.value}"
                        )
                    }
                }
                if (configurations.findByName("kotlinCompilerPluginClasspath")
                                ?.dependencies
                                ?.any { it.group == "androidx.compose.compiler" } == true) {
                    freeCompilerArgs += listOf(
                            "-P", "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true"
                    )
                }
            }
        }
    }
}