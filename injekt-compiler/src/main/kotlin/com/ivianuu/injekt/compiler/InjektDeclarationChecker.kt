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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class InjektDeclarationChecker : DeclarationChecker {

    private val injectables = mutableListOf<BindingDescriptor>()

    init {
        generateNotifier = { generate() }
    }

    private fun generate() {
        injectables
            .map { BindingGenerator(it) }
            .map { it.generate() }
            .forEach {
                // todo write
            }
    }

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor !is ClassDescriptor) return

        msg {
            "is injectable $descriptor ? ${descriptor.isInjectable()} has inject ${descriptor.annotations.hasAnnotation(
                InjectAnnotation
            )} has annotated ${descriptor.hasAnnotatedAnnotations(ScopeAnnotation)}"
        }

        if (!descriptor.isInjectable()) return

    }
}

val InjectAnnotation = FqName("com.ivianuu.injekt.Inject")
val NameAnnotation = FqName("com.ivianuu.injekt.Name")
val ScopeAnnotation = FqName("com.ivianuu.injekt.Scope")

fun DeclarationDescriptor.isInjectable() = annotations.hasAnnotation(InjectAnnotation)
        || hasAnnotatedAnnotations(ScopeAnnotation)

fun DeclarationDescriptor.hasAnnotatedAnnotations(annotation: FqName): Boolean =
    annotations.any { it.isScope(module) }

fun AnnotationDescriptor.isScope(module: ModuleDescriptor): Boolean {
    val descriptor = module.findClassAcrossModuleDependencies(ClassId.topLevel(fqName!!))!!
    return descriptor.annotations.hasAnnotation(ScopeAnnotation)
}