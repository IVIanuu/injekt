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

package com.ivianuu.injekt.gradle

import com.android.build.gradle.api.BaseVariant
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile

fun KotlinCompile<*>.setupForInjekt(): Provider<List<SubpluginOption>> {
    val compilation = AbstractKotlinCompile::class.java
        .getDeclaredMethod("getTaskData\$kotlin_gradle_plugin")
        .invoke(this)
        .let { taskData ->
            taskData.javaClass
                .getDeclaredMethod("getCompilation")
                .invoke(taskData) as KotlinCompilation<*>
        }
    val androidVariantData: BaseVariant? =
        (compilation as? KotlinJvmAndroidCompilation)?.androidVariant

    if (this is org.jetbrains.kotlin.gradle.tasks.KotlinCompile) {
        usePreciseJavaTracking = false
    }

    val sourceSetName =
        androidVariantData?.javaClass?.getMethod("getName")?.run {
            isAccessible = true
            invoke(androidVariantData) as String
        } ?: compilation.compilationName

    val cacheDir = project.buildDir.resolve("injekt/cache/$sourceSetName")
        .also { it.mkdirs() }
    val dumpDir = project.buildDir.resolve("injekt/dump/$sourceSetName")
        .also { it.mkdirs() }

    project.afterEvaluate {
        val cleanGeneratedFiles = project.tasks.create(
            "${name}InjektCleanGeneratedFiles", CleanGeneratedFiles::class.java)
        cleanGeneratedFiles.cacheDir = cacheDir
        cleanGeneratedFiles.dumpDir = dumpDir
        cleanGeneratedFiles.srcDirs = if (androidVariantData != null) {
            androidVariantData.sourceSets
                .flatMap { it.javaDirectories }
                .flatMap {
                    it.walkTopDown()
                        .toList()
                }
        } else {
            project.extensions.findByType(SourceSetContainer::class.java)!!
                .findByName(sourceSetName)
                ?.allSource
                ?.toList()
                ?: emptyList()
        }
        dependsOn(cleanGeneratedFiles)
    }

    return project.provider {
        listOf(
            SubpluginOption(
                key = "cacheDir",
                value = cacheDir.absolutePath
            ),
            SubpluginOption(
                key = "dumpDir",
                value = dumpDir.absolutePath
            )
        )
    }
}
