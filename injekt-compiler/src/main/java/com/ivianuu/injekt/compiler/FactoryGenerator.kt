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

import com.ivianuu.injekt.BindingFactory
import com.ivianuu.injekt.DefinitionContext
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Parameters
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

class FactoryGenerator(private val descriptor: BindingDescriptor) {

    fun generate(): FileSpec {
        val file =
            FileSpec.builder(descriptor.factoryName.packageName, descriptor.factoryName.simpleName)

        val imports = imports()
        if (imports.isNotEmpty()) {
            file.addImport("com.ivianuu.injekt", *imports().toTypedArray())
        }

        file.addType(factory())

        return file.build()
    }

    private fun imports(): Set<String> {
        val imports = mutableSetOf<String>()

        if (descriptor.constructorParams.any { it.kind == ParamDescriptor.Kind.VALUE }) {
            imports.add("get")
        }

        if (descriptor.constructorParams.any { it.kind == ParamDescriptor.Kind.LAZY }) {
            imports.add("inject")
        }

        if (descriptor.constructorParams.any { it.kind == ParamDescriptor.Kind.PROVIDER }) {
            imports.add("getProvider")
        }

        return imports
    }

    private fun factory(): TypeSpec {
        return TypeSpec.classBuilder(descriptor.factoryName)
            .addSuperinterface(
                BindingFactory::class.asClassName().plusParameter(descriptor.target)
            )
            .addProperty(creatorProperty())
            .addProperty(definitionProperty())
            .addProperty(argsProperty())
            .addFunction(createFunction())
            .build()
    }

    private fun creatorProperty() = PropertySpec.builder("creator", descriptor.creator)
        .initializer("%T()", descriptor.creator)
        .build()

    private fun definitionProperty() = PropertySpec.builder(
        "definition", LambdaTypeName.get(
            receiver = DefinitionContext::class.asClassName(),
            returnType = descriptor.target,
            parameters = *arrayOf(ParameterSpec.unnamed(Parameters::class))
        )
    ).initializer(
        " { params -> %T(${descriptor.constructorParams.joinToString(", ") {
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
        }}) } ", descriptor.target
    ).build()

    private fun argsProperty() = PropertySpec.builder(
        "args",
        Map::class.asTypeName()
            .plusParameter(
                String::class.asTypeName()
            )
            .plusParameter(
                Any::class.asTypeName()
            )
    )
        .initializer(
            "mapOf(" +
                    descriptor.args.joinToString(", ") {
                        "\"${it.key}\" to " + when {
                            it.isType -> it.value.toString() + "::class"
                            it.isTypeArray -> {
                                "arrayOf( ${
                                (it.value as List<String>).joinToString(", ")
                                { "$it::class" }
                                })"
                            }
                            it.isStringArray -> {
                                "arrayOf( ${
                                (it.value as List<String>).joinToString(", ")
                                })"
                            }
                            else -> it.value.toString()
                        }
                    } + ")"
        )
        .build()

    private fun createFunction() = FunSpec.builder("create")
        .addModifiers(KModifier.OVERRIDE)
        .returns(Module::class)
        .apply {
            addCode("return creator.create(%T::class, definition, args)", descriptor.target)
        }
        .build()
}