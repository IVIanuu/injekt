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
    val BoundBehavior = FqName("com.ivianuu.injekt.BoundBehavior")
    val Component = FqName("com.ivianuu.injekt.Component")
    val ComponentBuilder = FqName("com.ivianuu.injekt.ComponentBuilder")
    val ComponentBuilderContributor = FqName("com.ivianuu.injekt.ComponentBuilderContributor")
    val DuplicateStrategy = FqName("com.ivianuu.injekt.DuplicateStrategy")
    val Injekt = FqName("com.ivianuu.injekt.Injekt")
    val InjektConstructor = FqName("com.ivianuu.injekt.InjektConstructor")
    val IntoComponent = FqName("com.ivianuu.injekt.IntoComponent")
    val Key = FqName("com.ivianuu.injekt.Key")
    val KeyOverload = FqName("com.ivianuu.injekt.KeyOverload")
    val Param = FqName("com.ivianuu.injekt.Param")
    val Parameters = FqName("com.ivianuu.injekt.Parameters")
    val Qualifier = FqName("com.ivianuu.injekt.Qualifier")
    val QualifierMarker = FqName("com.ivianuu.injekt.QualifierMarker")
    val Scope = FqName("com.ivianuu.injekt.Scope")
    val ScopeMarker = FqName("com.ivianuu.injekt.ScopeMarker")
    val Tag = FqName("com.ivianuu.injekt.Tag")
    val TagMarker = FqName("com.ivianuu.injekt.TagMarker")
}

fun DeclarationDescriptor.hasAnnotatedAnnotations(annotation: FqName): Boolean =
    annotations.any { it.hasAnnotation(annotation, module) }

fun DeclarationDescriptor.getAnnotatedAnnotations(annotation: FqName): List<AnnotationDescriptor> =
    annotations.filter {
        it.hasAnnotation(annotation, module)
    }

fun AnnotationDescriptor.hasAnnotation(annotation: FqName, module: ModuleDescriptor): Boolean {
    val thisFqName = this.fqName ?: return false
    val descriptor =
        module.findClassAcrossModuleDependencies(ClassId.topLevel(thisFqName)) ?: return false
    return descriptor.annotations.hasAnnotation(annotation)
}
