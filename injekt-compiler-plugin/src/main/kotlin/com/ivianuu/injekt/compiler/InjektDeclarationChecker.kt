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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.js.descriptorUtils.hasPrimaryConstructor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class InjektStorageComponentContainerContributorExtension : StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        super.registerModuleComponents(container, platform, moduleDescriptor)
        container.useInstance(InjektDeclarationChecker(moduleDescriptor))
    }
}

class InjektDeclarationChecker(
    private val moduleDescriptor: ModuleDescriptor
) : DeclarationChecker {

    private val behavior by lazy {
        moduleDescriptor.findClassAcrossModuleDependencies(
            ClassId.topLevel(InjektClassNames.Behavior)
        )!!
    }
    private val key by lazy {
        moduleDescriptor.findClassAcrossModuleDependencies(
            ClassId.topLevel(InjektClassNames.Key)
        )!!
    }
    private val module by lazy {
        moduleDescriptor.findClassAcrossModuleDependencies(
            ClassId.topLevel(InjektClassNames.Module)
        )!!
    }
    private val qualifier by lazy {
        moduleDescriptor.findClassAcrossModuleDependencies(
            ClassId.topLevel(InjektClassNames.Qualifier)
        )!!
    }
    private val scope by lazy {
        moduleDescriptor.findClassAcrossModuleDependencies(
            ClassId.topLevel(InjektClassNames.Scope)
        )!!
    }

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor.getSyntheticAnnotationPropertiesOfType(scope.defaultType).size > 1) {
            context.trace.report(InjektErrors.OnlyOneScope.on(declaration))
        }

        if (descriptor.annotations.hasAnnotation(InjektClassNames.Param) &&
            descriptor.getSyntheticAnnotationPropertiesOfType(qualifier.defaultType)
                .isNotEmpty()
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
            descriptor.getSyntheticAnnotationPropertiesOfType(behavior.defaultType)
                .isNotEmpty() &&
            !descriptor.hasPrimaryConstructor() &&
            descriptor.constructors.none { it.annotations.hasAnnotation(InjektClassNames.InjektConstructor) }
        ) {
            context.trace.report(InjektErrors.NeedsPrimaryConstructorOrAnnotation.on(declaration))
        }

        if (descriptor is FunctionDescriptor &&
            descriptor.annotations.hasAnnotation(InjektClassNames.KeyOverload)
        ) {
            if (descriptor.typeParameters.isEmpty() ||
                descriptor.valueParameters.none { valueParameter ->
                    valueParameter.type.constructor.declarationDescriptor == key
                }
            ) {
                context.trace.report(
                    InjektErrors.InvalidKeyOverload.on(
                        declaration
                    )
                )
            }
        }

        if (descriptor is PropertyDescriptor) {
            fun checkTypes(
                annotation: FqName,
                type: KotlinType
            ) {
                if (descriptor.annotations.hasAnnotation(annotation)) {
                    if (!descriptor.type.isSubtypeOf(type)) {
                        context.trace.report(InjektErrors.InvalidType.on(declaration))
                    }
                    if (descriptor.containingDeclaration !is PackageFragmentDescriptor ||
                        descriptor.extensionReceiverParameter != null
                    ) {
                        context.trace.report(InjektErrors.MustBeStatic.on(declaration))
                    }
                }
            }

            checkTypes(InjektClassNames.ModuleMarker, module.defaultType)
            checkTypes(InjektClassNames.BehaviorMarker, behavior.defaultType)
            checkTypes(InjektClassNames.GenerateDslBuilder, behavior.defaultType)
            checkTypes(InjektClassNames.QualifierMarker, qualifier.defaultType)
            checkTypes(InjektClassNames.ScopeMarker, scope.defaultType)
        }
    }
}
