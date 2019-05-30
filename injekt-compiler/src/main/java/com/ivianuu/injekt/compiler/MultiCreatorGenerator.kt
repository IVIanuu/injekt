/*
 * Copyright 2018 Manuel Wrage
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

import com.ivianuu.injekt.MultiCreator
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec

class MultiCreatorGenerator(private val descriptor: MultiCreatorDescriptor) {

    fun generate(): FileSpec {
        return FileSpec.builder("", descriptor.multiCreatorName.canonicalName)
            .addType(
                TypeSpec.classBuilder(descriptor.multiCreatorName)
                    .addSuperinterface(MultiCreator::class)
                    .addFunction(
                        FunSpec.builder("create")
                            .addModifiers(KModifier.OVERRIDE)
                            .addCode("return listOf(")
                            .apply {
                                val creatorNames = descriptor.creatorNames.toList()
                                creatorNames.forEachIndexed { index, className ->
                                    addCode("%T", className)
                                    if (index != creatorNames.lastIndex) {
                                        addCode(", ")
                                    }
                                }
                            }
                            .addCode(")")
                            .addCode("\n")
                            .addCode(".map { it.create() }")
                            .build()
                    )
                    .build()
            )
            .build()
    }

}