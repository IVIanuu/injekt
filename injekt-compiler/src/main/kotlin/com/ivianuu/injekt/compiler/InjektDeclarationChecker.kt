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
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.js.descriptorUtils.hasPrimaryConstructor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

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
        if (descriptor is ClassDescriptor &&
            descriptor.annotations.hasAnnotation(InjektClassNames.ScopeMarker) &&
            descriptor.companionObjectDescriptor == null
        ) {
            context.trace.report(InjektErrors.NeedsAScopeCompanionObject.on(declaration))
        }

        if (descriptor is ClassDescriptor &&
            descriptor.annotations.hasAnnotation(InjektClassNames.QualifierMarker) &&
            (descriptor.companionObjectDescriptor == null ||
                    !descriptor.companionObjectDescriptor!!.defaultType.isSubtypeOf(
                        descriptor.module.findClassAcrossModuleDependencies(
                            ClassId.topLevel(
                                InjektClassNames.Qualifier
                            )
                        )!!
                            .defaultType
                    ))
        ) {
            context.trace.report(InjektErrors.NeedsAQualifierCompanionObject.on(declaration))
        }

        if (descriptor is ClassDescriptor &&
            descriptor.annotations.hasAnnotation(InjektClassNames.TagMarker) &&
            (descriptor.companionObjectDescriptor == null ||
                    !descriptor.companionObjectDescriptor!!.defaultType.isSubtypeOf(
                        descriptor.module.findClassAcrossModuleDependencies(
                            ClassId.topLevel(
                                InjektClassNames.Tag
                            )
                        )!!
                            .defaultType
                    ))
        ) {
            context.trace.report(InjektErrors.NeedsATagCompanionObject.on(declaration))
        }

        if (descriptor.getAnnotatedAnnotations(InjektClassNames.ScopeMarker).size > 1) {
            context.trace.report(InjektErrors.OnlyOneScope.on(declaration))
        }

        if (descriptor.annotations.hasAnnotation(InjektClassNames.Param) &&
            descriptor.annotations.hasAnnotation(InjektClassNames.Qualifier)
        ) {
            context.trace.report(InjektErrors.ParamCannotBeNamed.on(declaration))
        }

        if (descriptor is ClassDescriptor && descriptor.constructors.filter {
                it.annotations.hasAnnotation(InjektClassNames.InjektConstructor)
            }.size > 1) {
            context.trace.report(InjektErrors.OnlyOneInjektConstructor.on(declaration))
        }

        if (descriptor is ClassDescriptor &&
            descriptor.kind != ClassKind.OBJECT &&
            descriptor.getAnnotatedAnnotations(InjektClassNames.TagMarker).isNotEmpty() &&
            !descriptor.hasPrimaryConstructor() &&
            descriptor.constructors.none { it.annotations.hasAnnotation(InjektClassNames.InjektConstructor) }
        ) {
            context.trace.report(InjektErrors.NeedsPrimaryConstructorOrAnnotation.on(declaration))
        }
    }
}
