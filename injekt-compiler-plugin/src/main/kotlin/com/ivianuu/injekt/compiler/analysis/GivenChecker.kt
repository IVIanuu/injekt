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

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.resolution.ContributionKind
import com.ivianuu.injekt.compiler.resolution.contributionKind
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext

class GivenChecker(private val declarationStore: DeclarationStore) : DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
    ) {
        checkType(descriptor, declaration, context.trace)

        if (descriptor is SimpleFunctionDescriptor) {
            checkTypeParameters(descriptor.typeParameters, context.trace)
            checkMultipleContributions(descriptor, declaration, context.trace)
            descriptor.allParameters
                .filterNot { it === descriptor.dispatchReceiverParameter }
                .checkParameters(declaration, descriptor, context.trace)
            if (descriptor.isTailrec) {
                context.trace.report(
                    InjektErrors.GIVEN_TAILREC_FUNCTION
                        .on(declaration)
                )
            }
            val givenConstraints = descriptor.typeParameters.filter {
                it.hasAnnotation(InjektFqNames.Given)
            }
            if (givenConstraints.isNotEmpty() &&
                    descriptor.contributionKind(declarationStore) == null) {
                context.trace.report(
                    InjektErrors.GIVEN_CONSTRAINT_WITHOUT_CONTRIBUTION
                        .on(declaration)
                )
            }
            if (givenConstraints.size > 1) {
                context.trace.report(
                    InjektErrors.MULTIPLE_GIVEN_CONSTRAINTS
                        .on(declaration)
                )
            }
        } else if (descriptor is ConstructorDescriptor) {
            checkMultipleContributions(descriptor, declaration, context.trace)
            descriptor.valueParameters
                .checkParameters(declaration, if (descriptor.constructedClass.contributionKind(declarationStore) != null)
                    descriptor.constructedClass else descriptor, context.trace)
        } else if (descriptor is ClassDescriptor) {
            checkTypeParameters(descriptor.declaredTypeParameters, context.trace)
            val hasGivenAnnotation = descriptor.hasAnnotation(InjektFqNames.Given)
            checkMultipleContributions(descriptor, declaration, context.trace)
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

            descriptor.constructors
                .forEach {
                    it.valueParameters
                        .checkParameters(it.findPsi() as KtDeclaration, descriptor, context.trace)
                }
        } else if (descriptor is PropertyDescriptor) {
            checkTypeParameters(descriptor.typeParameters, context.trace)
            if (descriptor.hasAnnotation(InjektFqNames.Given) &&
                descriptor.extensionReceiverParameter != null &&
                descriptor.extensionReceiverParameter?.type?.contributionKind(
                    declarationStore
                ) == null
            ) {
                checkMultipleContributions(descriptor, declaration, context.trace)
                context.trace.report(
                    InjektErrors.NON_GIVEN_PARAMETER_ON_GIVEN_DECLARATION
                        .on(
                            descriptor.extensionReceiverParameter?.findPsi() ?: declaration,
                            when {
                                descriptor.hasAnnotation(InjektFqNames.Given) -> InjektFqNames.Given.shortName()
                                descriptor.hasAnnotation(InjektFqNames.Module) -> InjektFqNames.Module.shortName()
                                descriptor.hasAnnotation(InjektFqNames.GivenSetElement) -> InjektFqNames.GivenSetElement.shortName()
                                else -> error("")
                            }
                        )
                )
            }
        } else if (descriptor is VariableDescriptor) {
            checkMultipleContributions(descriptor, declaration, context.trace)
        } else if (descriptor is TypeAliasDescriptor) {
            checkTypeParameters(descriptor.declaredTypeParameters, context.trace)
        }
    }

    private fun checkTypeParameters(
        typeParameters: List<TypeParameterDescriptor>,
        trace: BindingTrace
    ) {
        typeParameters.forEach {
            if (it.hasAnnotation(InjektFqNames.Given) &&
                it.containingDeclaration !is SimpleFunctionDescriptor) {
                trace.report(
                    InjektErrors.GIVEN_CONSTRAINT_ON_NON_FUNCTION
                        .on(it.findPsi()!!)
                )
            }
        }
    }

    private fun checkMultipleContributions(
        descriptor: DeclarationDescriptor,
        declaration: KtDeclaration,
        trace: BindingTrace
    ) {
        if (descriptor.annotations.count {
                it.fqName == InjektFqNames.Given ||
                        it.fqName == InjektFqNames.GivenSetElement ||
                        it.fqName == InjektFqNames.Module
        } > 1) {
            trace.report(
                InjektErrors.DECLARATION_WITH_MULTIPLE_CONTRIBUTIONS
                    .on(declaration)
            )
        }
    }

    private fun checkType(
        descriptor: DeclarationDescriptor,
        declaration: KtDeclaration,
        trace: BindingTrace
    ) {
        val type = when (descriptor) {
            is CallableDescriptor -> descriptor.returnType
            is ValueDescriptor -> descriptor.type
            else -> return
        } ?: return
        if ((type.isFunctionType ||
                    type.isSuspendFunctionType) &&
            (type.hasAnnotation(InjektFqNames.Given) ||
                    type.hasAnnotation(InjektFqNames.Module) ||
                    type.hasAnnotation(InjektFqNames.GivenSetElement)) &&
            type.arguments.dropLast(1)
                .any { it.type.contributionKind(declarationStore) == null }) {
            trace.report(
                InjektErrors.NON_GIVEN_PARAMETER_ON_GIVEN_DECLARATION
                    .on(
                        declaration,
                        when {
                            type.hasAnnotation(InjektFqNames.Given) -> InjektFqNames.Given.shortName()
                            type.hasAnnotation(InjektFqNames.Module) -> InjektFqNames.Module.shortName()
                            type.hasAnnotation(InjektFqNames.GivenSetElement) -> InjektFqNames.GivenSetElement.shortName()
                            else -> error("")
                        }
                    )
            )
        }
    }

    private fun List<ParameterDescriptor>.checkParameters(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        trace: BindingTrace,
    ) {
        if (descriptor.hasAnnotation(InjektFqNames.Given) ||
            descriptor.hasAnnotation(InjektFqNames.Module) ||
            declaration.hasAnnotation(InjektFqNames.GivenSetElement) ||
            (descriptor is ConstructorDescriptor && (
                    descriptor.constructedClass.hasAnnotation(InjektFqNames.Given) ||
                            descriptor.constructedClass.hasAnnotation(InjektFqNames.Module) ||
                            descriptor.constructedClass.hasAnnotation(InjektFqNames.GivenSetElement)))
        ) {
            this
                .filter {
                    it.contributionKind(declarationStore) == null &&
                            it.type.contributionKind(declarationStore) == null
                }
                .forEach {
                    trace.report(
                        InjektErrors.NON_GIVEN_PARAMETER_ON_GIVEN_DECLARATION
                            .on(
                                it.findPsi() ?: declaration,
                                when {
                                    descriptor.hasAnnotation(InjektFqNames.Given) -> InjektFqNames.Given.shortName()
                                    descriptor.hasAnnotation(InjektFqNames.Module) -> InjektFqNames.Module.shortName()
                                    descriptor.hasAnnotation(InjektFqNames.GivenSetElement) -> InjektFqNames.GivenSetElement.shortName()
                                    else -> error("")
                                }
                            )
                    )
                }
        }
    }
}
