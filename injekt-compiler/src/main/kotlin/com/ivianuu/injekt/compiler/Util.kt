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

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.module

object InjektClassNames {
    val InjektPackage = FqName("com.ivianuu.injekt")
    val Binding = FqName("com.ivianuu.injekt.Binding")
    val Component = FqName("com.ivianuu.injekt.Component")
    val Factory = FqName("com.ivianuu.injekt.Factory")
    val InjektConstructor = FqName("com.ivianuu.injekt.InjektConstructor")
    val Key = FqName("com.ivianuu.injekt.Key")
    val LinkedBinding = FqName("com.ivianuu.injekt.LinkedBinding")
    val Name = FqName("com.ivianuu.injekt.Name")
    val Param = FqName("com.ivianuu.injekt.Param")
    val Parameters = FqName("com.ivianuu.injekt.Parameters")
    val Provider = FqName("com.ivianuu.injekt.Provider")
    val Scope = FqName("com.ivianuu.injekt.Scope")
    val Scoping = FqName("com.ivianuu.injekt.Scoping")
    val Single = FqName("com.ivianuu.injekt.Single")
    val UnlinkedBinding = FqName("com.ivianuu.injekt.UnlinkedBinding")
}

fun DeclarationDescriptor.hasAnnotatedAnnotations(annotation: FqName): Boolean =
    annotations.any { it.hasAnnotation(annotation, module) }

fun DeclarationDescriptor.getAnnotatedAnnotations(annotation: FqName): List<AnnotationDescriptor> =
    annotations.filter { it.hasAnnotation(annotation, module) }

fun AnnotationDescriptor.hasAnnotation(annotation: FqName, module: ModuleDescriptor): Boolean {
    val thisFqName = this.fqName ?: return false
    val descriptor =
        module.findClassAcrossModuleDependencies(ClassId.topLevel(thisFqName)) ?: return false
    return descriptor.annotations.hasAnnotation(annotation)
}
