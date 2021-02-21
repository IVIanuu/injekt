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

package com.ivianuu.injekt.compiler.index

import com.ivianuu.injekt.compiler.FileManager
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.UniqueNameProvider
import com.ivianuu.injekt.compiler.asNameId
import org.jetbrains.kotlin.psi.KtFile

class IndexGenerator(private val fileManager: FileManager) {

    fun generate(files: List<KtFile>) {
        files.forEach { file ->
            val indices = file.collectIndices()

            if (indices.isEmpty()) return@forEach

            val fileName = file.packageFqName.pathSegments().joinToString("_") +
                    "_${file.name.removeSuffix(".kt")}Indices.kt"
            val nameProvider = UniqueNameProvider()
            fileManager.generateFile(
                originatingFile = file.virtualFilePath,
                packageFqName = InjektFqNames.IndexPackage,
                fileName = fileName,
                code = buildString {
                    appendLine("// injekt_${file.virtualFilePath}")
                    appendLine("package ${InjektFqNames.IndexPackage}")
                    appendLine("import ${InjektFqNames.Index}")
                    indices
                        .distinct()
                        .forEach { index ->
                            val indexName = nameProvider(
                                index.fqName.pathSegments().joinToString("_") + "${file.name}_index"
                            ).asNameId()
                            appendLine("@Index(fqName = \"${index.fqName}\", type = \"${index.type}\")")
                            appendLine("internal val $indexName = Unit")
                        }
                }
            )
        }
    }

}
