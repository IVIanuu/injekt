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
import com.ivianuu.injekt.compiler.SrcDir
import com.ivianuu.injekt.compiler.log
import org.jetbrains.kotlin.name.FqName
import java.io.File

@Binding(GenerationComponent::class)
class FileManager(
    private val srcDir: SrcDir,
    private val log: log,
) {

    val newFiles = mutableListOf<File>()

    fun generateFile(
        packageFqName: FqName,
        fileName: String,
        code: String,
    ): File {
        val newFile = srcDir
            .resolve(packageFqName.asString().replace(".", "/"))
            .also { it.mkdirs() }
            .resolve(fileName)
            .also { newFiles += it }

        log { "generated file $packageFqName.$fileName $code" }

        return newFile
            .also { it.createNewFile() }
            .also { it.writeText(code) }
    }
}
