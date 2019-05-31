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

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Creator
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter

class CreatorGenerator(private val descriptor: CreatorDescriptor) {

    fun generate(): FileSpec {
        val file =
            FileSpec.builder(descriptor.creatorName.packageName, descriptor.creatorName.simpleName)

        val imports = imports()
        if (imports.isNotEmpty()) {
            file.addImport("com.ivianuu.injekt", *imports().toTypedArray())
        }

        file.addType(creator())

        return file.build()
    }

    private fun imports(): Set<String> {
        val imports = mutableSetOf<String>()

        imports.add("binding")

        if (descriptor.constructorParams.any { it.kind == ParamDescriptor.Kind.VALUE }) {
            imports.add("get")
        }

        if (descriptor.constructorParams.any { it.kind == ParamDescriptor.Kind.LAZY }) {
            imports.add("inject")
        }

        if (descriptor.constructorParams.any { it.kind == ParamDescriptor.Kind.PROVIDER }) {
            imports.add("provider.getProvider")
        }

        return imports
    }

    private fun creator(): TypeSpec {
        return TypeSpec.objectBuilder(descriptor.creatorName)
            .addSuperinterface(
                Creator::class.asClassName().plusParameter(descriptor.target)
            )
            .addFunction(createFunction())
            .build()
    }

    private fun createFunction(): FunSpec {
        return FunSpec.builder("create")
            .addModifiers(KModifier.OVERRIDE)
            .returns(Binding::class.asClassName().plusParameter(descriptor.target))
            .apply {
                addCode("return binding(\n")
                addCode("kind = %T,\n", descriptor.kind.impl)
                descriptor.scope?.let { addCode("scope = %T,\n", it) }
                addCode("definition = { ")
                if (descriptor.constructorParams.any { it.paramIndex != -1 }) {
                    addCode("params -> ")
                }
                addCode("%T(", descriptor.target)

                descriptor.constructorParams.forEachIndexed { i, param ->
                    if (param.paramIndex == -1) {
                        when (param.kind) {
                            ParamDescriptor.Kind.VALUE -> {
                                when {
                                    param.name != null -> {
                                        addCode("get(%T)", param.name)
                                    }
                                    else -> {
                                        addCode("get()")
                                    }
                                }
                            }
                            ParamDescriptor.Kind.LAZY -> {
                                when {
                                    param.name != null -> {
                                        addCode("inject(%T)", param.name)
                                    }
                                    else -> {
                                        addCode("inject()")
                                    }
                                }
                            }
                            ParamDescriptor.Kind.PROVIDER -> {
                                when {
                                    param.name != null -> {
                                        addCode("getProvider(%T)", param.name)
                                    }
                                    else -> {
                                        addCode("getProvider()")
                                    }
                                }
                            }
                        }
                    } else {
                        addCode("params.get(${param.paramIndex})")
                    }

                    if (i != descriptor.constructorParams.lastIndex) {
                        addCode(", ")
                    }
                }

                addCode(")")
                addCode(" }")
                addCode("\n)")
            }
            .build()
    }
}