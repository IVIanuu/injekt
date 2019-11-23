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
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module

fun createBindingDescriptor(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    trace: BindingTrace
): BindingDescriptor? {
    val annotatedType = if (descriptor is ClassDescriptor
        && (descriptor.annotations.hasAnnotation(InjectAnnotation)
                || descriptor.hasAnnotatedAnnotations(ScopeAnnotation))) {
        descriptor
    } else if (descriptor is FunctionDescriptor && descriptor.annotations.hasAnnotation(InjectAnnotation)) {
        descriptor.containingDeclaration as ClassDescriptor
    } else {
        return null
    }

    msg { "process $descriptor annotated type $declaration" }

    if (descriptor is FunctionDescriptor
        && annotatedType.annotations.hasAnnotation(InjectAnnotation)) {
        report(descriptor, trace) { OnlyOneAnnotation }
        return null
    }

    val isInternal = annotatedType.visibility == Visibilities.INTERNAL

    val isObject = annotatedType.kind == ClassKind.OBJECT

    if (annotatedType.visibility != Visibilities.PUBLIC
        && annotatedType.visibility != Visibilities.INTERNAL) {
        report(descriptor, trace) { CannotBePrivate }
        return null
    }

    val scopeAnnotations = annotatedType.annotations.filter {
        it.hasAnnotation(ScopeAnnotation, descriptor.module)
    }

    if (scopeAnnotations.size > 1) {
        report(descriptor, trace) { OnlyOneAnnotation }
        return null
    }

    val scopeName = scopeAnnotations.firstOrNull()?.fqName?.asClassName()

    var currentParamsIndex = -1

    val targetName = annotatedType.fqNameSafe.asClassName()

    val factoryName = ClassName(
        targetName.packageName,
        targetName.simpleName + "__Binding"
    )

    var constructorArgs: List<ArgDescriptor>? = null

    if (!isObject) {
        val constructor = if (descriptor is ConstructorDescriptor) {
            descriptor
        } else {
            (descriptor as ClassDescriptor).unsubstitutedPrimaryConstructor!!
        }

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

                val nameAnnotations = listOfNotNull(param.annotations.findAnnotation(NameAnnotation)) +
                        (param.annotations.filter { it.hasAnnotation(NameAnnotation, descriptor.module) })

                if (nameAnnotations.size > 1) {
                    report(param, trace) { OnlyOneName }
                    return null
                }

                val nameType = nameAnnotations
                    .map { descriptor.module.findClassAcrossModuleDependencies(ClassId.topLevel(it.fqName!!)) }
                    .firstOrNull()
                    ?.annotations
                    ?.findAnnotation(NameAnnotation)
                    ?.allValueArguments
                    ?.getValue(Name.identifier("name"))
                    ?.value
                    ?.let { it as? String }
                    ?.let { ClassName.bestGuess(it) }

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
        targetName,
        factoryName,
        isInternal,
        isObject,
        scopeName,
        constructorArgs ?: emptyList()
    )
}