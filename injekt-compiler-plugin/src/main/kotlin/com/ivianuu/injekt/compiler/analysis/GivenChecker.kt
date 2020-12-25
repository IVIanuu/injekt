package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.types.KotlinType

class GivenChecker : DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
    ) {
        checkType(descriptor, declaration, context.trace)

        if (descriptor is SimpleFunctionDescriptor) {
            descriptor.allParameters
                .filterNot { it === descriptor.dispatchReceiverParameter }
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
                    InjektErrors.NON_GIVEN_PARAMETER_ON_GIVEN_DECLARATION
                        .on(
                            declaration,
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
                .any { !it.type.hasAnnotation(InjektFqNames.Given) }) {
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
            declaration.hasAnnotation(InjektFqNames.GivenSetElement)
        ) {
            this
                .filter {
                    !it.hasAnnotation(InjektFqNames.Given) &&
                        !it.type.hasAnnotation(InjektFqNames.Given)
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
