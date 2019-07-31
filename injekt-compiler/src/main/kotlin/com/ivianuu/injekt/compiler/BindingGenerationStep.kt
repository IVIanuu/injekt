/*
 * Copyright 2019 Manuel Wrage
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
import com.ivianuu.processingx.messager
import com.ivianuu.processingx.steps.ProcessingStep
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import me.eugeniomarletti.kotlin.metadata.KotlinClassMetadata
import me.eugeniomarletti.kotlin.metadata.kotlinMetadata
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.deserialization.Flags
import me.eugeniomarletti.kotlin.metadata.shadow.name.FqName
import me.eugeniomarletti.kotlin.metadata.shadow.platform.JavaToKotlinClassMap
import me.eugeniomarletti.kotlin.metadata.visibility
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic.Kind.ERROR
import kotlin.reflect.KClass

class BindingGenerationStep : ProcessingStep() {

    override fun annotations() = setOf(Inject::class)

    override fun process(elementsByAnnotation: SetMultimap<KClass<out Annotation>, Element>): Set<Element> {
        annotations()
            .flatMap { elementsByAnnotation[it] }
            .mapNotNull { createDescriptor(it) }
            .map { BindingGenerator(it) }
            .map { it.generate() }
            .forEach { it.writeTo(filer) }

        return emptySet()
    }

    private fun createDescriptor(element: Element): BindingDescriptor? {
        val annotatedType = if (element is TypeElement) element
        else element.enclosingElement as TypeElement

        if (element is ExecutableElement
            && annotatedType.hasAnnotation<Inject>()
        ) {
            messager.printMessage(
                ERROR,
                "Can only have @Inject on the type or the constructor",
                element
            )
            return null
        }

        val classMetadata =
            annotatedType.kotlinMetadata as? KotlinClassMetadata

        if (classMetadata == null) {
            messager.printMessage(
                ERROR,
                "Must be a kotlin class",
                annotatedType
            )
            return null
        }

        val isInternal =
            classMetadata.data.classProto.visibility == ProtoBuf.Visibility.INTERNAL

        val isObject =
            Flags.CLASS_KIND.get(classMetadata.data.classProto.flags) == ProtoBuf.Class.Kind.OBJECT

        if ((element is TypeElement || !isObject)
            && (annotatedType.modifiers.contains(Modifier.PRIVATE)
                    || annotatedType.modifiers.contains(Modifier.PROTECTED))
        ) {
            messager.printMessage(
                ERROR,
                "Must be a public or internal",
                annotatedType
            )
            return null
        }

        val scopeAnnotations =
            annotatedType.getAnnotatedAnnotations<Scope>()

        if (scopeAnnotations.size > 1) {
            messager.printMessage(
                ERROR,
                "Can only have 1 scope annotation",
                annotatedType
            )
            return null
        }

        val scopeName = scopeAnnotations.firstOrNull()
            ?.annotationType
            ?.asElement()
            ?.asType()
            ?.asTypeName() as? ClassName

        var currentParamsIndex = -1

        val targetName = annotatedType.asClassName().javaToKotlinType() as ClassName

        val factoryName = ClassName(
            targetName.packageName,
            annotatedType.simpleName.toString() + "__Binding"
        )

        var constructorArgs: List<ArgDescriptor>? = null

        if (!isObject) {
            val constructor = if (element is ExecutableElement) {
                element
            } else {
                element.enclosedElements
                    // todo consider multiple constructors
                    .filterIsInstance<ExecutableElement>()
                    .first { it.kind == ElementKind.CONSTRUCTOR }
            }

            if (constructor.modifiers.contains(Modifier.PRIVATE)
                || constructor.modifiers.contains(Modifier.PROTECTED)
            ) {
                messager.printMessage(
                    ERROR,
                    "Must be a public or internal",
                    annotatedType
                )
                return null
            }

            constructorArgs = constructor
                .parameters
                .map { param ->
                    val paramName = param.simpleName.toString()

                    val paramIndex = if (param.hasAnnotation<Param>()) {
                        ++currentParamsIndex
                    } else {
                        -1
                    }

                    var nameType = param.getAnnotationMirrorOrNull<Name>()
                        ?.getAsType("name")

                    val nameAnnotations =
                        param.getAnnotatedAnnotations<Name>()

                    if (nameType != null && nameAnnotations.isNotEmpty()) {
                        messager.printMessage(
                            ERROR,
                            "Can only have 1 name annotation",
                            param
                        )
                        return@createDescriptor null
                    }

                    if (nameType == null) {
                        if (nameAnnotations.size > 1) {
                            messager.printMessage(
                                ERROR,
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
                            ERROR,
                            "Only one of @Param or @Name can be annotated per parameter",
                            param
                        )
                        return@createDescriptor null
                    }

                    val paramType = param.asType().asTypeName().javaToKotlinType()

                    if (paramIndex != -1) {
                        ArgDescriptor.Parameter(paramName, paramIndex)
                    } else {
                        ArgDescriptor.Dependency(paramName, paramType, qualifierName)
                    }
                }
        }

        return BindingDescriptor(
            targetName,
            factoryName,
            isInternal,
            isObject,
            scopeName,
            constructorArgs ?: emptyList()
        )
    }

    private fun TypeName.javaToKotlinType(): TypeName {
        return if (this is WildcardTypeName) {
            if (outTypes.isNotEmpty()) {
                outTypes.first().javaToKotlinType()
            } else {
                inTypes.first().javaToKotlinType()
            }
        } else if (this is ParameterizedTypeName) {
            (rawType.javaToKotlinType() as ClassName).parameterizedBy(
                *typeArguments.map { it.javaToKotlinType() }.toTypedArray()
            )
        } else {
            val className =
                JavaToKotlinClassMap.mapJavaToKotlin(FqName(toString()))?.asSingleFqName()
                    ?.asString()
            if (className == null) this
            else ClassName.bestGuess(className)
        }
    }
}