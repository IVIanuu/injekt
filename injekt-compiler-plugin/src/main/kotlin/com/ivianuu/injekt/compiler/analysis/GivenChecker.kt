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
import com.ivianuu.injekt.compiler.findAnnotation
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.isGivenConstraint
import com.ivianuu.injekt.compiler.resolution.ClassifierRef
import com.ivianuu.injekt.compiler.resolution.forEachType
import com.ivianuu.injekt.compiler.resolution.getGivenConstructor
import com.ivianuu.injekt.compiler.resolution.isAssignableTo
import com.ivianuu.injekt.compiler.resolution.isGiven
import com.ivianuu.injekt.compiler.resolution.toCallableRef
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
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
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

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

            descriptor.allParameters
                .filterNot { it === descriptor.dispatchReceiverParameter }
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
        } else {
            checkOverrides(declaration, descriptor, trace)
            checkGivenConstraintsOnNonGivenDeclaration(descriptor.typeParameters, trace)
            if (descriptor.extensionReceiverParameter?.isGiven(this.context, trace) == true) {
                trace.report(
                    InjektErrors.GIVEN_RECEIVER_ON_NON_GIVEN_DECLARATION
                        .on(declaration.safeAs<KtNamedFunction>()
                            ?.receiverTypeReference ?: declaration)
                )
            }
        }
    }

    private fun checkClass(
        declaration: KtDeclaration,
        descriptor: ClassDescriptor,
        trace: BindingTrace
    ) {
        val hasGivenAnnotation = descriptor.hasAnnotation(InjektFqNames.Given)
        val givenConstructors = descriptor.constructors
            .filter { it.hasAnnotation(InjektFqNames.Given) }

        if (descriptor.kind == ClassKind.ANNOTATION_CLASS) {
            if (hasGivenAnnotation) {
                trace.report(
                    InjektErrors.GIVEN_ANNOTATION_CLASS
                        .on(
                            declaration.findAnnotation(InjektFqNames.Given)
                                ?: declaration
                        )
                )
            }

            if (givenConstructors.isNotEmpty()) {
                givenConstructors
                    .forEach {
                        trace.report(
                            InjektErrors.GIVEN_CONSTRUCTOR_ON_ANNOTATION_CLASS
                                .on(it.findPsi() ?: declaration)
                        )
                    }
            }
        }

        if (hasGivenAnnotation && descriptor.kind == ClassKind.ENUM_CLASS) {
            trace.report(
                InjektErrors.GIVEN_ENUM_CLASS
                    .on(
                        declaration.findAnnotation(InjektFqNames.Given)
                            ?: declaration
                    )
            )
        }

        if (hasGivenAnnotation && descriptor.modality == Modality.ABSTRACT) {
            trace.report(
                InjektErrors.GIVEN_ABSTRACT_CLASS
                    .on(
                        declaration.modifierList
                            ?.getModifier(KtTokens.ABSTRACT_KEYWORD)
                            ?: declaration
                    )
            )
        }

        if (hasGivenAnnotation && descriptor.isInner) {
            trace.report(
                InjektErrors.GIVEN_INNER_CLASS
                    .on(
                        declaration.modifierList
                            ?.getModifier(KtTokens.INNER_KEYWORD)
                            ?: declaration
                    )
            )
        }

        if (hasGivenAnnotation && givenConstructors.isNotEmpty()) {
            trace.report(
                InjektErrors.GIVEN_ON_CLASS_WITH_GIVEN_CONSTRUCTOR
                    .on(
                        declaration.findAnnotation(InjektFqNames.Given)
                            ?: declaration
                    )
            )
        }

        if (givenConstructors.size > 1) {
            givenConstructors
                .drop(1)
                .forEach {
                    trace.report(
                        InjektErrors.CLASS_WITH_MULTIPLE_GIVEN_CONSTRUCTORS
                            .on(it.findPsi() ?: declaration)
                    )
                }
        }

        val singleGivenConstructor = descriptor.getGivenConstructor(context, trace)
        if (singleGivenConstructor != null) {
            checkConstrainedGiven(declaration, singleGivenConstructor.callable,
                descriptor.declaredTypeParameters, trace)
        } else {
            checkGivenConstraintsOnNonGivenDeclaration(descriptor.declaredTypeParameters, trace)
        }
    }

    private fun checkConstructor(
        declaration: KtDeclaration,
        descriptor: ConstructorDescriptor,
        trace: BindingTrace
    ) {
        if (descriptor.isGiven(this.context, trace)) {
            descriptor.valueParameters
                .checkGivenCallableHasOnlyGivenParameters(declaration, trace)
        }
    }

    private fun checkProperty(
        declaration: KtDeclaration,
        descriptor: PropertyDescriptor,
        trace: BindingTrace
    ) {
        checkOverrides(declaration, descriptor, trace)
        checkGivenConstraintsOnNonGivenDeclaration(descriptor.typeParameters, trace)
        if (descriptor.isGiven(this.context, trace)) {
            checkUnresolvableGivenTypeParameters(declaration,
                descriptor.typeParameters, descriptor.returnType!!, trace)
            if (descriptor.extensionReceiverParameter != null &&
                descriptor.extensionReceiverParameter?.type?.isGiven(this.context, trace) != true
            ) {
                trace.report(
                    InjektErrors.NON_GIVEN_PARAMETER_ON_GIVEN_DECLARATION
                        .on(
                            declaration.safeAs<KtProperty>()
                                ?.receiverTypeReference
                                ?: declaration
                        )
                )
            }
        } else {
            if (descriptor.extensionReceiverParameter?.isGiven(this.context, trace) == true) {
                trace.report(
                    InjektErrors.GIVEN_RECEIVER_ON_NON_GIVEN_DECLARATION
                        .on(declaration.safeAs<KtProperty>()
                            ?.receiverTypeReference ?: declaration)
                )
            }
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

        if (givenConstraints.size == 1) {
            val callable = descriptor.toCallableRef(this.context, trace)
            val constraintType = callable.typeParameters
                .single { it.isGivenConstraint }
                .defaultType
            if (callable.type.isAssignableTo(this.context, constraintType)) {
                trace.report(
                    InjektErrors.DIVERGENT_GIVEN_CONSTRAINT
                        .on(declaration.safeAs<KtNamedFunction>()?.typeReference
                            ?: givenConstraints.single().findPsi() ?: declaration)
                )
            }
        }

        if (descriptor.valueParameters.isNotEmpty()) {
            val callable = descriptor.toCallableRef(this.context, trace)
            callable.parameterTypes
                .filter { callable.type == it.value }
                .forEach { (divergentParameterName) ->
                    trace.report(
                        InjektErrors.DIVERGENT_GIVEN
                            .on(declaration.safeAs<KtFunction>()
                                ?.valueParameters
                                ?.firstOrNull { it.name == divergentParameterName } ?: declaration)
                    )
                }
        }
    }

    private fun checkOverrides(
        declaration: KtDeclaration,
        descriptor: CallableDescriptor,
        trace: BindingTrace
    ) {
        val isGiven = descriptor.hasAnnotation(InjektFqNames.Given)
        if (isGiven) return
        if (descriptor.overriddenTreeUniqueAsSequence(false)
                .drop(1)
                .any { it.isGiven(context, trace) }) {
            trace.report(
                InjektErrors.NO_GIVEN_ANNOTATION_ON_GIVEN_OVERRIDE
                    .on(declaration)
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

    private fun checkGivenConstraintsOnNonGivenDeclaration(
        typeParameters: List<TypeParameterDescriptor>,
        trace: BindingTrace
    ) {
        if (typeParameters.isEmpty()) return
        typeParameters
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
            .filter { !it.isGiven(context, trace) }
            .forEach {
                trace.report(
                    InjektErrors.NON_GIVEN_PARAMETER_ON_GIVEN_DECLARATION
                        .on(it.findPsi() ?: declaration)
                )
            }
    }
}
