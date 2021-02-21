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

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.BulkAwareDocumentListener
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.extensions.LoadingOrder
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.FileIndexFacade
import com.intellij.openapi.startup.StartupActivity
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.Alarm
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import com.ivianuu.injekt.ide.relevantFile
import com.ivianuu.injekt.ide.relevantFiles
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun Application.registerIndexStoreRunner(project: Project) {
    fun projectOpened() {
        ReadAction.nonBlocking {
            val projectFiles = project.relevantFiles()
            IdeIndexStore.refresh(projectFiles)
        }.submit(IdeIndexStore.cacheExecutor)
    }

    runBackgroundableTask("Initialize injekt") { projectOpened() }

    Extensions.getRootArea()
        .getExtensionPoint(StartupActivity.POST_STARTUP_ACTIVITY)
        .registerExtension(StartupActivity {
            projectOpened()
        }, LoadingOrder.FIRST, project)

    val editorQueue =
        MergingUpdateQueue("injekt doc events", 1000, true,
            null, project, null, Alarm.ThreadToUse.POOLED_THREAD)

    /*VirtualFileManager.getInstance().addAsyncFileListener(
        { events ->
            println("prepare events $events")

            val relevantFiles =
                events.filter { vfile -> vfile.isValid && vfile is KotlinFileType }
                    .mapNotNull { vFile -> vFile.file }

            if (relevantFiles.isEmpty()) {
                return@addAsyncFileListener null
            }

            object : AsyncFileListener.ChangeApplier {
                override fun afterVfsChange() {
                    println("after vfs change ${relevantFiles}")
                }
            }
        },
        project
    )*/

    EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : BulkAwareDocumentListener.Simple {
        override fun afterDocumentChange(document: Document) {
            editorQueue.queue(Update.create(document) {
                ReadAction.nonBlocking {
                    FileDocumentManager.getInstance()
                        .getFile(document)
                        // proceed unless
                        ?.takeUnless {
                            it is LightVirtualFile ||
                                    !it.relevantFile() ||
                                    !FileIndexFacade.getInstance(project).isInSourceContent(it)
                        }
                        ?.let { _ ->
                            PsiDocumentManager.getInstance(project)
                                .getPsiFile(document)
                                ?.safeAs<KtFile>()
                                ?.takeIf { it.isPhysical && !it.isCompiled }
                                ?.let { IdeIndexStore.refresh(listOf(it)) }
                        }
                }.expireWith(project).submit(IdeIndexStore.cacheExecutor)
            })
        }
    }, project)
}
