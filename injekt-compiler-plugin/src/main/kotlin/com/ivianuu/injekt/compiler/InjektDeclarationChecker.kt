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
import org.jetbrains.kotlin.descriptors.CallableDescriptor
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
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
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
        if (descriptor.getSyntheticAnnotationDeclarationsOfType(scope.defaultType).size > 1) {
            context.trace.report(InjektErrors.OnlyOneScope.on(declaration))
        }

        if (descriptor.annotations.hasAnnotation(InjektClassNames.Param) &&
            descriptor.getSyntheticAnnotationDeclarationsOfType(qualifier.defaultType)
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
            descriptor.getSyntheticAnnotationDeclarationsOfType(behavior.defaultType)
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

        fun DeclarationDescriptor.checkStatic() {
            if (containingDeclaration !is PackageFragmentDescriptor ||
                (this is CallableDescriptor && extensionReceiverParameter != null)
            ) {
                context.trace.report(InjektErrors.MustBeStatic.on(declaration))
            }
        }

        fun DeclarationDescriptor.checkAnnotationSupportedParams() {
            if (this is FunctionDescriptor) {
                valueParameters.forEach {
                    if (!it.type.isAcceptableTypeForAnnotationParameter()) {
                        context.trace.report(InjektErrors.NotAValidAnnotationType.on(it.findPsi()!!))
                    }
                }
            }
        }

        if (descriptor is PropertyDescriptor &&
            descriptor.annotations.hasAnnotation(InjektClassNames.ModuleMarker)
        ) {
            if (!descriptor.type.isSubtypeOf(module.defaultType)) {
                context.trace.report(InjektErrors.MustBeAModule.on(declaration))
            }
            descriptor.checkStatic()
        }

        if (descriptor is PropertyDescriptor &&
            (descriptor.annotations.hasAnnotation(InjektClassNames.BehaviorMarker) ||
                    descriptor.annotations.hasAnnotation(InjektClassNames.GenerateDslBuilder))
        ) {
            if (!descriptor.type.isSubtypeOf(behavior.defaultType)) {
                context.trace.report(InjektErrors.MustBeABehavior.on(declaration))
            }
            descriptor.checkStatic()
        }

        if (descriptor is PropertyDescriptor &&
            descriptor.annotations.hasAnnotation(InjektClassNames.QualifierMarker)
        ) {
            if (!descriptor.type.isSubtypeOf(qualifier.defaultType)) {
                context.trace.report(InjektErrors.MustBeAQualifier.on(declaration))
            }
            descriptor.checkStatic()
        }

        if (descriptor is PropertyDescriptor &&
            descriptor.annotations.hasAnnotation(InjektClassNames.ScopeMarker)
        ) {
            if (!descriptor.type.isSubtypeOf(scope.defaultType)) {
                context.trace.report(InjektErrors.MustBeAScope.on(declaration))
            }
            descriptor.checkStatic()
        }

        if (descriptor is FunctionDescriptor &&
            (descriptor.annotations.hasAnnotation(InjektClassNames.BehaviorMarker) ||
                    descriptor.annotations.hasAnnotation(InjektClassNames.GenerateDslBuilder))
        ) {
            if (!descriptor.returnType!!.isSubtypeOf(behavior.defaultType)) {
                context.trace.report(InjektErrors.MustBeABehavior.on(declaration))
            }
            descriptor.checkStatic()
            descriptor.checkAnnotationSupportedParams()
        }

        if (descriptor is FunctionDescriptor &&
            descriptor.annotations.hasAnnotation(InjektClassNames.QualifierMarker)
        ) {
            if (!descriptor.returnType!!.isSubtypeOf(qualifier.defaultType)) {
                context.trace.report(InjektErrors.MustBeAQualifier.on(declaration))
            }
            descriptor.checkStatic()
            descriptor.checkAnnotationSupportedParams()
        }

        if (descriptor is FunctionDescriptor &&
            descriptor.annotations.hasAnnotation(InjektClassNames.ScopeMarker)
        ) {
            if (!descriptor.returnType!!.isSubtypeOf(scope.defaultType)) {
                context.trace.report(InjektErrors.MustBeAScope.on(declaration))
            }
            descriptor.checkStatic()
            descriptor.checkAnnotationSupportedParams()

        }
    }
}
