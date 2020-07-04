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

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class BindingEffectChecker : DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor !is ClassDescriptor) return

        if (descriptor.hasAnnotation(InjektFqNames.BindingAdapter) ||
            descriptor.hasAnnotation(InjektFqNames.BindingEffect)
        ) {
            if (descriptor.hasAnnotation(InjektFqNames.BindingAdapter) &&
                descriptor.hasAnnotation(InjektFqNames.BindingEffect)
            ) {
                context.trace.report(
                    InjektErrors.EITHER_BINDING_ADAPTER_OR_BINDING_EFFECT
                        .on(declaration)
                )
            }

            val companion = descriptor.companionObjectDescriptor
            if (companion == null) {
                context.trace.report(
                    InjektErrors.BINDING_ADAPTER_WITHOUT_COMPANION
                        .on(declaration)
                )
                return
            }

            val moduleFunction = companion.unsubstitutedMemberScope
                .getContributedDescriptors()
                .filterIsInstance<FunctionDescriptor>()
                .singleOrNull { it.hasAnnotation(InjektFqNames.Module) }

            if (moduleFunction == null) {
                context.trace.report(
                    InjektErrors.BINDING_EFFECT_COMPANION_WITHOUT_MODULE
                        .on(declaration)
                )
                return
            }

            if (moduleFunction.typeParameters.size != 1) {
                context.trace.report(
                    InjektErrors.BINDING_EFFECT_MODULE_NEEDS_1_TYPE_PARAMETER
                        .on(declaration)
                )
                return
            }

            if (moduleFunction.valueParameters.isNotEmpty()) {
                context.trace.report(
                    InjektErrors.BINDING_EFFECT_MODULE_CANNOT_HAVE_VALUE_PARAMETERS
                        .on(declaration)
                )
                return
            }

            val installInComponent = (descriptor.annotations
                .findAnnotation(InjektFqNames.BindingAdapter)
                ?: descriptor.annotations.findAnnotation(InjektFqNames.BindingEffect)!!)
                .allValueArguments
                .values
                .single()
                .let { it as KClassValue }
                .getArgumentType(descriptor.module)
                .constructor.declarationDescriptor

            if (installInComponent?.annotations
                    ?.hasAnnotation(InjektFqNames.CompositionComponent) != true
            ) {
                context.trace.report(
                    InjektErrors.NOT_A_COMPOSITION_COMPONENT
                        .on(declaration)
                )
            }
        }

        if (descriptor.hasAnnotatedAnnotations(InjektFqNames.BindingAdapter, descriptor.module) ||
            descriptor.hasAnnotatedAnnotations(InjektFqNames.BindingEffect, descriptor.module)
        ) {
            if (descriptor.getAnnotatedAnnotations(
                    InjektFqNames.BindingAdapter,
                    descriptor.module
                ).size > 1
            ) {
                context.trace.report(InjektErrors.MULTIPLE_BINDING_ADAPTER.on(declaration))
            }
            if (descriptor.hasAnnotatedAnnotations(
                    InjektFqNames.BindingAdapter,
                    descriptor.module
                ) &&
                (descriptor.hasAnnotation(InjektFqNames.Unscoped) ||
                        descriptor.hasAnnotation(InjektFqNames.Scoped))
            ) {
                context.trace.report(
                    InjektErrors.BINDING_ADAPTER_WITH_UNSCOPED_OR_SCOPED
                        .on(declaration)
                )
            }

            if (descriptor.hasAnnotatedAnnotations(
                    InjektFqNames.BindingEffect,
                    descriptor.module
                ) &&
                !descriptor.hasAnnotatedAnnotations(
                    InjektFqNames.BindingAdapter,
                    descriptor.module
                ) &&
                !descriptor.hasAnnotation(InjektFqNames.Unscoped) &&
                !descriptor.hasAnnotation(InjektFqNames.Scoped)
            ) {
                context.trace.report(
                    InjektErrors.BINDING_EFFECT_WITHOUT_UNSCOPED_OR_SCOPED
                        .on(declaration)
                )
            }

            val effectAnnotations = listOfNotNull(
                descriptor.getAnnotatedAnnotations(InjektFqNames.BindingAdapter, descriptor.module)
                    .singleOrNull()
            ) + descriptor.getAnnotatedAnnotations(InjektFqNames.BindingEffect, descriptor.module)

            val upperBounds = effectAnnotations
                .mapNotNull {
                    it.type
                        .constructor
                        .declarationDescriptor
                        .let { it as ClassDescriptor }
                        .companionObjectDescriptor
                    ?.unsubstitutedMemberScope
                    ?.getContributedDescriptors()
                    ?.filterIsInstance<FunctionDescriptor>()
                        ?.singleOrNull { it.hasAnnotation(InjektFqNames.Module) }
                    ?.typeParameters
                    ?.singleOrNull()
                    ?.upperBounds
                    ?.singleOrNull()
                }


            upperBounds.forEach { upperBound ->
                if (!descriptor.defaultType.isSubtypeOf(upperBound)) {
                    context.trace.report(
                        InjektErrors.NOT_IN_BINDING_EFFECT_BOUNDS
                            .on(declaration)
                    )
                }
            }
        }
    }

}
