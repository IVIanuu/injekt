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
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

class CreatorGenerator(private val descriptor: CreatorDescriptor) {

    fun generate() =
        FileSpec.builder(descriptor.creatorName.packageName, descriptor.creatorName.simpleName)
            .apply {
                val imports = imports()
                if (imports.isNotEmpty()) {
                    addImport("com.ivianuu.injekt", *imports().toTypedArray())
                }
            }
            .addType(creator())
            .build()

    private fun imports() = mutableSetOf("binding", "find")

    private fun creator() = TypeSpec.classBuilder(descriptor.creatorName)
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
        .add("return binding(")
        .add("\n")
        .indent()
        .addStatement("kind = %T,", descriptor.kind)
        .apply {
            descriptor.scope?.let { addStatement("scope = %T,", it) }
        }
        .add("definition = { ")
        .apply {
            if (descriptor.constructorParams.any { it is ParamDescriptor.Parameter }) {
                add("params -> ")
            }
        }
        .add("\n")
        .indent()
        .addStatement("%T(", descriptor.target)
        .indent()
        .apply {
            descriptor.constructorParams.forEachIndexed { i, param ->
                when (param) {
                    is ParamDescriptor.Parameter -> {
                        add("${param.paramName} = params.find(${param.index})")
                    }
                    is ParamDescriptor.Dependency -> {
                        if (param.qualifierName != null) {
                            add("${param.paramName} = find(%T)", param.qualifierName)
                        } else {
                            add("${param.paramName} = find()")
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
        .add(")")
        .add("\n")
        .unindent()
        .add("}")
        .add("\n")
        .unindent()
        .add(")")
        .add("\n")
        .build()
}