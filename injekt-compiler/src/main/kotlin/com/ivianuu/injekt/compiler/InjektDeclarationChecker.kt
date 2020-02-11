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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.js.descriptorUtils.hasPrimaryConstructor
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext

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
        if (descriptor.annotations.hasAnnotation(InjektClassNames.Factory) &&
            descriptor.annotations.hasAnnotation(InjektClassNames.Single)
        ) {
            report(
                descriptor,
                context.trace
            ) { EitherFactoryOrSingle }
        }

        if (descriptor is ClassDescriptor &&
            (descriptor.annotations.hasAnnotation(InjektClassNames.Name) ||
                    descriptor.annotations.hasAnnotation(InjektClassNames.Scope)) &&
            descriptor.companionObjectDescriptor == null
        ) {
            report(
                descriptor,
                context.trace
            ) { NeedsACompanionObject }
        }

        if (descriptor.getAnnotatedAnnotations(InjektClassNames.Scope).size > 1) {
            report(
                descriptor,
                context.trace
            ) { OnlyOneScope }
        }

        if (descriptor.getAnnotatedAnnotations(InjektClassNames.Name).size > 1) {
            report(
                descriptor,
                context.trace
            ) { OnlyOneName }
        }

        if (descriptor.annotations.hasAnnotation(InjektClassNames.Param) &&
            descriptor.annotations.hasAnnotation(InjektClassNames.Name)
        ) {
            report(
                descriptor,
                context.trace
            ) { ParamCannotBeNamed }
        }

        if (descriptor is ClassDescriptor && descriptor.constructors.filter {
                it.annotations.hasAnnotation(
                    InjektClassNames.InjektConstructor
                )
            }.size > 1) {
            report(
                descriptor,
                context.trace
            ) { OnlyOneInjektConstructor }
        }

        if (descriptor is ClassDescriptor && (descriptor.annotations.hasAnnotation(InjektClassNames.Factory) ||
                    descriptor.annotations.hasAnnotation(InjektClassNames.Single)) &&
            !descriptor.hasPrimaryConstructor() &&
            descriptor.constructors.none { it.annotations.hasAnnotation(InjektClassNames.InjektConstructor) }
        ) {
            report(
                descriptor,
                context.trace
            ) { NeedsPrimaryConstructorOrAnnotation }
        }
    }
}