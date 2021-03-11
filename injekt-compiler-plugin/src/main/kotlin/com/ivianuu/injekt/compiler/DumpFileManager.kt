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

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

class DumpFileManager(
    private val dumpDir: DumpDir,
    private val cacheDir: CacheDir
) {
    private val originatingFilePaths = mutableMapOf<File, String>()

    private val cacheFile = cacheDir.resolve("file_pairs")

    val newFiles = mutableListOf<File>()

    private val cacheEntries = if (!cacheFile.exists()) mutableSetOf()
    else cacheFile.readText()
        .split("\n")
        .filter { it.isNotEmpty() }
        .map {
            val tmp = it.split("=:=")
            tmp[0] to tmp[1]
        }
        .toMutableSet()

    fun generateFile(
        packageFqName: FqName,
        fileName: String,
        originatingFile: String?,
        code: String,
    ) {
        val newFile = dumpDir
            .resolve(packageFqName.asString().replace(".", "/"))
            .also { it.mkdirs() }
            .resolve(fileName)
            .also { newFiles += it }
        if (originatingFile != null) {
            originatingFilePaths[newFile] = originatingFile
        }

        try {
            newFile.createNewFile()
            newFile.writeText(code)
            println("Generated $newFile:\n$code")
        } catch (e: Throwable) {
            throw RuntimeException("Failed to create file ${newFile.absolutePath}\n$code")
        }
    }

    fun postGenerate() {
        originatingFilePaths.forEach { (newFile, originatingFilePath) ->
            cacheEntries += originatingFilePath to newFile.absolutePath
        }

        cacheEntries
            .joinToString("\n") { "${it.first}=:=${it.second}" }
            .let {
                if (!cacheFile.exists()) {
                    cacheFile.parentFile.mkdirs()
                    cacheFile.createNewFile()
                }
                cacheFile.writeText(it)
            }
    }

}
