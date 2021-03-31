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

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

fun KotlinCompilation<*>.setupForInjekt(): Provider<List<SubpluginOption>> {
    (compileKotlinTask as? org.jetbrains.kotlin.gradle.tasks.KotlinCompile)
        ?.usePreciseJavaTracking = false

    val sourceSetName = name

    val project = compileKotlinTask.project

    val cacheDir = project.buildDir.resolve("injekt/cache/$sourceSetName")
        .also { it.mkdirs() }
    val dumpDir = project.buildDir.resolve("injekt/dump/$sourceSetName")
        .also { it.mkdirs() }

    project.afterEvaluate {
        val cleanGeneratedFiles = project.tasks.create(
            "${compileKotlinTask.name}InjektCleanGeneratedFiles", CleanGeneratedFiles::class.java)
        cleanGeneratedFiles.cacheDir = cacheDir
        cleanGeneratedFiles.dumpDir = dumpDir
        cleanGeneratedFiles.srcDirs = allKotlinSourceSets
            .flatMap { it.kotlin.srcDirs }
        compileKotlinTask.dependsOn(cleanGeneratedFiles)
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
