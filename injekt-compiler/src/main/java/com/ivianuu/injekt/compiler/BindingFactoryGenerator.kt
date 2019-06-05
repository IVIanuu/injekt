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
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.UnlinkedBinding
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

class BindingFactoryGenerator(private val descriptor: BindingFactoryDescriptor) {

    fun generate() =
        FileSpec.builder(descriptor.creatorName.packageName, descriptor.creatorName.simpleName)
            .apply {
                val imports = imports()
                if (imports.isNotEmpty()) {
                    addImport("com.ivianuu.injekt", *imports().toTypedArray())
                }
            }
            .addType(bindingFactory())
            .build()

    private fun imports() = setOf("get")

    private fun bindingFactory() = TypeSpec.classBuilder(descriptor.creatorName)
        .addSuperinterface(
            BindingFactory::class.asClassName().plusParameter(descriptor.target)
        )
        .addProperty(
            PropertySpec.builder(
                "scope",
                Scope::class.asClassName().copy(nullable = true),
                KModifier.OVERRIDE
            )
                .initializer("null")
                .build()
        )
        .addFunction(
            FunSpec.builder("create")
                .addModifiers(KModifier.OVERRIDE)
                .returns(Binding::class.asClassName().plusParameter(descriptor.target))
                .addCode("return UnlinkedBindingImpl()")
                .build()
        )
        .addType(unlinkedBinding())
        .addType(linkedBinding())
        .build()

    private fun unlinkedBinding() = TypeSpec.classBuilder("UnlinkedBindingImpl")
        .addModifiers(KModifier.PRIVATE)
        .superclass(
            UnlinkedBinding::class.asClassName().plusParameter(descriptor.target)
        )
        .addFunction(
            FunSpec.builder("link")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("linker", Linker::class.asClassName())
                .returns(
                    LinkedBinding::class.asTypeName().plusParameter(descriptor.target)
                )
                .addCode(
                    CodeBlock.builder()
                        .add("return LinkedBindingImpl(\n")
                        .apply {
                            add(
                                descriptor.constructorParams
                                    .filterIsInstance<ParamDescriptor.Dependency>()
                                    .joinToString(separator = ", ") { "linker.get()" }
                            )
                        }
                        .add(")")
                        .add("\n")
                        .build()
                )
                .build()
        )
        .build()

    private fun linkedBinding() = TypeSpec.classBuilder("LinkedBindingImpl")
        .addModifiers(KModifier.PRIVATE)
        .superclass(
            LinkedBinding::class.asClassName().plusParameter(descriptor.target)
        )
        .primaryConstructor(
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
        .apply {
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
            if (descriptor.constructorParams.any { it is ParamDescriptor.Parameter }) {
                add("val params = parameters?.invoke()\n")
            }
        }
        .add("return %T(\n", descriptor.target)
        .indent()
        .apply {
            descriptor.constructorParams.forEachIndexed { i, param ->
                when (param) {
                    is ParamDescriptor.Parameter -> {
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

    /*private fun bindingImpl() = TypeSpec.classBuilder("BindingImpl")
        .addModifiers(KModifier.PRIVATE)
        .addSuperinterface(
        )
        .apply {
            descriptor.constructorParams
                .filterIsInstance<ParamDescriptor.Dependency>()
                .forEach { param ->
                    addProperty(
                        PropertySpec.builder(
                            param.paramName + "Binding",
                            Binding::class.asTypeName().plusParameter(param.paramType)
                        )
                            .addModifiers(KModifier.PRIVATE, KModifier.LATEINIT)
                            .mutable()
                            .build()
                    )
                }
        }
        .addFunction(

        )
        .addFunction(

        )
        .build()
*/
}