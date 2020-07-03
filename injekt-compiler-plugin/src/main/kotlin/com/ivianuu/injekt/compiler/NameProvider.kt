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

import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.name.Name

class NameProvider {

    private val indicesByName = mutableMapOf<String, Int>()

    fun allocate(name: String) {
        check(name !in indicesByName) {
            "Name already exists $name"
        }
    }

    fun allocate(name: Name) {
        allocate(name.asString())
    }

    fun allocateForGroup(group: Name): Name {
        return allocateForGroup(group.asString()).asNameId()
    }

    fun allocateForGroup(group: String): String {
        val index = indicesByName[group]
        indicesByName[group] = (index ?: 0) + 1
        return nameWithoutIllegalChars(
            "${group}${index?.toString().orEmpty()}"
        ).asString()
    }

    fun allocateForType(type: IrType): Name {
        return allocateForGroup(
            type.classifierOrFail.descriptor.name.asString()
                .decapitalize()
        ).asNameId()
    }

}
