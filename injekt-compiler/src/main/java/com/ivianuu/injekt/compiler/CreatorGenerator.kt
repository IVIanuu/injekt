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
import com.ivianuu.injekt.Interceptor
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

        descriptor.constructorParams
            .filter { it.mapName != null }
            .forEach {
                when (it.kind) {
                    ParamDescriptor.Kind.VALUE -> imports.add("multibinding.getMap")
                    ParamDescriptor.Kind.LAZY -> imports.add("multibinding.getLazyMap")
                    ParamDescriptor.Kind.PROVIDER -> imports.add("multibinding.getProviderMap")
                }
            }

        descriptor.constructorParams
            .filter { it.setName != null }
            .forEach {
                when (it.kind) {
                    ParamDescriptor.Kind.VALUE -> imports.add("multibinding.getSet")
                    ParamDescriptor.Kind.LAZY -> imports.add("multibinding.getLazySet")
                    ParamDescriptor.Kind.PROVIDER -> imports.add("multibinding.getProviderSet")
                }
            }

        return imports
    }

    private fun creator() = TypeSpec.objectBuilder(descriptor.creatorName)
        .addSuperinterface(
            Creator::class.asClassName().plusParameter(descriptor.target)
        )
        .addFunction(createFunction())
        .build()

    private fun createFunction() = FunSpec.builder("create")
        .addModifiers(KModifier.OVERRIDE)
        .returns(Binding::class.asClassName().plusParameter(descriptor.target))
        .addCode(createBody())
        .build()

    private fun createBody() = CodeBlock.builder()
        .addStatement("val binding = binding(")
        .indent()
        .addStatement("kind = %T,", descriptor.kind)
        .apply {
            descriptor.scope?.let { addStatement("scope = %T,", it) }
        }
        .add("definition = { ")
        .apply {
            if (descriptor.constructorParams.any { it.paramIndex != -1 }) {
                add("params -> ")
            }
        }
        .add("\n")
        .indent()
        .addStatement("%T(", descriptor.target)
        .indent()
        .apply {
            descriptor.constructorParams.forEachIndexed { i, param ->
                when {
                    param.paramIndex != -1 -> {
                        add("${param.paramName} = params.get(${param.paramIndex})")
                    }
                    param.mapName != null -> {
                        val keyword = when (param.kind) {
                            ParamDescriptor.Kind.VALUE -> "getMap"
                            ParamDescriptor.Kind.LAZY -> "getLazyMap"
                            ParamDescriptor.Kind.PROVIDER -> "getProviderMap"
                        }

                        add("${param.paramName} = $keyword(%T)", param.mapName)
                    }
                    param.setName != null -> {
                        val keyword = when (param.kind) {
                            ParamDescriptor.Kind.VALUE -> "getSet"
                            ParamDescriptor.Kind.LAZY -> "getLazySet"
                            ParamDescriptor.Kind.PROVIDER -> "getProviderSet"
                        }

                        add("${param.paramName} = $keyword(%T)", param.setName)
                    }
                    else -> {
                        val keyword = when (param.kind) {
                            ParamDescriptor.Kind.VALUE -> "get"
                            ParamDescriptor.Kind.LAZY -> "inject"
                            ParamDescriptor.Kind.PROVIDER -> "getProvider"
                        }

                        if (param.name != null) {
                            add("${param.paramName} = $keyword(%T)", param.name)
                        } else {
                            add("${param.paramName} = $keyword()")
                        }
                    }
                }

                if (i != descriptor.constructorParams.lastIndex) {
                    addStatement(", ")
                }
            }
        }
        .unindent()
        .add("\n")
        .addStatement(")")
        .unindent()
        .addStatement("}")
        .unindent()
        .addStatement(")")
        .add("\n")
        .apply {
            // todo the cast is dirty
            descriptor.interceptors.forEach {
                addStatement(
                    "(%T as %T<%T>).intercept(binding)",
                    it,
                    Interceptor::class.asClassName(),
                    descriptor.target
                )
            }

            if (descriptor.interceptors.isNotEmpty()) {
                add("\n")
            }
        }
        .addStatement("return binding")
        .build()
}