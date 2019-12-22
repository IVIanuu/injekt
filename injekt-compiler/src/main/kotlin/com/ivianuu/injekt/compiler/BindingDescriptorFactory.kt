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
    if (descriptor.defaultType.asTypeName() == null) return null

    msg { "process $descriptor" }

    val isFactory = descriptor.annotations.hasAnnotation(FactoryAnnotation)
    val isSingle = descriptor.annotations.hasAnnotation(SingleAnnotation)

    if (!isFactory && !isSingle) return null
    if (isFactory && isSingle) {
        report(descriptor, trace) { EitherFactoryOrSingle }
        return null
    }

    val isInternal = descriptor.visibility == Visibilities.INTERNAL

    val isObject = descriptor.kind == ClassKind.OBJECT

    if (descriptor.visibility != Visibilities.PUBLIC &&
        descriptor.visibility != Visibilities.INTERNAL
    ) {
        report(descriptor, trace) { CannotBePrivate }
        return null
    }

    val scopeAnnotations = descriptor.annotations.filter {
        it.hasAnnotation(ScopeAnnotation, descriptor.module)
    }

    if (scopeAnnotations.size > 1) {
        report(descriptor, trace) { OnlyOneScope }
        return null
    }

    val scopeAnnotation = if (scopeAnnotations.isEmpty()) null else {
        scopeAnnotations
            .map {
                descriptor.module.findClassAcrossModuleDependencies(
                    ClassId.topLevel(it.fqName!!)
                )!!
            }
            .first()
    }

    if (scopeAnnotation != null && !scopeAnnotation.hasCompanionObject) {
        report(scopeAnnotation, trace) { NeedsACompanionObject }
        return null
    }

    val scopeType = if (scopeAnnotation != null)
        scopeAnnotation.companionObjectDescriptor!!.asClassName() else null

    var currentParamsIndex = -1

    val className = descriptor.asClassName()

    val bindingName = ClassName(
        className.packageName,
        className.simpleName + "__Binding"
    )

    val constructorArgs: List<ArgDescriptor>?

    if (isObject) {
        constructorArgs = null
    } else {
        val constructor = descriptor.constructors
            .firstOrNull { it.annotations.hasAnnotation(InjektConstructorAnnotation) }
            ?: descriptor.constructors.first()

        if (constructor.visibility != Visibilities.PUBLIC &&
            constructor.visibility != Visibilities.INTERNAL
        ) {
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
                    report(param, trace) { ParamCannotBeNamed }
                    return null
                }

                val isOptional = param.annotations.hasAnnotation(OptionalAnnotation)

                if (paramIndex != null && isOptional) {
                    report(param, trace) { ParamCannotBeOptional }
                    return null
                }

                val paramType = param.type.asTypeName() ?: return null

                if (paramIndex != null) {
                    ArgDescriptor.Parameter(
                        argName = paramName,
                        index = paramIndex
                    )
                } else {
                    ArgDescriptor.Dependency(
                        argName = paramName,
                        isOptional = isOptional,
                        paramType = paramType,
                        qualifierName = nameType
                    )
                }
            }
    }

    return BindingDescriptor(
        target = className,
        bindingName = bindingName,
        isInternal = isInternal,
        isObject = isObject,
        isSingle = isSingle,
        scope = scopeType,
        constructorArgs = constructorArgs ?: emptyList()
    )
}
