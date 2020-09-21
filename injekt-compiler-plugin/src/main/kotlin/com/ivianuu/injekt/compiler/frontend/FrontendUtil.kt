/*
 * Copyright 2020 Manuel Wrage
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

package com.ivianuu.injekt.compiler.frontend

import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

fun Annotated.hasAnnotation(fqName: FqName): Boolean {
    return annotations.hasAnnotation(fqName)
}

fun AnnotationDescriptor.hasAnnotation(annotation: FqName, module: ModuleDescriptor): Boolean {
    val thisFqName = this.fqName ?: return false
    val descriptor =
        module.findClassAcrossModuleDependencies(ClassId.topLevel(thisFqName)) ?: return false
    return descriptor.hasAnnotation(annotation)
}

fun Annotated.hasAnnotatedAnnotations(
    annotation: FqName,
    module: ModuleDescriptor
): Boolean = annotations.any { it.hasAnnotation(annotation, module) }

@Reader
fun Annotated.hasAnnotatedAnnotations(
    annotation: FqName
): Boolean = hasAnnotatedAnnotations(annotation, given())

fun Annotated.getAnnotatedAnnotations(
    annotation: FqName,
    module: ModuleDescriptor
): List<AnnotationDescriptor> =
    annotations.filter {
        it.hasAnnotation(annotation, module)
    }

fun DeclarationDescriptor.isMarkedAsReader(): Boolean =
    hasAnnotation(InjektFqNames.Reader) ||
            hasAnnotation(InjektFqNames.Given) ||
            hasAnnotation(InjektFqNames.GivenMapEntries) ||
            hasAnnotation(InjektFqNames.GivenSetElements) ||
            hasAnnotatedAnnotations(InjektFqNames.Effect, module)

fun DeclarationDescriptor.isReader(): Boolean =
    isMarkedAsReader() ||
            (this is PropertyAccessorDescriptor && correspondingProperty.isReader()) ||
            (this is ClassDescriptor && constructors.any { it.isReader() }) ||
            (this is ConstructorDescriptor && constructedClass.isMarkedAsReader())

fun FunctionDescriptor.getFunctionType(): KotlinType {
    return (if (isSuspend) builtIns.getSuspendFunction(valueParameters.size)
    else builtIns.getFunction(valueParameters.size))
        .defaultType
        .replace(newArguments = valueParameters.map { it.type.asTypeProjection() } + returnType!!.asTypeProjection())
}
