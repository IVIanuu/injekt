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
import com.squareup.kotlinpoet.FunSpec

/**
 * @author Manuel Wrage (IVIanuu)
 */
class AutoInjektGenerator(private val module: ModuleDescriptor) {

    fun generate(): FileSpec {
        val file = FileSpec.builder(module.packageName, module.fileName)

        file.addImport("com.ivianuu.injekt", *imports().toTypedArray())

        file.addFunction(module())

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
            imports.add("get")
        }

        return imports
    }

    private fun module() = FunSpec.builder(module.fileName)
        .returns(Module::class)
        .addCode("return module {\n")
        .apply {
            module.declarations.forEach { addCode(declaration(it)) }
        }
        .addCode("}")
        .build()

    private fun declaration(declaration: DeclarationDescriptor) = CodeBlock.builder()
        .apply {
            val constructorStatement = "\n%T(\n${declaration.constructorParams.joinToString(",\n") {
                "${it.name} = " + when {
                    it.getName != null -> "get(name = \"${it.getName}\")"
                    it.paramIndex != -1 -> "params.get(${it.paramIndex})"
                    else -> "get()"
                }
            }})\n"

            val bindStatement = if (declaration.secondaryTypes.isNotEmpty()) {
                " bind " + declaration.secondaryTypes.joinToString(" bind ") { "%T::class" }
            } else {
                ""
            }

            val setBindingsStatement = if (declaration.setBindings.isNotEmpty()) {
                "intoSet " + declaration.setBindings.joinToString(" intoSet ") { it }
            } else {
                ""
            }

            val mapBindingsStatement = if (declaration.setBindings.isNotEmpty()) {
                "intoMap " + declaration.setBindings.joinToString(" intoMap ") { it }
            } else {
                ""
            }

            val funName = when (declaration.kind) {
                DeclarationDescriptor.Kind.FACTORY -> "factory"
                DeclarationDescriptor.Kind.SINGLE -> "single"
            }
            add("$funName(override = ${declaration.override}")
            if (declaration.kind == DeclarationDescriptor.Kind.SINGLE) {
                add(", createOnStart = ${declaration.createOnStart}")
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