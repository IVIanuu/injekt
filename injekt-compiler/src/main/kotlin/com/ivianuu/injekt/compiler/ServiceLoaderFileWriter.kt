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

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.io.File

class ServiceLoaderFileWriter(
    private val module: ModuleDescriptor,
    private val outputDir: String
) {

    private val contributors = mutableListOf<String>()

    fun add(contributor: FqName) {
        contributors += contributor.asString()
    }

    fun writeFile() {
        val servicesDir = File(outputDir, "META-INF/services")
        servicesDir.run { if (!exists()) mkdirs() }

        val file = File(servicesDir, InjektClassNames.ComponentBuilderContributor.asString())
        if (!file.exists()) {
            file.createNewFile()
        }

        val newContributors = file.readText().split("\n")
            .filter { it.isNotEmpty() }
            .filter {
                module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName(it))) != null
            }
            .toMutableList()

        contributors
            .filter { it !in newContributors }
            .forEach { newContributors += it }

        if (newContributors.isNotEmpty()) {
            file.writeText(newContributors.joinToString("\n"))
        } else {
            file.delete()
        }
    }

}
