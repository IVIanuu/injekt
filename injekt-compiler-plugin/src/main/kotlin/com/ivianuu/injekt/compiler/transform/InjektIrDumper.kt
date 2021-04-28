/*
 * Copyright 2021 Manuel Wrage
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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import java.io.*

class InjektIrDumper(private val dumpDir: File) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        dumpDir.deleteRecursively()
        moduleFragment.files
            .asSequence()
            .filter {
                pluginContext.bindingContext[InjektWritableSlices.FILE_HAS_GIVEN_CALLS,
                        it.fileEntry.name] != null
            }
            .forEach { irFile ->
                val file = File(irFile.fileEntry.name)
                val content = try {
                    irFile.dumpKotlinLike(
                        KotlinLikeDumpOptions(
                            useNamedArguments = true,
                            printFakeOverridesStrategy = FakeOverridesStrategy.NONE
                        )
                    )
                } catch (e: Throwable) {
                    e.stackTraceToString()
                }
                val newFile = dumpDir
                    .resolve(irFile.fqName.asString().replace(".", "/"))
                    .also { it.mkdirs() }
                    .resolve(file.name.removeSuffix(".kt"))
                try {
                    newFile.createNewFile()
                    newFile.writeText(content)
                    println("Generated $newFile:\n$content")
                } catch (e: Throwable) {
                    throw RuntimeException("Failed to create file ${newFile.absolutePath}\n$content")
                }
            }
    }
}