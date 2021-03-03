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

import com.android.build.gradle.BaseExtension
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun AbstractKotlinCompile<*>.setupForInjekt(): List<SubpluginOption> {
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

    if (this is KotlinCompile) {
        usePreciseJavaTracking = false
    }

    val sourceSetName =
        androidVariantData?.javaClass?.getMethod("getName")?.run {
            isAccessible = true
            invoke(androidVariantData) as String
        } ?: compilation.compilationName

    val srcDir = project.buildDir.resolve("generated/source/injekt/$sourceSetName")
        .also { it.mkdirs() }
    val cacheDir = project.buildDir.resolve("injekt/cache")
        .also { it.mkdirs() }
    val dumpDir = project.buildDir.resolve("injekt/dump")
        .also { it.mkdirs() }

    project.afterEvaluate {
        val cleanGeneratedFiles = project.tasks.create(
            "${name}InjektCleanGeneratedFiles", CleanGeneratedFiles::class.java)
        cleanGeneratedFiles.cacheDir = cacheDir
        cleanGeneratedFiles.dumpDir = dumpDir
        cleanGeneratedFiles.generatedSrcDir = srcDir
        cleanGeneratedFiles.srcDirs = if (androidVariantData != null) {
            androidVariantData.sourceSets
                .flatMap { it.javaDirectories }
                .flatMap {
                    it.walkTopDown()
                        .onEnter { it != srcDir }
                        .toList()
                }
        } else {
            project.extensions.findByType(SourceSetContainer::class.java)!!
                .findByName(sourceSetName)
                ?.allSource
                ?.filterNot { it.absolutePath.startsWith(srcDir.absolutePath) }
                ?: emptyList()
        }
        dependsOn(cleanGeneratedFiles)

        log("Setup in ${project.name} $name\n" +
                "source set $sourceSetName\n" +
                "cache dir $cacheDir\n" +
                "gen dir $srcDir\n" +
                "src dirs ${cleanGeneratedFiles.srcDirs.joinToString("\n")}\n" +
                "compilation $compilation" +
                "variant data $androidVariantData")
    }

    return listOf(
        SubpluginOption(
            key = "srcDir",
            value = srcDir.absolutePath
        ),
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
