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
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.forEachType
import com.ivianuu.injekt.compiler.resolution.isAssignableTo
import com.ivianuu.injekt.compiler.resolution.isGiven
import com.ivianuu.injekt.compiler.resolution.toTypeRef
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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class GivenChecker(private val context: InjektContext) : DeclarationChecker {
    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
    ) {
        if (descriptor is SimpleFunctionDescriptor) {
            if (descriptor.isGiven(this.context, context.trace)) {
                checkUnresolvableGivenTypeParameters(declaration,
                    descriptor.typeParameters, descriptor.returnType!!, context.trace)

                descriptor.allParameters
                    .filterNot { it === descriptor.dispatchReceiverParameter }
                    .checkGivenCallableHasOnlyGivenParameters(declaration, context.trace)

                if (descriptor.isTailrec) {
                    context.trace.report(
                        InjektErrors.GIVEN_TAILREC_FUNCTION
                            .on(
                                declaration.modifierList
                                    ?.getModifier(KtTokens.TAILREC_KEYWORD)
                                    ?: declaration
                            )
                    )
                }

                val givenConstraints = descriptor.typeParameters.filter {
                    it.hasAnnotation(InjektFqNames.Given)
                }
                if (givenConstraints.size > 1) {
                    descriptor
                        .typeParameters
                        .filter { it.hasAnnotation(InjektFqNames.Given) }
                        .drop(1)
                        .forEach {
                            context.trace.report(
                                InjektErrors.MULTIPLE_GIVEN_CONSTRAINTS
                                    .on(it.findPsi() ?: declaration)
                            )
                        }
                }

                if (givenConstraints.size == 1) {
                    val constraintType = givenConstraints.single()
                        .defaultType.toTypeRef(this.context, context.trace)
                    val returnType = descriptor.returnType!!.toTypeRef(this.context, context.trace)
                    if (returnType.isAssignableTo(this.context, constraintType)) {
                        context.trace.report(
                            InjektErrors.DIVERGENT_GIVEN_CONSTRAINT
                                .on(declaration.safeAs<KtNamedFunction>()?.typeReference
                                    ?: givenConstraints.single().findPsi() ?: declaration)
                        )
                    }
                }

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
                            .on(
                                declaration.findAnnotation(InjektFqNames.Given)
                                    ?: declaration
                            )
                    )
                }

                if (givenConstructors.isNotEmpty()) {
                    givenConstructors
                        .forEach {
                            context.trace.report(
                                InjektErrors.GIVEN_CONSTRUCTOR_ON_ANNOTATION_CLASS
                                    .on(it.findPsi() ?: declaration)
                            )
                        }
                }
            }

            if (hasGivenAnnotation &&
                descriptor.kind == ClassKind.ENUM_CLASS) {
                context.trace.report(
                    InjektErrors.GIVEN_ENUM_CLASS
                        .on(
                            declaration.findAnnotation(InjektFqNames.Given)
                                ?: declaration
                        )
                )
            }

            if (hasGivenAnnotation &&
                    descriptor.modality == Modality.ABSTRACT) {
                context.trace.report(
                    InjektErrors.GIVEN_ABSTRACT_CLASS
                        .on(
                            declaration.modifierList
                                ?.getModifier(KtTokens.ABSTRACT_KEYWORD)
                                ?: declaration
                        )
                )
            }

            if (hasGivenAnnotation &&
                givenConstructors.isNotEmpty()
            ) {
                context.trace.report(
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
                        context.trace.report(
                            InjektErrors.CLASS_WITH_MULTIPLE_GIVEN_CONSTRUCTORS
                                .on(it.findPsi() ?: declaration)
                        )
                    }
            }
        } else if (descriptor is PropertyDescriptor) {
            checkGivenTypeParametersOnNonGivenFunction(descriptor.typeParameters, context.trace)
            if (descriptor.hasAnnotation(InjektFqNames.Given)) {
                checkUnresolvableGivenTypeParameters(declaration,
                    descriptor.typeParameters, descriptor.returnType!!, context.trace)
                if (descriptor.extensionReceiverParameter != null &&
                    descriptor.extensionReceiverParameter?.type?.isGiven(this.context, context.trace) != true
                ) {
                    context.trace.report(
                        InjektErrors.NON_GIVEN_PARAMETER_ON_GIVEN_DECLARATION
                            .on(
                                declaration.safeAs<KtProperty>()
                                    ?.receiverTypeReference
                                    ?: declaration
                            )
                    )
                }
            }
        } else if (descriptor is TypeAliasDescriptor) {
            checkGivenTypeParametersOnNonGivenFunction(descriptor.declaredTypeParameters, context.trace)
        }
    }

    private fun checkUnresolvableGivenTypeParameters(
        declaration: KtDeclaration,
        typeParameters: List<TypeParameterDescriptor>,
        returnType: KotlinType,
        trace: BindingTrace
    ) {
        if (typeParameters.isEmpty()) return
        val typeParameterTypes = typeParameters
            .filterNot { it.hasAnnotation(InjektFqNames.Given) }
            .mapTo(mutableSetOf()) { it.defaultType.toTypeRef(context, trace) }
        val seenTypeParameterTypes = mutableSetOf<TypeRef>()
        returnType.toTypeRef(context, trace)
            .forEachType {
                if (it in typeParameterTypes)
                    seenTypeParameterTypes += it
            }
        typeParameterTypes
            .filter { it !in seenTypeParameterTypes }
            .map { unresolvableTypeParameter ->
                typeParameters
                    .single { it.name == unresolvableTypeParameter.classifier.fqName.shortName() }
            }
            .forEach {
                trace.report(
                    InjektErrors.GIVEN_WITH_UNRESOLVABLE_TYPE_PARAMETER
                        .on(it.findPsi() ?: declaration)
                )
            }
    }

    private fun checkGivenTypeParametersOnNonGivenFunction(
        typeParameters: List<TypeParameterDescriptor>,
        trace: BindingTrace
    ) {
        if (typeParameters.isEmpty()) return
        typeParameters
            .filter { it.hasAnnotation(InjektFqNames.Given) }
            .forEach { typeParameter ->
                trace.report(
                    InjektErrors.GIVEN_CONSTRAINT_ON_NON_GIVEN_FUNCTION
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
