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

import com.google.common.collect.SetMultimap
import com.ivianuu.injekt.annotations.*
import com.ivianuu.processingx.*
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class AutoInjektProcessingStep(override val processingEnv: ProcessingEnvironment) : ProcessingStep,
    ProcessingEnvHolder {

    override fun annotations() = setOf(
        AutoModuleConfig::class.java,
        Factory::class.java,
        Single::class.java
    )

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): Set<Element> {
        val configurations = elementsByAnnotation[AutoModuleConfig::class.java]

        when {
            configurations.size > 1 -> {
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Only one class should be annotated with AutoModuleConfig"
                )
                return emptySet()
            }
            configurations.size == 0 -> {
                messager.printMessage(Diagnostic.Kind.ERROR, "Missing AutoModuleConfig annotation")
                return emptySet()
            }
        }

        val packageName = configurations.first()
            .getAnnotationMirror<AutoModuleConfig>()["packageName"].value as String

        if (packageName.isEmpty()) {
            messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Package name must not be empty",
                configurations.first()
            )
            return emptySet()
        }

        val types = mutableSetOf<DeclarationDescriptor>()

        types += elementsByAnnotation[Factory::class.java]
            .filterIsInstance<TypeElement>()
            .mapNotNull { createDescriptor(it) }

        types += elementsByAnnotation[Single::class.java]
            .filterIsInstance<TypeElement>()
            .mapNotNull { createDescriptor(it) }

        val module = ModuleDescriptor(packageName, "autoModule", types)
        val generator = AutoInjektGenerator(module)

        generator.generate()
            .write(processingEnv)

        return emptySet()
    }

    private fun createDescriptor(element: TypeElement): DeclarationDescriptor? {
        val kind = if (element.hasAnnotation<Single>()) {
            DeclarationDescriptor.Kind.SINGLE
        } else {
            DeclarationDescriptor.Kind.FACTORY
        }

        val annotation = when (kind) {
            DeclarationDescriptor.Kind.FACTORY -> element.getAnnotationMirror<Factory>()
            DeclarationDescriptor.Kind.SINGLE -> element.getAnnotationMirror<Single>()
        }

        var name: String? = annotation["name"].value as String
        if (name!!.isEmpty()) {
            name = null
        }

        val override = annotation["override"].value as Boolean
        val createOnStart = annotation["createOnStart"].value as Boolean

        val secondaryTypes = annotation.getAsTypeList("secondaryTypes")
            .map { it.asTypeName().javaToKotlinType() }.toSet()

        var paramsIndex = -1
        return DeclarationDescriptor(
            element.asClassName().javaToKotlinType() as ClassName,
            kind,
            name,
            override,
            createOnStart,
            secondaryTypes,
            element.enclosedElements
                .filterIsInstance<ExecutableElement>()
                .first { it.kind == ElementKind.CONSTRUCTOR }
                .parameters
                .map {
                    val paramIndex = if (it.hasAnnotation<Param>()) {
                        paramsIndex++
                        paramsIndex
                    } else {
                        -1
                    }

                    val getName =
                        it.getAnnotationMirrorOrNull<Name>()?.get("name")?.value as? String

                    if (getName != null && getName.isEmpty()) {
                        messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            "Name must not be empty", it
                        )
                        return null
                    }

                    if (paramIndex != -1 && getName != null) {
                        messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            "Only one of @Name and @Param can be annotated per parameter"
                        )
                        return null
                    }

                    ParamDescriptor(
                        it.simpleName.toString(),
                        getName,
                        paramIndex
                    )
                }
        )
    }
}