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
import com.squareup.kotlinpoet.PropertySpec

/**
 * @author Manuel Wrage (IVIanuu)
 */
class AutoInjektGenerator(private val module: ModuleDescriptor) {

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

        if (module.declarations.flatMap { it.constructorParams }.any { it.paramIndex == -1 }) {
        }

        if (module.declarations.flatMap { it.constructorParams }.any { it.kind == ParamDescriptor.Kind.VALUE }) {
            imports.add("get")
        }

        if (module.declarations.flatMap { it.constructorParams }.any { it.kind == ParamDescriptor.Kind.LAZY }) {
            imports.add("lazy")
        }

        if (module.declarations.flatMap { it.constructorParams }.any { it.kind == ParamDescriptor.Kind.PROVIDER }) {
            imports.add("provider")
        }

        return imports
    }

    private fun module() = PropertySpec.builder(module.moduleName, Module::class)
        .initializer(
            CodeBlock.builder()
                .add("module(\"${module.moduleName}\", ${module.override}, ${module.createOnStart}) {\n")
                .apply {
                    module.declarations.forEach { add(declaration(it)) }
                }
                .add("}")
                .build()
        )
        .build()

    private fun declaration(declaration: DeclarationDescriptor) = CodeBlock.builder()
        .apply {
            val constructorStatement = "\n%T(\n${declaration.constructorParams.joinToString(",\n") {
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
                                it.name != null -> "lazy(\"${it.name}\")"
                                else -> "lazy()"
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
            }})\n"

            val bindStatement = if (declaration.secondaryTypes.isNotEmpty()) {
                " bind " + declaration.secondaryTypes.joinToString(" bind ") { "%T::class" }
            } else {
                ""
            }

            val funName = when (declaration.kind) {
                DeclarationDescriptor.Kind.FACTORY -> "factory"
                DeclarationDescriptor.Kind.SINGLE -> "single"
            }
            add("$funName(")
            if (declaration.name != null) {
                add("\"${declaration.name}\"")
            } else {
                add("null")
            }
            add(", ${declaration.override}")
            if (declaration.kind == DeclarationDescriptor.Kind.SINGLE) {
                add(", ${declaration.createOnStart}")
            }

            add(") { ")
            if (declaration.constructorParams.any { it.paramIndex != -1 }) {
                add("params -> ")
            }

            add(constructorStatement, declaration.target)
            add(" }")
            add(bindStatement, *declaration.secondaryTypes.toTypedArray())
            add("\n")
            add("\n")

        }
        .build()
}