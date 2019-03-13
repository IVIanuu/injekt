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
import com.ivianuu.injekt.Qualifier
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
        validateParameterAnnotations(elementsByAnnotation)

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
                .map { param ->
                    val paramName = param.simpleName.toString()

                    val paramIndex = if (param.hasAnnotation<Param>()) {
                        paramsIndex++
                        paramsIndex
                    } else {
                        -1
                    }

                    val namedAnnotation =
                        param.getAnnotationMirrorOrNull<Named>()

                    val qualifierName = namedAnnotation?.get("name")?.value as? String

                    if (namedAnnotation != null && qualifierName!!.isEmpty()) {
                        messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            "@Named must not be empty", param
                        )
                        return@createBindingDescriptor null
                    }

                    val qualifiedAnnotation =
                        param.getAnnotationMirrorOrNull<Qualified>()

                    val qualifierType = qualifiedAnnotation?.getAsType("qualifier")

                    if (qualifiedAnnotation != null
                        && qualifierType!!.asTypeName() == Qualifier::class.asTypeName()
                    ) {
                        messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            "@Qualified must be set", param
                        )
                        return@createBindingDescriptor null
                    }

                    val qualifier = qualifierType
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

                    if ((paramIndex != -1 && qualifier != null)
                        || (paramIndex != -1 && qualifier != null)
                        || (qualifierName != null && qualifier != null)
                    ) {
                        messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            "Only one of @Named, @Param or @Qualified can be annotated per parameter",
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
                        qualifierName,
                        qualifier,
                        paramIndex
                    )
                }
        )
    }

    private fun validateOnlyOneKindAnnotation(elements: Set<Element>) {
        elements
            .filter {
                (it.hasAnnotation<Factory>() && it.hasAnnotation<Single>())
                        || (it.hasAnnotation<Factory>() && it.hasAnnotation<Reusable>())
                        || (it.hasAnnotation<Reusable>() && it.hasAnnotation<Single>())
            }
            .forEach {
                messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "Annotated class can only be annotated with one off @Factory, @Reusable or @Single",
                    it
                )
            }
    }

    private fun validateParameterAnnotations(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>) {
        elementsByAnnotation[Named::class.java].validateHasBuilderAnnotation {
            "@Named can only used in a class which annotated with @Factory, @Reusable or @Single"
        }

        elementsByAnnotation[Param::class.java].validateHasBuilderAnnotation {
            "@Param can only used in a class which annotated with @Factory, @Reusable or @Single"
        }

        elementsByAnnotation[Qualified::class.java].validateHasBuilderAnnotation {
            "@Qualified can only used in a class which annotated with @Factory, @Reusable or @Single"
        }

        elementsByAnnotation[Raw::class.java].validateHasBuilderAnnotation {
            "@Raw can only used in a class which annotated with @Factory, @Reusable or @Single"
        }
    }

    private inline fun Set<Element>.validateHasBuilderAnnotation(message: (Element) -> String) {
        filter {
            val type = it.enclosingElement.enclosingElement
            !type.hasAnnotation<Factory>()
                    && !type.hasAnnotation<Reusable>()
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