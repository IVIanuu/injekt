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

import com.ivianuu.injekt.BeanDefinition
import com.ivianuu.injekt.DefinitionFactory
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName


/**
 * @author Manuel Wrage (IVIanuu)
 */
class FactoryGenerator(private val descriptor: DefinitionDescriptor) {

    fun generate(): FileSpec {
        val file =
            FileSpec.builder(descriptor.factoryName.packageName, descriptor.factoryName.simpleName)

        file.addImport("com.ivianuu.injekt", *imports().toTypedArray())
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
                DefinitionFactory::class.asClassName().plusParameter(descriptor.target)
            )
            .addFunction(
                FunSpec.builder("create")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(BeanDefinition::class.asClassName().plusParameter(descriptor.target))
                    .apply {
                        val constructorStatement =
                            "%T(${descriptor.constructorParams.joinToString(", ") {
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

                        addCode(
                            "return BeanDefinition.create(" +
                                    "%T::class, " +
                                    "${if (descriptor.name != null) "\"${descriptor.name}\"" else "null"}, " +
                                    "BeanDefinition.Kind.${descriptor.kind.name}, " +
                                    "${if (descriptor.scopeName != null) "\"${descriptor.scopeName}\"" else "null"}, " +
                                    "${descriptor.override}, " +
                                    "${descriptor.eager}, " +
                                    "{ params -> $constructorStatement }" +
                                    ")", descriptor.target, descriptor.target
                        )
                    }
                    .build()
            )
            .build()
    }

}