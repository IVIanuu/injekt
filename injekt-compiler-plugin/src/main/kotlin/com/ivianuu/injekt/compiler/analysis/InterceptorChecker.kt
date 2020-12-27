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
import com.ivianuu.injekt.compiler.resolution.callContext
import com.ivianuu.injekt.compiler.resolution.contributionKind
import com.ivianuu.injekt.compiler.resolution.providerType
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt.compiler.resolution.typeWith
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class InterceptorChecker : DeclarationChecker {
    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (!descriptor.hasAnnotation(InjektFqNames.Interceptor) &&
            (descriptor !is ValueDescriptor ||
                    !descriptor.type.hasAnnotation(InjektFqNames.Interceptor)))
                        return

        if (descriptor is FunctionDescriptor) {
            val providerType = descriptor.callContext.providerType(descriptor.module)
                .typeWith(listOf(descriptor.returnType!!.toTypeRef()))
            val factoryParameter = descriptor
                .valueParameters
                .singleOrNull { it.type.toTypeRef() == providerType }
            if (factoryParameter == null) {
                context.trace.report(
                    InjektErrors.INTERCEPTOR_WITHOUT_FACTORY_PARAMETER
                        .on(declaration)
                )
            }
            descriptor
                .valueParameters
                .filter { it != factoryParameter }
                .filter { it.contributionKind() == null }
                .forEach {
                    context.trace.report(
                        InjektErrors.NON_GIVEN_PARAMETER_ON_GIVEN_DECLARATION
                            .on(
                                it.findPsi() ?: declaration,
                                InjektFqNames.Interceptor.shortName()
                            )
                    )
                }
        } else if (descriptor is ValueDescriptor) {
            val providerType = descriptor.type.toTypeRef()
                .callContext
                .providerType(descriptor.module)
                .typeWith(listOf(descriptor.type.arguments.last().type.toTypeRef()))
            val factoryParameter = descriptor
                .type
                .toTypeRef()
                .typeArguments
                .dropLast(1)
                .singleOrNull { it == providerType }
            if (factoryParameter == null) {
                context.trace.report(
                    InjektErrors.INTERCEPTOR_WITHOUT_FACTORY_PARAMETER
                        .on(declaration)
                )
            }
            descriptor
                .type
                .toTypeRef()
                .typeArguments
                .dropLast(1)
                .filter { it != factoryParameter }
                .filter { it.contributionKind == null }
                .forEach {
                    context.trace.report(
                        InjektErrors.NON_GIVEN_PARAMETER_ON_GIVEN_DECLARATION
                            .on(declaration, InjektFqNames.Interceptor.shortName())
                    )
                }
        }
    }
}
