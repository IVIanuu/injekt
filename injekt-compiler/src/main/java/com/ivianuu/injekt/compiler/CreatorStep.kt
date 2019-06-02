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
import com.ivianuu.processingx.*
import com.ivianuu.processingx.steps.ProcessingStep
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.tools.Diagnostic
import kotlin.reflect.KClass

class CreatorStep(
    private val kindCollector: KindCollector
) : ProcessingStep() {

    lateinit var roundEnv: RoundEnvironment

    override fun annotations() = setOf(Bind::class) + kindAnnotations

    override fun process(elementsByAnnotation: SetMultimap<KClass<out Annotation>, Element>): Set<Element> {
        val kindsWithoutBind = kindAnnotations
            .flatMap { elementsByAnnotation[it] }
            .filterNot { it.hasAnnotation<Bind>() }

        val dynamicKinds = kindCollector.kinds
            .flatMap { roundEnv.getElementsAnnotatedWith(it) }
            .filterNot { it.hasAnnotation<Bind>() }

        (elementsByAnnotation[Bind::class] + kindsWithoutBind + dynamicKinds)
            .filterIsInstance<TypeElement>()
            .mapNotNull { createBindingDescriptor(it) }
            .map { CreatorGenerator(it) }
            .map { it.generate() }
            .forEach { it.writeTo(filer) }

        return emptySet()
    }

    private fun createBindingDescriptor(element: TypeElement): CreatorDescriptor? {
        var kindAnnotatedElement: TypeElement? = null

        var kindAnnotation = element.getAnnotationMirrorOrNull<KindAnnotation>()
        if (kindAnnotation != null) kindAnnotatedElement = element

        val kindAnnotations =
            element.getAnnotatedAnnotations<KindAnnotation>()

        if (kindAnnotation != null && kindAnnotations.isNotEmpty()) {
            messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Can only have 1 kind annotation",
                element
            )
            return null
        }

        if (kindAnnotation == null) {
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

            kindAnnotation = kindAnnotations.first()
                .annotationType
                .asElement()
                .also { kindAnnotatedElement = it as TypeElement }
                .getAnnotationMirror<KindAnnotation>()
        }

        val kindName = kindAnnotation.getAsType("kind")
            .asTypeName() as ClassName

        var scopeAnnotation = element.getAnnotationMirrorOrNull<ScopeAnnotation>()

        val scopeAnnotations =
            element.getAnnotatedAnnotations<ScopeAnnotation>()

        if (scopeAnnotation != null && scopeAnnotations.isNotEmpty()) {
            messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Can only have 1 scope annotation",
                element
            )
            return null
        }

        if (scopeAnnotation == null) {
            if (scopeAnnotations.size > 1) {
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Can only have 1 scope annotation",
                    element
                )
                return null
            }

            scopeAnnotation = scopeAnnotations.firstOrNull()
                ?.annotationType
                ?.asElement()
                ?.getAnnotationMirror<ScopeAnnotation>()
        }

        if (scopeAnnotation == null) {
            scopeAnnotation = kindAnnotation.annotationType.asElement()
                .getAnnotationMirrorOrNull<ScopeAnnotation>()
        }

        val scopeName = scopeAnnotation?.getAsType("scope")
            ?.asTypeName() as? ClassName

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

                var bindingMapAnnotation = param.getAnnotationMirrorOrNull<BindingMap>()
                if (bindingMapAnnotation == null) {
                    bindingMapAnnotation = param.getAnnotatedAnnotations<BindingMap>()
                        .firstOrNull()
                        ?.annotationType
                        ?.asElement()
                        ?.getAnnotationMirror<BindingMap>()
                }

                val mapName = bindingMapAnnotation
                    ?.getAsType("mapName")

                var bindingSetAnnotation = param.getAnnotationMirrorOrNull<BindingSet>()
                if (bindingSetAnnotation == null) {
                    bindingSetAnnotation = param.getAnnotatedAnnotations<BindingSet>()
                        .firstOrNull()
                        ?.annotationType
                        ?.asElement()
                        ?.getAnnotationMirror<BindingSet>()
                }

                val setName = bindingSetAnnotation
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

        val interceptors = mutableListOf<ClassName>()

        element.getAnnotationMirrorOrNull<Interceptors>()
            ?.getAsTypeList("interceptors")
            ?.map { it.asTypeName() as ClassName }
            ?.forEach { interceptors.add(it) }

        kindAnnotatedElement
            ?.getAnnotationMirrorOrNull<Interceptors>()
            ?.getAsTypeList("interceptors")
            ?.map { it.asTypeName() as ClassName }
            ?.forEach { interceptors.add(it) }

        return CreatorDescriptor(
            targetName,
            creatorName,
            kindName,
            scopeName,
            constructorParams,
            interceptors
        )
    }

}