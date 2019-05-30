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
import com.ivianuu.injekt.*
import com.ivianuu.injekt.provider.Provider
import com.ivianuu.processingx.*
import com.ivianuu.processingx.steps.ProcessingStep
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic
import kotlin.reflect.KClass

class CreatorStep : ProcessingStep() {

    override fun annotations(): Set<KClass<out Annotation>> = setOf(
        Bind::class,
        Factory::class,
        Name::class,
        Param::class,
        Raw::class,
        Single::class
    )

    override fun process(elementsByAnnotation: SetMultimap<KClass<out Annotation>, Element>): Set<Element> {
        validateParameterAnnotations(elementsByAnnotation)

        val bindingElements = (elementsByAnnotation[Factory::class]
                + elementsByAnnotation[Single::class])

        validateOnlyOneKindAnnotation(bindingElements)

        (elementsByAnnotation[Factory::class] +
                elementsByAnnotation[Single::class])
            .filterIsInstance<TypeElement>()
            .mapNotNull { createBindingDescriptor(it) }
            .map { CreatorGenerator(it) }
            .map { it.generate() }
            .forEach { it.writeTo(filer) }

        return emptySet()
    }

    private fun createBindingDescriptor(element: TypeElement): CreatorDescriptor? {
        val annotation = when {
            element.hasAnnotation<Bind>() -> element.getAnnotationMirror<Bind>()
            element.hasAnnotation<Factory>() -> element.getAnnotationMirror<Factory>()
            element.hasAnnotation<Single>() -> element.getAnnotationMirror<Single>()
            else -> return null
        }

        val kind = when {
            element.hasAnnotation<Bind>() -> CreatorDescriptor.Kind(
                annotation.getAsType("kind").asTypeName() as ClassName
            )
            element.hasAnnotation<Factory>() -> CreatorDescriptor.Kind.Factory
            element.hasAnnotation<Single>() -> CreatorDescriptor.Kind.Single
            else -> error("unknown annotation type $element")
        }

        var scopeType: TypeMirror? = annotation["scope"].asTypeValue()
        if (scopeType!!.asTypeName() == Nothing::class.asTypeName()) {
            scopeType = null
        }

        val scope = scopeType
            ?.let { typeUtils.asElement(it) }
            ?.let {
                if (!it.isObject) {
                    messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "scope must be an object", element
                    )
                    return@createBindingDescriptor null
                }

                scopeType.asTypeName() as ClassName
            }

        var paramsIndex = -1

        val targetName = element.asClassName().javaToKotlinType() as ClassName

        val factoryName = ClassName(
            targetName.packageName,
            element.simpleName.toString() + "__Creator"
        )

        return CreatorDescriptor(
            targetName,
            factoryName,
            kind,
            scope,
            element.enclosedElements
                .filterIsInstance<ExecutableElement>()
                .first { it.kind == ElementKind.CONSTRUCTOR }
                .parameters
                .map { param ->
                    val paramName = param.simpleName.toString()

                    val paramIndex = if (param.hasAnnotation<Param>()) {
                        paramsIndex++
                        paramsIndex
                    } else {
                        -1
                    }

                    val nameAnnotation =
                        param.getAnnotationMirrorOrNull<Name>()

                    val nameType = nameAnnotation?.getAsType("name")

                    val name = nameType
                        ?.let { typeUtils.asElement(it) }
                        ?.let {
                            if (!it.isObject) {
                                messager.printMessage(
                                    Diagnostic.Kind.ERROR,
                                    "name must be an object", element
                                )
                                return@createBindingDescriptor null
                            }

                            it.asType().asTypeName() as ClassName
                        }

                    if (paramIndex != -1 && name != null) {
                        messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            "Only one of @Param or @Name can be annotated per parameter",
                            param
                        )
                        return@createBindingDescriptor null
                    }

                    val type = typeUtils.erasure(param.asType())
                    val lazyType = elementUtils.getTypeElement(Lazy::class.java.name).asType()
                    val providerType =
                        elementUtils.getTypeElement(Provider::class.java.name).asType()

                    val isRaw = param.hasAnnotation<Raw>()

                    val paramKind = when {
                        !isRaw && typeUtils.isAssignable(
                            lazyType,
                            type
                        ) -> ParamDescriptor.Kind.LAZY
                        !isRaw && typeUtils.isAssignable(
                            providerType,
                            type
                        ) -> ParamDescriptor.Kind.PROVIDER
                        else -> ParamDescriptor.Kind.VALUE
                    }

                    ParamDescriptor(
                        paramKind,
                        paramName,
                        name,
                        paramIndex
                    )
                }
        )
    }

    private fun validateOnlyOneKindAnnotation(elements: Set<Element>) {
        elements
            .filter {
                it.hasAnnotation<Factory>() && it.hasAnnotation<Single>()
            }
            .forEach {
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Annotated class can only be annotated with one off @Factory or @Single",
                    it
                )
            }
    }

    private fun validateParameterAnnotations(elementsByAnnotation: SetMultimap<KClass<out Annotation>, Element>) {
        elementsByAnnotation[Name::class].validateHasBuilderAnnotation {
            "@Name can only used in a class which annotated with @Bind, @Factory or @Single"
        }

        elementsByAnnotation[Param::class].validateHasBuilderAnnotation {
            "@Param can only used in a class which annotated with @Bind, @Factory or @Single"
        }

        elementsByAnnotation[Raw::class].validateHasBuilderAnnotation {
            "@Raw can only used in a class which annotated with @Bind, @Factory or @Single"
        }
    }

    private inline fun Set<Element>.validateHasBuilderAnnotation(message: (Element) -> String) {
        filter {
            val type = it.enclosingElement.enclosingElement
            !type.hasAnnotation<Bind>()
                    && !type.hasAnnotation<Factory>()
                    && !type.hasAnnotation<Single>()
        }
            .forEach {
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    message(it),
                    it
                )
            }
    }
}