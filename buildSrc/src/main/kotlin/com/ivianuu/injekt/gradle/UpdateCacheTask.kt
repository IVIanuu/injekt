package com.ivianuu.injekt.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

abstract class UpdateCacheTask : DefaultTask() {

    @InputFiles
    lateinit var srcFiles: FileCollection
    @InputFile
    lateinit var cacheFile: File
    lateinit var kotlinCompile: KotlinCompile

    @OutputFile
    val outputFile = File("ignore")

    @TaskAction
    fun run(inputs: IncrementalTaskInputs) {
        val changed = mutableListOf<File>()
        val deleted = mutableListOf<File>()
        inputs.outOfDate { changed += file }
        inputs.removed { deleted += file }

        project.logger.log(
            LogLevel.WARN,
            "testt: run -> changed: $changed deleted: $deleted"
        )

        val incrementalFileCache = IncrementalFileCache(cacheFile)
        changed.forEach { incrementalFileCache.deleteDependents(it) }
        deleted.forEach { incrementalFileCache.deleteDependents(it) }
        kotlinCompile.exclude { it.file.absolutePath in incrementalFileCache.deletedFiles }
    }

}
