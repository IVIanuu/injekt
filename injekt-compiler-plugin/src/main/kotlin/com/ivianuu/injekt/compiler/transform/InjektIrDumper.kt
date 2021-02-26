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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.CacheDir
import com.ivianuu.injekt.compiler.DumpDir
import com.ivianuu.injekt.compiler.FileManager
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.FakeOverridesStrategy
import org.jetbrains.kotlin.ir.util.KotlinLikeDumpOptions
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import java.io.File

class InjektIrDumper(
    private val cacheDir: CacheDir,
    private val dumpDir: DumpDir
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val fileManager = FileManager(dumpDir, cacheDir)
        moduleFragment.files.forEach {
            val file = File(it.fileEntry.name)
            fileManager.generateFile(
                it.fqName,
                file.name.removeSuffix(".kt"),
                file.absolutePath,
                it.dumpKotlinLike(
                    KotlinLikeDumpOptions(
                        useNamedArguments = true,
                        printFakeOverridesStrategy = FakeOverridesStrategy.NONE
                    )
                )
            )
        }
        fileManager.postGenerate()
    }
}