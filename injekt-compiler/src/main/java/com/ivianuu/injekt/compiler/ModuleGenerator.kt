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

        if (module.declarations.any { it.kind == DeclarationDescriptor.Kind.FACTORY }) {
            imports.add("factory")
        }

        if (module.declarations.any { it.kind == DeclarationDescriptor.Kind.SINGLE }) {
            imports.add("single")
        }

        if (module.declarations.flatMap { it.constructorParams }.any { it.kind == ParamDescriptor.Kind.VALUE }) {
            imports.add("get")
        }

        if (module.declarations.flatMap { it.constructorParams }.any { it.kind == ParamDescriptor.Kind.LAZY }) {
            imports.add("inject")
        }

        if (module.declarations.flatMap { it.constructorParams }.any { it.kind == ParamDescriptor.Kind.PROVIDER }) {
            imports.add("provider")
        }

        return imports
    }

    private fun module() = PropertySpec.builder(module.moduleName, Module::class)
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
                        "override" to module.override,
                        "createOnStart" to module.createOnStart
                    )

                    addNamed("module(%name:S, %override:L, %createOnStart:L)", args)
                }
                .beginControlFlow(" {")
                .apply { module.declarations.forEach { add(declaration(it)) } }
                .endControlFlow()
                .build()
        )
        .build()

    private fun declaration(declaration: DeclarationDescriptor) = CodeBlock.builder()
        .apply {
            add(
                when (declaration.kind) {
                    DeclarationDescriptor.Kind.FACTORY -> "factory"
                    DeclarationDescriptor.Kind.SINGLE -> "single"
                }
            )

            add("(")

            val declarationArgs = mutableMapOf(
                "name" to declaration.name,
                "override" to declaration.override
            )

            val isSingle = declaration.kind == DeclarationDescriptor.Kind.SINGLE

            if (isSingle) {
                declarationArgs["createOnStart"] = declaration.createOnStart
            }

            val declarationStatement = "%name:S, %override:L" + if (isSingle) {
                ", %createOnStart:L"
            } else {
                ""
            }

            addNamed(declarationStatement, declarationArgs)

            add(")")

            if (declaration.constructorParams.any { it.paramIndex != -1 }) {
                beginControlFlow(" { params ->")
            } else {
                beginControlFlow(" {")
            }

            val constructorStatement = "%T(${declaration.constructorParams.joinToString(", ") {
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
                                it.name != null -> "provider(\"${it.name}\")"
                                else -> "provider()"
                            }
                        }
                    }
                } else {
                    "params.get(${it.paramIndex})"
                }
            }})"

            add(constructorStatement, declaration.target)
            add("\n")
            endControlFlow()
            add("\n")

            if (declaration.secondaryTypes.isNotEmpty()) {
                add(
                    " bind " + declaration.secondaryTypes.joinToString(" bind ") { "%T::class" },
                    *declaration.secondaryTypes.toTypedArray()
                )
            }
        }
        .build()
}