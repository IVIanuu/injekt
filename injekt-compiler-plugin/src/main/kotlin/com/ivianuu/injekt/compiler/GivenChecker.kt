package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class GivenChecker : CallChecker, DeclarationChecker {

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext,
    ) {
        if (resolvedCall.resultingDescriptor.fqNameSafe == InjektFqNames.givenFun &&
            resolvedCall.resultingDescriptor.valueParameters.isEmpty()
        ) {
            val parameter = (resolvedCall.call.callElement.parent as? KtParameter)
                ?.descriptor<ValueParameterDescriptor>(context.trace.bindingContext)
            if (parameter == null) {
                context.trace.report(
                    InjektErrors.GIVEN_CALL_NOT_AS_DEFAULT_VALUE
                        .on(reportOn)
                )
            }
        } else if (resolvedCall.resultingDescriptor.fqNameSafe == InjektFqNames.givenOrElseFun) {
            val parameter = (resolvedCall.call.callElement.parent as? KtParameter)
                ?.descriptor<ValueParameterDescriptor>(context.trace.bindingContext)
            if (parameter == null) {
                context.trace.report(
                    InjektErrors.GIVEN_OR_ELSE_CALL_NOT_AS_DEFAULT_VALUE
                        .on(reportOn)
                )
            }
        }
    }

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext,
    ) {
        if (descriptor is SimpleFunctionDescriptor) {
            if (descriptor.hasAnnotation(InjektFqNames.Given) &&
                descriptor.extensionReceiverParameter != null
            ) {
                context.trace.report(
                    InjektErrors.GIVEN_DECLARATION_WITH_EXTENSION_RECEIVER
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
                descriptor.extensionReceiverParameter?.type?.hasAnnotation(InjektFqNames.Given) == true
            ) {
                context.trace.report(
                    InjektErrors.GIVEN_DECLARATION_WITH_EXTENSION_RECEIVER
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
            declaration.hasAnnotation(InjektFqNames.GivenMap) ||
            descriptor.hasAnnotation(InjektFqNames.GivenSet)
        ) {
            this
                .filter {
                    val defaultValue = (it.findPsi() as? KtParameter)?.defaultValue
                    defaultValue?.text != "given" &&
                            defaultValue?.text?.startsWith("givenOrElse") != true
                }
                .forEach {
                    trace.report(
                        InjektErrors.NON_GIVEN_VALUE_PARAMETER_ON_GIVEN_DECLARATION
                            .on(
                                it.findPsi() ?: declaration,
                                when {
                                    descriptor.hasAnnotation(InjektFqNames.Given) -> InjektFqNames.Given.shortName()
                                    descriptor.hasAnnotation(InjektFqNames.GivenMap) -> InjektFqNames.GivenMap.shortName()
                                    descriptor.hasAnnotation(InjektFqNames.GivenSet) -> InjektFqNames.GivenSet.shortName()
                                    else -> error("")
                                }
                            )
                    )
                }
        }
    }
}
