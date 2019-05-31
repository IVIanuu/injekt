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
import com.ivianuu.injekt.multibinding.BindingMap
import com.ivianuu.injekt.multibinding.BindingSet
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
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic
import kotlin.reflect.KClass

class CreatorStep : ProcessingStep() {

    override fun annotations(): Set<KClass<out Annotation>> =
        kindAnnotations + paramAnnotations

    override fun process(elementsByAnnotation: SetMultimap<KClass<out Annotation>, Element>): Set<Element> {
        val allKindElements = kindAnnotations
            .flatMap { elementsByAnnotation[it] }
            .filterIsInstance<TypeElement>()

        validateParameterAnnotationsOnlyUsedWithKind(elementsByAnnotation)

        validateOnlyOneKindAnnotation(allKindElements)

        allKindElements
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
                // todo consider multiple constructors
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

                    val mapName = param.getAnnotationMirrorOrNull<BindingMap>()
                        ?.getAsType("mapName")
                    val setName = param.getAnnotationMirrorOrNull<BindingSet>()
                        ?.getAsType("setName")

                    val typeForParamKind = when {
                        mapName != null -> {
                            val mapType = param.asType()

                            messager.printMessage(
                                Diagnostic.Kind.WARNING,
                                "map type is $mapType ${mapType.javaClass}"
                            )

                            if (mapType is DeclaredType) {
                                typeUtils.erasure(mapType.typeArguments[1])
                            } else {
                                // todo error
                                null
                            }
                        }
                        setName != null -> {
                            val setType = param.asType()
                            if (setType is DeclaredType) {
                                typeUtils.erasure(setType.typeArguments[0])
                            } else {
                                // todo error
                                null
                            }
                        }
                        else -> typeUtils.erasure(param.asType())
                    }

                    if (typeForParamKind == null) {
                        messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            "failed to parse type for ${param.asType()}", param
                        )
                        return@createBindingDescriptor null
                    }

                    val lazyType = elementUtils.getTypeElement(Lazy::class.java.name).asType()
                    val providerType =
                        elementUtils.getTypeElement(Provider::class.java.name).asType()

                    val isRaw = param.hasAnnotation<Raw>()

                    val paramKind = when {
                        !isRaw && typeUtils.isAssignable(
                            lazyType,
                            typeForParamKind
                        ) -> ParamDescriptor.Kind.LAZY
                        !isRaw && typeUtils.isAssignable(
                            providerType,
                            typeForParamKind
                        ) -> ParamDescriptor.Kind.PROVIDER
                        else -> ParamDescriptor.Kind.VALUE
                    }

                    ParamDescriptor(
                        paramKind,
                        paramName,
                        name,
                        paramIndex,
                        mapName?.asTypeName() as? ClassName,
                        setName?.asTypeName() as? ClassName
                    )
                }
        )
    }

    private fun validateOnlyOneKindAnnotation(elements: Iterable<Element>) {
        elements
            .filter { element ->
                kindAnnotations.count { element.hasAnnotation(it) } > 1
            }
            .forEach {
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Classes can only have one kind annotation",
                    it
                )
            }
    }

    private fun validateParameterAnnotationsOnlyUsedWithKind(
        elementsByAnnotation: SetMultimap<KClass<out Annotation>, Element>
    ) {
        paramAnnotations
            .flatMap { annotation ->
                elementsByAnnotation[annotation].map { annotation to it }
            }
            .filterNot { (_, param) ->
                kindAnnotations.any {
                    param
                        // constructor
                        .enclosingElement
                        // class
                        .enclosingElement
                        .hasAnnotation(it)
                }
            }
            .forEach { (annotation, param) ->
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "@${annotation.java.simpleName} can only used in a class which annotated with a kind annotation",
                    param
                )
            }
    }

}