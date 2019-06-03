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
import com.ivianuu.injekt.Name
import com.ivianuu.injekt.Param
import com.ivianuu.injekt.ScopeAnnotation
import com.ivianuu.processingx.*
import com.ivianuu.processingx.steps.ProcessingStep
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import kotlin.reflect.KClass

class CreatorStep : ProcessingStep() {

    override fun annotations() =
        Kind.values().map { it.annotation }.toSet()

    override fun process(elementsByAnnotation: SetMultimap<KClass<out Annotation>, Element>): Set<Element> {
        annotations()
            .flatMap { elementsByAnnotation[it] }
            .filterIsInstance<TypeElement>()
            .mapNotNull { createBindingDescriptor(it) }
            .map { CreatorGenerator(it) }
            .map { it.generate() }
            .forEach { it.writeTo(filer) }

        return emptySet()
    }

    private fun createBindingDescriptor(element: TypeElement): CreatorDescriptor? {
        val kindAnnotations =
            Kind.values()
                .mapNotNull { element.getAnnotationMirrorOrNull(it.annotation) }

        if (kindAnnotations.size > 1) {
            messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Can only have 1 kind annotation",
                element
            )
            return null
        }

        val kindAnnotation = kindAnnotations.first()

        val kindName = kindAnnotation
            .annotationType
            .asElement()
            .toString()
            .let { type -> Kind.values().first { it.annotation.java.name == type } }
            .impl

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

                var nameType = param.getAnnotationMirrorOrNull<Name>()
                    ?.getAsType("name")

                val nameAnnotations =
                    param.getAnnotatedAnnotations<Name>()

                if (nameType != null && nameAnnotations.isNotEmpty()) {
                    messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Can only have 1 name annotation",
                        param
                    )
                    return@createBindingDescriptor null
                }

                if (nameType == null) {
                    if (nameAnnotations.size > 1) {
                        messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            "Can only have 1 name annotation",
                            param
                        )
                        return@createBindingDescriptor null
                    }

                    nameType = nameAnnotations.firstOrNull()
                        ?.annotationType
                        ?.asElement()
                        ?.getAnnotationMirror<Name>()
                        ?.getAsType("name")
                }

                val qualifierName = nameType?.asTypeName() as? ClassName

                if (paramIndex != -1 && qualifierName != null) {
                    messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Only one of @Param or @Name can be annotated per parameter",
                        param
                    )
                    return@createBindingDescriptor null
                }

                if (paramIndex != -1) {
                    ParamDescriptor.Parameter(paramName, paramIndex)
                } else {
                    ParamDescriptor.Dependency(paramName, qualifierName)
                }
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