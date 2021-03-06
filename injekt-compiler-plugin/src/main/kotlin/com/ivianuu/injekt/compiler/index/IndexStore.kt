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

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

interface IndexStore {
    val indices: Set<Index>
}

class CliIndexStore(private val module: ModuleDescriptor) : IndexStore {

    override val indices: Set<Index>
        get() = module.getPackage(InjektFqNames.IndexPackage)
            .memberScope
            .getContributedDescriptors(DescriptorKindFilter.VALUES)
            .filterIsInstance<PropertyDescriptor>()
            .filter { it.hasAnnotation(InjektFqNames.Index) }
            .mapTo(mutableSetOf()) { indexProperty ->
                val annotation = indexProperty.annotations.findAnnotation(InjektFqNames.Index)!!
                val fqName = annotation.allValueArguments["fqName".asNameId()]!!.value as String
                val type = annotation.allValueArguments["type".asNameId()]!!.value as String
                Index(FqName(fqName), type)
            }

}

typealias IndexStoreFactory = (ModuleDescriptor) -> IndexStore

val CliIndexStoreFactory: IndexStoreFactory = { CliIndexStore(it) }
