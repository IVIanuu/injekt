package com.ivianuu.injekt.compiler

import java.io.File

class KeyValueFileCache<T>(
    private val cacheFile: File,
    private val fromString: (String) -> T,
    private val toString: (T) -> String,
    private val onDelete: (T) -> Unit
) {

    private val cache = (if (cacheFile.exists()) cacheFile.readText() else "")
        .split("\n")
        .filter { it.isNotEmpty() }
        .map { entry ->
            val tmp = entry.split("=:=")
            fromString(tmp[0]) to fromString(tmp[1])
        }
        .groupBy { it.first }
        .mapValues {
            it.value
                .map { it.second }
                .toMutableSet()
        }
        .toMutableMap()

    fun recordDependency(
        dependent: T,
        dependency: T
    ) {
        cache.getOrPut(dependency) { mutableSetOf() } += dependent
    }

    fun deleteDependents(dependency: T) {
        cache.remove(dependency)?.forEach(onDelete)
    }

    fun flush() {
        if (cache.isNotEmpty()) {
            cacheFile.parentFile.mkdirs()
            cacheFile.createNewFile()
            cacheFile.writeText(
                buildString {
                    cache
                        .forEach { (dependency, dependents) ->
                            dependents.forEach { dependent ->
                                appendLine("${toString(dependency)}=:=${toString(dependent)}")
                            }
                        }
                }
            )
        } else {
            cacheFile.delete()
        }
    }

}
