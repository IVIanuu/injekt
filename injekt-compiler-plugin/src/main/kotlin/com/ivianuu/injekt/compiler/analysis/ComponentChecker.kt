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
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.types.typeUtil.supertypes

class ComponentChecker : DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor !is ClassDescriptor) return

        if (descriptor.hasAnnotation(InjektFqNames.Component)) {
            if (descriptor.kind != ClassKind.INTERFACE) {
                context.trace.report(
                    InjektErrors.COMPONENT_MUST_BE_AN_INTERFACE
                        .on(declaration)
                )
            }

            if (descriptor.declaredTypeParameters.isNotEmpty()) {
                context.trace.report(
                    InjektErrors.COMPONENT_WITH_TYPE_PARAMETERS
                        .on(declaration)
                )
            }

            if (descriptor.getAllDeclarations().isNotEmpty()) {
                context.trace.report(
                    InjektErrors.COMPONENT_MUST_BE_EMPTY
                        .on(declaration)
                )
            }
        }
    }

    private fun ClassDescriptor.getAllDeclarations(): Set<DeclarationDescriptor> {
        val declarations = mutableSetOf<DeclarationDescriptor>()
        fun ClassDescriptor.collect() {
            declarations += unsubstitutedMemberScope.getContributedDescriptors()
                .filter {
                    (it is FunctionDescriptor
                            && it.dispatchReceiverParameter?.type?.isAnyOrNullableAny() != true)
                            || it is PropertyDescriptor
                }
            defaultType.supertypes()
                .mapNotNull { it.constructor.declarationDescriptor as? ClassDescriptor }
                .forEach { it.collect() }
        }

        collect()

        return declarations
    }

}
