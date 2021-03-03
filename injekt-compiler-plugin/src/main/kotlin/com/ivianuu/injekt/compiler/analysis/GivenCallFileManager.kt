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

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.CacheDir

class GivenCallFileManager(cacheDir: CacheDir) {

    private val givenCallsFile = cacheDir.resolve("given_calls_file")

    private val filesWithGivenCalls = if (!givenCallsFile.exists()) mutableSetOf()
    else givenCallsFile.readText()
        .split("\n")
        .filter { it.isNotEmpty() }
        .toMutableSet()

    fun setFileHasGivenCalls(filePath: String, hasGivenCalls: Boolean) {
        if (hasGivenCalls) {
            filesWithGivenCalls += filePath
        } else {
            filesWithGivenCalls -= filePath
        }
    }

    fun flush() {
        filesWithGivenCalls
            .joinToString("\n")
            .let {
                if (!givenCallsFile.exists()) {
                    givenCallsFile.parentFile.mkdirs()
                    givenCallsFile.createNewFile()
                }
                givenCallsFile.writeText(it)
            }
    }

}
