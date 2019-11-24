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

import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.hasCompanionObject
import org.jetbrains.kotlin.resolve.descriptorUtil.module

fun createBindingDescriptor(
    descriptor: ClassDescriptor,
    trace: BindingTrace
): BindingDescriptor? {
    msg { "process $descriptor" }

    val hasClassInjectAnnotation = descriptor.annotations.hasAnnotation(InjectAnnotation)
    val injectableConstructors = descriptor.constructors.filter {
        it.annotations.hasAnnotation(
            InjectAnnotation
        )
    }

    if (!hasClassInjectAnnotation && injectableConstructors.isEmpty()) return null

    if ((hasClassInjectAnnotation && injectableConstructors.isNotEmpty()) || injectableConstructors.size > 1) {
        report(descriptor, trace) { OnlyOneAnnotation }
        return null
    }

    val isInternal = descriptor.visibility == Visibilities.INTERNAL

    val isObject = descriptor.kind == ClassKind.OBJECT

    if (descriptor.visibility != Visibilities.PUBLIC
        && descriptor.visibility != Visibilities.INTERNAL
    ) {
        report(descriptor, trace) { CannotBePrivate }
        return null
    }

    val scopeAnnotations = descriptor.annotations.filter {
        it.hasAnnotation(ScopeAnnotation, descriptor.module)
    }

    if (scopeAnnotations.size > 1) {
        report(descriptor, trace) { OnlyOneAnnotation }
        return null
    }

    val scopeAnnotation = scopeAnnotations
        .map {
            descriptor.module.findClassAcrossModuleDependencies(
                ClassId.topLevel(it.fqName!!)
            )!!
        }
        .first()

    if (!scopeAnnotation.hasCompanionObject) {
        report(scopeAnnotation, trace) { NeedsACompanionObject }
        return null
    }

    val scopeType = scopeAnnotation.companionObjectDescriptor!!.asClassName()

    var currentParamsIndex = -1

    val className = descriptor.asClassName()

    val factoryName = ClassName(
        className.packageName,
        className.simpleName + "__Binding"
    )

    val constructorArgs: List<ArgDescriptor>?

    if (isObject) {
        constructorArgs = null
    } else {
        val constructor = injectableConstructors.firstOrNull()
            ?: descriptor.constructors.first()

        if (constructor.visibility != Visibilities.PUBLIC
            && constructor.visibility != Visibilities.INTERNAL) {
            report(constructor, trace) { CannotBePrivate }
            return null
        }

        constructorArgs = constructor
            .valueParameters
            .map { param ->
                val paramName = param.name.asString()

                val paramIndex: Int? = if (param.annotations.hasAnnotation(ParamAnnotation)) {
                    ++currentParamsIndex
                } else {
                    null
                }

                val nameAnnotations =
                    param.annotations.filter { it.hasAnnotation(NameAnnotation, descriptor.module) }

                if (nameAnnotations.size > 1) {
                    report(param, trace) { OnlyOneName }
                    return null
                }

                val nameType = if (nameAnnotations.isNotEmpty()) {
                    val namedAnnotation = nameAnnotations
                        .map {
                            descriptor.module.findClassAcrossModuleDependencies(
                                ClassId.topLevel(it.fqName!!)
                            )!!
                        }
                        .first()

                    if (!namedAnnotation.hasCompanionObject) {
                        report(namedAnnotation, trace) { NeedsACompanionObject }
                        return null
                    }

                    namedAnnotation.companionObjectDescriptor!!.asClassName()
                } else null

                if (paramIndex != null && nameType != null) {
                    report(param, trace) { EitherNameOrParam }
                    return null
                }

                val paramType = param.type.asTypeName()

                if (paramIndex != null) {
                    ArgDescriptor.Parameter(paramName, paramIndex)
                } else {
                    ArgDescriptor.Dependency(paramName, paramType, nameType)
                }
            }
    }

    return BindingDescriptor(
        className,
        factoryName,
        isInternal,
        isObject,
        scopeType,
        constructorArgs ?: emptyList()
    )
}