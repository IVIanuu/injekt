/*
 * Copyright 2019 Manuel Wrage
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

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

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
            if (descriptor.hasDependencyArgs) {
                superclass(
                    InjektClassNames.UnlinkedBinding.plusParameter(descriptor.target)
                )
            } else {
                superclass(
                    InjektClassNames.LinkedBinding.plusParameter(descriptor.target)
                )
            }
        }
        .apply {
            if (descriptor.scope != null) {
                addSuperinterface(InjektClassNames.HasScope)
                addProperty(
                    PropertySpec.builder(
                        "scope",
                        Any::class,
                        KModifier.OVERRIDE
                    )
                        .apply {
                            getter(
                                FunSpec.getterBuilder()
                                    .addStatement("return %T", descriptor.scope)
                                    .build()
                            )
                        }
                        .build()
                )
            }
        }
        .apply {
            if (descriptor.isSingle) {
                addSuperinterface(InjektClassNames.IsSingle)
            }
        }
        .apply {
            if (descriptor.hasDependencyArgs) {
                descriptor.constructorArgs
                    .filterIsInstance<ArgDescriptor.Dependency>()
                    .forEach { param ->
                        addProperty(
                            PropertySpec.builder(
                                "${param.argName}Key",
                                InjektClassNames.Key,
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
            if (descriptor.hasDependencyArgs) {
                addFunction(
                    FunSpec.builder("link")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("linker", InjektClassNames.Linker)
                        .returns(InjektClassNames.LinkedBinding.plusParameter(descriptor.target))
                        .addCode(
                            CodeBlock.builder()
                                .add("return Linked(\n")
                                .indent()
                                .apply {
                                    val dependencyArgs = descriptor.constructorArgs
                                        .filterIsInstance<ArgDescriptor.Dependency>()

                                    dependencyArgs.forEachIndexed { i, param ->
                                        if (param.isOptional) {
                                            add("${param.argName}Binding = linker.getOrNull(${param.argName}Key)")
                                        } else {
                                            add("${param.argName}Binding = linker.get(${param.argName}Key)")
                                        }
                                        if (i != dependencyArgs.lastIndex) {
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
                    FunSpec.builder("invoke")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter(
                            "parameters",
                            LambdaTypeName.get(
                                returnType = InjektClassNames.Parameters
                            ).copy(nullable = true)
                        )
                        .returns(descriptor.target)
                        .apply {
                            if (descriptor.isObject) {
                                addStatement("return %T", descriptor.target)
                            } else {
                                addCode(createBody())
                            }
                        }
                        .build()
                )
            }
        }
        .apply {
            if (descriptor.hasDependencyArgs) {
                addType(linkedBinding())
            }
        }
        .build()

    private fun linkedBinding() =
        (if (descriptor.hasDependencyArgs) TypeSpec.classBuilder("Linked")
        else TypeSpec.objectBuilder("Linked"))
            .addModifiers(KModifier.PRIVATE)
            .superclass(InjektClassNames.LinkedBinding.plusParameter(descriptor.target))
            .apply {
                primaryConstructor(
                    FunSpec.constructorBuilder()
                        .apply {
                            descriptor.constructorArgs
                                .filterIsInstance<ArgDescriptor.Dependency>()
                                .forEach { param ->
                                    addParameter(
                                        param.argName + "Binding",
                                        InjektClassNames.LinkedBinding.plusParameter(param.paramType)
                                            .copy(nullable = param.isOptional)
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
                                InjektClassNames.LinkedBinding.plusParameter(param.paramType)
                                    .copy(nullable = param.isOptional),
                                KModifier.PRIVATE
                            )
                                .initializer(param.argName + "Binding")
                                .build()
                        )
                    }
            }
            .addFunction(
                FunSpec.builder("invoke")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter(
                        "parameters",
                        LambdaTypeName.get(
                            returnType = InjektClassNames.Parameters
                        ).copy(nullable = true)
                    )
                    .returns(descriptor.target)
                    .addCode(createBody())
                    .build()
            )
            .build()

    private fun createBody() = CodeBlock.builder()
        .apply {
            if (!descriptor.hasDependencyArgs && !descriptor.hasParamArgs) {
                addStatement("return %T()", descriptor.target)
                return@createBody build()
            }
        }
        .apply {
            if (descriptor.hasParamArgs) {
                addStatement("val params = parameters?.invoke() ?: error(\"Missing required parameters\")")
            }
        }
        .add("return %T(\n", descriptor.target)
        .indent()
        .apply {
            descriptor.constructorArgs.forEachIndexed { i, param ->
                when (param) {
                    is ArgDescriptor.Parameter -> {
                        add("${param.argName} = params.get(${param.index})")
                    }
                    is ArgDescriptor.Dependency -> {
                        if (param.isOptional) {
                            add("${param.argName} = ${param.argName}Binding?.invoke()")
                        } else {
                            add("${param.argName} = ${param.argName}Binding.invoke()")
                        }
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

private object InjektClassNames {
    val HasScope = ClassName("com.ivianuu.injekt", "HasScope")
    val IsSingle = ClassName("com.ivianuu.injekt", "IsSingle")
    val Key = ClassName("com.ivianuu.injekt", "Key")
    val LinkedBinding = ClassName("com.ivianuu.injekt", "LinkedBinding")
    val Linker = ClassName("com.ivianuu.injekt", "Linker")
    val Parameters = ClassName("com.ivianuu.injekt", "Parameters")
    val UnlinkedBinding = ClassName("com.ivianuu.injekt", "UnlinkedBinding")
}
