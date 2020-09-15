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

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.name.FqName
import java.io.File

class ModuleRegistrarManager(
    private val serviceLoaderFile: File
) {

    private val impls = (if (serviceLoaderFile.exists()) serviceLoaderFile.readText() else "")
        .split("\n")
        .filter { it.isNotEmpty() }
        .toMutableSet()

    fun addImpl(impl: FqName) {
        impls += impl.asString()
    }

    fun removeImpl(impl: FqName) {
        impls -= impl.asString()
    }

    fun flush() {
        if (impls.isNotEmpty()) {
            serviceLoaderFile.parentFile.mkdirs()
            serviceLoaderFile.createNewFile()
            serviceLoaderFile.writeText(
                impls.joinToString("\n")
            )
        } else {
            serviceLoaderFile.delete()
        }
    }

}
