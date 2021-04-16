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

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.lexer.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.checkers.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.multiplatform.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class GivenChecker(private val context: InjektContext) : DeclarationChecker {
    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
    ) {
        when (descriptor) {
            is SimpleFunctionDescriptor -> checkFunction(declaration, descriptor, context.trace)
            is ConstructorDescriptor -> checkConstructor(declaration, descriptor, context.trace)
            is ClassDescriptor -> checkClass(declaration, descriptor, context.trace)
            is PropertyDescriptor -> checkProperty(declaration, descriptor, context.trace)
            is TypeAliasDescriptor -> checkTypeAlias(declaration, descriptor, context.trace)
        }
    }

    private fun checkFunction(
        declaration: KtDeclaration,
        descriptor: FunctionDescriptor,
        trace: BindingTrace
    ) {
        if (descriptor.isGiven(this.context, trace)) {
            checkUnresolvableGivenTypeParameters(declaration,
                descriptor.typeParameters, descriptor.returnType!!, trace)

            descriptor.valueParameters
                .checkGivenCallableHasOnlyGivenParameters(declaration, trace)

            if (descriptor.isTailrec) {
                trace.report(
                    InjektErrors.GIVEN_TAILREC_FUNCTION
                        .on(
                            declaration.modifierList
                                ?.getModifier(KtTokens.TAILREC_KEYWORD)
                                ?: declaration
                        )
                )
            }

            checkConstrainedGiven(declaration, descriptor, descriptor.typeParameters, trace)
            checkGivenTypeParametersMismatch(descriptor, declaration, trace)
        } else {
            checkOverrides(declaration, descriptor, trace)
            checkExceptActual(declaration, descriptor, trace)
            checkGivenConstraintsOnNonGivenDeclaration(descriptor.typeParameters, trace)
        }
        if (descriptor.extensionReceiverParameter?.isGiven(this.context, trace) == true) {
            trace.report(
                InjektErrors.GIVEN_RECEIVER
                    .on(declaration.safeAs<KtNamedFunction>()
                        ?.receiverTypeReference ?: declaration)
            )
        }
    }

    private fun checkClass(
        declaration: KtDeclaration,
        descriptor: ClassDescriptor,
        trace: BindingTrace
    ) {
        val givenConstructors = descriptor.getGivenConstructors(context, trace)
        val isGiven = givenConstructors.isNotEmpty()

        if (isGiven && descriptor.kind == ClassKind.ANNOTATION_CLASS) {
            trace.report(
                InjektErrors.GIVEN_ANNOTATION_CLASS
                    .on(
                        declaration.findAnnotation(InjektFqNames.Given)
                            ?: declaration
                    )
            )
        }

        if (isGiven && descriptor.kind == ClassKind.ENUM_CLASS) {
            trace.report(
                InjektErrors.GIVEN_ENUM_CLASS
                    .on(
                        declaration.findAnnotation(InjektFqNames.Given)
                            ?: declaration
                    )
            )
        }

        if (descriptor.kind == ClassKind.INTERFACE &&
            descriptor.hasAnnotation(InjektFqNames.Given)) {
            trace.report(
                InjektErrors.GIVEN_INTERFACE
                    .on(
                        declaration.findAnnotation(InjektFqNames.Given)
                            ?: declaration
                    )
            )
        }

        if (isGiven && descriptor.modality == Modality.ABSTRACT) {
            trace.report(
                InjektErrors.GIVEN_ABSTRACT_CLASS
                    .on(
                        declaration.modalityModifier()
                            ?: declaration
                    )
            )
        }

        if (isGiven && descriptor.isInner) {
            trace.report(
                InjektErrors.GIVEN_INNER_CLASS
                    .on(
                        declaration.modifierList
                            ?.getModifier(KtTokens.INNER_KEYWORD)
                            ?: declaration
                    )
            )
        }

        if (descriptor.hasAnnotation(InjektFqNames.Given) &&
                descriptor.unsubstitutedPrimaryConstructor
                    ?.hasAnnotation(InjektFqNames.Given) == true) {
            trace.report(
                InjektErrors.GIVEN_ON_CLASS_WITH_PRIMARY_GIVEN_CONSTRUCTOR
                    .on(
                        declaration.findAnnotation(InjektFqNames.Given)
                            ?: declaration
                    )
            )
        }

        if (givenConstructors.isNotEmpty()) {
            givenConstructors
                .forEach {
                    checkConstrainedGiven(declaration, it.callable,
                        descriptor.declaredTypeParameters, trace)
                }
        } else {
            checkGivenConstraintsOnNonGivenDeclaration(descriptor.declaredTypeParameters, trace)
        }

        checkExceptActual(declaration, descriptor, trace)
    }

    private fun checkConstructor(
        declaration: KtDeclaration,
        descriptor: ConstructorDescriptor,
        trace: BindingTrace
    ) {
        if (descriptor.isGiven(this.context, trace)) {
            descriptor.valueParameters
                .checkGivenCallableHasOnlyGivenParameters(declaration, trace)
        } else {
            checkExceptActual(declaration, descriptor, trace)
        }
    }

    private fun checkProperty(
        declaration: KtDeclaration,
        descriptor: PropertyDescriptor,
        trace: BindingTrace
    ) {
        checkGivenConstraintsOnNonGivenDeclaration(descriptor.typeParameters, trace)
        if (descriptor.isGiven(this.context, trace)) {
            checkUnresolvableGivenTypeParameters(declaration,
                descriptor.typeParameters, descriptor.returnType!!, trace)
            checkGivenTypeParametersMismatch(descriptor, declaration, trace)
        } else {
            checkOverrides(declaration, descriptor, trace)
            checkExceptActual(declaration, descriptor, trace)
        }
        if (descriptor.extensionReceiverParameter?.isGiven(this.context, trace) == true) {
            trace.report(
                InjektErrors.GIVEN_RECEIVER
                    .on(declaration.safeAs<KtProperty>()
                        ?.receiverTypeReference ?: declaration)
            )
        }
    }

    private fun checkTypeAlias(
        declaration: KtDeclaration,
        descriptor: TypeAliasDescriptor,
        trace: BindingTrace
    ) {
        checkGivenConstraintsOnNonGivenDeclaration(descriptor.declaredTypeParameters, trace)
    }

    private fun checkConstrainedGiven(
        declaration: KtDeclaration,
        descriptor: CallableDescriptor,
        typeParameters: List<TypeParameterDescriptor>,
        trace: BindingTrace
    ) {
        val givenConstraints = typeParameters.filter {
            it.isGivenConstraint(context, trace)
        }
        if (givenConstraints.size > 1) {
            givenConstraints
                .drop(1)
                .forEach {
                    trace.report(
                        InjektErrors.MULTIPLE_GIVEN_CONSTRAINTS
                            .on(it.findPsi() ?: declaration)
                    )
                }
        }
    }

    private fun checkOverrides(
        declaration: KtDeclaration,
        descriptor: CallableMemberDescriptor,
        trace: BindingTrace
    ) {
        val isGiven = descriptor.hasAnnotation(InjektFqNames.Given)
        if (isGiven) return
        if (descriptor.overriddenTreeUniqueAsSequence(false)
                .drop(1)
                .any { it.hasAnnotation(InjektFqNames.Given) }) {
            trace.report(
                Errors.NOTHING_TO_OVERRIDE
                    .on(declaration, descriptor)
            )
        }
    }

    private fun checkExceptActual(
        declaration: KtDeclaration,
        descriptor: MemberDescriptor,
        trace: BindingTrace
    ) {
        if (!descriptor.isActual) return
        val isGiven = descriptor.hasAnnotation(InjektFqNames.Given)
        if (isGiven) return
        if (descriptor.findExpects().any { it.hasAnnotation(InjektFqNames.Given) }) {
            trace.report(
                Errors.ACTUAL_WITHOUT_EXPECT
                    .on(declaration.cast(), descriptor, emptyMap())
            )
        }
    }

    private fun checkUnresolvableGivenTypeParameters(
        declaration: KtDeclaration,
        typeParameters: List<TypeParameterDescriptor>,
        returnType: KotlinType,
        trace: BindingTrace
    ) {
        if (typeParameters.isEmpty()) return
        val allTypeParameterRefs = typeParameters
            .map { it.toClassifierRef(context, trace) }
        val nonConstraintTypeParameterRefs = allTypeParameterRefs
            .filterNot { it.isGivenConstraint }
        if (nonConstraintTypeParameterRefs.isEmpty()) return
        val usedNonConstraintTypeParameterRefs = mutableSetOf<ClassifierRef>()
        returnType.toTypeRef(context, trace)
            .forEachType {
                if (it.classifier in nonConstraintTypeParameterRefs)
                    usedNonConstraintTypeParameterRefs += it.classifier
            }
        allTypeParameterRefs
            .forEach { typeParameterRef ->
                typeParameterRef.superTypes
                    .forEach { typeParameterSuperType ->
                        typeParameterSuperType.forEachType {
                            if (it.classifier in nonConstraintTypeParameterRefs)
                                usedNonConstraintTypeParameterRefs += it.classifier
                        }
                    }
            }
        nonConstraintTypeParameterRefs
            .asSequence()
            .filter { it !in usedNonConstraintTypeParameterRefs }
            .map { it.descriptor!! }
            .forEach {
                trace.report(
                    InjektErrors.GIVEN_WITH_UNRESOLVABLE_TYPE_PARAMETER
                        .on(it.findPsi() ?: declaration)
                )
            }
    }

    private fun checkGivenTypeParametersMismatch(
        descriptor: CallableDescriptor,
        declaration: KtDeclaration,
        trace: BindingTrace
    ) {
        if (descriptor.typeParameters.isEmpty()) return
        if (descriptor.overriddenDescriptors.isEmpty()) return

        descriptor.overriddenDescriptors
            .filter { overriddenDescriptor ->
                var hasDifferentTypeParameters = false
                descriptor.typeParameters.forEachWith(overriddenDescriptor.typeParameters) { a, b ->
                    hasDifferentTypeParameters = hasDifferentTypeParameters || b.isGiven(context, trace) &&
                            !a.isGiven(context, trace)
                }
                hasDifferentTypeParameters
            }
            .toList()
            .takeIf { it.isNotEmpty() }
            ?.let {
                trace.report(
                    Errors.CONFLICTING_OVERLOADS
                        .on(declaration, it)
                )
            }
    }

    private fun checkGivenConstraintsOnNonGivenDeclaration(
        typeParameters: List<TypeParameterDescriptor>,
        trace: BindingTrace
    ) {
        if (typeParameters.isEmpty()) return
        typeParameters
            .asSequence()
            .filter { it.isGivenConstraint(context, trace) }
            .forEach { typeParameter ->
                trace.report(
                    InjektErrors.GIVEN_CONSTRAINT_ON_NON_GIVEN_DECLARATION
                        .on(typeParameter.findPsi()!!)
                )
        }
    }

    private fun List<ParameterDescriptor>.checkGivenCallableHasOnlyGivenParameters(
        declaration: KtDeclaration,
        trace: BindingTrace,
    ) {
        if (isEmpty()) return
        this
            .asSequence()
            .filter { !it.isGiven(context, trace) }
            .forEach {
                trace.report(
                    InjektErrors.NON_GIVEN_PARAMETER_ON_GIVEN_DECLARATION
                        .on(it.findPsi() ?: declaration)
                )
            }
    }
}
