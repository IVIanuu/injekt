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
import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.Name
import com.ivianuu.injekt.Param
import com.ivianuu.injekt.Scope
import com.ivianuu.processingx.filer
import com.ivianuu.processingx.getAnnotatedAnnotations
import com.ivianuu.processingx.getAnnotationMirror
import com.ivianuu.processingx.getAnnotationMirrorOrNull
import com.ivianuu.processingx.getAsType
import com.ivianuu.processingx.hasAnnotation
import com.ivianuu.processingx.javaToKotlinType
import com.ivianuu.processingx.messager
import com.ivianuu.processingx.steps.ProcessingStep
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import me.eugeniomarletti.kotlin.metadata.KotlinClassMetadata
import me.eugeniomarletti.kotlin.metadata.kotlinMetadata
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf
import me.eugeniomarletti.kotlin.metadata.visibility
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import kotlin.reflect.KClass

class BindingFactoryGenerationStep : ProcessingStep() {

    override fun annotations() = setOf(Inject::class)

    override fun process(elementsByAnnotation: SetMultimap<KClass<out Annotation>, Element>): Set<Element> {
        annotations()
            .flatMap { elementsByAnnotation[it] }
            .filterIsInstance<TypeElement>()
            .mapNotNull { createDescriptor(it) }
            .map { BindingFactoryGenerator(it) }
            .map { it.generate() }
            .forEach { it.writeTo(filer) }

        return emptySet()
    }

    private fun createDescriptor(element: TypeElement): BindingFactoryDescriptor? {
        val classMetadata = element.kotlinMetadata as? KotlinClassMetadata

        if (classMetadata == null) {
            messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Must be a kotlin class",
                element
            )
            return null
        }

        val visibility = classMetadata.data.classProto.visibility

        if (visibility != ProtoBuf.Visibility.PUBLIC
            && visibility != ProtoBuf.Visibility.INTERNAL
        ) {
            messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Must be a public or internal",
                element
            )
            return null
        }

        val isInternal = classMetadata.data.classProto.visibility == ProtoBuf.Visibility.INTERNAL

        val scopeAnnotations =
            element.getAnnotatedAnnotations<Scope>()

        if (scopeAnnotations.size > 1) {
            messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Can only have 1 scope annotation",
                element
            )
            return null
        }

        val scopeName = scopeAnnotations.firstOrNull()
            ?.annotationType
            ?.asElement()
            ?.asType()
            ?.asTypeName() as? ClassName

        var paramsIndex = -1

        val targetName = element.asClassName().javaToKotlinType() as ClassName

        val factoryName = ClassName(
            targetName.packageName,
            element.simpleName.toString() + "__Binding"
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
                    return@createDescriptor null
                }

                if (nameType == null) {
                    if (nameAnnotations.size > 1) {
                        messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            "Can only have 1 name annotation",
                            param
                        )
                        return@createDescriptor null
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
                    return@createDescriptor null
                }

                val paramType = param.asType().asTypeName().javaToKotlinType()

                if (paramIndex != -1) {
                    ParamDescriptor.Parameter(paramName, paramIndex)
                } else {
                    ParamDescriptor.Dependency(paramName, paramType, qualifierName)
                }
            }

        return BindingFactoryDescriptor(
            targetName,
            factoryName,
            isInternal,
            scopeName,
            constructorParams
        )
    }

}