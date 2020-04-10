/*
 * Copyright 2018 Manuel Wrage
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

buildscript {
    // todo remove
    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(0, java.util.concurrent.TimeUnit.SECONDS)
    }

    repositories {
        mavenLocal()
        maven("https://dl.bintray.com/ivianuu/maven")
        google()
        jcenter()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://plugins.gradle.org/m2")
    }
    dependencies {
        classpath(Deps.androidGradlePlugin)
        classpath(Deps.bintrayGradlePlugin)
        classpath(Deps.buildConfigGradlePlugin)
        classpath(Deps.injektGradlePlugin)
        classpath(Deps.Kotlin.gradlePlugin)
        classpath(Deps.mavenGradlePlugin)
        classpath(Deps.spotlessGradlePlugin)
    }
}

allprojects {
    // todo move
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            useIR = true
        }
    }

    // todo remove
    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(0, java.util.concurrent.TimeUnit.SECONDS)
        resolutionStrategy.force("com.squareup:kotlinpoet:1.5.0")
    }

    repositories {
        mavenLocal()
        maven("https://dl.bintray.com/ivianuu/maven")
        google()
        jcenter()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://plugins.gradle.org/m2")
    }
}