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

import com.ivianuu.injekt.Module
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec

/**
 * @author Manuel Wrage (IVIanuu)
 */
class ModuleGenerator(private val module: ModuleDescriptor) {

    fun generate(): FileSpec {
        val file = FileSpec.builder(module.packageName, module.moduleName)

        file.addImport("com.ivianuu.injekt", *imports().toTypedArray())

        file.addProperty(module())

        return file.build()
    }

    private fun imports(): Set<String> {
        val imports = mutableSetOf<String>()

        imports.add("module")

        if (module.definitions.any { it.kind == DefinitionDescriptor.Kind.FACTORY }) {
            imports.add("factory")
        }

        if (module.definitions.any { it.kind == DefinitionDescriptor.Kind.SINGLE }) {
            imports.add("single")
        }

        if (module.definitions.flatMap { it.constructorParams }.any { it.kind == ParamDescriptor.Kind.VALUE }) {
            imports.add("get")
        }

        if (module.definitions.flatMap { it.constructorParams }.any { it.kind == ParamDescriptor.Kind.LAZY }) {
            imports.add("inject")
        }

        if (module.definitions.flatMap { it.constructorParams }.any { it.kind == ParamDescriptor.Kind.PROVIDER }) {
            imports.add("getProvider")
        }

        return imports
    }

    private fun module(): PropertySpec = PropertySpec.builder(module.moduleName, Module::class)
        .apply {
            if (module.internal) {
                addModifiers(KModifier.INTERNAL)
            }
        }
        .initializer(
            CodeBlock.builder()
                .apply {
                    val args = mapOf(
                        "name" to module.moduleName,
                        "scopeId" to module.scopeId,
                        "override" to module.override,
                        "eager" to module.eager
                    )

                    addNamed("module(%name:S, %scopeId:S, %override:L, %eager:L)", args)
                }
                .beginControlFlow(" {")
                .apply { module.definitions.forEach { add(definition(it)) } }
                .endControlFlow()
                .build()
        )
        .build()

    private fun definition(definition: DefinitionDescriptor) = CodeBlock.builder()
        .apply {
            add(
                when (definition.kind) {
                    DefinitionDescriptor.Kind.FACTORY -> "factory"
                    DefinitionDescriptor.Kind.SINGLE -> "single"
                }
            )

            add("(")

            val definitionArgs = mutableMapOf(
                "name" to definition.name,
                "scopeId" to definition.scope,
                "override" to definition.override
            )

            val isSingle = definition.kind == DefinitionDescriptor.Kind.SINGLE

            if (isSingle) {
                definitionArgs["eager"] = definition.eager
            }

            val definitionStatement = "%name:S, %scopeId:S, %override:L" + if (isSingle) {
                ", %eager:L"
            } else {
                ""
            }

            addNamed(definitionStatement, definitionArgs)

            add(")")

            if (definition.constructorParams.any { it.paramIndex != -1 }) {
                beginControlFlow(" { params ->")
            } else {
                beginControlFlow(" {")
            }

            val constructorStatement = "%T(${definition.constructorParams.joinToString(", ") {
                if (it.paramIndex == -1) {
                    when (it.kind) {
                        ParamDescriptor.Kind.VALUE -> {
                            when {
                                it.name != null -> "get(\"${it.name}\")"
                                else -> "get()"
                            }
                        }
                        ParamDescriptor.Kind.LAZY -> {
                            when {
                                it.name != null -> "inject(\"${it.name}\")"
                                else -> "inject()"
                            }
                        }
                        ParamDescriptor.Kind.PROVIDER -> {
                            when {
                                it.name != null -> "getProvider(\"${it.name}\")"
                                else -> "getProvider()"
                            }
                        }
                    }
                } else {
                    "params.get(${it.paramIndex})"
                }
            }})"

            add(constructorStatement, definition.target)
            add("\n")
            endControlFlow()
            add("\n")

            if (definition.secondaryTypes.isNotEmpty()) {
                add(
                    " bind " + definition.secondaryTypes.joinToString(" bind ") { "%T::class" },
                    *definition.secondaryTypes.toTypedArray()
                )
            }
        }
        .build()
}