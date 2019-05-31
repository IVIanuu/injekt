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
import javax.tools.Diagnostic
import kotlin.reflect.KClass

class CreatorStep : ProcessingStep() {

    override fun annotations() = setOf(Bind::class) + kindAnnotations

    override fun process(elementsByAnnotation: SetMultimap<KClass<out Annotation>, Element>): Set<Element> {
        val kindsWithoutBind = kindAnnotations
            .flatMap { elementsByAnnotation[it] }
            .filterNot { it.hasAnnotation<Bind>() }

        (elementsByAnnotation[Bind::class] + kindsWithoutBind)
            .filterIsInstance<TypeElement>()
            .mapNotNull { createBindingDescriptor(it) }
            .map { CreatorGenerator(it) }
            .map { it.generate() }
            .forEach { it.writeTo(filer) }

        return emptySet()
    }

    private fun createBindingDescriptor(element: TypeElement): CreatorDescriptor? {
        var kindType = element.getAnnotationMirrorOrNull<KindAnnotation>()
            ?.getAsType("kind")

        val kindAnnotations =
            element.getAnnotatedAnnotations<KindAnnotation>()

        if (kindType != null && kindAnnotations.isNotEmpty()) {
            messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Can only have 1 kind annotation",
                element
            )
            return null
        }

        if (kindType == null) {
            when {
                kindAnnotations.size > 1 -> {
                    messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Can only have 1 kind annotation",
                        element
                    )
                    return null
                }
                kindAnnotations.isEmpty() -> {
                    messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Must have a kind annotation",
                        element
                    )
                    return null
                }
            }

            kindType = kindAnnotations.first()
                .annotationType
                .asElement()
                .getAnnotationMirror<KindAnnotation>()
                .getAsType("kind")
        }

        val kindName = kindType.asTypeName() as ClassName

        var scopeType = element.getAnnotationMirrorOrNull<ScopeAnnotation>()
            ?.getAsType("scope")

        val scopeAnnotations =
            element.getAnnotatedAnnotations<ScopeAnnotation>()

        if (scopeType != null && scopeAnnotations.isNotEmpty()) {
            messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Can only have 1 scope annotation",
                element
            )
            return null
        }

        if (scopeType == null) {
            if (scopeAnnotations.size > 1) {
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Can only have 1 scope annotation",
                    element
                )
                return null
            }

            scopeType = scopeAnnotations.firstOrNull()
                ?.annotationType
                ?.asElement()
                ?.getAnnotationMirror<ScopeAnnotation>()
                ?.getAsType("scope")
        }

        val scopeName = scopeType?.asTypeName() as? ClassName

        var paramsIndex = -1

        val targetName = element.asClassName().javaToKotlinType() as ClassName

        val creatorName = ClassName(
            targetName.packageName,
            element.simpleName.toString() + "__Creator"
        )

        val constructorParams = element.enclosedElements
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

                var nameType = element.getAnnotationMirrorOrNull<Name>()
                    ?.getAsType("name")

                val nameAnnotations =
                    element.getAnnotatedAnnotations<Name>()

                if (nameType != null && nameAnnotations.isNotEmpty()) {
                    messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Can only have 1 name annotation",
                        element
                    )
                    return@createBindingDescriptor null
                }

                if (nameType == null) {
                    if (nameAnnotations.size > 1) {
                        messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            "Can only have 1 name annotation",
                            element
                        )
                        return@createBindingDescriptor null
                    }

                    nameType = nameAnnotations.firstOrNull()
                        ?.annotationType
                        ?.asElement()
                        ?.getAnnotationMirror<Name>()
                        ?.getAsType("name")
                }

                val nameName = nameType?.asTypeName() as? ClassName

                if (paramIndex != -1 && nameName != null) {
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
                    nameName,
                    paramIndex,
                    mapName?.asTypeName() as? ClassName,
                    setName?.asTypeName() as? ClassName
                )
            }

        return CreatorDescriptor(
            targetName,
            creatorName,
            kindName,
            scopeName,
            constructorParams
        )
    }

}