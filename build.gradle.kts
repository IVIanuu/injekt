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

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile

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

buildscript {
    repositories {
        mavenLocal()
        maven("https://dl.bintray.com/ivianuu/maven")
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        google()
        jcenter()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://plugins.gradle.org/m2")
    }
    dependencies {
        classpath(Deps.androidGradlePlugin)
        classpath(Deps.bintrayGradlePlugin)
        classpath(Deps.buildConfigGradlePlugin)
        classpath(Deps.Kotlin.gradlePlugin)
        classpath(Deps.mavenGradlePlugin)
        classpath(Deps.spotlessGradlePlugin)
    }
}

allprojects {
    // todo remove
    configurations.all {
        resolutionStrategy.force("com.squareup:kotlinpoet:1.5.0")
    }

    repositories {
        mavenLocal()
        maven("https://dl.bintray.com/ivianuu/maven")
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        google()
        jcenter()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://androidx.dev/snapshots/builds/${Deps.AndroidX.Compose.snapshot}/artifacts/repository")
        maven("https://plugins.gradle.org/m2")
    }

    // todo move
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        val compilation = AbstractKotlinCompile::class.java
            .getDeclaredMethod("getTaskData\$kotlin_gradle_plugin")
            .invoke(this)
            .let { taskData ->
                taskData.javaClass
                    .getDeclaredMethod("getCompilation")
                    .invoke(taskData) as KotlinCompilation<*>
            }
        val androidVariantData: com.android.build.gradle.api.BaseVariant? =
            (compilation as? org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation)?.androidVariant

        val sourceSetName = androidVariantData?.name ?: compilation.compilationName

        val srcDir = buildDir.resolve("generated/source/injekt/$sourceSetName")
            .also { it.mkdirs() }.absolutePath

        if (androidVariantData != null) {
            project.extensions.findByType(com.android.build.gradle.BaseExtension::class.java)
                ?.sourceSets
                ?.findByName(sourceSetName)
                ?.java
                ?.srcDir(srcDir)
        } else {
            project.extensions.findByType(SourceSetContainer::class.java)
                ?.findByName(sourceSetName)
                ?.java
                ?.srcDir(srcDir)
        }

        kotlinOptions {
            useIR = true
            freeCompilerArgs += listOf(
                "-P", "plugin:com.ivianuu.injekt:srcDir=$srcDir"
            )
        }
    }
}