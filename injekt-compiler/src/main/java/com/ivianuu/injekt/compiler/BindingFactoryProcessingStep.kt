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

import com.google.auto.common.AnnotationMirrors.getAnnotationValuesWithDefaults
import com.google.common.collect.SetMultimap
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.annotations.Creator
import com.ivianuu.injekt.annotations.CreatorsRegistry
import com.ivianuu.injekt.annotations.Factory
import com.ivianuu.injekt.annotations.Name
import com.ivianuu.injekt.annotations.Param
import com.ivianuu.injekt.annotations.Raw
import com.ivianuu.injekt.annotations.Reusable
import com.ivianuu.injekt.annotations.Single
import com.ivianuu.processingx.ProcessingEnvHolder
import com.ivianuu.processingx.ProcessingStep
import com.ivianuu.processingx.asStringListValueOrNull
import com.ivianuu.processingx.asTypeListValueOrNull
import com.ivianuu.processingx.asTypeValueOrNull
import com.ivianuu.processingx.elementUtils
import com.ivianuu.processingx.get
import com.ivianuu.processingx.getAnnotatedAnnotations
import com.ivianuu.processingx.getAnnotationMirror
import com.ivianuu.processingx.getAnnotationMirrorOrNull
import com.ivianuu.processingx.getAsType
import com.ivianuu.processingx.getAsTypeList
import com.ivianuu.processingx.hasAnnotation
import com.ivianuu.processingx.messager
import com.ivianuu.processingx.typeUtils
import com.ivianuu.processingx.write
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class BindingFactoryProcessingStep(override val processingEnv: ProcessingEnvironment) :
    ProcessingStep,
    ProcessingEnvHolder {

    override fun annotations(): Set<Class<out Annotation>> = setOf(
        CreatorsRegistry::class.java,
        Name::class.java,
        Param::class.java,
        Raw::class.java
    )

    private val creators = mutableSetOf<String>(
        Factory::class.java.name,
        Reusable::class.java.name,
        Single::class.java.name
    )

    var roundEnv: RoundEnvironment? = null

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): Set<Element> {
        /* validateNameUsages(elementsByAnnotation[Name::class.java])
         validateParamUsages(elementsByAnnotation[Param::class.java])
         validateRawUsages(elementsByAnnotation[Raw::class.java])
         */

        // collect creators
        elementsByAnnotation[CreatorsRegistry::class.java]
            .map { it.getAnnotationMirror<CreatorsRegistry>() }
            .flatMap { it.getAsTypeList("creators") }
            .map { it.toString() }
            .forEach { creators.add(it) }

        val elements = creators
            .map { processingEnv.elementUtils.getTypeElement(it)!! }
            .flatMap { roundEnv!!.getElementsAnnotatedWith(it) }
            .toSet()

        validateOnlyOneCreatorAnnotation(elements)

        elements
            .asSequence()
            .filterIsInstance<TypeElement>()
            .map { it to it.getAnnotatedAnnotations<Creator>().firstOrNull() }
            .mapNotNull { createBindingDescriptor(it.first, it.second) }
            .map { FactoryGenerator(it) }
            .map { it.generate() }
            .toList()
            .forEach { it.write(processingEnv) }

        return emptySet()
    }

    private fun createBindingDescriptor(
        element: TypeElement,
        creatorAnnotatedAnnotation: AnnotationMirror?
    ): BindingDescriptor? {
        if (creatorAnnotatedAnnotation == null) return null

        val creatorAnnotatedAnnotationElement =
            creatorAnnotatedAnnotation.annotationType.asElement()

        val creatorAnnotation =
            creatorAnnotatedAnnotationElement.getAnnotationMirror<Creator>()

        val args = getAnnotationValuesWithDefaults(
            creatorAnnotatedAnnotation
        )
            .map { (key, value) ->
                val asType = value.asTypeValueOrNull()
                val asTypeList = value.asTypeListValueOrNull()
                val asStringList = value.asStringListValueOrNull()

                val realValue: Any = when {
                    asType != null -> asType.asTypeName().toString()
                    asTypeList != null -> asTypeList.map { it.asTypeName() }.map { it.toString() }
                    asStringList != null -> asStringList
                    else -> value
                }

                ArgDescriptor(
                    key.simpleName.toString(),
                    isType = asType != null,
                    isTypeArray = asTypeList != null,
                    isStringArray = asStringList != null,
                    value = realValue
                )
            }.toSet()

        val creatorName = ClassName.bestGuess(
            creatorAnnotation.getAsType("value").toString()
        )

        var paramsIndex = -1

        val targetName = element.asClassName().javaToKotlinType() as ClassName

        val factoryName = ClassName(
            targetName.packageName,
            element.simpleName.toString() + "__Factory"
        )

        return BindingDescriptor(
            targetName,
            factoryName,
            creatorName,
            args,
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
                            "Only one of @Name and @Param can be annotated per parameter",
                            it
                        )
                        return null
                    }

                    val type = typeUtils.erasure(it.asType())
                    val lazyType = elementUtils.getTypeElement(Lazy::class.java.name).asType()
                    val providerType =
                        elementUtils.getTypeElement(Provider::class.java.name).asType()

                    val isRaw = it.hasAnnotation<Raw>()

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
                        getName,
                        paramIndex
                    )
                }
        )
    }

    /* private fun validateNameUsages(elements: Set<Element>) {
         elements
             .filter { !it.hasAnnotation<Bind>() }
             .forEach {
                 messager.printMessage(
                     Diagnostic.Kind.ERROR,
                     "@Name annotation should only be used inside a class which is annotated with @Bind",
                     it
                 )
             }
     }

     private fun validateParamUsages(elements: Set<Element>) {
         elements
             .filter { !it.hasAnnotation<Bind>() }
             .forEach {
                 messager.printMessage(
                     Diagnostic.Kind.ERROR,
                     "@Param annotation should only be used inside a class which is annotated with @Bind",
                     it
                 )
             }
     }

     private fun validateRawUsages(elements: Set<Element>) {
         elements
             .filter { it.getAnnotatedAnnotations<Creator>().isEmpty() }
             .forEach {
                 messager.printMessage(
                     Diagnostic.Kind.ERROR,
                     "@Raw annotation should only be used inside a class which is annotated with @Factory, @Reusable or @Single",
                     it
                 )
             }
     }
 */
    private fun validateOnlyOneCreatorAnnotation(elements: Set<Element>) {
        elements
            .filter { it.getAnnotatedAnnotations<Creator>().isEmpty() }
            .forEach {
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Annotated class can only be annotated one annotation which itself is annotated with creator",
                    it
                )
            }
    }

}