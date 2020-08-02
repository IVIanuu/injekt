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
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns

class ScopingChecker : DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor !is ClassDescriptor ||
            !descriptor.hasAnnotation(InjektFqNames.Scoping)
        ) return

        if (descriptor.kind != ClassKind.OBJECT) {
            context.trace.report(
                InjektErrors.SCOPING_MUST_BE_AN_OBJECT
                    .on(declaration)
            )
        }

        fun reportCorrectFunctionMissing() {
            context.trace.report(
                InjektErrors.SCOPING_EXACT_ONE_FUNCTION
                    .on(declaration)
            )
        }

        val function = descriptor.unsubstitutedMemberScope
            .getContributedDescriptors()
            .filterIsInstance<FunctionDescriptor>()
            .filter {
                it.dispatchReceiverParameter?.type == descriptor.defaultType
            }
            .singleOrNull()

        if (function == null) {
            reportCorrectFunctionMissing()
            return
        }

        if (!function.hasAnnotation(InjektFqNames.Reader)) {
            reportCorrectFunctionMissing()
            return
        }

        if (function.typeParameters.size != 1) {
            reportCorrectFunctionMissing()
            return
        }

        if (function.valueParameters.size != 2) {
            reportCorrectFunctionMissing()
            return
        }

        val firstParameter = function.valueParameters[0]

        if (firstParameter.type != firstParameter.builtIns.anyType) {
            reportCorrectFunctionMissing()
            return
        }

        val secondParameter = function.valueParameters[1]

        if (!secondParameter.type.isFunctionType ||
            secondParameter.type.arguments.size != 1
        ) {
            reportCorrectFunctionMissing()
            return
        }

        if (function.returnType != function.typeParameters.single().defaultType) {
            reportCorrectFunctionMissing()
            return
        }


    }

}
