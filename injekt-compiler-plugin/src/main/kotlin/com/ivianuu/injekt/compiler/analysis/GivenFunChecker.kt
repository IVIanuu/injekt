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
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class GivenFunChecker : DeclarationChecker {
    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
    ) {
        if (descriptor !is SimpleFunctionDescriptor) return
        if (!descriptor.hasAnnotation(InjektFqNames.GivenFun)) return

        declaration as KtNamedFunction
        if (!declaration.hasDeclaredReturnType() && !declaration.hasBlockBody()) {
            context.trace.report(
                InjektErrors.GIVEN_FUN_WITHOUT_EXPLICIT_RETURN_TYPE
                    .on(declaration)
            )
        }

        if (descriptor.containingDeclaration is ClassDescriptor) {
            context.trace.report(
                InjektErrors.GIVEN_FUN_AS_MEMBER
                    .on(declaration)
            )
        } else {
            val functionsWithSameName = descriptor.module.getPackage(descriptor.findPackage().fqName)
                .memberScope
                .getContributedFunctions(descriptor.name, NoLookupLocation.FROM_BACKEND)
                .filter { it.hasAnnotation(InjektFqNames.GivenFun) }
            if (functionsWithSameName.size > 1) {
                context.trace.report(
                    InjektErrors.GIVEN_FUN_MUST_HAVE_UNIQUE_NAME
                        .on(declaration)
                )
            }
        }
    }
}
