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
import com.ivianuu.injekt.BindingFactory
import com.ivianuu.injekt.LinkedBinding
import com.ivianuu.injekt.Linker
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.UnlinkedBinding
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import kotlin.reflect.KClass

class BindingFactoryGenerator(private val descriptor: BindingFactoryDescriptor) {

    fun generate() =
        FileSpec.builder(descriptor.factoryName.packageName, descriptor.factoryName.simpleName)
            .addType(bindingFactory())
            .build()

    private fun bindingFactory() = TypeSpec.objectBuilder(descriptor.factoryName)
        .apply {
            if (descriptor.isInternal) addModifiers(KModifier.INTERNAL)
        }
        .apply {
            if (descriptor.hasDependencies) {
                superclass(
                    UnlinkedBinding::class.asClassName().plusParameter(descriptor.target)
                )
            } else {
                superclass(
                    LinkedBinding::class.asClassName().plusParameter(descriptor.target)
                )
            }
        }
        .addSuperinterface(
            BindingFactory::class.asClassName().plusParameter(descriptor.target)
        )
        .addProperty(
            PropertySpec.builder(
                "scope",
                KClass::class.asClassName().plusParameter(
                    WildcardTypeName.producerOf(Annotation::class)
                ).copy(nullable = true),
                KModifier.OVERRIDE
            )
                .apply {
                    getter(
                        FunSpec.getterBuilder()
                            .apply {
                                if (descriptor.scope != null) {
                                    addStatement("return %T::class", descriptor.scope)
                                } else {
                                    addStatement("return null")
                                }
                            }
                            .build()
                    )
                }
                .build()
        )
        .addFunction(
            FunSpec.builder("create")
                .addModifiers(KModifier.OVERRIDE)
                .returns(Binding::class.asClassName().plusParameter(descriptor.target))
                .addStatement("return this")
                .build()
        )
        .apply {
            if (descriptor.hasDependencies) {
                addFunction(
                    FunSpec.builder("link")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("linker", Linker::class.asClassName())
                        .returns(
                            LinkedBinding::class.asTypeName().plusParameter(descriptor.target)
                        )
                        .addCode(
                            CodeBlock.builder()
                                .add("return Linked(\n")
                                .apply {
                                    add(
                                        descriptor.constructorParams
                                            .filterIsInstance<ParamDescriptor.Dependency>()
                                            .joinToString(separator = ", ") { "linker.get()" }
                                    )
                                }
                                .add(")\n")
                                .build()
                        )
                        .build()
                )
            } else {
                addFunction(
                    FunSpec.builder("get")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter(
                            "parameters",
                            LambdaTypeName.get(
                                returnType = Parameters::class.asClassName()
                            ).copy(nullable = true)
                        )
                        .returns(descriptor.target)
                        .addCode(createBody())
                        .build()
                )
            }
        }
        .apply {
            if (descriptor.hasDependencies) {
                addType(linkedBinding())
            }
        }
        .build()

    private fun linkedBinding() =
        (if (descriptor.hasDependencies) TypeSpec.classBuilder("Linked")
        else TypeSpec.objectBuilder("Linked"))
            .addModifiers(KModifier.PRIVATE)
            .superclass(LinkedBinding::class.asClassName().plusParameter(descriptor.target))
            .apply {
                primaryConstructor(
                    FunSpec.constructorBuilder()
                        .apply {
                            descriptor.constructorParams
                                .filterIsInstance<ParamDescriptor.Dependency>()
                                .forEach { param ->
                                    addParameter(
                                        param.paramName + "Binding",
                                        LinkedBinding::class.asTypeName().plusParameter(param.paramType)
                                    )
                                }
                        }
                        .build()
                )

                descriptor.constructorParams
                    .filterIsInstance<ParamDescriptor.Dependency>()
                    .forEach { param ->
                        addProperty(
                            PropertySpec.builder(
                                param.paramName + "Binding",
                                LinkedBinding::class.asTypeName().plusParameter(param.paramType),
                                KModifier.PRIVATE
                            )
                                .initializer(param.paramName + "Binding")
                                .build()
                        )
                    }
            }
            .addFunction(
                FunSpec.builder("get")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter(
                        "parameters",
                        LambdaTypeName.get(
                            returnType = Parameters::class.asClassName()
                        ).copy(nullable = true)
                    )
                    .returns(descriptor.target)
                    .addCode(createBody())
                    .build()
            )
            .build()

    private fun createBody() = CodeBlock.builder()
        .apply {
            if (!descriptor.hasDependencies && !descriptor.hasDynamicParams) {
                addStatement("return %T()", descriptor.target)
                return@createBody build()
            }
        }
        .apply {
            if (descriptor.hasDynamicParams) {
                addStatement("val params = parameters?.invoke()")
            }
        }
        .add("return %T(\n", descriptor.target)
        .indent()
        .apply {
            descriptor.constructorParams.forEachIndexed { i, param ->
                when (param) {
                    is ParamDescriptor.Dynamic -> {
                        add("${param.paramName} = params!!.get(${param.index})")
                    }
                    is ParamDescriptor.Dependency -> {
                        add("${param.paramName} = ${param.paramName}Binding()")
                    }
                }

                if (i != descriptor.constructorParams.lastIndex) {
                    add(",\n")
                }
            }
        }
        .unindent()
        .add("\n")
        .add(")")
        .add("\n")
        .build()
}