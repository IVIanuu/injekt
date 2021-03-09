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

    @get:InputFiles
    lateinit var srcDirs: List<File>

    @get:Input
    var incrementalFixEnabled = true

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

    private val givenCallsFile by lazy {
        cacheDir.resolve("given_calls_file")
    }

    private val filesWithGivenCalls by lazy {
        if (!givenCallsFile.exists()) mutableSetOf()
        else givenCallsFile.readText()
            .split("\n")
            .filter { it.isNotEmpty() }
            .toMutableSet()
    }

    @TaskAction
    operator fun invoke(inputs: IncrementalTaskInputs) {
        log("clean files")

        val oldFilesWithGivenCalls = filesWithGivenCalls.toSet()
        val oldCacheEntries = cacheEntries.toMap()
        var hasChanges = false
        inputs.outOfDate { details ->
            hasChanges = true
            cacheEntries.remove(details.file.absolutePath)
                ?.onEach {
                    log("clean files: Delete $it because ${details.file} has changed")
                }
                ?.forEach {
                    File(it).delete()
                    filesWithGivenCalls -= it
                }
        }
        inputs.removed { details ->
            hasChanges = true
            cacheEntries.remove(details.file.absolutePath)
                ?.onEach {
                    log("clean files: Delete $it because ${details.file} was removed")
                }
                ?.forEach {
                    File(it).delete()
                    filesWithGivenCalls -= it
                }
            filesWithGivenCalls -= details.file.absolutePath
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

        if (filesWithGivenCalls != oldFilesWithGivenCalls) {
            filesWithGivenCalls
                .joinToString("\n")
                .let {
                    if (!givenCallsFile.exists()) {
                        givenCallsFile.parentFile.mkdirs()
                        givenCallsFile.createNewFile()
                    }
                    givenCallsFile.writeText(it)
                    log("clean files: Updated files with given calls $it")
                }
        }

        if (incrementalFixEnabled && hasChanges) {
            filesWithGivenCalls
                .map { File(it) }
                .filter { it.exists() }
                .forEach { fileWithGivenCall ->
                    val text = fileWithGivenCall.readText()
                    val newText = if (text.startsWith("// injekt-incremental-fix")) {
                        "// injekt-incremental-fix ${System.currentTimeMillis()} injekt-end\n" + text.split("injekt-end\n")[1]
                    } else {
                        "// injekt-incremental-fix ${System.currentTimeMillis()} injekt-end\n" + text
                    }
                    log("clean files: Force recompilation of $fileWithGivenCall")
                    fileWithGivenCall.writeText(newText)
                }
        } else {
            log("clean files: Do not force recompilation: is enabled: $incrementalFixEnabled, has changes: $hasChanges")
        }
    }

}
