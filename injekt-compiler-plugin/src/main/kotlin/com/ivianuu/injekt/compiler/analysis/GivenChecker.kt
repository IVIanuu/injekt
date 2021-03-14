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

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.resolution.isGiven
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext

class GivenChecker(private val context: InjektContext) : DeclarationChecker {
    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
    ) {
        if (descriptor is SimpleFunctionDescriptor) {
            if (descriptor.isGiven(this.context, context.trace)) {
                descriptor.allParameters
                    .filterNot { it === descriptor.dispatchReceiverParameter }
                    .checkGivenCallableHasOnlyGivenParameters(declaration, context.trace)

                if (descriptor.isTailrec) {
                    context.trace.report(
                        InjektErrors.GIVEN_TAILREC_FUNCTION
                            .on(declaration)
                    )
                }

                val givenConstraints = descriptor.typeParameters.filter {
                    it.hasAnnotation(InjektFqNames.Given)
                }
                if (givenConstraints.size > 1) {
                    context.trace.report(
                        InjektErrors.MULTIPLE_GIVEN_CONSTRAINTS
                            .on(declaration)
                    )
                }

                /*if (givenConstraints.size == 1) {
                val constraintType = givenConstraints.single()
                    .defaultType.toTypeRef(this.context, context.trace)
                val returnType = descriptor.returnType!!.toTypeRef(this.context, context.trace)
                if (returnType.isAssignableTo(this.context, constraintType)) {
                    context.trace.report(
                        InjektErrors.DIVERGENT_GIVEN_CONSTRAINT
                            .on(declaration)
                    )
                }
            }*/
            } else {
                checkGivenTypeParametersOnNonGivenFunction(descriptor.typeParameters, context.trace)
            }
        } else if (descriptor is ConstructorDescriptor) {
            if (descriptor.isGiven(this.context, context.trace)) {
                descriptor.valueParameters
                    .checkGivenCallableHasOnlyGivenParameters(declaration, context.trace)
            }
        } else if (descriptor is ClassDescriptor) {
            checkGivenTypeParametersOnNonGivenFunction(descriptor.declaredTypeParameters, context.trace)

            val hasGivenAnnotation = descriptor.hasAnnotation(InjektFqNames.Given)
            val givenConstructors = descriptor.constructors
                .filter { it.hasAnnotation(InjektFqNames.Given) }

            if (descriptor.kind == ClassKind.ANNOTATION_CLASS) {
                if (hasGivenAnnotation) {
                    context.trace.report(
                        InjektErrors.GIVEN_ANNOTATION_CLASS
                            .on(declaration)
                    )
                }

                if (givenConstructors.isNotEmpty()) {
                    context.trace.report(
                        InjektErrors.GIVEN_CONSTRUCTOR_ON_ANNOTATION_CLASS
                            .on(declaration)
                    )
                }
            }

            if (hasGivenAnnotation &&
                descriptor.kind == ClassKind.ENUM_CLASS) {
                context.trace.report(
                    InjektErrors.GIVEN_ENUM_CLASS
                        .on(declaration)
                )
            }

            if (hasGivenAnnotation &&
                    descriptor.modality == Modality.ABSTRACT) {
                context.trace.report(
                    InjektErrors.GIVEN_ABSTRACT_CLASS
                        .on(declaration)
                )
            }

            if (hasGivenAnnotation &&
                givenConstructors.isNotEmpty()
            ) {
                context.trace.report(
                    InjektErrors.GIVEN_CLASS_WITH_GIVEN_CONSTRUCTOR
                        .on(declaration)
                )
            }

            if (givenConstructors.size > 1) {
                context.trace.report(
                    InjektErrors.CLASS_WITH_MULTIPLE_GIVEN_CONSTRUCTORS
                        .on(declaration)
                )
            }
        } else if (descriptor is PropertyDescriptor) {
            checkGivenTypeParametersOnNonGivenFunction(descriptor.typeParameters, context.trace)
            if (descriptor.hasAnnotation(InjektFqNames.Given) &&
                descriptor.extensionReceiverParameter != null &&
                descriptor.extensionReceiverParameter?.type?.isGiven(this.context, context.trace) != true
            ) {
                context.trace.report(
                    InjektErrors.NON_GIVEN_PARAMETER_ON_GIVEN_DECLARATION
                        .on(descriptor.extensionReceiverParameter?.findPsi() ?: declaration)
                )
            }
        } else if (descriptor is TypeAliasDescriptor) {
            checkGivenTypeParametersOnNonGivenFunction(descriptor.declaredTypeParameters, context.trace)
        }
    }

    private fun checkGivenTypeParametersOnNonGivenFunction(
        typeParameters: List<TypeParameterDescriptor>,
        trace: BindingTrace
    ) {
        typeParameters.forEach {
            if (it.hasAnnotation(InjektFqNames.Given)) {
                trace.report(
                    InjektErrors.GIVEN_CONSTRAINT_ON_NON_GIVEN_FUNCTION
                        .on(it.findPsi()!!)
                )
            }
        }
    }

    private fun List<ParameterDescriptor>.checkGivenCallableHasOnlyGivenParameters(
        declaration: KtDeclaration,
        trace: BindingTrace,
    ) {
        this
            .filter { !it.isGiven(context, trace) }
            .forEach {
                trace.report(
                    InjektErrors.NON_GIVEN_PARAMETER_ON_GIVEN_DECLARATION
                        .on(it.findPsi() ?: declaration)
                )
            }
    }
}
