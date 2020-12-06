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

package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.ApplicationComponent
import com.ivianuu.injekt.compiler.CacheDir
import com.ivianuu.injekt.compiler.SrcDir
import com.ivianuu.injekt.compiler.log
import org.jetbrains.kotlin.com.intellij.openapi.vfs.local.CoreLocalFileSystem
import org.jetbrains.kotlin.com.intellij.openapi.vfs.local.CoreLocalVirtualFile
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.psi.SingleRootFileViewProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

@Binding(ApplicationComponent::class)
class FileManager(
    private val srcDir: SrcDir,
    private val cacheDir: CacheDir,
    private val log: log,
    private val psiManager: PsiManager
) {
    private val originatingFilePaths = mutableMapOf<File, String>()

    private val cacheFile = cacheDir.resolve("file_pairs")

    val newFiles = mutableListOf<File>()

    private val additionalFiles = mutableListOf<File>()

    private var compilingFiles = emptyList<KtFile>()

    private val cacheEntries = if (!cacheFile.exists()) mutableSetOf()
    else cacheFile.readText()
        .split("\n")
        .filter { it.isNotEmpty() }
        .map {
            val tmp = it.split("=:=")
            tmp[0] to tmp[1]
        }
        .toMutableSet()

    fun preGenerate(files: List<KtFile>): List<KtFile> {
        val finalFiles = mutableListOf<KtFile>()

        files.forEach { file ->
            if (additionalFiles.any { it.absolutePath == file.virtualFilePath }) {
                finalFiles += file
                return@forEach
            }

            val originatingFilePath = cacheEntries
                .singleOrNull { it.second == file.virtualFilePath }
                ?.first
            if (originatingFilePath == null) {
                finalFiles += file
                return@forEach
            }

            val compilingOriginatingFile = files.singleOrNull {
                it.virtualFilePath == originatingFilePath
            }

            if (compilingOriginatingFile != null) {
                File(file.virtualFilePath).delete()
                cacheEntries.removeAll {
                    it.second == file.virtualFilePath
                }
                return@forEach
            }

            if (!File(originatingFilePath).exists()) {
                File(file.virtualFilePath).delete()
                cacheEntries.removeAll {
                    it.second == file.virtualFilePath
                }
                return@forEach
            }

            finalFiles += file
        }

        compilingFiles = finalFiles
        return finalFiles.distinctBy { it.virtualFilePath }
    }

    fun generateFile(
        packageFqName: FqName,
        fileName: String,
        originatingFile: KtFile?,
        code: String,
        forAdditionalSource: Boolean
    ): KtFile? {
        val newFile = srcDir
            .resolve(packageFqName.asString().replace(".", "/"))
            .also { it.mkdirs() }
            .resolve(fileName)
            .also {
                if (forAdditionalSource) additionalFiles += it
                else newFiles += it
            }
        if (originatingFile != null) {
            originatingFilePaths[newFile] = originatingFile.virtualFilePath
        }

        log { "generated file $packageFqName.$fileName $code" }

        newFile.createNewFile()
        newFile.writeText(code)

        return if (forAdditionalSource) {
            val virtualFile = CoreLocalVirtualFile(CoreLocalFileSystem(), newFile)
            KtFile(
                object : SingleRootFileViewProvider(
                    psiManager,
                    virtualFile
                ) {
                },
                false
            )
        } else null
    }

    fun postGenerate() {
        originatingFilePaths.forEach { (newFile, originatingFilePath) ->
            cacheEntries += originatingFilePath to newFile.absolutePath
        }

        cacheDir.resolve("generated_files")
            .also {
                if (!it.exists()) {
                    it.parentFile.mkdirs()
                    it.createNewFile()
                }
            }
            .writeText(originatingFilePaths.keys.joinToString("\n"))

        cacheDir.resolve("compiling_files")
            .also {
                if (!it.exists()) {
                    it.parentFile.mkdirs()
                    it.createNewFile()
                }
            }
            .writeText(compilingFiles.map { it.virtualFilePath }.joinToString("\n"))

        cacheEntries
            .joinToString("\n") { "${it.first}=:=${it.second}" }
            .let {
                if (!cacheFile.exists()) {
                    cacheFile.parentFile.mkdirs()
                    cacheFile.createNewFile()
                }
                cacheFile.writeText(it)
                log { "Updated cache:\n $it" }
            }
    }

}
