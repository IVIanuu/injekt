package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext

class GivenChecker : DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
    ) {
        if (descriptor is SimpleFunctionDescriptor) {
            if (descriptor.hasAnnotation(InjektFqNames.Given) &&
                descriptor.extensionReceiverParameter != null &&
                !descriptor.extensionReceiverParameter!!.type.hasAnnotation(InjektFqNames.Given)
            ) {
                context.trace.report(
                    InjektErrors.GIVEN_DECLARATION_WITH_NON_GIVEN_EXTENSION_RECEIVER
                        .on(declaration)
                )
            }
            descriptor.valueParameters
                .checkParameters(declaration, descriptor, context.trace)
        } else if (descriptor is ClassDescriptor) {
            val givenConstructors = descriptor.constructors
                .filter { it.hasAnnotation(InjektFqNames.Given) }

            if (descriptor.hasAnnotation(InjektFqNames.Given) &&
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
            if (descriptor.hasAnnotation(InjektFqNames.Given) &&
                descriptor.extensionReceiverParameter != null &&
                descriptor.extensionReceiverParameter?.type?.hasAnnotation(InjektFqNames.Given) != true
            ) {
                context.trace.report(
                    InjektErrors.GIVEN_DECLARATION_WITH_NON_GIVEN_EXTENSION_RECEIVER
                        .on(declaration)
                )
            }
        }
    }

    private fun List<ParameterDescriptor>.checkParameters(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        trace: BindingTrace,
    ) {
        if (descriptor.hasAnnotation(InjektFqNames.Given) ||
            declaration.hasAnnotation(InjektFqNames.GivenSetElement)
        ) {
            this
                .filter { !it.hasAnnotation(InjektFqNames.Given) }
                .forEach {
                    trace.report(
                        InjektErrors.NON_GIVEN_VALUE_PARAMETER_ON_GIVEN_DECLARATION
                            .on(
                                it.findPsi() ?: declaration,
                                when {
                                    descriptor.hasAnnotation(InjektFqNames.Given) -> InjektFqNames.Given.shortName()
                                    descriptor.hasAnnotation(InjektFqNames.GivenSetElement) -> InjektFqNames.GivenSetElement.shortName()
                                    else -> error("")
                                }
                            )
                    )
                }
        }
    }
}
