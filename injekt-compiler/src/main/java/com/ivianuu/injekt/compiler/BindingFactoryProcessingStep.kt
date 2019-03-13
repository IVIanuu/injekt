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
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.annotations.Factory
import com.ivianuu.injekt.annotations.Named
import com.ivianuu.injekt.annotations.Param
import com.ivianuu.injekt.annotations.Qualified
import com.ivianuu.injekt.annotations.Raw
import com.ivianuu.injekt.annotations.Reusable
import com.ivianuu.injekt.annotations.Single
import com.ivianuu.processingx.ProcessingEnvHolder
import com.ivianuu.processingx.ProcessingStep
import com.ivianuu.processingx.asTypeValue
import com.ivianuu.processingx.elementUtils
import com.ivianuu.processingx.get
import com.ivianuu.processingx.getAnnotationMirror
import com.ivianuu.processingx.getAnnotationMirrorOrNull
import com.ivianuu.processingx.getAsType
import com.ivianuu.processingx.hasAnnotation
import com.ivianuu.processingx.messager
import com.ivianuu.processingx.typeUtils
import com.ivianuu.processingx.write
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic

class BindingFactoryProcessingStep(override val processingEnv: ProcessingEnvironment) :
    ProcessingStep,
    ProcessingEnvHolder {

    override fun annotations(): Set<Class<out Annotation>> = setOf(
        Factory::class.java,
        Named::class.java,
        Param::class.java,
        Qualified::class.java,
        Raw::class.java,
        Reusable::class.java,
        Single::class.java
    )

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): Set<Element> {
        validateNameUsages(elementsByAnnotation[Named::class.java])
        validateParamUsages(elementsByAnnotation[Param::class.java])
        validateQualifiedUsages(elementsByAnnotation[Qualified::class.java])
        validateRawUsages(elementsByAnnotation[Raw::class.java])

        val bindingElements = (elementsByAnnotation[Factory::class.java]
                + elementsByAnnotation[Reusable::class.java]
                + elementsByAnnotation[Single::class.java])

        validateOnlyOneKindAnnotation(bindingElements)

        (elementsByAnnotation[Factory::class.java] +
                elementsByAnnotation[Reusable::class.java] +
                elementsByAnnotation[Single::class.java])
            .filterIsInstance<TypeElement>()
            .mapNotNull(this::createBindingDescriptor)
            .map(::FactoryGenerator)
            .map(FactoryGenerator::generate)
            .forEach { it.write(processingEnv) }

        return emptySet()
    }

    private fun createBindingDescriptor(element: TypeElement): BindingDescriptor? {
        val kind = when {
            element.hasAnnotation<Factory>() -> BindingDescriptor.Kind.FACTORY
            element.hasAnnotation<Single>() -> BindingDescriptor.Kind.SINGLE
            element.hasAnnotation<Reusable>() -> BindingDescriptor.Kind.REUSABLE
            else -> error("unknown annotation type $element")
        }

        val annotation = when (kind) {
            BindingDescriptor.Kind.FACTORY -> element.getAnnotationMirror<Factory>()
            BindingDescriptor.Kind.REUSABLE -> element.getAnnotationMirror<Reusable>()
            BindingDescriptor.Kind.SINGLE -> element.getAnnotationMirror<Single>()
        }

        var scopeType: TypeMirror? = annotation["scope"].asTypeValue()
        if (scopeType!!.asTypeName() == Scope::class.asTypeName()) {
            scopeType = null
        }

        val scope = scopeType
            ?.let(typeUtils::asElement)
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
            element.simpleName.toString() + "__Factory"
        )

        return BindingDescriptor(
            targetName,
            factoryName,
            kind,
            scope,
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
                        it.getAnnotationMirrorOrNull<Named>()?.get("name")?.value as? String

                    if (getName != null && getName.isEmpty()) {
                        messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            "Named must not be empty", it
                        )
                        return@createBindingDescriptor null
                    }

                    val qualifier =
                        it.getAnnotationMirrorOrNull<Qualified>()?.getAsType("qualifier")
                            ?.let(typeUtils::asElement)
                            ?.let {
                                if (!it.isObject) {
                                    messager.printMessage(
                                        Diagnostic.Kind.ERROR,
                                        "qualifier must be an object", element
                                    )
                                    return@createBindingDescriptor null
                                }

                                it.asType().asTypeName() as ClassName
                            }

                    if ((paramIndex != -1 && getName != null)
                        || (paramIndex != -1 && qualifier != null)
                        || (getName != null && qualifier != null)
                    ) {
                        messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            "Only one of @Named and @Param @Qualified can be annotated per parameter",
                            it
                        )
                        return@createBindingDescriptor null
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
                        qualifier,
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
                        && !type.hasAnnotation<Reusable>()
                        && !type.hasAnnotation<Single>()
            }
            .forEach {
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "@Named annotation should only be used inside a class which is annotated with @Factory, @Reusable or @Single",
                    it
                )
            }
    }

    private fun validateParamUsages(elements: Set<Element>) {
        elements
            .filter {
                val type = it.enclosingElement.enclosingElement
                !type.hasAnnotation<Factory>()
                        && !type.hasAnnotation<Reusable>()
                        && !type.hasAnnotation<Single>()
            }
            .forEach {
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "@Param annotation should only be used inside a class which is annotated with @Factory, @Reusable or @Single",
                    it
                )
            }
    }

    private fun validateQualifiedUsages(elements: Set<Element>) {
        elements
            .filter {
                val type = it.enclosingElement.enclosingElement
                !type.hasAnnotation<Factory>()
                        && !type.hasAnnotation<Reusable>()
                        && !type.hasAnnotation<Single>()
            }
            .forEach {
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "@Qualified annotation should only be used inside a class which is annotated with @Factory, @Reusable or @Single",
                    it
                )
            }
    }

    private fun validateRawUsages(elements: Set<Element>) {
        elements
            .filter {
                val type = it.enclosingElement.enclosingElement
                !type.hasAnnotation<Factory>()
                        && !type.hasAnnotation<Reusable>()
                        && !type.hasAnnotation<Single>()
            }
            .forEach {
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "@Raw annotation should only be used inside a class which is annotated with @Factory, @Reusable or @Single",
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
                    "Annotated class can only be annotated with one off @Factory, @Reusable or @Single",
                    it
                )
            }
    }

}