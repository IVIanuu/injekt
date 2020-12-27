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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File

// Incremental compilation
// Delete generated code if the originating file was removed
// Regenerate code if the originating file has changed

abstract class CleanGeneratedFiles : DefaultTask() {

    @get:InputFiles
    @get:Optional
    lateinit var cacheDir: File

    @get:InputFiles
    @get:Optional
    lateinit var dumpDir: File

    @get:InputFiles
    @get:Optional
    lateinit var generatedSrcDir: File

    @get:Input
    var isIncremental = true

    @get:InputFiles
    lateinit var srcDirs: List<File>

    @Suppress("unused")
    @get:OutputFile
    @get:Optional
    val someFile = File(project.buildDir, "not-existing-file-because-gradle-needs-an-output")

    private val cacheFile by lazy {
        cacheDir.resolve("file_pairs")
    }

    private val cacheEntries by lazy {
        (if (!cacheFile.exists()) mutableSetOf()
        else cacheFile.readText()
            .split("\n")
            .filter { it.isNotEmpty() }
            .map {
                val tmp = it.split("=:=")
                tmp[0] to tmp[1]
            }
            .toMutableSet())
            .groupBy { it.first }
            .mapValues { it.value.map { it.second } }
            .toMutableMap()
    }

    @TaskAction
    operator fun invoke(inputs: IncrementalTaskInputs) {
        log("clean files: incremental $isIncremental")

        if (!isIncremental) {
            log("clean files: Clear all files because not incremental")
            generatedSrcDir.deleteRecursively()
            dumpDir.deleteRecursively()
            project.buildDir.resolve("classes").deleteRecursively()
            project.buildDir.resolve("kotlin").deleteRecursively()
            return
        }

        val oldCacheEntries = cacheEntries.toMap()
        inputs.outOfDate { details ->
            cacheEntries.remove(details.file.absolutePath)
                ?.onEach {
                    log("clean files: Delete $it because ${details.file} has changed")
                }
                ?.forEach {
                    File(it).delete()
                }
        }
        inputs.removed { details ->
            cacheEntries.remove(details.file.absolutePath)
                ?.onEach {
                    log("clean files: Delete $it because ${details.file} was removed")
                }
                ?.forEach {
                    File(it).delete()
                }
        }

        if (cacheEntries != oldCacheEntries) {
            cacheEntries
                .flatMap { (originatingFile, generatedFiles) ->
                    generatedFiles
                        .map { originatingFile to it }
                }
                .joinToString("\n") { "${it.first}=:=${it.second}" }
                .let {
                    if (!cacheFile.exists()) {
                        cacheFile.parentFile.mkdirs()
                        cacheFile.createNewFile()
                    }
                    cacheFile.writeText(it)
                    log("clean files: Updated cache $it")
                }
        }
    }

}
