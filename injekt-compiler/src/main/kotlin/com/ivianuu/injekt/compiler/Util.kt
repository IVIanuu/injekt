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
    val Behavior = FqName("com.ivianuu.injekt.Behavior")
    val Binding = FqName("com.ivianuu.injekt.Binding")
    val BindingFactory = FqName("com.ivianuu.injekt.BindingFactory")
    val BoundBehavior = FqName("com.ivianuu.injekt.BoundBehavior")
    val Component = FqName("com.ivianuu.injekt.Component")
    val Factory = FqName("com.ivianuu.injekt.Factory")
    val InjektConstructor = FqName("com.ivianuu.injekt.InjektConstructor")
    val Key = FqName("com.ivianuu.injekt.Key")
    val Param = FqName("com.ivianuu.injekt.Param")
    val Parameters = FqName("com.ivianuu.injekt.Parameters")
    val Qualifier = FqName("com.ivianuu.injekt.Qualifier")
    val QualifierMarker = FqName("com.ivianuu.injekt.QualifierMarker")
    val ScopeMarker = FqName("com.ivianuu.injekt.ScopeMarker")
    val Single = FqName("com.ivianuu.injekt.Single")
    val SingleBehavior = FqName("com.ivianuu.injekt.SingleBehavior")
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
