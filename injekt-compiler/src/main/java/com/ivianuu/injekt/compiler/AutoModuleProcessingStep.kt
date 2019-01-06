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
import com.ivianuu.injekt.Provider
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

class AutoModuleProcessingStep(override val processingEnv: ProcessingEnvironment) : ProcessingStep,
    ProcessingEnvHolder {

    override fun annotations() = setOf(
        Factory::class.java,
        Name::class.java,
        Module::class.java,
        Param::class.java,
        Single::class.java
    )

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): Set<Element> {
        validateNameUsages(elementsByAnnotation[Name::class.java])
        validateParamUsages(elementsByAnnotation[Param::class.java])

        val definitionElements = (elementsByAnnotation[Factory::class.java]
                + elementsByAnnotation[Single::class.java])

        validateOnlyOneKindAnnotation(definitionElements)

        val definitions =
            (elementsByAnnotation[Factory::class.java] + elementsByAnnotation[Single::class.java])
                .filterIsInstance<TypeElement>()
                .mapNotNull { createDefinitionDescriptor(it) }
                .toSet()

        val moduleElement = elementsByAnnotation[Module::class.java].first()
        val annotation = moduleElement.getAnnotationMirror<Module>()
        var packageName = annotation["packageName"].value as String

        if (packageName.isEmpty()) {
            packageName = moduleElement.getPackage().qualifiedName.toString()
        }

        var moduleName = annotation["moduleName"].value as String

        if (moduleName.isEmpty()) {
            moduleName = moduleElement.simpleName.toString().decapitalize()
        }

        val internal = annotation["internal"].value as Boolean
        var scopeId: String? = annotation["scopeId"].value as String
        if (scopeId!!.isEmpty()) {
            scopeId = null
        }

        val override = annotation["override"].value as Boolean
        val createOnStart = annotation.getOrNull("scopeId")?.value as? Boolean ?: false

        val module = ModuleDescriptor(
            packageName, moduleName, internal,
            scopeId, override, createOnStart, definitions
        )

        ModuleGenerator(module).generate().write(processingEnv)

        return emptySet()
    }

    private fun createDefinitionDescriptor(element: TypeElement): DefinitionDescriptor? {
        val kind = if (element.hasAnnotation<Single>()) {
            DefinitionDescriptor.Kind.SINGLE
        } else {
            DefinitionDescriptor.Kind.FACTORY
        }

        val annotation = when (kind) {
            DefinitionDescriptor.Kind.FACTORY -> element.getAnnotationMirror<Factory>()
            DefinitionDescriptor.Kind.SINGLE -> element.getAnnotationMirror<Single>()
        }

        var name: String? = annotation["name"].value as String
        if (name!!.isEmpty()) {
            name = null
        }

        var scope: String? = annotation["scopeId"].value as String
        if (scope!!.isEmpty()) {
            scope = null
        }

        val override = annotation["override"].value as Boolean
        val createOnStart = annotation.getOrNull("createOnStart")?.value as? Boolean

        val secondaryTypes = annotation.getAsTypeList("secondaryTypes")
            .map { it.asTypeName().javaToKotlinType() }.toSet()

        var paramsIndex = -1
        return DefinitionDescriptor(
            element.asClassName().javaToKotlinType() as ClassName,
            kind,
            name,
            scope,
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
                        it.getAnnotationMirrorOrNull<Name>()?.get("scopeId")?.value as? String

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
                            "Only one of @Name and @Param can be annotated per parameter",
                            it
                        )
                        return null
                    }

                    val type = typeUtils.erasure(it.asType())
                    val lazyType = elementUtils.getTypeElement(Lazy::class.java.name).asType()
                    val providerType =
                        elementUtils.getTypeElement(Provider::class.java.name).asType()

                    val paramKind = when {
                        typeUtils.isAssignable(lazyType, type) -> ParamDescriptor.Kind.LAZY
                        typeUtils.isAssignable(providerType, type) -> ParamDescriptor.Kind.PROVIDER
                        else -> ParamDescriptor.Kind.VALUE
                    }

                    ParamDescriptor(
                        paramKind,
                        getName,
                        paramIndex
                    )
                }
        )
    }

    private fun validateNameUsages(elements: Set<Element>) {
        elements
            .filter {
                val type = it.enclosingElement.enclosingElement
                !type.hasAnnotation<Factory>()
                        && !type.hasAnnotation<Single>()
            }
            .forEach {
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "@Name annotation should only be used inside a class which is annotated with @Single or @Factory",
                    it
                )
            }
    }

    private fun validateParamUsages(elements: Set<Element>) {
        elements
            .filter {
                val type = it.enclosingElement.enclosingElement
                !type.hasAnnotation<Factory>()
                        && !type.hasAnnotation<Single>()
            }
            .forEach {
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "@Param annotation should only be used inside a class which is annotated with @Single or @Factory",
                    it
                )
            }
    }

    private fun validateOnlyOneKindAnnotation(elements: Set<Element>) {
        elements
            .filter { it.hasAnnotation<Factory>() && it.hasAnnotation<Single>() }
            .forEach {
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "It's not possible to annotate classes with @Factory AND @Single", it
                )
            }
    }

}