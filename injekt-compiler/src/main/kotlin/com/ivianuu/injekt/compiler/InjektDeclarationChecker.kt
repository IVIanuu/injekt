
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

import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.js.descriptorUtils.hasPrimaryConstructor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class InjektStorageComponentContainerContributorExtension : StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        super.registerModuleComponents(container, platform, moduleDescriptor)
        container.useInstance(InjektDeclarationChecker())
    }
}

class InjektDeclarationChecker : DeclarationChecker {
    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor.getAnnotatedAnnotations(KindMarkerAnnotation).size > 1) {
            report(
                descriptor,
                context.trace
            ) { OnlyOneKind }
        }

        if (descriptor is ClassDescriptor &&
            (descriptor.annotations.hasAnnotation(NameAnnotation) ||
                    descriptor.annotations.hasAnnotation(ScopeAnnotation)) &&
                descriptor.companionObjectDescriptor == null) {
            report(
                descriptor,
                context.trace
            ) { NeedsACompanionObject }
        }

        if (descriptor.getAnnotatedAnnotations(ScopeAnnotation).size > 1) {
            report(
                descriptor,
                context.trace
            ) { OnlyOneScope }
        }

        if (descriptor.getAnnotatedAnnotations(NameAnnotation).size > 1) {
            report(
                descriptor,
                context.trace
            ) { OnlyOneName }
        }

        if (descriptor.annotations.hasAnnotation(ParamAnnotation) &&
            descriptor.annotations.hasAnnotation(NameAnnotation)) {
            report(
                descriptor,
                context.trace
            ) { ParamCannotBeNamed }
        }

        if (descriptor is ClassDescriptor && descriptor.constructors.filter {
                it.annotations.hasAnnotation(
                    InjektConstructorAnnotation
                )
            }.size > 1) {
            report(
                descriptor,
                context.trace
            ) { OnlyOneInjektConstructor }
        }

        if (descriptor is ClassDescriptor &&
            (descriptor.hasAnnotatedAnnotations(KindMarkerAnnotation)) &&
            !descriptor.hasPrimaryConstructor() &&
            descriptor.constructors.none { it.annotations.hasAnnotation(InjektConstructorAnnotation) }
        ) {
            report(
                descriptor,
                context.trace
            ) { NeedsPrimaryConstructorOrAnnotation }
        }

        if (descriptor.annotations.hasAnnotation(KindMarkerAnnotation)) {
            val kindType = descriptor.annotations.findAnnotation(KindMarkerAnnotation)!!
                .allValueArguments[Name.identifier("type")]!!.getType(descriptor.module)
            val kindDescriptor = kindType.constructor.declarationDescriptor!! as ClassDescriptor
            if (kindDescriptor.kind != ClassKind.OBJECT) {
                report(
                    descriptor,
                    context.trace
                ) { MustBeAObject }
            }
        }
    }
}