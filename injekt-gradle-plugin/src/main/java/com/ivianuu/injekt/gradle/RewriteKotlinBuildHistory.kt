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

package com.ivianuu.injekt.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.incremental.BuildDifference
import org.jetbrains.kotlin.incremental.BuildDiffsStorage
import org.jetbrains.kotlin.incremental.DirtyData
import org.jetbrains.kotlin.incremental.LookupSymbol
import java.io.File

abstract class RewriteKotlinBuildHistory : DefaultTask() {

    @get:InputFiles
    @get:Optional
    lateinit var buildHistoryFile: File

    @TaskAction
    operator fun invoke() {
        val existingBuildHistory = BuildDiffsStorage.readFromFile(
            buildHistoryFile, null
        )
        if (existingBuildHistory == null) {
            println("no build history found")
            return
        }

        val newBuildHistory = existingBuildHistory.copy(
            buildDiffs = existingBuildHistory.buildDiffs + BuildDifference(
                ts = 0L,
                dirtyData = DirtyData(
                    dirtyLookupSymbols = listOf(
                        LookupSymbol(
                            name = "trigger",
                            scope = "com.ivianuu.injekt.setinvalidation"
                        )
                    )
                ),
                isIncremental = true
            )
        )

        BuildDiffsStorage.writeToFile(buildHistoryFile, newBuildHistory, null)

        log("rewrote build history old $existingBuildHistory\n\nnew $newBuildHistory")
    }

}
