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
