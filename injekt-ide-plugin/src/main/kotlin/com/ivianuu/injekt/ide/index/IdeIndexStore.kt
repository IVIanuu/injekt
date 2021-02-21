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

package com.ivianuu.injekt.ide.index

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.util.concurrency.AppExecutorUtil
import com.ivianuu.injekt.compiler.analysis.Index
import com.ivianuu.injekt.compiler.index.IndexStore
import com.ivianuu.injekt.compiler.index.IndexStoreFactory
import com.ivianuu.injekt.compiler.index.collectIndices
import org.jetbrains.kotlin.psi.KtFile

object IdeIndexStore : IndexStore {

    private val indicesByFile = mutableMapOf<String, List<Index>>()
    override val indices: List<Index>
        get() = indicesByFile.values.flatten()

    val cacheExecutor =
        AppExecutorUtil.createBoundedApplicationPoolExecutor("Injekt index worker", 1)

    fun refresh(files: List<KtFile>) {
        ReadAction.nonBlocking {
            try {
                println("refresh indices for files $files")

                for (file in files) {
                    println("Process $file")
                    indicesByFile[file.virtualFilePath] = file.collectIndices()
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

val IdeIndexStoreFactory: IndexStoreFactory = { IdeIndexStore }
