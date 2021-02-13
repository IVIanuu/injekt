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

package com.ivianuu.injekt.ide.playground

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotifications
import com.intellij.util.concurrency.AppExecutorUtil
import com.ivianuu.injekt.compiler.CacheDirOption
import com.ivianuu.injekt.compiler.FileManager
import com.ivianuu.injekt.compiler.SrcDirOption
import com.ivianuu.injekt.compiler.generator.Generator
import com.ivianuu.injekt.compiler.generator.GivenFunGenerator
import com.ivianuu.injekt.compiler.generator.IndexGenerator
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import java.io.File

class GeneratorManager(private val application: Application, private val project: Project) {

    val cacheExecutor =
        AppExecutorUtil.createBoundedApplicationPoolExecutor("Injekt worker", 1)

    private val generators: List<Generator> = listOf(IndexGenerator(), GivenFunGenerator())

    private val fileStates = mutableMapOf<String, Int>()

    fun refresh(files: List<KtFile>) {
        ReadAction.nonBlocking {
            try {
                val changedFiles = files
                    .filter {
                        try {
                            val state = fileStates[it.virtualFilePath]
                            state == null || it.text.hashCode() != state
                        } catch (e: Throwable) {
                            Thread.sleep(1000)
                            throw ProcessCanceledException()
                        }
                    }
                changedFiles.forEach {
                    fileStates[it.virtualFilePath] = it.text.hashCode()
                }
                println("refresh files $files: changed $changedFiles")

                val deletedFiles = mutableListOf<File>()
                val fileManagers = changedFiles
                    .mapNotNull { it.module }
                    .distinct()
                    .filter { it.srcDir != null }
                    .associateWith { module ->
                        FileManager(
                            File(module.srcDir!!),
                            File(module.cacheDir!!)
                        ) { deletedFile ->
                            deletedFiles += deletedFile
                        }
                    }

                changedFiles
                    .filter { it.module != null }
                    .groupBy { fileManagers[it.module!!]!! }
                    .forEach { (fileManager, filesForFileManager) ->
                        fileManager.preGenerate(filesForFileManager)
                    }

                if (deletedFiles.isNotEmpty()) {
                    application.invokeLaterOnWriteThread {
                        LocalFileSystem.getInstance()
                            .refreshFiles(deletedFiles.mapNotNull { it.toVirtualFile() })
                    }
                }

                val newFiles = mutableListOf<Pair<File, FileManager>>()
                for (generator in generators) {
                    println("Process $changedFiles with $generator")
                    generator.generate(
                        object : Generator.Context {
                            override fun generateFile(
                                packageFqName: FqName,
                                fileName: String,
                                originatingFile: KtFile,
                                code: String,
                            ) {
                                val fileManager = fileManagers[originatingFile.module!!]!!
                                newFiles += fileManager.generateFile(
                                    packageFqName,
                                    fileName, originatingFile.virtualFilePath, code
                                ) to fileManager

                                println("generated $packageFqName $fileName")
                            }
                        },
                        changedFiles
                    )
                }

                if (newFiles.isNotEmpty()) {
                    newFiles
                        .groupBy { it.second }
                        .forEach { it.key.postGenerate() }
                    application.invokeLaterOnWriteThread {
                        LocalFileSystem.getInstance()
                            .refreshIoFiles(newFiles.map { it.first })
                    }
                }
            } catch (e: Throwable) {
                if (e is ProcessCanceledException) {
                    Thread.sleep(1000)
                    cacheExecutor.submit {
                        refresh(files)
                    }
                } else {
                    e.printStackTrace()
                }
            }
        }
            .submit(cacheExecutor)
    }

}

private val Module.srcDir: String?
    get() = KotlinFacet.get(this)
        ?.configuration?.settings?.compilerArguments?.pluginOptions
        ?.firstOrNull {
            it.startsWith("plugin:com.ivianuu.injekt:${SrcDirOption.optionName}=")
        }?.replace("plugin:com.ivianuu.injekt:${SrcDirOption.optionName}=", "")

private val Module.cacheDir: String?
    get() = KotlinFacet.get(this)
        ?.configuration?.settings?.compilerArguments?.pluginOptions
        ?.firstOrNull {
            it.startsWith("plugin:com.ivianuu.injekt:${CacheDirOption.optionName}=")
        }?.replace("plugin:com.ivianuu.injekt:${CacheDirOption.optionName}=", "")