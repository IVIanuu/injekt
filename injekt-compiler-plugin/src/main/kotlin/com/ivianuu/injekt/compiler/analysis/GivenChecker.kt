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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext

class GivenChecker : DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor is ClassDescriptor) {
            checkClass(declaration, descriptor, context)
        }
    }

    private fun checkClass(
        declaration: KtDeclaration,
        descriptor: ClassDescriptor,
        context: DeclarationCheckerContext
    ) {
        val classHasAnnotation = descriptor.hasAnnotation(InjektFqNames.Given)

        val annotatedConstructors = descriptor.constructors
            .filter { it.hasAnnotation(InjektFqNames.Given) }

        if (!classHasAnnotation && annotatedConstructors.isEmpty()) return

        if (classHasAnnotation && descriptor.constructors.size > 1 &&
            annotatedConstructors.isEmpty()
        ) {
            context.trace.report(
                InjektErrors.MULTIPLE_CONSTRUCTORS_ON_GIVEN_CLASS
                    .on(declaration)
            )
        }

        if (classHasAnnotation && annotatedConstructors.isNotEmpty()) {
            context.trace.report(
                InjektErrors.EITHER_CLASS_OR_CONSTRUCTOR_GIVEN
                    .on(declaration)
            )
        }

        if (annotatedConstructors.size > 1) {
            context.trace.report(
                InjektErrors.MULTIPLE_GIVEN_ANNOTATED_CONSTRUCTORS
                    .on(declaration)
            )
        }

        if ((descriptor.kind != ClassKind.CLASS && descriptor.kind != ClassKind.OBJECT) ||
            descriptor.modality == Modality.ABSTRACT
        ) {
            context.trace.report(InjektErrors.GIVEN_CLASS_CANNOT_BE_ABSTRACT.on(declaration))
        }
    }

}
