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

import com.ivianuu.injekt.HasScope
import com.ivianuu.injekt.Key
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

class BindingGenerator(private val descriptor: BindingDescriptor) {

    fun generate() =
        FileSpec.builder(descriptor.bindingName.packageName, descriptor.bindingName.simpleName)
            .addType(binding())
            .addImport("com.ivianuu.injekt", "keyOf")
            .build()

    private fun binding() = TypeSpec.objectBuilder(descriptor.bindingName)
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
        .apply {
            if (descriptor.scope != null) {
                addSuperinterface(HasScope::class)
                addProperty(
                    PropertySpec.builder(
                        "scope",
                        KClass::class.asClassName().plusParameter(
                            WildcardTypeName.producerOf(Annotation::class)
                        ),
                        KModifier.OVERRIDE
                    )
                        .apply {
                            getter(
                                FunSpec.getterBuilder()
                                    .addStatement("return %T::class", descriptor.scope)
                                    .build()
                            )
                        }
                        .build()
                )
            }
        }
        .apply {
            if (descriptor.hasDependencies) {
                descriptor.constructorArgs
                    .filterIsInstance<ArgDescriptor.Dependency>()
                    .forEach { param ->
                        addProperty(
                            PropertySpec.builder(
                                "${param.argName}Key",
                                Key::class,
                                KModifier.PRIVATE
                            )
                                .apply {
                                    if (param.qualifierName != null) {
                                        initializer(
                                            "keyOf<%T>(%T)",
                                            param.paramType,
                                            param.qualifierName
                                        )
                                    } else {
                                        initializer("keyOf<%T>()", param.paramType)
                                    }
                                }
                                .build()
                        )
                    }
            }
        }
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
                                .indent()
                                .apply {
                                    descriptor.constructorArgs
                                        .filterIsInstance<ArgDescriptor.Dependency>()
                                        .forEachIndexed { i, param ->
                                            add("${param.argName}Binding = linker.get(${param.argName}Key)")
                                            if (i != descriptor.constructorArgs.lastIndex) {
                                                add(",\n")
                                            }
                                        }
                                }
                                .unindent()
                                .add("\n)\n")
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
                            descriptor.constructorArgs
                                .filterIsInstance<ArgDescriptor.Dependency>()
                                .forEach { param ->
                                    addParameter(
                                        param.argName + "Binding",
                                        LinkedBinding::class.asTypeName().plusParameter(param.paramType)
                                    )
                                }
                        }
                        .build()
                )

                descriptor.constructorArgs
                    .filterIsInstance<ArgDescriptor.Dependency>()
                    .forEach { param ->
                        addProperty(
                            PropertySpec.builder(
                                param.argName + "Binding",
                                LinkedBinding::class.asTypeName().plusParameter(param.paramType),
                                KModifier.PRIVATE
                            )
                                .initializer(param.argName + "Binding")
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
            if (!descriptor.hasDependencies && !descriptor.hasDynamicArgs) {
                addStatement("return %T()", descriptor.target)
                return@createBody build()
            }
        }
        .apply {
            if (descriptor.hasDynamicArgs) {
                addStatement("val params = parameters?.invoke()")
            }
        }
        .add("return %T(\n", descriptor.target)
        .indent()
        .apply {
            descriptor.constructorArgs.forEachIndexed { i, param ->
                when (param) {
                    is ArgDescriptor.Parameter -> {
                        add("${param.argName} = params!!.get(${param.index})")
                    }
                    is ArgDescriptor.Dependency -> {
                        add("${param.argName} = ${param.argName}Binding()")
                    }
                }

                if (i != descriptor.constructorArgs.lastIndex) {
                    add(",\n")
                }
            }
        }
        .unindent()
        .add("\n)\n")
        .build()
}