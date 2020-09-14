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

class IncrementalHelper(private val cacheFile: File) {

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
    }

    fun deleteDependentFiles(dependency: File) {
        val deleted = mutableListOf<File>()
        cache.remove(dependency)?.forEach {
            if (it.exists()) it.delete()
                .also { _ -> deleted += it }
        }
    }

    fun flush() {
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
    }

}
