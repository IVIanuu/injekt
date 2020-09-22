package com.ivianuu.injekt.compiler

import java.io.File

class IncrementalFileCache(private val cacheFile: File) {

    val deletedFiles = mutableSetOf<String>()

    private val cache = (if (cacheFile.exists()) cacheFile.readText() else "")
        .split("\n")
        .filter { it.isNotEmpty() }
        .map { entry ->
            val tmp = entry.split("=:=")
            File(tmp[0]) to File(tmp[1])
        }
        .groupBy { it.first }
        .mapValues {
            it.value
                .map { it.second }
                .toMutableSet()
        }
        .toMutableMap()

    fun recordDependency(
        dependent: File,
        dependency: File
    ) {
        cache.getOrPut(dependency) { mutableSetOf() } += dependent
        println("$dependency record dependent $dependent")
    }

    fun deleteDependents(dependency: File) {
        cache.remove(dependency)?.forEach {
            deleteDependents(it)
            it.delete()
            println("$dependency delete dependents $it")
            deletedFiles += it.absolutePath
        }
    }

    fun flush() {
        deletedFiles.clear()
        if (cache.isNotEmpty()) {
            cacheFile.parentFile.mkdirs()
            cacheFile.createNewFile()
            cacheFile.writeText(
                buildString {
                    cache
                        .forEach { (dependency, dependents) ->
                            dependents.forEach { dependent ->
                                appendLine("${dependency.absolutePath}=:=${dependent.absolutePath}")
                            }
                        }
                }
            )
        } else {
            cacheFile.delete()
        }
    }

}
